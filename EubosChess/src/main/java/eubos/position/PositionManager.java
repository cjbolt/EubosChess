package eubos.position;

import java.util.HashMap;
import java.util.Map;

import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.IntRank;

import eubos.board.BitBoard;
import eubos.board.Board;
import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.score.IEvaluate;
import eubos.score.PawnEvalHashTable;
import eubos.score.PositionEvaluator;
import eubos.search.DrawChecker;

public class PositionManager implements IChangePosition, IPositionAccessors {
	
	public PositionManager( String fenString, DrawChecker dc, PawnEvalHashTable pawnHash) {
		moveTracker = new MoveTracker();
		new fenParser( this, fenString );
		hash = new ZobristHashCode(this, castling);
		this.dc = dc;
		pe = new PositionEvaluator(this, pawnHash);
	}
	
	public PositionManager(String fenString) {
		this(fenString, new DrawChecker(), new PawnEvalHashTable());
	}
	
	public PositionManager() {
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", new DrawChecker(), new PawnEvalHashTable());
	}

	public CastlingManager castling;
	private Board theBoard;
	public Board getTheBoard() {
		return theBoard;
	}
	public CastlingManager getCastling() {
		return castling;
	}
		
	public String toString() {
		return this.theBoard.getAsFenString();
	}
	
	private MoveTracker moveTracker = new MoveTracker();
	
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
	
	public int getPlyNumber() {
		// Index checker from 0
		int plyNumber = (moveNumber-1) * 2;
		plyNumber += (!onMoveIsWhite()) ? 1 : 0;
		return plyNumber;
	}

	public boolean isKingInCheck() {
		return theBoard.isKingInCheck(onMoveIsWhite());
	}
	
	private ZobristHashCode hash;

	public long getHash() {
		return hash.hashCode;
	}
	
	boolean repetitionPossible = false;
	public boolean isThreefoldRepetitionPossible() {
		return repetitionPossible;
	}
	
	DrawChecker dc;
	
	public boolean moveLeadsToThreefold(int move) {
		boolean isDrawing = false;
		int captureBitOffset = BitBoard.INVALID;
		int pieceToMove = Move.getOriginPiece(move);
		int targetBitOffset = Move.getTargetPosition(move);
		int targetPiece = Move.getTargetPiece(move);
		byte enPassantOffset = BitBoard.INVALID;
		
		// Calculate targetSquare and en passant file, needed for hash code update
		if (targetPiece != Piece.NONE) {
			// Handle captures
			if (Move.isEnPassantCapture(move)) {
				captureBitOffset = theBoard.generateCaptureBitOffsetForEnPassant(pieceToMove, targetBitOffset);
				enPassantOffset = BitBoard.getFile(captureBitOffset);
			} else {
				captureBitOffset = targetBitOffset;
			}
		}	
		
		// Generate hash code
		hash.update(move, captureBitOffset, enPassantOffset);

		// Update the draw checker - do we need to change ply number?
		isDrawing = dc.setPositionReached(getHash(), getPlyNumber());
		
		// Undo the change
		int reversedMove = Move.reverse(move);
		hash.update(reversedMove, captureBitOffset, enPassantOffset);
		
		return isDrawing;
	}
	
	public void performMove( int move )  {
		performMove(move, true);
	}
	
	public void performMove( int move, boolean computeHash ) {
		boolean kingInCheckBeforeMove = false;
		boolean initialOnMoveIsWhite = onMoveIsWhite();
		if (EubosEngineMain.ENABLE_ASSERTS) {
			if ((initialOnMoveIsWhite ? theBoard.getWhiteKing():theBoard.getBlackKing()) != 0L) {
				kingInCheckBeforeMove = theBoard.isKingInCheck(initialOnMoveIsWhite);
			}
		}
		
		// Preserve state
		int prevEnPassantTargetSq = theBoard.getEnPassantTargetSq();
		long pp = theBoard.getPassedPawns();
		int captureBitOffset = theBoard.doMove(move);
		moveTracker.push(pp, move, castling.getFlags(), prevEnPassantTargetSq);
		
		// update state
		castling.updateFlags(move);
		
		if (computeHash) {
			hash.update(move, captureBitOffset, (byte)theBoard.getEnPassantTargetSq());

			// Update the draw checker
			repetitionPossible = dc.setPositionReached(getHash(), getPlyNumber());			
		}
		
		// Update onMove
		onMove = Colour.getOpposite(onMove);
		if (Colour.isWhite(onMove)) {
			moveNumber++;
		}
		if (EubosEngineMain.ENABLE_ASSERTS) {
			if (kingInCheckBeforeMove && computeHash) {
				// need to have moved out of check!!!
				assert !theBoard.isKingInCheck(initialOnMoveIsWhite) :
					String.format("%s %s", this.unwindMoveStack(), getFen());
			}
		}
	}

	public void unperformMove() {
		unperformMove(true);
	}
	
	public void unperformMove(boolean computeHash) {

		moveTracker.pop();
		int move = moveTracker.getMove();
		int castlingFlags = moveTracker.getCastling();
		long pp = moveTracker.getPassedPawns();
		int enPasTargetSq = moveTracker.getEnPassant();
		int reversedMove = Move.reverse(move);
		
		int captureBitOffset = theBoard.undoMove(reversedMove);
		
		// Restore castling
		castling.setFlags(castlingFlags);
		
		// Restore Passed pawn mask
		theBoard.setPassedPawns(pp);
		
		// Restore en passant target
		theBoard.setEnPassantTargetSq(enPasTargetSq);
		
		if (computeHash) {
			hash.update(reversedMove, captureBitOffset, (byte)enPasTargetSq);
			
			// Clear draw indicator flag
			repetitionPossible = false;
		}
		
		// Update onMove flag
		onMove = Piece.Colour.getOpposite(onMove);
		if (Colour.isBlack(onMove)) {
			moveNumber--;
		}
	}
	
	public void performNullMove() {
		
		// Preserve state
		int prevEnPassantTargetSq = theBoard.getEnPassantTargetSq();
		theBoard.setEnPassantTargetSq(BitBoard.INVALID);
		moveTracker.push(0L, Move.NULL_MOVE, castling.getFlags(), prevEnPassantTargetSq);

		hash.updateNullMove((byte)BitBoard.INVALID);

		// Update the draw checker
		repetitionPossible = dc.setPositionReached(getHash(), getPlyNumber());
		
		// Update onMove
		onMove = Colour.getOpposite(onMove);
		if (Colour.isWhite(onMove)) {
			moveNumber++;
		}
	}
	
	public void unperformNullMove() {

		moveTracker.pop();
		// Restore castling
		castling.setFlags(moveTracker.getCastling());
		// Restore en passant target
		int enPasTargetSq = moveTracker.getEnPassant();
		theBoard.setEnPassantTargetSq(enPasTargetSq);
		
		hash.updateNullMove((byte)enPasTargetSq);
		
		// Clear draw indicator flag
		repetitionPossible = false;
		
		// Update onMove flag
		onMove = Piece.Colour.getOpposite(onMove);
		if (Colour.isBlack(onMove)) {
			moveNumber--;
		}
	}
		
	public String getFen() {
		StringBuilder fen = new StringBuilder(theBoard.getAsFenString());
		fen.append(' ');
		fen.append(Colour.isWhite(getOnMove()) ? 'w' : 'b');
		fen.append(' ');
		fen.append(castling.getFenFlags());
		fen.append(' ');
		// en passant square
		GenericPosition pos = (theBoard.getEnPassantTargetSq() == BitBoard.INVALID) ? 
				null : Position.toGenericPosition(BitBoard.bitToPosition_Lut[theBoard.getEnPassantTargetSq()]);
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
					pl.put(BitBoard.bitValueOf(f, r), Piece.BLACK_ROOK);
					f = advanceFile(f);
					break;
				case 'R':
					pl.put(BitBoard.bitValueOf(f, r), Piece.WHITE_ROOK);
					f = advanceFile(f);
					break;
				case 'n':
					pl.put(BitBoard.bitValueOf(f, r), Piece.BLACK_KNIGHT);
					f = advanceFile(f);
					break;
				case 'N':
					pl.put(BitBoard.bitValueOf(f, r), Piece.WHITE_KNIGHT);
					f = advanceFile(f);
					break;
				case 'b':
					pl.put(BitBoard.bitValueOf(f, r), Piece.BLACK_BISHOP);
					f = advanceFile(f);
					break;
				case 'B':
					pl.put(BitBoard.bitValueOf(f, r), Piece.WHITE_BISHOP);
					f = advanceFile(f);
					break;
				case 'q':
					pl.put(BitBoard.bitValueOf(f, r), Piece.BLACK_QUEEN);
					f = advanceFile(f);
					break;
				case 'Q':
					pl.put(BitBoard.bitValueOf(f, r), Piece.WHITE_QUEEN);
					f = advanceFile(f);
					break;
				case 'k':
					pl.put(BitBoard.bitValueOf(f, r), Piece.BLACK_KING);
					f = advanceFile(f);
					break;
				case 'K':
					pl.put(BitBoard.bitValueOf(f, r), Piece.WHITE_KING);
					f = advanceFile(f);
					break;
				case 'p':
					pl.put(BitBoard.bitValueOf(f, r), Piece.BLACK_PAWN);
					f = advanceFile(f);
					break;
				case 'P':
					pl.put(BitBoard.bitValueOf(f, r), Piece.WHITE_PAWN);
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
				theBoard.setEnPassantTargetSq(BitBoard.positionToBit_Lut[Position.valueOf(GenericPosition.valueOf(targetSq))]);
			} else {
				theBoard.setEnPassantTargetSq(BitBoard.INVALID);
			}
		}
		private int advanceFile(int f) {
			if ( f != IntFile.Fh )
				f += 1;
			return f;
		}
		private void create() {
			theBoard =  new Board( pl, onMove );
		}
	}

	IEvaluate pe;

	@Override
	public IEvaluate getPositionEvaluator() {
		return pe;
	}
	
	public String unwindMoveStack() {
		StringBuilder s = new StringBuilder();
		while (!moveTracker.isEmpty()) {
			// build the movelist backwards, the first move popped shall be the end of the list
			s.insert(0, ' ');
			moveTracker.pop();
			s.insert(0, Move.toString(moveTracker.getMove()));
		}
		return s.toString();
	}

	@Override
	public int getPawnHash() {
		long pawns = theBoard.getPawns();
		long temp = (pawns >>> 32) ^ (pawns & 0xFFFF_FFFFL);
		long pawnHash = (temp >>> 16) ^ (temp & 0xFFFFL);
		return (int)pawnHash;
	}
}
