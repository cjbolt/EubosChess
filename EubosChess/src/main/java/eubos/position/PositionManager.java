package eubos.position;

import java.util.HashMap;
import java.util.Map;

import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.IntRank;

import eubos.board.Board;

import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.score.IEvaluate;
import eubos.score.PositionEvaluator;
import eubos.score.ReferenceScore;
import eubos.search.DrawChecker;

public class PositionManager implements IChangePosition, IPositionAccessors {
	
	public PositionManager( String fenString, DrawChecker dc, ReferenceScore refScore) {
		moveTracker = new MoveTracker();
		new fenParser( this, fenString );
		hash = new ZobristHashCode(this, castling);
		this.dc = dc;
		if (refScore != null) {
			pe = new PositionEvaluator(this, refScore);
		}
	}
	
	public PositionManager( String fenString) {
		this(fenString, new DrawChecker(), new ReferenceScore(null));
	}
	
	public PositionManager() {
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", new DrawChecker(), null);
	}

	CastlingManager castling;
	private Board theBoard;
	public Board getTheBoard() {
		return theBoard;
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
		int plyNumber = moveNumber * 2;
		plyNumber += (!onMoveIsWhite()) ? 1 : 0;
		return plyNumber;
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
	
	boolean repetitionPossible = false;
	public boolean isThreefoldRepetitionPossible() {
		return (EubosEngineMain.ENABLE_REPETITION_DETECTION) ? repetitionPossible : false;
	}
	
	DrawChecker dc;
	
	public void performMove( int move )  {
		performMove(move, true);
	}
	
	public void performMove( int move, boolean computeHash ) {

		// Save previous en passant square
		int prevEnPassantTargetSq = theBoard.getEnPassantTargetSq();
		int capturePosition = theBoard.doMove(move);
		moveTracker.push(TrackedMove.valueOf(move, prevEnPassantTargetSq, castling.getFlags()));
		
		// update castling flags
		castling.updateFlags(move);
		
		if (computeHash) {
			// Determine whether move set en Passant file
			int enPassantFile = IntFile.NOFILE;
			int enPasTargetSq = theBoard.getEnPassantTargetSq();
			if (enPasTargetSq != Position.NOPOSITION)
				enPassantFile = Position.getFile(enPasTargetSq);
			
			hash.update(move, capturePosition, enPassantFile);

			// Update the draw checker
			repetitionPossible = dc.setPositionReached(getHash(), getPlyNumber());
		}
		
		// Update onMove
		onMove = Colour.getOpposite(onMove);
		if (Colour.isWhite(onMove)) {
			moveNumber++;
		}
	}

	public void unperformMove() {
		unperformMove(true);
	}
	
	public void unperformMove(boolean computeHash) {
		long tm = moveTracker.pop();		
		int move = TrackedMove.getMove(tm);
		int reversedMove = Move.reverse(move);
		int capturePosition = theBoard.undoMove(reversedMove);
		
		// Restore castling
		castling.setFlags(TrackedMove.getCastlingFlags(tm));
		
		// Restore en passant target
		int enPasTargetSq = TrackedMove.getEnPassantTarget(tm);
		theBoard.setEnPassantTargetSq(enPasTargetSq);
		
		if (computeHash) {

			int enPassantFile = (enPasTargetSq != Position.NOPOSITION) ? Position.getFile(enPasTargetSq) : IntFile.NOFILE;
			hash.update(reversedMove, capturePosition, enPassantFile);
			
			// Clear draw indicator flag
			repetitionPossible = false;
		}
		
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
			theBoard =  new Board( pl, onMove );
		}
	}

	IEvaluate pe;

	@Override
	public IEvaluate getPositionEvaluator() {
		return pe;
	}
}
