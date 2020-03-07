package eubos.board;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Map.Entry;
import java.util.function.IntConsumer;

import eubos.board.Piece.Colour;
import eubos.position.CaptureData;
import eubos.position.CastlingManager;
import eubos.position.Move;
import eubos.position.Position;

import com.fluxchess.jcpi.models.IntChessman;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IntRank;

public class Board {
	public static final CaptureData NULL_CAPTURE = new CaptureData(Piece.NONE, Position.NOPOSITION);
	
	private BitBoard allPieces = null;
	private BitBoard whitePieces = null;
	private BitBoard blackPieces = null;
	
	public BitBoard getWhitePieces() {
		return whitePieces;
	}
	public BitBoard getBlackPieces() {
		return blackPieces;
	}

	private static final int INDEX_PAWN = Piece.PAWN;
	private static final int INDEX_KNIGHT = Piece.KNIGHT;
	private static final int INDEX_BISHOP = Piece.BISHOP;
	private static final int INDEX_ROOK = Piece.ROOK;
	private static final int INDEX_QUEEN = Piece.QUEEN;
	private static final int INDEX_KING = Piece.KING;
	//private static final int INDEX_NONE = Piece.NONE;
	
	private BitBoard[] pieces = new BitBoard[7]; // N.b. INDEX_NONE is an empty BitBoard at index 0.
	
	@SuppressWarnings("unchecked")
	private static final List<BitBoard>[] RankFileMask_Lut = (List<BitBoard>[]) new List[128];
	static {
		Direction [] rankFile = { Direction.left, Direction.up, Direction.right, Direction.down };
		for (int square : Position.values) {
			List<BitBoard> array = new ArrayList<BitBoard>();
			for (int index=1; index<8; index++) {
				createMask(square, array, index, rankFile);
			}
			RankFileMask_Lut[square] = array;
		}
	}
	static private void createMask(int square, List<BitBoard> array, int index, Direction [] directions) {
		Long currMask = 0L;
		for (Direction dir: directions) {
			currMask = setAllInDirection(dir, square, currMask, index);
		}
		// Only add the mask if it isn't the same as previous (i.e. no more squares to add)
		BitBoard toAdd = new BitBoard(currMask);
		toAdd.setNumBits();
		if (array.size()-1 >= 0) {
			if (currMask != array.get(array.size()-1).getValue())
				array.add(toAdd);
		} else {
			array.add(toAdd);
		}
	}
	static private Long setAllInDirection(Direction dir, int fromSq, Long currMask, int index) {
		int newSquare = fromSq;
		for (int i=0; i < index; i++) {
			if (newSquare != Position.NOPOSITION)
				newSquare = Direction.getDirectMoveSq(dir, newSquare);
			if (newSquare != Position.NOPOSITION)
				currMask |= BitBoard.positionToMask_Lut[newSquare].getValue();
		}
		return currMask;
	}
	
	private static final BitBoard[] directAttacksOnPosition_Lut = new BitBoard[128];
	static {
		Direction [] allDirect = { Direction.left, Direction.up, Direction.right, Direction.down, Direction.downLeft, Direction.upLeft, Direction.upRight, Direction.downRight };
		for (int square : Position.values) {
			Long allAttacksMask = 0L;
			for (Direction dir: allDirect) {
				allAttacksMask = setAllInDirection(dir, square, allAttacksMask, 8);
			}
			directAttacksOnPosition_Lut[square] = new BitBoard(allAttacksMask);
		}
	}
	
	@SuppressWarnings("unchecked")
	private static final List<BitBoard>[] DiagonalMask_Lut = (List<BitBoard>[]) new List[128];
	static {
		Direction [] diagonals = { Direction.downLeft, Direction.upLeft, Direction.upRight, Direction.downRight };
		for (int square : Position.values) {
			List<BitBoard> array = new ArrayList<BitBoard>();
			for (int index=1; index<8; index++) {
				createMask(square, array, index, diagonals);
			}
			DiagonalMask_Lut[square] = array;
		}
	}
	
	private static final BitBoard[] FileMask_Lut = new BitBoard[8];
	static {
		for (int file : IntFile.values) {
			long mask = 0;
			int f=file;
			for (int r = 0; r<8; r++) {
				mask  |= 1L << r*8+f;
			}
			FileMask_Lut[file]= new BitBoard(mask);
		}
	}
	
	private static final BitBoard[] RankMask_Lut = new BitBoard[8];
	static {
		for (int r : IntRank.values) {
			long mask = 0;
			for (int f = 0; f<8; f++) {
				mask  |= 1L << r*8+f;
			}
			RankMask_Lut[r] = new BitBoard(mask);
		}
	}
	
	public Board( Map<Integer, Integer> pieceMap ) {
		allPieces = new BitBoard();
		whitePieces = new BitBoard();
		blackPieces = new BitBoard();
		for (int i=0; i<=INDEX_PAWN; i++) {
			pieces[i] = new BitBoard();
		}
		for ( Entry<Integer, Integer> nextPiece : pieceMap.entrySet() ) {
			setPieceAtSquare( nextPiece.getKey(), nextPiece.getValue() );
		}
	}
	
	public CaptureData doMove(int move) throws InvalidPieceException {
		CaptureData captureTarget = NULL_CAPTURE;
		int pieceToMove = Move.getOriginPiece(move);
		int targetSquare = Move.getTargetPosition(move);
		if (isEnPassantCapture(pieceToMove, targetSquare)) {
			// Handle en passant captures, don't need to do other checks in this case
			int rank = IntRank.NORANK;
			if (pieceToMove == Piece.WHITE_PAWN) {
				rank = IntRank.R5;
			} else if (pieceToMove == Piece.BLACK_PAWN){
				rank = IntRank.R4;
			} else {
				assert false;
			}
			int capturePos = Position.valueOf(Position.getFile(targetSquare), rank);
			captureTarget = new CaptureData(pickUpPieceAtSquare(capturePos), capturePos);
		} else {
			// handle castling, setting en passant etc
			if (checkToSetEnPassantTargetSq(move) == IntFile.NOFILE) {
				// Handle castling secondary rook moves...
				if (Piece.isKing(pieceToMove)) {
					performSecondaryCastlingMove(move);
				}
				captureTarget = new CaptureData(pickUpPieceAtSquare(targetSquare), targetSquare);
			}			
		}
		movePiece(move);
		return captureTarget;
	}
	
	
	private void movePiece(int move) {
		int pieceToMove = Move.getOriginPiece(move);
		int initialSquare = Move.getOriginPosition(move);
		int targetSquare = Move.getTargetPosition(move);
		if (Move.getPromotion(move) != IntChessman.NOCHESSMAN) {
			pickUpPieceAtSquare(initialSquare);
			setPieceAtSquare(targetSquare, pieceToMove);
		} else {
			// TODO Doesn't work for promotions because we don't know the BitBoard index - this can be resolved 
			int pieceType = Move.getOriginPiece(move);
			BitBoard positionsMask = BitBoard.positionToMask_Lut[initialSquare].or(BitBoard.positionToMask_Lut[targetSquare]);
			allPieces.xor(positionsMask);
			pieces[Piece.PIECE_NO_COLOUR_MASK & pieceType].xor(positionsMask);
			if (Piece.isWhite(pieceType)) {
				whitePieces.xor(positionsMask);
			} else {
				blackPieces.xor(positionsMask);
			}
		}
	}
	
	private boolean isEnPassantCapture(int pieceToMove, int targetSquare) {
		boolean enPassantCapture = false;
		if ( getEnPassantTargetSq() != Position.NOPOSITION &&
			 Piece.isPawn(pieceToMove) && 
			 targetSquare == getEnPassantTargetSq()) {
			enPassantCapture = true;
		}
		setEnPassantTargetSq(Position.NOPOSITION);
		return enPassantCapture;
	}
	
	private int checkToSetEnPassantTargetSq(int move) {
		int enPassantFile = IntFile.NOFILE;
		if (Move.getOriginPiece(move) == Piece.WHITE_PAWN) {
			int potentialEnPassantFile = Position.getFile(Move.getOriginPosition(move));
			if ( Position.getRank(Move.getOriginPosition(move)) == IntRank.R2) {
				if (Position.getRank(Move.getTargetPosition(move)) == IntRank.R4) {
					enPassantFile = potentialEnPassantFile;
					int enPassantWhite = Position.valueOf(enPassantFile,IntRank.R3);
					setEnPassantTargetSq(enPassantWhite);
				}
			}
		} else if (Move.getOriginPiece(move) == Piece.BLACK_PAWN) {
			int potentialEnPassantFile = Position.getFile(Move.getOriginPosition(move));
			if (Position.getRank(Move.getOriginPosition(move)) == IntRank.R7) {
				if (Position.getRank(Move.getTargetPosition(move)) == IntRank.R5) {
					enPassantFile = potentialEnPassantFile;
					int enPassantBlack = Position.valueOf(enPassantFile,IntRank.R6);
					setEnPassantTargetSq(enPassantBlack);
				}
			}
		}
		return enPassantFile;
	}
	
	private static final BitBoard wksc_mask = BitBoard.positionToMask_Lut[Position.h1].or(BitBoard.positionToMask_Lut[Position.f1]);
	private static final BitBoard wqsc_mask = BitBoard.positionToMask_Lut[Position.a1].or(BitBoard.positionToMask_Lut[Position.d1]);
	private static final BitBoard bksc_mask = BitBoard.positionToMask_Lut[Position.h8].or(BitBoard.positionToMask_Lut[Position.f8]);
	private static final BitBoard bqsc_mask = BitBoard.positionToMask_Lut[Position.a8].or(BitBoard.positionToMask_Lut[Position.d8]);
	
	private void performSecondaryCastlingMove(int move) throws InvalidPieceException {
		if (Move.areEqual(move, CastlingManager.wksc)) {
			pieces[INDEX_ROOK].xor(wksc_mask);
			whitePieces.xor(wksc_mask);
			allPieces.xor(wksc_mask);
		} else if (Move.areEqual(move, CastlingManager.wqsc)) {
			pieces[INDEX_ROOK].xor(wqsc_mask);
			whitePieces.xor(wqsc_mask);
			allPieces.xor(wqsc_mask);
		} else if (Move.areEqual(move, CastlingManager.bksc)) {
			pieces[INDEX_ROOK].xor(bksc_mask);
			blackPieces.xor(bksc_mask);
			allPieces.xor(bksc_mask);
		} else if (Move.areEqual(move, CastlingManager.bqsc)) {
			pieces[INDEX_ROOK].xor(bqsc_mask);
			blackPieces.xor(bqsc_mask);
			allPieces.xor(bqsc_mask);
		}
	}
	
	public void undoMove(int reversedMove, CaptureData cap) throws InvalidPieceException {
		// Handle reversal of any castling secondary rook moves
		if (Piece.isKing(Move.getOriginPiece(reversedMove))) {
			unperformSecondaryCastlingMove(reversedMove);
		}
		movePiece(reversedMove);
		// Undo any capture that had been previously performed.
		if (cap.getPiece() != Piece.NONE) {
			setPieceAtSquare(cap.getSquare(), cap.getPiece());
		}
	}
	
	private void unperformSecondaryCastlingMove(int move) throws InvalidPieceException {
		if (Move.areEqual(move, CastlingManager.undo_wksc)) {
			pieces[INDEX_ROOK].xor(wksc_mask);
			whitePieces.xor(wksc_mask);
			allPieces.xor(wksc_mask);
		} else	if (Move.areEqual(move, CastlingManager.undo_wqsc)) {
			pieces[INDEX_ROOK].xor(wqsc_mask);
			whitePieces.xor(wqsc_mask);
			allPieces.xor(wqsc_mask);
		} else if (Move.areEqual(move, CastlingManager.undo_bksc)) {
			pieces[INDEX_ROOK].xor(bksc_mask);
			blackPieces.xor(bksc_mask);
			allPieces.xor(bksc_mask);
		} else if (Move.areEqual(move, CastlingManager.undo_bqsc)) {
			pieces[INDEX_ROOK].xor(bqsc_mask);
			blackPieces.xor(bqsc_mask);
			allPieces.xor(bqsc_mask);
		}
	}
	
	public List<Integer> getRegularPieceMoves(Piece.Colour side) {
		BitBoard bitBoardToIterate = Colour.isWhite(side) ? whitePieces : blackPieces;
		ArrayList<Integer> movesList = new ArrayList<Integer>();
		
		PrimitiveIterator.OfInt iter = bitBoardToIterate.iterator();
		while (iter.hasNext()) {
			int bit_index = iter.nextInt();
			int atSquare = BitBoard.bitToPosition_Lut[bit_index];
			long mask = 1L<<bit_index;
			if (pieces[INDEX_KING].isSet(mask)) {
				movesList.addAll(Piece.king_generateMoves(this, atSquare, side));
			} else if (pieces[INDEX_QUEEN].isSet(mask)) {
				movesList.addAll(Piece.queen_generateMoves(this, atSquare, side));
			} else if (pieces[INDEX_ROOK].isSet(mask)) {
				movesList.addAll(Piece.rook_generateMoves(this, atSquare, side));
			} else if (pieces[INDEX_BISHOP].isSet(mask)) {
				movesList.addAll(Piece.bishop_generateMoves(this, atSquare, side));
			} else if (pieces[INDEX_KNIGHT].isSet(mask)) {
				movesList.addAll(Piece.knight_generateMoves(this, atSquare, side));
			} else if (pieces[INDEX_PAWN].isSet(mask)) {
				movesList.addAll(Piece.pawn_generateMoves(this, atSquare, side));
			}
		}
		return movesList;
	}
		
	private int enPassantTargetSq = Position.NOPOSITION;
	public int getEnPassantTargetSq() {
		return enPassantTargetSq;
	}
	public void setEnPassantTargetSq(int enPassantTargetSq) {
		// TODO: add bounds checking - only certain en passant squares can be legal.
		this.enPassantTargetSq = enPassantTargetSq;
	}
	
	public boolean squareIsEmpty( int atPos ) {
		return !allPieces.isSet(BitBoard.positionToBit_Lut[atPos]);		
	}
	
	public boolean squareIsAttacked( int atPos, Piece.Colour ownColour ) {
		return SquareAttackEvaluator.isAttacked(this, atPos, ownColour);
	}
	
	public int getPieceAtSquare( int atPos ) {
		int type = Piece.NONE;
		int bit_index = BitBoard.positionToBit_Lut[atPos];
		long mask = 1L<<bit_index;
		BitBoard pieceToPickUp = new BitBoard(mask);
		if (allPieces.and(pieceToPickUp).isNonZero()) {	
			if (blackPieces.and(pieceToPickUp).isNonZero()) {
				type |= Piece.BLACK;
			} else assert whitePieces.and(pieceToPickUp).isNonZero();
			if (pieces[INDEX_KING].isSet(mask)) {
				type |= Piece.KING;
			} else if (pieces[INDEX_QUEEN].isSet(mask)) {
				type |= Piece.QUEEN;
			} else if (pieces[INDEX_ROOK].isSet(mask)) {
				type |= Piece.ROOK;
			} else if (pieces[INDEX_BISHOP].isSet(mask)) {
				type |= Piece.BISHOP;
			} else if (pieces[INDEX_KNIGHT].isSet(mask)) {
				type |= Piece.KNIGHT;
			} else if (pieces[INDEX_PAWN].isSet(mask)) {
				type |= Piece.PAWN;
			}
		}
		return type;
	}
	
	public void setPieceAtSquare( int atPos, int pieceToPlace ) {
		assert pieceToPlace != Piece.NONE;
		int bit_index = BitBoard.positionToBit_Lut[atPos];
		if (Piece.isKing(pieceToPlace)) {
			pieces[INDEX_KING].set(bit_index);
		} else if (Piece.isQueen(pieceToPlace)) {
			pieces[INDEX_QUEEN].set(bit_index);
		} else if (Piece.isRook(pieceToPlace)) {
			pieces[INDEX_ROOK].set(bit_index);
		} else if (Piece.isBishop(pieceToPlace)) {
			pieces[INDEX_BISHOP].set(bit_index);
		} else if (Piece.isKnight(pieceToPlace)) {
			pieces[INDEX_KNIGHT].set(bit_index);
		} else if (Piece.isPawn(pieceToPlace)) {
			pieces[INDEX_PAWN].set(bit_index);
		} else {
			assert false;
		}
		if (Piece.isBlack(pieceToPlace)) {
			blackPieces.set(bit_index);
		} else {
			whitePieces.set(bit_index);
		}
		allPieces.set(bit_index);
	}
	
	public boolean isKingInCheck(Piece.Colour side) {
		boolean inCheck = false;
		BitBoard getFromBoard = Colour.isWhite(side) ? whitePieces : blackPieces;
		BitBoard kingMask = getFromBoard.and(pieces[INDEX_KING]);
		if (kingMask.isNonZero()) {
			// The conditional is needed because some unit test positions don't have a king...
			int kingSquare = BitBoard.maskToPosition_Lut.get(kingMask.getValue());
			inCheck = squareIsAttacked(kingSquare, side);
		}
		return inCheck;
	}
	
	public int pickUpPieceAtSquare( int atPos ) {
		int type = Piece.NONE;
		int bit_index = BitBoard.positionToBit_Lut[atPos];
		long mask = 1L<<bit_index;
		BitBoard pieceToPickUp = new BitBoard(mask);
		if (allPieces.and(pieceToPickUp).isNonZero()) {	
			if (blackPieces.and(pieceToPickUp).isNonZero()) {
				blackPieces.clear(mask);
				type |= Piece.BLACK;
			} else {
				assert whitePieces.and(pieceToPickUp).isNonZero();
				whitePieces.clear(mask);
			}
			if (pieces[INDEX_KING].isSet(mask)) {
				pieces[INDEX_KING].clear(mask);
				type |= Piece.KING;
			} else if (pieces[INDEX_QUEEN].isSet(mask)) {
				pieces[INDEX_QUEEN].clear(mask);
				type |= Piece.QUEEN;
			} else if (pieces[INDEX_ROOK].isSet(mask)) {
				pieces[INDEX_ROOK].clear(mask);
				type |= Piece.ROOK;
			} else if (pieces[INDEX_BISHOP].isSet(mask)) {
				pieces[INDEX_BISHOP].clear(mask);
				type |= Piece.BISHOP;
			} else if (pieces[INDEX_KNIGHT].isSet(mask)) {
				pieces[INDEX_KNIGHT].clear(mask);
				type |= Piece.KNIGHT;
			} else if (pieces[INDEX_PAWN].isSet(mask)) {
				pieces[INDEX_PAWN].clear(mask);
				type |= Piece.PAWN;
			}
			allPieces.clear(mask);
		}
		return type;
	}
	
	public int countDoubledPawnsForSide(Colour side) {
		int doubledCount = 0;
		BitBoard pawns = Colour.isWhite(side) ? getWhitePawns() : getBlackPawns();
		for (int file : IntFile.values) {
			BitBoard mask = FileMask_Lut[file];
			long fileMask = pawns.and(mask).getValue();
			int numPawnsInFile = Long.bitCount(fileMask);
			if (numPawnsInFile > 1) {
				doubledCount += numPawnsInFile-1;
			}
		}
		return doubledCount;
	}
	
	public boolean isPassedPawn(int atPos, Colour side) {
		boolean isPassed = true;
		BitBoard mask = PassedPawn_Lut[side.ordinal()][atPos];
		BitBoard otherSidePawns = Colour.isWhite(side) ? getBlackPawns() : getWhitePawns();
		if (mask.and(otherSidePawns).isNonZero()) {
			isPassed  = false;
		}
		return isPassed;
	}
	
	private static final BitBoard[][] PassedPawn_Lut = new BitBoard[2][]; 
	static {
		BitBoard[] white_map = new BitBoard[128];
		PassedPawn_Lut[Colour.white.ordinal()] = white_map;
		for (int atPos : Position.values) {
			white_map[atPos] = buildPassedPawnFileMask(Position.getFile(atPos), Position.getRank(atPos), true);
		}
		BitBoard[] black_map = new BitBoard[128];
		PassedPawn_Lut[Colour.black.ordinal()] = black_map;
		for (int atPos : Position.values) {
			black_map[atPos] = buildPassedPawnFileMask(Position.getFile(atPos), Position.getRank(atPos), false);
		}
	}
	private static BitBoard buildPassedPawnFileMask(int f, int r, boolean isWhite) {
		long mask = 0;
		boolean hasPrevFile = IntFile.toGenericFile(f).hasPrev();
		boolean hasNextFile = IntFile.toGenericFile(f).hasNext();
		if (isWhite) {
			for (r=r+1; r < 7; r++) {
				mask = addRankForPassedPawnMask(mask, r, f, hasPrevFile,
						hasNextFile);
			}
		} else {
			for (r=r-1; r > 0; r--) {
				mask = addRankForPassedPawnMask(mask, r, f, hasPrevFile,
						hasNextFile);	
			}
		}
		return new BitBoard(mask);
	}
	private static long addRankForPassedPawnMask(long mask, int r, int f,
			boolean hasPrevFile, boolean hasNextFile) {
		if (hasPrevFile) {
			mask |= 1L << r*8+(f-1);
		}
		mask |= 1L << r*8+f;
		if (hasNextFile) {
			mask |= 1L << r*8+(f+1);
		}
		return mask;
	}
	
	public String getAsFenString() {
		int currPiece = Piece.NONE;
		int spaceCounter = 0;
		StringBuilder fen = new StringBuilder();
		for (int rank=7; rank>=0; rank--) {
			for (int file=0; file<8; file++) {
				currPiece = this.getPieceAtSquare(Position.valueOf(file,rank));
				if (currPiece != Piece.NONE) {
					if (spaceCounter != 0)
						fen.append(spaceCounter);
					fen.append(Piece.toFenChar(currPiece));
					spaceCounter=0;					
				} else {
					spaceCounter++;
				}
			}
			if (spaceCounter != 0)
				fen.append(spaceCounter);
			if (rank != 0)
				fen.append('/');
			spaceCounter=0;
		}
		return fen.toString();
	}
	
	class allPiecesOnBoardIterator implements PrimitiveIterator.OfInt {	
		private int[] pieces = null;
		private int count = 0;
		private int next = 0;

		allPiecesOnBoardIterator() throws InvalidPieceException {
			pieces = new int[64];
			buildIterList(allPieces);
		}

		allPiecesOnBoardIterator( int typeToIterate ) throws InvalidPieceException {
			pieces = new int[64];
			BitBoard bitBoardToIterate;
			if (typeToIterate == Piece.WHITE_PAWN) {
				bitBoardToIterate = getWhitePawns();
			} else if (typeToIterate == Piece.BLACK_PAWN) {
				bitBoardToIterate = getBlackPawns();
			} else {
				bitBoardToIterate = new BitBoard();
			}
			buildIterList(bitBoardToIterate);
		}

		private void buildIterList(BitBoard bitBoardToIterate) {
			PrimitiveIterator.OfInt iter = bitBoardToIterate.iterator();
			while (iter.hasNext()) {
				int bit_index = iter.nextInt();
				pieces[count++] = BitBoard.bitToPosition_Lut[bit_index];
			}
		}	

		public boolean hasNext() {
			return next < pieces.length && next < count;
		}

		public Integer next() {
			assert false; // should always use nextInt()
			return pieces[next++];
		}

		@Override
		public void remove() {
		}

		@Override
		public void forEachRemaining(IntConsumer action) {
		}

		@Override
		public int nextInt() {
			return pieces[next++];
		}
	}

	public PrimitiveIterator.OfInt iterator() {
		// default iterator returns all the pieces on the board, not all positions
		try {
			return new allPiecesOnBoardIterator( );
		} catch (InvalidPieceException e) {
			return null;
		}
	}
	
	public BitBoard getMaskForType(int type) {
		BitBoard mask = null;
		switch(type) {
		case Piece.WHITE_KING:
			mask = getWhiteKing();
			break;
		case Piece.WHITE_QUEEN:
			mask = getWhiteQueens();
			break;
		case Piece.WHITE_ROOK:
			mask = getWhiteRooks();
			break;
		case Piece.WHITE_BISHOP:
			mask = getWhiteBishops();
			break;
		case Piece.WHITE_KNIGHT:
			mask = getWhiteKnights();
			break;
		case Piece.WHITE_PAWN:
			mask = getWhitePawns();
			break;
		case Piece.BLACK_KING:
			mask = getBlackKing();
			break;
		case Piece.BLACK_QUEEN:
			mask = getBlackQueens();
			break;
		case Piece.BLACK_ROOK:
			mask = getBlackRooks();
			break;
		case Piece.BLACK_BISHOP:
			mask = getBlackBishops();
			break;
		case Piece.BLACK_KNIGHT:
			mask = getBlackKnights();
			break;
		case Piece.BLACK_PAWN:
			mask = getBlackPawns();
			break;
		case Piece.NONE:
		default:
			assert false;
			break;
		}
		return mask;
	}
		
	public BitBoard getBlackPawns() {
		return blackPieces.and(pieces[INDEX_PAWN]);
	}
	
	public BitBoard getBlackKnights() {
		return blackPieces.and(pieces[INDEX_KNIGHT]);
	}
	
	public BitBoard getBlackBishops() {
		return blackPieces.and(pieces[INDEX_BISHOP]);
	}
	
	public BitBoard getBlackRooks() {
		return blackPieces.and(pieces[INDEX_ROOK]);
	}
	
	public BitBoard getBlackQueens() {
		return blackPieces.and(pieces[INDEX_QUEEN]);
	}
	
	public BitBoard getBlackKing() {
		return blackPieces.and(pieces[INDEX_KING]);
	}
	
	public BitBoard getWhitePawns() {
		return whitePieces.and(pieces[INDEX_PAWN]);
	}
	
	public BitBoard getWhiteBishops() {
		return whitePieces.and(pieces[INDEX_BISHOP]);
	}
	
	public BitBoard getWhiteRooks() {
		return whitePieces.and(pieces[INDEX_ROOK]);
	}
	
	public BitBoard getWhiteQueens() {
		return whitePieces.and(pieces[INDEX_QUEEN]);
	}
	
	public BitBoard getWhiteKnights() {
		return whitePieces.and(pieces[INDEX_KNIGHT]);
	}
	
	public BitBoard getWhiteKing() {
		return whitePieces.and(pieces[INDEX_KING]);
	}
	
	public PrimitiveIterator.OfInt iterateType( int typeToIterate ) {
		try {
			return new allPiecesOnBoardIterator( typeToIterate );
		} catch (InvalidPieceException e) {
			return null;
		}
	}

	public int getNumRankFileSquaresAvailable(int atPos) {
		return getSquaresAvaillableFromPosition(atPos, RankFileMask_Lut);
	}
	
	public int getNumDiagonalSquaresAvailable(int atPos) {
		return getSquaresAvaillableFromPosition(atPos, DiagonalMask_Lut);
	}
	
	private int getSquaresAvaillableFromPosition(int atPos, List<BitBoard>[] maskMap ) {
		int squaresCount = 0;
		int bit = BitBoard.positionToBit_Lut[atPos];
		List<BitBoard> list = maskMap[atPos];
		for (BitBoard levelMask : list) {
			if (checkSingleMask(bit, levelMask))
				squaresCount = levelMask.getNumBits();
		}
		return squaresCount;
	}

	private boolean checkSingleMask(int bit, BitBoard levelMask) {
		levelMask.clear(bit);
		return allPieces.and(levelMask).getValue() == 0;
	}
	
	public boolean isOnHalfOpenFile(GenericPosition atPos, int type) {
		boolean isHalfOpen = false;
		BitBoard fileMask = new BitBoard(FileMask_Lut[IntFile.valueOf(atPos.file)].getValue());
		BitBoard otherSide = Piece.getOpposite(type) == Colour.white ? whitePieces : blackPieces;
		BitBoard pawnMask = otherSide.and(pieces[INDEX_PAWN]);
		boolean opponentPawnOnFile = pawnMask.and(fileMask).isNonZero();
		if (opponentPawnOnFile) {
			BitBoard ownSide = Piece.isWhite(type) ? whitePieces : blackPieces;
			pawnMask = ownSide.and(pieces[INDEX_PAWN]);
			// and no pawns of own side
			isHalfOpen = !pawnMask.and(fileMask).isNonZero();
		}
		return isHalfOpen;
	}
	
	public boolean moveCouldLeadToOwnKingDiscoveredCheck(Integer move) {
		int piece = Move.getOriginPiece(move);
		BitBoard king = (Piece.isWhite(piece)) ? getWhiteKing() : getBlackKing();
		
		if (king == null || king.getValue() == 0)  return false;
		
		int atSquare = Move.getOriginPosition(move);
		// establish if the square is on a multisquare slider mask from the king position
		BitBoard square = BitBoard.positionToMask_Lut[atSquare];
		int kingPosition = BitBoard.maskToPosition_Lut.get(king.getValue());
		BitBoard attackingSquares = directAttacksOnPosition_Lut[kingPosition];
		return square.and(attackingSquares).isNonZero();
	}
	
	private boolean isPromotionPawnBlocked(BitBoard pawns, Direction dir) {
		boolean potentialPromotion = false;
		PrimitiveIterator.OfInt iter = pawns.iterator();
		while (iter.hasNext()) {
			int pawn_bit = iter.nextInt();
			int pos = BitBoard.bitToPosition_Lut[pawn_bit];
			if (squareIsEmpty(Direction.getDirectMoveSq(dir, pos))) {
				potentialPromotion = true;
				break;
			}
		}
		return potentialPromotion;
	}
	
	public boolean isPromotionPossible(Colour onMove) {
		// TODO At the moment this doesn't consider if the pawn is pinned.
		boolean potentialPromotion = false;
		if (Piece.Colour.isWhite(onMove)) {
			BitBoard pawns = getWhitePawns().and(RankMask_Lut[IntRank.R7]);
			potentialPromotion = isPromotionPawnBlocked(pawns, Direction.up);
		} else {
			BitBoard pawns = getBlackPawns().and(RankMask_Lut[IntRank.R2]);
			potentialPromotion = isPromotionPawnBlocked(pawns, Direction.down);
		}
		return potentialPromotion;
	}
}
