package eubos.position;

import java.util.HashMap;
import java.util.Map;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;

import eubos.board.Board;
import eubos.board.InvalidPieceException;
import eubos.board.pieces.King;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Piece.PieceType;
import eubos.search.DrawChecker;
import eubos.board.pieces.Piece.Colour;

public class PositionManager implements IChangePosition, IPositionAccessors {
	
	public PositionManager() {
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", new DrawChecker());
	}
	
	public PositionManager( Board startingPosition, Piece.Colour colourToMove ) {
		moveTracker = new MoveTracker();
		theBoard = startingPosition;
		castling = new CastlingManager(this);
		onMove = colourToMove;
		pe = new PositionEvaluator( this, new DrawChecker() );
	}
	
	public PositionManager( String fenString, DrawChecker dc) {
		moveTracker = new MoveTracker();
		new fenParser( this, fenString );
		this.hash = new ZobristHashCode(this);
		pe = new PositionEvaluator( this, dc );
	}
	
	public PositionManager( String fenString) {
		moveTracker = new MoveTracker();
		new fenParser( this, fenString );
		this.hash = new ZobristHashCode(this);
		pe = new PositionEvaluator( this, new DrawChecker() );
	}

	private Board theBoard;
	public Board getTheBoard() {
		return theBoard;
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
	
	CaptureData getCapturedPiece() {
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
		return onMove.equals(Colour.white);
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
	boolean isKingInCheck( Colour colour ) {
		King ownKing = theBoard.getKing(colour);
		boolean kingIsInCheck = (ownKing != null) ? theBoard.squareIsAttacked(ownKing.getSquare(), colour) : false;
		return kingIsInCheck;		
	}
	
	private ZobristHashCode hash;

	public long getHash() {
		return hash.hashCode;
	}
	
	PositionEvaluator pe;
	public IEvaluate getPositionEvaluator() {
		return this.pe;
	}
	
	public void performMove( GenericMove move ) throws InvalidPieceException {
		// Get the piece to move
		PieceType pieceToMove = theBoard.pickUpPieceAtSquare( move.from );
		// Flag if move is an en passant capture
		boolean isEnPassantCapture = isEnPassantCapture(move, pieceToMove);
		// Save previous en passant square and initialise for this move
		GenericPosition prevEnPassantTargetSq = theBoard.getEnPassantTargetSq();
		theBoard.setEnPassantTargetSq(null);
		// Handle pawn promotion moves
		pieceToMove = checkForPawnPromotions(move, pieceToMove);
		// Handle castling secondary rook moves...
		if (pieceToMove.equals(PieceType.BlackKing) || pieceToMove.equals(PieceType.WhiteKing))
			castling.performSecondaryCastlingMove(move);
		// Handle any initial 2 square pawn moves that are subject to en passant rule
		GenericFile enPassantFile = checkToSetEnPassantTargetSq(move, pieceToMove);
		// Handle capture target (note, this will be null if the move is not a capture)
		CaptureData captureTarget = getCaptureTarget(move, pieceToMove, isEnPassantCapture);
		// Store the necessary information to undo this move on the move tracker stack
		moveTracker.push( new TrackedMove(move, captureTarget, prevEnPassantTargetSq, castling.getFenFlags()));
		// Update the piece's square.
		updateSquarePieceOccupies(move.to, pieceToMove);
		// update castling flags
		castling.updateFlags(pieceToMove, move);
		// Update hash code
		if (hash != null) {
			hash.update(move, captureTarget, enPassantFile);
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
		PieceType pieceToMove = theBoard.pickUpPieceAtSquare( reversedMove.from );
		// Handle reversal of any castling secondary rook moves and associated flags...
		if (pieceToMove.equals(PieceType.WhiteKing) || pieceToMove.equals(PieceType.BlackKing)) {
			castling.unperformSecondaryCastlingMove(reversedMove);
		}
		updateSquarePieceOccupies(reversedMove.to, pieceToMove);
		castling.setFenFlags(tm.getFenFlags());
		// Undo any capture that had been previously performed.
		if ( tm.isCapture()) {
			theBoard.setPieceAtSquare(tm.getCaptureData().square, tm.getCaptureData().target);
		}
		// Restore en passant target
		GenericPosition enPasTargetSq = tm.getEnPassantTarget();
		theBoard.setEnPassantTargetSq(enPasTargetSq);
		// Update hash code
		if (hash != null) {
			CaptureData capturedPiece = tm.isCapture() ? tm.getCaptureData() : new CaptureData(PieceType.NONE, null);
			Boolean setEnPassant = (enPasTargetSq != null);
			hash.update(reversedMove, capturedPiece, setEnPassant ? enPasTargetSq.file : null);
		}
		// Update onMove flag
		onMove = Piece.Colour.getOpposite(onMove);
		if (onMove==Colour.black) {
			moveNumber--;
		}
	}
	
	void updateSquarePieceOccupies(GenericPosition newSq, PieceType pieceToMove) {
		theBoard.setPieceAtSquare(newSq, pieceToMove);
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
			PieceType type = theBoard.pickUpPieceAtSquare(moveToUndo.to);
			if (type.equals(PieceType.BlackKnight) || type.equals(PieceType.BlackBishop) || type.equals(PieceType.BlackRook) ||
			    type.equals(PieceType.BlackQueen)) {
				type = PieceType.BlackPawn;
			} else {
				type = PieceType.WhitePawn;
			}
			theBoard.setPieceAtSquare(moveToUndo.to, type);
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
			cap.target = theBoard.captureAtSquare(capturePos);
			cap.square = capturePos;
		} else {
			cap.target = theBoard.captureAtSquare(move.to);
			cap.square = move.to;
		}
		return cap;
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
		private Map<GenericPosition, PieceType> pl;
		
		public fenParser( PositionManager pm, String fenString ) {
			pl = new HashMap<GenericPosition, PieceType>();
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
					pl.put(GenericPosition.valueOf(f,r), PieceType.BlackRook);
					f = advanceFile(f);
					break;
				case 'R':
					pl.put(GenericPosition.valueOf(f,r), PieceType.WhiteRook);
					f = advanceFile(f);
					break;
				case 'n':
					pl.put(GenericPosition.valueOf(f,r), PieceType.BlackKnight);
					f = advanceFile(f);
					break;
				case 'N':
					pl.put(GenericPosition.valueOf(f,r), PieceType.WhiteKnight);
					f = advanceFile(f);
					break;
				case 'b':
					pl.put(GenericPosition.valueOf(f,r), PieceType.BlackBishop);
					f = advanceFile(f);
					break;
				case 'B':
					pl.put(GenericPosition.valueOf(f,r), PieceType.WhiteBishop);
					f = advanceFile(f);
					break;
				case 'q':
					pl.put(GenericPosition.valueOf(f,r), PieceType.BlackQueen);
					f = advanceFile(f);
					break;
				case 'Q':
					pl.put(GenericPosition.valueOf(f,r), PieceType.WhiteQueen);
					f = advanceFile(f);
					break;
				case 'k':
					pl.put(GenericPosition.valueOf(f,r), PieceType.BlackKing);
					f = advanceFile(f);
					break;
				case 'K':
					pl.put(GenericPosition.valueOf(f,r), PieceType.WhiteKing);
					f = advanceFile(f);
					break;
				case 'p':
					pl.put(GenericPosition.valueOf(f,r), PieceType.BlackPawn);
					f = advanceFile(f);
					break;
				case 'P':
					pl.put(GenericPosition.valueOf(f,r), PieceType.WhitePawn);
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

	public long getHashForMove(GenericMove bestMove) {
		long hashForMove = (long) 0;
		try {
			performMove(bestMove);
			hashForMove = getHash();
			unperformMove();
		} catch (InvalidPieceException e) {
		}
		return hashForMove;
	}
}
