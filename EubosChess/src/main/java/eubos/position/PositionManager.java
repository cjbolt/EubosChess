package eubos.position;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.IntRank;

import eubos.board.Board;
import eubos.board.InvalidPieceException;
import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.board.Piece.PieceType;
import eubos.score.IEvaluate;
import eubos.score.PositionEvaluator;
import eubos.search.DrawChecker;

public class PositionManager implements IChangePosition, IPositionAccessors {
	
	public PositionManager( String fenString, DrawChecker dc) {
		moveTracker = new MoveTracker();
		new fenParser( this, fenString );
		hash = new ZobristHashCode(this);
		pe = new PositionEvaluator(this, dc);
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
	
	public List<Integer> generateMoves() {
		List<Integer> entireMoveList = theBoard.getRegularPieceMoves( onMove );
		castling.addCastlingMoves(entireMoveList);
		return entireMoveList;
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
	boolean lastMoveWasCastle() {
		return moveTracker.lastMoveWasCastle();
	}
	
	public boolean lastMoveWasCapture() {
		return moveTracker.lastMoveWasCapture();
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
	
	DrawChecker dc;
	
	public void performMove( int move ) throws InvalidPieceException {
		// Get the piece to move
		PieceType pieceToMove = theBoard.pickUpPieceAtSquare( Move.getOriginPosition(move));
		// Flag if move is an en passant capture
		boolean isEnPassantCapture = isEnPassantCapture(Move.toGenericMove(move), pieceToMove);
		// Save previous en passant square and initialise for this move
		GenericPosition prevEnPassantTargetSq = theBoard.getEnPassantTargetSq();
		theBoard.setEnPassantTargetSq(null);
		// Handle pawn promotion moves
		pieceToMove = checkForPawnPromotions(Move.toGenericMove(move), pieceToMove);
		// Handle castling secondary rook moves...
		if (PieceType.isKing(pieceToMove)) {
			castling.performSecondaryCastlingMove(Move.toGenericMove(move));
		}
		// Handle any initial 2 square pawn moves that are subject to en passant rule
		GenericFile enPassantFile = checkToSetEnPassantTargetSq(Move.toGenericMove(move), pieceToMove);
		// Handle capture target (note, this will be null if the move is not a capture)
		CaptureData captureTarget = getCaptureTarget(Move.toGenericMove(move), pieceToMove, isEnPassantCapture);
		// Store the necessary information to undo this move on the move tracker stack
		moveTracker.push( new TrackedMove(Move.toGenericMove(move), captureTarget, prevEnPassantTargetSq, castling.getFenFlags()));
		// Update the piece's square.
		theBoard.setPieceAtSquare(Move.getTargetPosition(move), pieceToMove);
		// update castling flags
		castling.updateFlags(pieceToMove, Move.toGenericMove(move));
		// Update hash code
		if (hash != null) {
			hash.update(Move.toGenericMove(move), captureTarget, enPassantFile);
		}
		// Update onMove
		onMove = Colour.getOpposite(onMove);
		if (Colour.isWhite(onMove)) {
			moveNumber++;
		}
		// Update the draw checker
		dc.incrementPositionReachedCount(getHash());
	}
	
	public void unperformMove() throws InvalidPieceException {
		if ( moveTracker.isEmpty())
			return;
		// Update the draw checker
		dc.decrementPositionReachedCount(getHash());
		
		theBoard.setEnPassantTargetSq(null);
		TrackedMove tm = moveTracker.pop();
		GenericMove moveToUndo = tm.getMove();
		// Check for reversal of any pawn promotion that had been previously applied
		checkToUndoPawnPromotion(moveToUndo);
		// Actually undo the move by reversing its direction and reapplying it.
		GenericMove reversedMove = new GenericMove( moveToUndo.to, moveToUndo.from, moveToUndo.promotion );
		// Get the piece to move
		PieceType pieceToMove = theBoard.pickUpPieceAtSquare( Move.getOriginPosition(Move.toMove(reversedMove)));
		// Handle reversal of any castling secondary rook moves and associated flags...
		if (PieceType.isKing(pieceToMove)) {
			castling.unperformSecondaryCastlingMove(reversedMove);
		}
		theBoard.setPieceAtSquare(Move.getTargetPosition(Move.toMove(reversedMove)), pieceToMove);
		castling.setFenFlags(tm.getFenFlags());
		// Undo any capture that had been previously performed.
		if ( tm.isCapture()) {
			theBoard.setPieceAtSquare(Position.valueOf(tm.getCaptureData().square), tm.getCaptureData().target);
		}
		// Restore en passant target
		GenericPosition enPasTargetSq = tm.getEnPassantTarget();
		theBoard.setEnPassantTargetSq(enPasTargetSq);
		// Update hash code
		if (hash != null) {
			CaptureData capturedPiece = tm.isCapture() ? tm.getCaptureData() : new CaptureData();
			Boolean setEnPassant = (enPasTargetSq != null);
			hash.update(reversedMove, capturedPiece, setEnPassant ? enPasTargetSq.file : null);
		}
		// Update onMove flag
		onMove = Piece.Colour.getOpposite(onMove);
		if (Colour.isBlack(onMove)) {
			moveNumber--;
		}
	}
	
	private PieceType checkForPawnPromotions(GenericMove move, PieceType pawn) {
		boolean isWhite = pawn.equals(PieceType.WhitePawn);
		PieceType type = pawn;
		if ( move.promotion != null ) {
			switch( move.promotion ) {
			case QUEEN:
				type = isWhite ? PieceType.WhiteQueen : PieceType.BlackQueen;
				break;
			case KNIGHT:
				type = isWhite ? PieceType.WhiteKnight : PieceType.BlackKnight;
				break;
			case BISHOP:
				type = isWhite ? PieceType.WhiteBishop : PieceType.BlackBishop;
				break;
			case ROOK:
				type = isWhite ? PieceType.WhiteRook : PieceType.BlackRook;
				break;
			default:
				break;
			}
		}
		return type;
	}
	
	private void checkToUndoPawnPromotion(GenericMove moveToUndo) {
		if ( moveToUndo.promotion != null ) {
			PieceType type = theBoard.pickUpPieceAtSquare(Position.valueOf(moveToUndo.to));
			if (PieceType.isBlack(type)) {
				type = PieceType.BlackPawn;
			} else {
				type = PieceType.WhitePawn;
			}
			theBoard.setPieceAtSquare(Position.valueOf(moveToUndo.to), type);
		}
	}
	
	private boolean isEnPassantCapture(GenericMove move, PieceType pieceToMove) {
		boolean enPassantCapture = false;
		GenericPosition enPassantTargetSq = theBoard.getEnPassantTargetSq();
		if ( enPassantTargetSq != null && (pieceToMove.equals(PieceType.BlackPawn) || pieceToMove.equals(PieceType.WhitePawn)) && move.to == enPassantTargetSq) {
			enPassantCapture = true;
		}
		return enPassantCapture;
	}
	
	private GenericFile checkToSetEnPassantTargetSq(GenericMove move, PieceType pieceToMove) {
		GenericFile enPassantFile = null;
		if ( pieceToMove.equals(PieceType.WhitePawn)) {
			if ( move.from.rank == GenericRank.R2) {
				if (move.to.rank == GenericRank.R4) {
					GenericPosition enPassantWhite = GenericPosition.valueOf(move.to.file,GenericRank.R3);
					theBoard.setEnPassantTargetSq(enPassantWhite);
					enPassantFile = enPassantWhite.file;
				}
			}
		} else if (pieceToMove.equals(PieceType.BlackPawn)) {
			if (move.from.rank == GenericRank.R7) {
				if (move.to.rank == GenericRank.R5) {
					GenericPosition enPassantBlack = GenericPosition.valueOf(move.to.file,GenericRank.R6);
					theBoard.setEnPassantTargetSq(enPassantBlack);
					enPassantFile = enPassantBlack.file;
				}
			}
		}
		return enPassantFile;
	}
	
	private CaptureData getCaptureTarget(GenericMove move, PieceType pieceToMove, boolean enPassantCapture) {
		CaptureData cap = new CaptureData(PieceType.NONE, null);
		if (enPassantCapture) {
			GenericRank rank = GenericRank.R1;
			if (pieceToMove.equals(PieceType.WhitePawn)) {
				rank = GenericRank.R5;
			} else if (pieceToMove.equals(PieceType.BlackPawn)){
				rank = GenericRank.R4;
			} else {
				assert false;
			}
			GenericPosition capturePos = GenericPosition.valueOf(move.to.file,rank);
			cap.target = theBoard.pickUpPieceAtSquare(Position.valueOf(capturePos));
			cap.square = capturePos;
		} else {
			cap.target = theBoard.pickUpPieceAtSquare(Position.valueOf(move.to));
			cap.square = move.to;
		}
		return cap;
	}
	
	public String getFen() {
		StringBuilder fen = new StringBuilder(theBoard.getAsFenString());
		fen.append(' ');
		fen.append(Colour.isWhite(getOnMove()) ? 'w' : 'b');
		fen.append(' ');
		fen.append(castling.getFenFlags());
		fen.append(' ');
		// en passant square
		GenericPosition pos = theBoard.getEnPassantTargetSq();
		if (pos != null) {
			fen.append(pos.toString());
		} else {
			fen.append('-');
		}
		fen.append(" - " + moveNumber);
		return fen.toString();
	}
	
	private class fenParser {
		private Map<Integer, PieceType> pl;
		
		public fenParser( PositionManager pm, String fenString ) {
			pl = new HashMap<Integer, PieceType>();
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
					pl.put(Position.valueOf(f,r), PieceType.BlackRook);
					f = advanceFile(f);
					break;
				case 'R':
					pl.put(Position.valueOf(f,r), PieceType.WhiteRook);
					f = advanceFile(f);
					break;
				case 'n':
					pl.put(Position.valueOf(f,r), PieceType.BlackKnight);
					f = advanceFile(f);
					break;
				case 'N':
					pl.put(Position.valueOf(f,r), PieceType.WhiteKnight);
					f = advanceFile(f);
					break;
				case 'b':
					pl.put(Position.valueOf(f,r), PieceType.BlackBishop);
					f = advanceFile(f);
					break;
				case 'B':
					pl.put(Position.valueOf(f,r), PieceType.WhiteBishop);
					f = advanceFile(f);
					break;
				case 'q':
					pl.put(Position.valueOf(f,r), PieceType.BlackQueen);
					f = advanceFile(f);
					break;
				case 'Q':
					pl.put(Position.valueOf(f,r), PieceType.WhiteQueen);
					f = advanceFile(f);
					break;
				case 'k':
					pl.put(Position.valueOf(f,r), PieceType.BlackKing);
					f = advanceFile(f);
					break;
				case 'K':
					pl.put(Position.valueOf(f,r), PieceType.WhiteKing);
					f = advanceFile(f);
					break;
				case 'p':
					pl.put(Position.valueOf(f,r), PieceType.BlackPawn);
					f = advanceFile(f);
					break;
				case 'P':
					pl.put(Position.valueOf(f,r), PieceType.WhitePawn);
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
					r -= 1;
					f = IntFile.Fa;
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
		private int advanceFile(int f) {
			if ( f != IntFile.Fh )
				f += 1;
			return f;
		}
		private void create() {
			theBoard =  new Board( pl );
		}
	}

	public long getHashForMove(GenericMove bestMove) {
		long hashForMove = (long) 0;
		try {
			performMove(Move.toMove(bestMove));
			hashForMove = getHash();
			unperformMove();
		} catch (InvalidPieceException e) {
		}
		return hashForMove;
	}
}
