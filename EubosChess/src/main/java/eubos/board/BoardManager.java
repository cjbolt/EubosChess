package eubos.board;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

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

	public class TrackedMove {
		private GenericMove move = null;
		private Piece capturedPiece = null;

		public TrackedMove( GenericMove inMove ) { move = inMove; }
		public TrackedMove( GenericMove inMove, Piece capture ) {  move = inMove; capturedPiece = capture;}
		public boolean isCapture() { return ((capturedPiece != null) ? true : false); }

		public GenericMove getMove() {
			return move;
		}
		public void setMove(GenericMove move) {
			this.move = move;
		}
		public Piece getCapturedPiece() {
			return capturedPiece;
		}
		public void setCapturedPiece(Piece capturedPiece) {
			this.capturedPiece = capturedPiece;
		}
	}

	public class fenParser {
		LinkedList<Piece> pl;
		
		public fenParser( String fenString ) {
			pl = new LinkedList<Piece>();
			String[] tokens = fenString.split(" ");
			String piecePlacement = tokens[0];
			String colourOnMove = tokens[1];
			String castlingAvaillability = tokens[2];
			String enPassanttargetSq = tokens[3];
			String halfMoveClock = tokens[4];
			String moveNumber = tokens[5];
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
			if (colourOnMove.equals("w"))
				onMove = Colour.white;
			else if (colourOnMove.equals("b"))
				onMove = Colour.black;
			// looks like may need to revisit castling class members...
		}
		private GenericFile advanceFile(GenericFile f) {
			if ( f != GenericFile.Fh )
				f = f.next();
			return f;
		}
		public Board create() {
			return new Board( pl );
		}
	}
	
	private Stack<TrackedMove> previousMoves;
	private Board theBoard;
	public Piece.Colour onMove;
	private King whiteKing;
	private King blackKing;
	private boolean whiteHasCastled;
	private boolean blackHasCastled;
	
	public Board getTheBoard() {
		return theBoard;
	}

	public BoardManager() { 
		previousMoves = new Stack<TrackedMove>();
		theBoard = new Board();
		setKing( (King) theBoard.getPieceAtSquare(GenericPosition.e1));
		setKing( (King) theBoard.getPieceAtSquare(GenericPosition.e8));
		whiteHasCastled = false;
		blackHasCastled = false;
		onMove = Colour.white;
	}
	
	public BoardManager( Board startingPosition, Piece.Colour colourToMove ) {
		previousMoves = new Stack<TrackedMove>();
		theBoard = startingPosition;
		Iterator<Piece> iterAllPieces = theBoard.iterator();
		while (iterAllPieces.hasNext()) {
			Piece currPiece = iterAllPieces.next();
			if ( currPiece instanceof King ) {
				setKing( (King)currPiece );
			}
		}
		whiteHasCastled = false;
		blackHasCastled = false;
		onMove = colourToMove;
	}
	
	public BoardManager( String fenString ) {
		previousMoves = new Stack<TrackedMove>();
		fenParser fp = new fenParser( fenString );
		theBoard = fp.create();
		Iterator<Piece> iterAllPieces = theBoard.iterator();
		while (iterAllPieces.hasNext()) {
			Piece currPiece = iterAllPieces.next();
			if ( currPiece instanceof King ) {
				setKing( (King)currPiece );
			}
		}
		whiteHasCastled = false;
		blackHasCastled = false;
	}
	
	public King getKing( Piece.Colour colour ) {
		return ((colour == Piece.Colour.white) ? whiteKing : blackKing);
	}
	
	public void setKing(King king) {
		if (king.isWhite())
			whiteKing = king;
		else 
			blackKing = king;
	}
	
	public boolean isWhiteHasCastled() {
		return whiteHasCastled;
	}

	public void setWhiteHasCastled(boolean whiteHasCastled) {
		this.whiteHasCastled = whiteHasCastled;
	}

	public boolean isBlackHasCastled() {
		return blackHasCastled;
	}

	public void setBlackHasCastled(boolean blackHasCastled) {
		this.blackHasCastled = blackHasCastled;
	}	
	
	public boolean hasCastled() {
		if ( onMove == Colour.white ) {
			return isWhiteHasCastled();
		} else {
			return isBlackHasCastled();
		}
	}
	
	private static final GenericPosition [] kscWhiteCheckSqs = {GenericPosition.e1, GenericPosition.f1, GenericPosition.g1};
	private static final GenericPosition [] kscBlackCheckSqs = {GenericPosition.e8, GenericPosition.f8, GenericPosition.g8};
	private static final GenericPosition [] kscWhiteEmptySqs = {GenericPosition.f1, GenericPosition.g1};
	private static final GenericPosition [] kscBlackEmptySqs = {GenericPosition.f8, GenericPosition.g8};
	
	public GenericMove addKingSideCastle() {
		if ( onMove == Colour.white ) {
			if ( canCastle(GenericPosition.h1, kscWhiteCheckSqs, kscWhiteEmptySqs))
				return new GenericMove(GenericPosition.e1,GenericPosition.g1);
		} else {
			if ( canCastle(GenericPosition.h8, kscBlackCheckSqs, kscBlackEmptySqs))
				return new GenericMove(GenericPosition.e8,GenericPosition.g8);	
		}
		return null;
	}
	
	private static final GenericPosition [] qscWhiteCheckSqs = {GenericPosition.c1, GenericPosition.d1, GenericPosition.e1};
	private static final GenericPosition [] qscBlackCheckSqs = {GenericPosition.c8, GenericPosition.d8, GenericPosition.e8};
	private static final GenericPosition [] qscWhiteEmptySqs = {GenericPosition.c1, GenericPosition.d1, GenericPosition.b1};
	private static final GenericPosition [] qscBlackEmptySqs = {GenericPosition.c8, GenericPosition.d8, GenericPosition.b8};
	
	public GenericMove addQueenSideCastle() {
		if ( onMove == Colour.white ) {
			if ( canCastle(GenericPosition.a1, qscWhiteCheckSqs, qscWhiteEmptySqs))
				return new GenericMove(GenericPosition.e1,GenericPosition.c1);
		} else {
			if ( canCastle(GenericPosition.a8, qscBlackCheckSqs, qscBlackEmptySqs))
				return new GenericMove(GenericPosition.e8,GenericPosition.c8);	
		}
		return null;
	}

	private boolean canCastle(GenericPosition rookSq,
			GenericPosition [] checkSqs,
			GenericPosition [] emptySqs) {
		// Target rook should not have moved and be on it initial square
		Piece qscTarget = theBoard.getPieceAtSquare( rookSq );
		if ( !(qscTarget instanceof Rook) || qscTarget.hasEverMoved())
			return false;
		// All the intervening squares between King and Rook should be empty
		for ( GenericPosition emptySq : emptySqs ) {
			if ( !theBoard.squareIsEmpty(emptySq))
				return false;
		}
		Iterator<Piece> iterPotentialAttackers = theBoard.iterateColour(Piece.Colour.getOpposite(onMove));
		while (iterPotentialAttackers.hasNext()) {
			// None of the intervening squares between King and Rook should be attacked
			// the king cannot be in check at the start or end of the move
			Piece currPiece = iterPotentialAttackers.next();
			if (currPiece.attacks( this, checkSqs)) {
				return false;
			}
		}
		return true;
	}
	
	public void undoPreviousMove() {
		if ( !previousMoves.isEmpty()) {
			TrackedMove tm = previousMoves.pop();
			GenericMove moveToUndo = tm.getMove();
			if ( moveToUndo.promotion != null ) {
				Piece.Colour colourToCreate = theBoard.getPieceAtSquare(moveToUndo.to).getColour();
				theBoard.setPieceAtSquare( new Pawn( colourToCreate, moveToUndo.to ));
			}
			unperformMove( new GenericMove( moveToUndo.to, moveToUndo.from ) );
			if ( tm.isCapture()) {
				theBoard.setPieceAtSquare(tm.getCapturedPiece());
			}
			// Update onMove
			onMove = Piece.Colour.getOpposite(onMove);
		}
	}
	
	public GenericMove getPreviousMove() {
		GenericMove lastMove = null;
		if ( !previousMoves.isEmpty()) {
			TrackedMove tm = previousMoves.peek();
			lastMove = tm.getMove();
		}
		return lastMove;
	}
	
	public void performMove( GenericMove move ) {
		// Move the piece
		Piece pieceToMove = theBoard.pickUpPieceAtSquare( move.from );
		if ( pieceToMove != null ) {
			// Handle pawn promotion moves
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
			// Store this move in the previous moves list
			Piece captureTarget = theBoard.getPieceAtSquare( move.to );
			previousMoves.push( new TrackedMove( move, captureTarget ));
			// Update the piece's square.
			// TODO duplicated information here - sub optimal...
			pieceToMove.setSquare( move.to );
			theBoard.setPieceAtSquare( pieceToMove );
			// Update onMove
			onMove = Piece.Colour.getOpposite(onMove);
		} else {
			// TODO throw an exception in this case?
		}
	}
	
	public void unperformMove( GenericMove move ) {
		// Move the piece
		Piece pieceToMove = theBoard.pickUpPieceAtSquare( move.from );
		if ( pieceToMove != null ) {
			pieceToMove.setSquare( move.to );
			theBoard.setPieceAtSquare( pieceToMove );
		} else {
			// TODO throw an exception in this case?
		}
	}
}
