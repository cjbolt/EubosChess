package eubos.position;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IntChessman;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.IntRank;

import eubos.board.Board;
import eubos.board.InvalidPieceException;
import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.score.IEvaluate;
import eubos.score.PositionEvaluator;
import eubos.search.DrawChecker;

public class PositionManager implements IChangePosition, IPositionAccessors {
	
	public PositionManager( String fenString, DrawChecker dc) {
		moveTracker = new MoveTracker();
		new fenParser( this, fenString );
		hash = new ZobristHashCode(this);
		pe = new PositionEvaluator(this);
		this.dc = dc; 
	}
	
	public PositionManager( String fenString) {
		this(fenString, new DrawChecker());
	}
	
	public PositionManager() {
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", new DrawChecker());
	}

	private Board theBoard;
	public Board getTheBoard() {
		return theBoard;
	}
	
	public int[] generateMoves() {
		List<Integer> entireMoveList = theBoard.getRegularPieceMoves( onMove );
		castling.addCastlingMoves(entireMoveList);
		return entireMoveList.stream().mapToInt(i->i).toArray();
	}
	
	public String toString() {
		return this.theBoard.getAsFenString();
	}
	
	CastlingManager castling;
	public static final int WHITE_KINGSIDE = 1<<0;
	public static final int WHITE_QUEENSIDE = 1<<1;
	public static final int BLACK_KINGSIDE = 1<<2;
	public static final int BLACK_QUEENSIDE = 1<<3;
	public int getCastlingFlags() {
		return castling.getFlags();
	}
	public void setCastlingFlags(int castlingFlags) {
		castling.setFlags(castlingFlags);
	}
	
	private MoveTracker moveTracker = new MoveTracker();
	boolean lastMoveWasCastle() {
		return moveTracker.lastMoveWasCastle();
	}
	
	public boolean lastMoveWasCapture() {
		return moveTracker.lastMoveWasCapture();
	}
	
	public boolean lastMoveWasPromotion() {
		return moveTracker.lastMoveWasPromotion();
	}
	
	public boolean lastMoveWasCheck() {
		return moveTracker.lastMoveWasCheck();
	}
	
	public CaptureData getCapturedPiece() {
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
	public boolean onMoveIsWhite() {
		return Colour.isWhite(onMove);
	}
	
	private int moveNumber;
	public int getMoveNumber() {
		return moveNumber;
	}
	private void setMoveNumber(int move) {
		moveNumber = move;
	}

	public boolean isKingInCheck() {
		return isKingInCheck(onMove);
	}
	public boolean isKingInCheck( Colour colour ) {
		return theBoard.isKingInCheck(colour);		
	}
	
	private ZobristHashCode hash;

	public long getHash() {
		return hash.hashCode;
	}
	
	PositionEvaluator pe;
	public IEvaluate getPositionEvaluator() {
		return this.pe;
	}
	
	boolean repetitionPossible = false;
	public boolean isThreefoldRepetitionPossible() {
		return repetitionPossible;
	}
	
	DrawChecker dc;
	
	public void performMove( int move ) throws InvalidPieceException {
		performMove(move, true);
	}
	
	public void performMove( int move, boolean computeHash ) throws InvalidPieceException {
		
		// Save previous en passant square and initialise for this move
		int prevEnPassantTargetSq = theBoard.getEnPassantTargetSq();
		
		// Handle pawn promotion moves - remains in position manager because it updates the move
		move = checkForPawnPromotions(move);
		CaptureData captureTarget = theBoard.doMove(move);
		moveTracker.push( new TrackedMove(move, captureTarget, prevEnPassantTargetSq, getCastlingFlags()));
		
		// update castling flags
		castling.updateFlags(Move.getOriginPiece(move), move);
		
		if (computeHash) {
			// Update hash code
			if (hash != null) {
				int enPasTargetSq = theBoard.getEnPassantTargetSq();
				Boolean setEnPassant = (enPasTargetSq != Position.NOPOSITION);
				hash.update(move, captureTarget, setEnPassant ? Position.getFile(enPasTargetSq) : IntFile.NOFILE);
			}
			// Update the draw checker
			repetitionPossible = dc.incrementPositionReachedCount(getHash());
		}
		
		// Update onMove
		onMove = Colour.getOpposite(onMove);
		if (Colour.isWhite(onMove)) {
			moveNumber++;
		}
	}
		
	public void unperformMove() throws InvalidPieceException {
		unperformMove(true);
	}
	
	public void unperformMove(boolean computeHash) throws InvalidPieceException {
		TrackedMove tm = moveTracker.pop();
		CaptureData cap = tm.getCaptureData();
		int moveToUndo = tm.getMove();
		
		// Check for reversal of any pawn promotion that had been previously applied
		moveToUndo = checkToUndoPawnPromotion(moveToUndo);
		
		// Actually undo the move.
		int reversedMove = Move.reverse(moveToUndo);
		theBoard.undoMove(reversedMove, cap);
		
		// Restore castling
		castling.setFlags(tm.getCastlingFlags());
		// Restore en passant target
		int enPasTargetSq = tm.getEnPassantTarget();
		theBoard.setEnPassantTargetSq(enPasTargetSq);
		
		if (computeHash) {
			dc.decrementPositionReachedCount(getHash());

			int enPassantFile = (enPasTargetSq != Position.NOPOSITION) ? Position.getFile(enPasTargetSq) : IntFile.NOFILE;
			hash.update(reversedMove, cap, enPassantFile);
			
			repetitionPossible = dc.isPositionOpponentCouldClaimDraw(getHash());
		}
		
		// Update onMove flag
		onMove = Piece.Colour.getOpposite(onMove);
		if (Colour.isBlack(onMove)) {
			moveNumber--;
		}
	}
	
	private int checkForPawnPromotions(int move) {
		if ( Move.getPromotion(move) != IntChessman.NOCHESSMAN ) {
			int piece = Move.getOriginPiece(move);
			piece &= Piece.BLACK; // preserve colour
			switch( Move.getPromotion(move) ) {
			case IntChessman.QUEEN:
				piece |= Piece.QUEEN;
				break;
			case IntChessman.KNIGHT:
				piece |= Piece.KNIGHT;
				break;
			case IntChessman.BISHOP:
				piece |= Piece.BISHOP;
				break;
			case IntChessman.ROOK:
				piece |= Piece.ROOK;
				break;
			default:
				assert false;
				break;
			}
			move = Move.setOriginPiece(move, piece);
		}
		return move;
	}
	
	private int checkToUndoPawnPromotion(int moveToUndo) {
		if ( Move.getPromotion(moveToUndo) != IntChessman.NOCHESSMAN ) {
			int type = theBoard.pickUpPieceAtSquare(Move.getTargetPosition(moveToUndo));
			if (Piece.isBlack(type)) {
				type = Piece.BLACK_PAWN;
			} else {
				type = Piece.WHITE_PAWN;
			}
			theBoard.setPieceAtSquare(Move.getTargetPosition(moveToUndo), type);
			moveToUndo = Move.setOriginPiece(moveToUndo, type);
		}
		return moveToUndo;
	}
		
	public String getFen() {
		StringBuilder fen = new StringBuilder(theBoard.getAsFenString());
		fen.append(' ');
		fen.append(Colour.isWhite(getOnMove()) ? 'w' : 'b');
		fen.append(' ');
		fen.append(castling.getFenFlags());
		fen.append(' ');
		// en passant square
		GenericPosition pos = (theBoard.getEnPassantTargetSq() == Position.NOPOSITION) ? null : Position.toGenericPosition(theBoard.getEnPassantTargetSq());
		if (pos != null) {
			fen.append(pos.toString());
		} else {
			fen.append('-');
		}
		fen.append(" - " + moveNumber);
		return fen.toString();
	}
	
	private class fenParser {
		private Map<Integer, Integer> pl;
		
		public fenParser( PositionManager pm, String fenString ) {
			pl = new HashMap<Integer, Integer>();
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
			int r = IntRank.R8;
			int f = IntFile.Fa;
			for ( char c: piecePlacement.toCharArray() ){
				switch(c)
				{
				case 'r':
					pl.put(Position.valueOf(f,r), Piece.BLACK_ROOK);
					f = advanceFile(f);
					break;
				case 'R':
					pl.put(Position.valueOf(f,r), Piece.WHITE_ROOK);
					f = advanceFile(f);
					break;
				case 'n':
					pl.put(Position.valueOf(f,r), Piece.BLACK_KNIGHT);
					f = advanceFile(f);
					break;
				case 'N':
					pl.put(Position.valueOf(f,r), Piece.WHITE_KNIGHT);
					f = advanceFile(f);
					break;
				case 'b':
					pl.put(Position.valueOf(f,r), Piece.BLACK_BISHOP);
					f = advanceFile(f);
					break;
				case 'B':
					pl.put(Position.valueOf(f,r), Piece.WHITE_BISHOP);
					f = advanceFile(f);
					break;
				case 'q':
					pl.put(Position.valueOf(f,r), Piece.BLACK_QUEEN);
					f = advanceFile(f);
					break;
				case 'Q':
					pl.put(Position.valueOf(f,r), Piece.WHITE_QUEEN);
					f = advanceFile(f);
					break;
				case 'k':
					pl.put(Position.valueOf(f,r), Piece.BLACK_KING);
					f = advanceFile(f);
					break;
				case 'K':
					pl.put(Position.valueOf(f,r), Piece.WHITE_KING);
					f = advanceFile(f);
					break;
				case 'p':
					pl.put(Position.valueOf(f,r), Piece.BLACK_PAWN);
					f = advanceFile(f);
					break;
				case 'P':
					pl.put(Position.valueOf(f,r), Piece.WHITE_PAWN);
					f = advanceFile(f);
					break;
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
					int loop =  Integer.valueOf(c-'0');
					for ( int i=0; i<loop; i++ ) {
						f = advanceFile(f);
					}
				case '8':
					break;
				case '/':
					r -= 1;
					f = IntFile.Fa;
					break;
				}
			}
		}
		private void parseEnPassant(String targetSq) {
			if (!targetSq.contentEquals("-")) {
				theBoard.setEnPassantTargetSq(Position.valueOf(GenericPosition.valueOf(targetSq)));
			} else {
				theBoard.setEnPassantTargetSq(Position.NOPOSITION);
			}
		}
		private int advanceFile(int f) {
			if ( f != IntFile.Fh )
				f += 1;
			return f;
		}
		private void create() {
			theBoard =  new Board( pl );
		}
	}

	public boolean isPromotionPossible() {
		return theBoard.isPromotionPossible(onMove);
	}

	public boolean noLastMove() {
		return moveTracker.isEmpty();
	}
}
