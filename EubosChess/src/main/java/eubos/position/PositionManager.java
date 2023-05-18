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
import eubos.position.MoveTracker.MoveStack;
import eubos.score.IEvaluate;
import eubos.score.PawnEvalHashTable;
import eubos.score.PositionEvaluator;
import eubos.search.DrawChecker;

public class PositionManager implements IChangePosition, IPositionAccessors {
	
	public PositionManager( String fenString, DrawChecker dc, PawnEvalHashTable pawnHash) {
		moveTracker = new MoveTracker();
		new fenParser( this, fenString );
		hash = new ZobristHashCode(this, castling);
		theBoard.setHash(hash);
		this.dc = dc;
		pe = new PositionEvaluator(this, pawnHash);
	}
	
	public PositionManager( String fenString, long hashCode, DrawChecker dc, PawnEvalHashTable pawnHash) {
		this(fenString, dc, pawnHash);
		hash.hashCode = hashCode;
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
	
	private int plyNumber;
	private int moveNumber;
	public int getMoveNumber() {
		return (plyNumber/2) + 1;
	}
	private void setMoveNumber(int move) {
		moveNumber = move;
		plyNumber = (moveNumber-1) * 2;
		plyNumber += onMoveIsWhite() ? 0 : 1;
	}
	
	public int getPlyNumber() {
		return plyNumber;
	}

	public boolean isKingInCheck() {
		return theBoard.isKingInCheck(onMoveIsWhite());
	}
	
	private ZobristHashCode hash;

	public long getHash() {
		return hash.hashCode;
	}
	
	public boolean isInsufficientMaterial() {
		return theBoard.insufficient;
	}
	
	boolean repetitionPossible = false;
	public boolean isThreefoldRepetitionPossible() {
		return repetitionPossible;
	}
		
	DrawChecker dc;
	
	public boolean moveLeadsToThreefold(int move) {
		boolean isDrawing = false;
		long old_hash = getHash();
		
		// Generate new position hash code
		int prevEnPassantTargetSq = theBoard.getEnPassantTargetSq();
		int old_flags = castling.getFlags();
		theBoard.doMoveForThreefoldCheck(move);
		castling.updateFlags(move);
		byte enPassantTargetBitOffs = (byte)theBoard.getEnPassantTargetSq();
		
		// Update Hash
		hash.doEnPassant(prevEnPassantTargetSq, enPassantTargetBitOffs);
		hash.doCastlingFlags(old_flags, castling.getFlags());
		hash.doOnMove();
		
		// Update onMove
		onMove = Colour.getOpposite(onMove);
		plyNumber++;
		
		// Update the draw checker
		isDrawing = dc.setPositionReached(getHash(), getPlyNumber());
		
		theBoard.undoMoveThreefoldCheck(Move.reverse(move));
		// Restore state
		castling.setFlags(old_flags);
		theBoard.setEnPassantTargetSq(prevEnPassantTargetSq);
		hash.hashCode = old_hash;
		
		// Update onMove flag
		onMove = Piece.Colour.getOpposite(onMove);
		plyNumber--;
		
		return isDrawing;
	}
	
	public boolean performMove(int move) {
		// Preserve state
		int prevEnPassantTargetSq = theBoard.getEnPassantTargetSq();
		long pp = theBoard.getPassedPawns();
		long old_hash = getHash();
		int old_flags = castling.getFlags();
		
		theBoard.doMove(move);
		// Legal move check
		if (theBoard.last_move_was_illegal) {
			int reversedMove = Move.reverse(move);
			theBoard.undoMove(reversedMove);
			
			castling.setFlags(old_flags);
			theBoard.setPassedPawns(pp);
			theBoard.setEnPassantTargetSq(prevEnPassantTargetSq);
			hash.hashCode = old_hash;			
			return false;
		}	

		// Store old state
		moveTracker.push(pp, move, old_flags, prevEnPassantTargetSq, old_hash, dc.checkFromPly);
		
		// Update state
		castling.updateFlags(move);
		
		// Update Hash
		hash.doEnPassant(prevEnPassantTargetSq, theBoard.getEnPassantTargetSq());
		hash.doCastlingFlags(old_flags, castling.getFlags());
		hash.doOnMove();

		// Update the draw checker
		if (Move.isCapture(move) || Move.isPawnMove(move)) {
			dc.reset(plyNumber);
		}
		repetitionPossible = dc.setPositionReached(getHash(), plyNumber);			
		
		// Update onMove
		onMove = Colour.getOpposite(onMove);
		plyNumber++;
		
		return true;
	}

	public void unperformMove() {
		MoveStack stack = moveTracker.pop();
		int reversedMove = Move.reverse(stack.move);
		theBoard.undoMove(reversedMove);
		
		// Restore state from move stack
		castling.setFlags(stack.castling);
		theBoard.setPassedPawns(stack.passed_pawn);
		theBoard.setEnPassantTargetSq(stack.en_passant_square);
		hash.hashCode = stack.hash;
		dc.checkFromPly = stack.draw_check_ply;
			
		// Clear draw indicator flag
		repetitionPossible = false;
		
		// Update onMove flag
		onMove = Piece.Colour.getOpposite(onMove);
		plyNumber--;
	}
	
	public void performNullMove() {
		
		// Preserve state
		int prevEnPassantTargetSq = theBoard.getEnPassantTargetSq();
		theBoard.setEnPassantTargetSq(BitBoard.INVALID);
		moveTracker.push(0L, Move.NULL_MOVE, castling.getFlags(), prevEnPassantTargetSq, 0L, 0);

		hash.doEnPassant(prevEnPassantTargetSq, BitBoard.INVALID);
		hash.doOnMove();
		
		// Update onMove
		onMove = Colour.getOpposite(onMove);
		plyNumber+=2;
	}
	
	public void unperformNullMove() {
		MoveStack stack = moveTracker.pop();
		
		// Restore state
		castling.setFlags(stack.castling);
		int enPasTargetSq = stack.en_passant_square;
		theBoard.setEnPassantTargetSq(enPasTargetSq);
		
		hash.doEnPassant(BitBoard.INVALID, enPasTargetSq);
		hash.doOnMove();
		
		// Clear draw indicator flag
		repetitionPossible = false;
		
		// Update onMove flag
		onMove = Piece.Colour.getOpposite(onMove);
		plyNumber-=2;
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
		fen.append(" - " + getMoveNumber());
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
			int moveNum = 1;
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
