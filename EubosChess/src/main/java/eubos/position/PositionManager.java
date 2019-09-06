package eubos.position;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;

import eubos.board.Board;
import eubos.board.InvalidPieceException;
import eubos.board.SquareAttackEvaluator;
import eubos.board.pieces.Bishop;
import eubos.board.pieces.King;
import eubos.board.pieces.Knight;
import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Queen;
import eubos.board.pieces.Rook;
import eubos.board.pieces.Piece.Colour;

public class PositionManager implements IChangePosition, IGenerateMoveList, IPositionAccessors {

	private Board theBoard;
	public Board getTheBoard() {
		return theBoard;
	}
	
	private MoveListGenerator mlgen;
	public List<GenericMove> getMoveList() throws InvalidPieceException {
		return mlgen.createMoveList();
	}
	public List<GenericMove> getMoveList(GenericMove prevBest) throws InvalidPieceException {
		List<GenericMove> ml = getMoveList();
		if (ml.contains(prevBest)) {
			ml.remove(prevBest);
			ml.add(0,prevBest);
		}
		return ml;
	}	
	
	CastlingManager castling;
	public static final int WHITE_KINGSIDE = 1<<0;
	public static final int WHITE_QUEENSIDE = 1<<1;
	public static final int BLACK_KINGSIDE = 1<<2;
	public static final int BLACK_QUEENSIDE = 1<<3;
	public int getCastlingAvaillability() {
		int castleMask = 0;
		castleMask |= (castling.isWhiteKsAvail() ? WHITE_KINGSIDE : 0);
		castleMask |= (castling.isWhiteQsAvail() ? WHITE_QUEENSIDE : 0);
		castleMask |= (castling.isBlackKsAvail() ? BLACK_KINGSIDE : 0);
		castleMask |= (castling.isBlackQsAvail() ? BLACK_QUEENSIDE : 0);
		return castleMask;
	}
	
	private MoveTracker moveTracker = new MoveTracker();
	boolean lastMoveWasCaptureOrCastle() {
		return (moveTracker.lastMoveWasCapture() || moveTracker.lastMoveWasCastle());
	}
	
	public boolean lastMoveWasCapture() {
		return moveTracker.lastMoveWasCapture();
	}
	
	Piece getCapturedPiece() {
		return moveTracker.getCapturedPiece();
	}
	
	public boolean hasCastled(Colour colour){
		return castling.everCastled(colour);
	}

	// No public setter, because onMove is only changed by performing a move on the board.
	private Colour onMove;
	public Colour getOnMove() {
		return onMove;
	}
	
	private int moveNumber;
	public int getMoveNumber() {
		return moveNumber;
	}
	private void setMoveNumber(int move) {
		moveNumber = move;
	}

	private King whiteKing;
	private King blackKing;
	King getKing( Colour colour ) {
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
	public boolean isKingInCheck() {
		return isKingInCheck(onMove);
	}
	boolean isKingInCheck( Colour colour ) {
		King ownKing = getKing(colour);
		boolean kingIsInCheck = (ownKing != null) ? squareIsAttacked(ownKing.getSquare(), colour) : false;
		return kingIsInCheck;		
	}
	
	private ZobristHashCode hash;

	public long getHash() {
		return hash.hashCode;
	}
	public PositionManager() {
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
	}
	
	PositionEvaluator pe;
	public IEvaluate getPositionEvaluator() {
		return this.pe;
	}
	
	public PositionManager( Board startingPosition, Piece.Colour colourToMove ) {
		moveTracker = new MoveTracker();
		theBoard = startingPosition;
		castling = new CastlingManager(this);
		mlgen = new MoveListGenerator(this);
		onMove = colourToMove;
		setKing();
		pe = new PositionEvaluator( this );
	}
	
	public PositionManager( String fenString ) {
		moveTracker = new MoveTracker();
		mlgen = new MoveListGenerator(this);
		new fenParser( this, fenString );
		setKing();
		this.hash = new ZobristHashCode(this);
		pe = new PositionEvaluator( this );
	}
	
	public void performMove( GenericMove move ) throws InvalidPieceException {
		// Get the piece to move
		Piece pieceToMove = theBoard.pickUpPieceAtSquare( move.from );
		// Flag if move is an en passant capture
		boolean isEnPassantCapture = isEnPassantCapture(move, pieceToMove);
		// Save previous en passant square and initialise for this move
		GenericPosition prevEnPassantTargetSq = theBoard.getEnPassantTargetSq();
		theBoard.setEnPassantTargetSq(null);
		// Handle pawn promotion moves
		pieceToMove = checkForPawnPromotions(move, pieceToMove);
		// Handle castling secondary rook moves...
		if (pieceToMove instanceof King)
			castling.performSecondaryCastlingMove(move);
		// Handle any initial 2 square pawn moves that are subject to en passant rule
		Boolean isEnPassant = checkToSetEnPassantTargetSq(move, pieceToMove);
		// Handle capture target (note, this will be null if the move is not a capture)
		Piece captureTarget = getCaptureTarget(move, pieceToMove, isEnPassantCapture);
		// Store the necessary information to undo this move on the move tracker stack
		moveTracker.push( new TrackedMove(move, captureTarget, prevEnPassantTargetSq, castling.getFenFlags()));
		// Update the piece's square.
		updateSquarePieceOccupies(move.to, pieceToMove);
		// update castling flags
		castling.updateFlags(pieceToMove, move);
		// Update hash code
		try {
			if (hash != null)
				hash.update(move, captureTarget, isEnPassant);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Update onMove
		onMove = Colour.getOpposite(onMove);
		if (onMove==Colour.white) {
			moveNumber++;
		}
	}
	
	public void unperformMove() throws InvalidPieceException {
		if ( moveTracker.isEmpty())
			return;
		theBoard.setEnPassantTargetSq(null);
		TrackedMove tm = moveTracker.pop();
		GenericMove moveToUndo = tm.getMove();
		// Check for reversal of any pawn promotion that had been previously applied
		checkToUndoPawnPromotion(moveToUndo);
		// Actually undo the move by reversing its direction and reapplying it.
		GenericMove reversedMove = new GenericMove( moveToUndo.to, moveToUndo.from, moveToUndo.promotion );
		// Get the piece to move
		Piece pieceToMove = theBoard.pickUpPieceAtSquare( reversedMove.from );
		// Handle reversal of any castling secondary rook moves and associated flags...
		if (pieceToMove instanceof King) {
			castling.unperformSecondaryCastlingMove(reversedMove);
		}
		updateSquarePieceOccupies(reversedMove.to, pieceToMove);
		castling.setFenFlags(tm.getFenFlags());
		// Undo any capture that had been previously performed.
		if ( tm.isCapture()) {
			theBoard.setPieceAtSquare(tm.getCapturedPiece());
		}
		// Restore en passant target
		GenericPosition enPasTargetSq = tm.getEnPassantTarget();
		theBoard.setEnPassantTargetSq(enPasTargetSq);
		// Update hash code
		try {
			if (hash != null) {
				Piece capturedPiece = tm.isCapture() ? tm.getCapturedPiece() : null;
				Boolean setEnPassant = (enPasTargetSq != null);
				hash.update(reversedMove, capturedPiece, setEnPassant);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Update onMove flag
		onMove = Piece.Colour.getOpposite(onMove);
		if (onMove==Colour.black) {
			moveNumber--;
		}
	}
	
	void updateSquarePieceOccupies(GenericPosition newSq, Piece pieceToMove) {
		pieceToMove.setSquare(newSq);
		theBoard.setPieceAtSquare(pieceToMove);
	}
	
	boolean squareIsAttacked( GenericPosition atPos, Piece.Colour ownColour ) {
		return SquareAttackEvaluator.isAttacked(theBoard, atPos, ownColour);
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
	
	private void checkToUndoPawnPromotion(GenericMove moveToUndo) {
		if ( moveToUndo.promotion != null ) {
			Piece.Colour colourToCreate = theBoard.getPieceAtSquare(moveToUndo.to).getColour();
			theBoard.setPieceAtSquare( new Pawn( colourToCreate, moveToUndo.to ));
		}
	}
	
	private boolean isEnPassantCapture(GenericMove move, Piece pieceToMove) {
		boolean enPassantCapture = false;
		GenericPosition enPassantTargetSq = theBoard.getEnPassantTargetSq();
		if ( enPassantTargetSq != null && pieceToMove instanceof Pawn && move.to == enPassantTargetSq) {
			enPassantCapture = true;
		}
		return enPassantCapture;
	}
	
	private Boolean checkToSetEnPassantTargetSq(GenericMove move, Piece pieceToMove) {
		Boolean isEnPassantMove = false;
		if ( pieceToMove instanceof Pawn ) {
			Pawn pawnPiece = (Pawn) pieceToMove;
			if ( pawnPiece.isAtInitialPosition()) {
				if ( pawnPiece.isWhite()) {
					if (move.to.rank == GenericRank.R4) {
						GenericPosition enPassantWhite = GenericPosition.valueOf(move.to.file,GenericRank.R3);
						theBoard.setEnPassantTargetSq(enPassantWhite);
						isEnPassantMove = true;
					}
				} else {
					if (move.to.rank == GenericRank.R5) {
						GenericPosition enPassantBlack = GenericPosition.valueOf(move.to.file,GenericRank.R6);
						theBoard.setEnPassantTargetSq(enPassantBlack);
						isEnPassantMove = true;
					}						
				}
			}
		}
		return isEnPassantMove;
	}
	
	private Piece getCaptureTarget(GenericMove move, Piece pieceToMove, boolean enPassantCapture) {
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
		return captureTarget;
	}
	
	public String getFen() {
		StringBuilder fen = new StringBuilder(this.theBoard.getAsFenString());
		fen.append(' ');
		fen.append((this.getOnMove()==Colour.white) ? 'w' : 'b');
		fen.append(' ');
		fen.append(this.castling.getFenFlags());
		fen.append(' ');
		// en passant square
		GenericPosition pos = this.theBoard.getEnPassantTargetSq();
		if (pos != null) {
			fen.append(pos.toString());
		} else {
			fen.append('-');
		}
		fen.append(" - " + moveNumber);
		return fen.toString();
	}
	
	private class fenParser {
		private List<Piece> pl;
		
		public fenParser( PositionManager pm, String fenString ) {
			pl = new LinkedList<Piece>();
			String[] tokens = fenString.split(" ");
			String piecePlacement = tokens[0];
			String colourOnMove = tokens[1];
			castling = new CastlingManager(pm, tokens[2]);
			String enPassanttargetSq = tokens[3];
			//String halfMoveClock = tokens[4];
			String moveNumber = tokens[5];
			parseMoveNumber(pm, moveNumber);
			parsePiecePlacement(piecePlacement);
			parseOnMove(colourOnMove);
			create();
			parseEnPassant(enPassanttargetSq);
		}
		private void parseOnMove(String colourOnMove) {
			if (colourOnMove.equals("w"))
				onMove = Colour.white;
			else if (colourOnMove.equals("b"))
				onMove = Colour.black;
		}
		private void parseMoveNumber(PositionManager pm, String moveNumber) {
			int moveNum = 0;
			if (!moveNumber.equals("-")) {
				moveNum = Integer.parseInt(moveNumber);
			}
			pm.setMoveNumber(moveNum);
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
					f = advanceFile(f);
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
				theBoard.setEnPassantTargetSq(GenericPosition.valueOf(targetSq));
			} else {
				theBoard.setEnPassantTargetSq(null);
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
