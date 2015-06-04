package eubos.board;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;

import eubos.pieces.Bishop;
import eubos.pieces.Knight;
import eubos.pieces.Pawn;
import eubos.pieces.Piece;
import eubos.pieces.Piece.Colour;
import eubos.pieces.Queen;
import eubos.pieces.Rook;
import eubos.pieces.King;

public class BoardManager implements IBoardManager {

	private Board theBoard;
	public Board getTheBoard() {
		return theBoard;
	}
	
	private CastlingManager castling;
	CastlingManager getCastlingManager() {
		return castling;
	}
	public void addCastlingMoves(List<GenericMove> ml) {
		castling.addCastlingMoves(ml);
	}

	private EnPassantManager enPassant = new EnPassantManager( null );
	public GenericPosition getEnPassantTargetSq() {
		return enPassant.getEnPassantTargetSq();
	}
	
	private MoveTracker moveTracker = new MoveTracker();
	public boolean lastMoveWasCapture() {
		return moveTracker.lastMoveWasCapture();
	}

	// No public setter, because onMove is only changed by performing a move on the board.
	private Colour onMove;
	public Colour getOnMove() {
		return onMove;
	}

	private King whiteKing;
	private King blackKing;
	public King getKing( Colour colour ) {
		return ((colour == Colour.white) ? whiteKing : blackKing);
	}
	private void setKing() {
		King king = null;
		Iterator<Piece> iterAllPieces = theBoard.iterator();
		while (iterAllPieces.hasNext()) {
			Piece currPiece = iterAllPieces.next();
			if ( currPiece instanceof King ) {
				king = (King)currPiece;
				if (king.isWhite())
					whiteKing = king;
				else 
					blackKing = king;
			}
		}
	}

	public BoardManager() {
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
	}
	
	public BoardManager( Board startingPosition, Piece.Colour colourToMove ) {
		moveTracker = new MoveTracker();
		theBoard = startingPosition;
		castling = new CastlingManager(this);
		onMove = colourToMove;
		setKing();
	}
	
	public BoardManager( String fenString ) {
		moveTracker = new MoveTracker();
		new fenParser( this, fenString );
		setKing();
	}
	
	public void performMove( GenericMove move ) throws InvalidPieceException {
		// Move the piece
		Piece pieceToMove = theBoard.pickUpPieceAtSquare( move.from );
		if ( pieceToMove != null ) {
			// Flag if move is an en passant capture
			boolean enPassantCapture = enPassant.isEnPassantCapture(move, pieceToMove);
			// Save previous en passant square and initialise for this move
			GenericPosition prevEnPassantTargetSq = enPassant.getEnPassantTargetSq();
			enPassant.setEnPassantTargetSq(null);
			// Handle pawn promotion moves
			pieceToMove = checkForPawnPromotions(move, pieceToMove);
			// Handle castling secondary rook moves...
			if (pieceToMove instanceof King)
				castling.performSecondaryCastlingMove(move);
			// Handle any initial 2 square pawn moves that are subject to en passant rule
			enPassant.checkToSetEnPassantTargetSq(move, pieceToMove);
			// Apply this move to the Move Tracker
			Piece captureTarget = null;
			if (enPassantCapture) {
				GenericRank rank;
				if (pieceToMove.isWhite()) {
					rank = GenericRank.R5;
				} else {
					rank = GenericRank.R4;
				}
				GenericPosition capturePos = GenericPosition.valueOf(move.to.file,rank);
				captureTarget = theBoard.captureAtSquare(capturePos);
			} else {
				captureTarget = theBoard.captureAtSquare(move.to);
			}
			moveTracker.push( new TrackedMove(move, captureTarget, prevEnPassantTargetSq));
			// Update the piece's square.
			updateSquarePieceOccupies(move.to, pieceToMove);
			// Update onMove
			onMove = Colour.getOpposite(onMove);
		} else {
			throw new InvalidPieceException(move.from);
		}
	}

	public void unperformMove() throws InvalidPieceException {
		if ( !moveTracker.isEmpty()) {
			enPassant.setEnPassantTargetSq(null);
			TrackedMove tm = moveTracker.pop();
			GenericMove moveToUndo = tm.getMove();
			// Handle reversal of any pawn promotion that had been previously applied
			if ( moveToUndo.promotion != null ) {
				Piece.Colour colourToCreate = theBoard.getPieceAtSquare(moveToUndo.to).getColour();
				theBoard.setPieceAtSquare( new Pawn( colourToCreate, moveToUndo.to ));
			}
			// Actually undo the move by reversing its direction and reapplying it.
			GenericMove reversedMove = new GenericMove( moveToUndo.to, moveToUndo.from );
			Piece pieceToMove = theBoard.pickUpPieceAtSquare( reversedMove.from );
			if ( pieceToMove != null ) {
				// Handle reversal of any castling secondary rook moves...
				if (pieceToMove instanceof King)
					castling.unperformSecondaryCastlingMove(reversedMove);
				updateSquarePieceOccupies(reversedMove.to, pieceToMove);
			} else {
				throw new InvalidPieceException(reversedMove.from);
			}
			// Undo any capture that had been previously performed.
			if ( tm.isCapture()) {
				theBoard.setPieceAtSquare(tm.getCapturedPiece());
			}
			enPassant.setEnPassantTargetSq(tm.getEnPassantTarget());
			// Update onMove flag
			onMove = Piece.Colour.getOpposite(onMove);
		}
	}

	void updateSquarePieceOccupies(GenericPosition newSq, Piece pieceToMove) {
		pieceToMove.setSquare(newSq);
		theBoard.setPieceAtSquare(pieceToMove);
	}
	
	public boolean squareIsAttacked( GenericPosition atPos, Piece.Colour ownColour ) {
		Piece.Colour attackingColour = Piece.Colour.getOpposite(ownColour);
		boolean attacked = false;
		// do/while loop is to allow the function to return attacked=true at earliest possibility
		do {
			// Check for pawn attacks
			attacked = attackedByPawn(Direction.getDirectMoveSq(Direction.upRight,atPos),attackingColour);
			if (attacked) break;
			attacked = attackedByPawn(Direction.getDirectMoveSq(Direction.upLeft,atPos),attackingColour);
			if (attacked) break;
			attacked = attackedByPawn(Direction.getDirectMoveSq(Direction.downRight,atPos),attackingColour);
			if (attacked) break;
			attacked = attackedByPawn(Direction.getDirectMoveSq(Direction.downLeft,atPos),attackingColour);
			if (attacked) break;
			// Check for king presence (to avoid moving into check by the enemy king)
			attacked = checkForKingPresence(atPos,attackingColour);
			if (attacked) break;
			// Check for knight attacks
			attacked = checkForKnightAttacks(atPos,attackingColour);
			if (attacked) break;
			// check for diagonal attacks
			attacked = checkForAttackerOnDiagonal(getAllSqs(Direction.downLeft, atPos),attackingColour);
			if (attacked) break;
			attacked = checkForAttackerOnDiagonal(getAllSqs(Direction.upLeft, atPos),attackingColour);
			if (attacked) break;
			attacked = checkForAttackerOnDiagonal(getAllSqs(Direction.downRight, atPos),attackingColour);
			if (attacked) break;
			attacked = checkForAttackerOnDiagonal(getAllSqs(Direction.upRight, atPos),attackingColour);
			if (attacked) break;
			// check for rank or file attacks
			attacked = checkForAttackerOnRankFile(getAllSqs(Direction.down, atPos),attackingColour);
			if (attacked) break;
			attacked = checkForAttackerOnRankFile(getAllSqs(Direction.up, atPos),attackingColour);
			if (attacked) break;
			attacked = checkForAttackerOnRankFile(getAllSqs(Direction.left, atPos),attackingColour);
			if (attacked) break;
			attacked = checkForAttackerOnRankFile(getAllSqs(Direction.right, atPos),attackingColour);
		} while (false);
		return attacked;	
	}

	private boolean checkForKnightAttacks(GenericPosition onSquare, Piece.Colour attackingColour) {
		boolean attacked = false;
		GenericPosition atPos;
		Piece currPiece;
		for (Direction dir: Direction.values()) {
			atPos = Direction.getIndirectMoveSq(dir, onSquare);
			if (atPos != null) {
				currPiece = theBoard.getPieceAtSquare(atPos);
				if ( currPiece != null && currPiece instanceof Knight && currPiece.getColour()==attackingColour) {
					attacked = true;
					break;
				}
			}
		}
		return attacked;
	}

	private boolean checkForKingPresence(GenericPosition onSquare, Piece.Colour attackingColour) {
		boolean attacked = false;
		GenericPosition atPos;
		Piece currPiece;
		for (Direction dir: Direction.values()) {
			atPos = Direction.getDirectMoveSq(dir, onSquare);
			if (atPos != null) {
				currPiece = theBoard.getPieceAtSquare(atPos);
				if ( currPiece != null && currPiece instanceof King && currPiece.getColour()==attackingColour) {
					attacked = true;
					break;
				}
			}
		}
		return attacked;
	}	

	private List<GenericPosition> getAllSqs(Direction dir, GenericPosition currTargetSq) {
		ArrayList<GenericPosition> targetSquares = new ArrayList<GenericPosition>();
		while ((currTargetSq = Direction.getDirectMoveSq(dir, currTargetSq)) != null) {
			targetSquares.add(currTargetSq);
		}
		return targetSquares;
	}

	private boolean checkForAttackerOnDiagonal(List<GenericPosition> targetSqs, Piece.Colour attackingColour) {
		boolean attacked = false;
		for (GenericPosition attackerSq: targetSqs) {
			Piece currPiece = theBoard.getPieceAtSquare(attackerSq);
			if (currPiece != null ) {
				if (((currPiece instanceof Bishop) || (currPiece instanceof Queen)) && currPiece.getColour()==attackingColour) {
					// Indicates attacked
					attacked = true;
				} // else blocked by own piece or non-attacking enemy
				break;
			}
		}
		return attacked;
	}

	private boolean checkForAttackerOnRankFile(List<GenericPosition> targetSqs, Piece.Colour attackingColour) {
		boolean attacked = false;
		for (GenericPosition attackerSq: targetSqs) {
			Piece currPiece = theBoard.getPieceAtSquare(attackerSq);
			if (currPiece != null ) {
				if (((currPiece instanceof Rook) || (currPiece instanceof Queen)) && currPiece.getColour()==attackingColour) {
					// Indicates attacked
					attacked = true;
				} // else blocked by own piece or non-attacking enemy
				break;
			}
		}
		return attacked;
	}	

	private boolean attackedByPawn(GenericPosition atPos, Piece.Colour attackingColour) {
		Piece currPiece;
		boolean attacked = false;
		if (atPos != null) {
			currPiece = theBoard.getPieceAtSquare(atPos);
			if ( currPiece != null && currPiece instanceof Pawn && currPiece.getColour()==attackingColour) {
				attacked = true;
			}
		}
		return attacked;
	}

	private Piece checkForPawnPromotions(GenericMove move, Piece pieceToMove) {
		if ( move.promotion != null ) {
			switch( move.promotion ) {
			case QUEEN:
				pieceToMove = new Queen(pieceToMove.getColour(), null );
				break;
			case KNIGHT:
				pieceToMove = new Knight(pieceToMove.getColour(), null );
				break;
			case BISHOP:
				pieceToMove = new Bishop(pieceToMove.getColour(), null );
				break;
			case ROOK:
				pieceToMove = new Rook(pieceToMove.getColour(), null );
				break;
			default:
				break;
			}
		}
		return pieceToMove;
	}
	
	private class fenParser {
		private LinkedList<Piece> pl;
		
		public fenParser( BoardManager bm, String fenString ) {
			pl = new LinkedList<Piece>();
			String[] tokens = fenString.split(" ");
			String piecePlacement = tokens[0];
			String colourOnMove = tokens[1];
			castling = new CastlingManager(bm, tokens[2]);
			String enPassanttargetSq = tokens[3];
//			String halfMoveClock = tokens[4];
//			String moveNumber = tokens[5];
			parsePiecePlacement(piecePlacement);
			parseOnMove(colourOnMove);
			parseEnPassant(enPassanttargetSq);
			create();
		}
		private void parseOnMove(String colourOnMove) {
			if (colourOnMove.equals("w"))
				onMove = Colour.white;
			else if (colourOnMove.equals("b"))
				onMove = Colour.black;
		}
		private void parsePiecePlacement(String piecePlacement) {
			GenericRank r = GenericRank.R8;
			GenericFile f = GenericFile.Fa;
			for ( char c: piecePlacement.toCharArray() ){
				switch(c)
				{
				case 'r':
					pl.add(new Rook( Colour.black, GenericPosition.valueOf(f,r)));
					f = advanceFile(f);
					break;
				case 'R':
					pl.add(new Rook( Colour.white, GenericPosition.valueOf(f,r)));
					f = advanceFile(f);
					break;
				case 'n':
					pl.add(new Knight( Colour.black, GenericPosition.valueOf(f,r)));
					f = advanceFile(f);
					break;
				case 'N':
					pl.add(new Knight( Colour.white, GenericPosition.valueOf(f,r)));
					f = advanceFile(f);
					break;
				case 'b':
					pl.add(new Bishop( Colour.black, GenericPosition.valueOf(f,r)));
					f = advanceFile(f);
					break;
				case 'B':
					pl.add(new Bishop( Colour.white, GenericPosition.valueOf(f,r)));
					f = advanceFile(f);
					break;
				case 'q':
					pl.add(new Queen( Colour.black, GenericPosition.valueOf(f,r)));
					f = f.next();
					break;
				case 'Q':
					pl.add(new Queen( Colour.white, GenericPosition.valueOf(f,r)));
					f = advanceFile(f);
					break;
				case 'k':
					pl.add(new King( Colour.black, GenericPosition.valueOf(f,r)));
					f = advanceFile(f);
					break;
				case 'K':
					pl.add(new King( Colour.white, GenericPosition.valueOf(f,r)));
					f = advanceFile(f);
					break;
				case 'p':
					pl.add(new Pawn( Colour.black, GenericPosition.valueOf(f,r)));
					f = advanceFile(f);
					break;
				case 'P':
					pl.add(new Pawn( Colour.white, GenericPosition.valueOf(f,r)));
					f = advanceFile(f);
					break;
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
					int loop = new Integer(c-'0');
					for ( int i=0; i<loop; i++ ) {
						f = advanceFile(f);
					}
				case '8':
					break;
				case '/':
					r = r.prev();
					f = GenericFile.Fa;
					break;
				}
			}
		}
		private void parseEnPassant(String targetSq) {
			if (!targetSq.contentEquals("-")) {
				enPassant = new EnPassantManager(GenericPosition.valueOf(targetSq));
			} else {
				enPassant = new EnPassantManager(null);
			}
		}
		private GenericFile advanceFile(GenericFile f) {
			if ( f != GenericFile.Fh )
				f = f.next();
			return f;
		}
		private void create() {
			theBoard =  new Board( pl );
		}
	}
}
