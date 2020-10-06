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

class MobilityMask {
	public long mask = 0;
	public int squares = 0;
}

public class Board {
	public static final CaptureData NULL_CAPTURE = new CaptureData(Piece.NONE, Position.NOPOSITION, false);
	
	private long allPieces = 0x0;
	private long whitePieces = 0x0;
	private long blackPieces = 0x0;
	
	public long getWhitePieces() {
		return whitePieces;
	}
	public long getBlackPieces() {
		return blackPieces;
	}

	private static final int INDEX_PAWN = Piece.PAWN;
	private static final int INDEX_KNIGHT = Piece.KNIGHT;
	private static final int INDEX_BISHOP = Piece.BISHOP;
	private static final int INDEX_ROOK = Piece.ROOK;
	private static final int INDEX_QUEEN = Piece.QUEEN;
	private static final int INDEX_KING = Piece.KING;
	//private static final int INDEX_NONE = Piece.NONE;
	
	private long[] pieces = new long[7]; // N.b. INDEX_NONE is an empty long at index 0.
	
	public boolean isInsufficientMaterial() {
		// Major pieces
		if (pieces[Piece.QUEEN] != 0)
			return false;
		if (pieces[Piece.ROOK] != 0)
			return false;
		// Possible promotions
		if (pieces[Piece.PAWN] != 0)
			return false;
		
		// Minor pieces
		int numWhiteBishops = Long.bitCount(getWhiteBishops());
		int numWhiteKnights = Long.bitCount(getWhiteKnights());
		int numBlackBishops = Long.bitCount(getBlackBishops());
		int numBlackKnights = Long.bitCount(getBlackKnights());
		
		if (numWhiteBishops >= 2 || numBlackBishops >= 2) {
			// One side has at least two bishops
			return false;
		}
		if ((numWhiteBishops == 1 && numWhiteKnights >= 1) ||
		    (numBlackBishops == 1 && numBlackKnights >= 1))
			// One side has Knight and Bishop
			return false;
		
		// else insufficient
		return true;
	}
	
	public boolean isInsufficientMaterial(Piece.Colour side) {
		long ownBitBoard =  Colour.isWhite(side) ? whitePieces : blackPieces;
		// Major pieces
		if ((pieces[Piece.QUEEN] & ownBitBoard) != 0)
			return false;
		if ((pieces[Piece.ROOK] & ownBitBoard) != 0)
			return false;
		// Possible promotions
		if ((pieces[Piece.PAWN] & ownBitBoard) != 0)
			return false;
		
		// Minor pieces
		int numBishops = Long.bitCount((pieces[Piece.BISHOP] & ownBitBoard));
		int numKnights = Long.bitCount((pieces[Piece.KNIGHT] & ownBitBoard));
		
		if (numBishops >= 2) {
			// side has at least two bishops
			return false;
		}
		if (numBishops == 1 && numKnights >= 1)
			// side has Knight and Bishop
			return false;
		
		// else insufficient
		return true;
	}
	
	@SuppressWarnings("unchecked")
	private static final List<MobilityMask>[] RankFileMask_Lut = (List<MobilityMask>[]) new List[128];
	static {
		Direction [] rankFile = { Direction.left, Direction.up, Direction.right, Direction.down };
		for (int square : Position.values) {
			List<MobilityMask> array = new ArrayList<MobilityMask>();
			for (int index=1; index<8; index++) {
				createMask(square, array, index, rankFile);
			}
			RankFileMask_Lut[square] = array;
		}
	}
	static private void createMask(int square, List<MobilityMask> array, int index, Direction [] directions) {
		MobilityMask currMask = new MobilityMask();
		for (Direction dir: directions) {
			setAllInDirection(dir, square, currMask, index);
		}
		// Clear the central bit
		currMask.mask &= ~BitBoard.positionToMask_Lut[square];
		currMask.squares = Long.bitCount(currMask.mask);
		// Only add the mask if it isn't the same as previous (i.e. no more squares to add)
		if (array.size()-1 >= 0) {
			if (currMask.mask != array.get(array.size()-1).mask)
				array.add(currMask);
		} else {
			array.add(currMask);
		}
	}
	static private void setAllInDirection(Direction dir, int fromSq, MobilityMask currMask, int index) {
		int newSquare = fromSq;
		for (int i=0; i < index; i++) {
			if (newSquare != Position.NOPOSITION)
				newSquare = Direction.getDirectMoveSq(dir, newSquare);
			if (newSquare != Position.NOPOSITION)
				currMask.mask |= BitBoard.positionToMask_Lut[newSquare];
		}
	}
	
	private static final long[] directAttacksOnPosition_Lut = new long[128];
	static {
		Direction [] allDirect = { Direction.left, Direction.up, Direction.right, Direction.down, Direction.downLeft, Direction.upLeft, Direction.upRight, Direction.downRight };
		for (int square : Position.values) {
			MobilityMask allAttacksMask = new MobilityMask();
			for (Direction dir: allDirect) {
				setAllInDirection(dir, square, allAttacksMask, 8);
			}
			directAttacksOnPosition_Lut[square] = allAttacksMask.mask;
		}
	}
	
	@SuppressWarnings("unchecked")
	private static final List<MobilityMask>[] DiagonalMask_Lut = (List<MobilityMask>[]) new List[128];
	static {
		Direction [] diagonals = { Direction.downLeft, Direction.upLeft, Direction.upRight, Direction.downRight };
		for (int square : Position.values) {
			List<MobilityMask> array = new ArrayList<MobilityMask>();
			for (int index=1; index<8; index++) {
				createMask(square, array, index, diagonals);
			}
			DiagonalMask_Lut[square] = array;
		}
	}
	
	private static final long[] FileMask_Lut = new long[8];
	static {
		for (int file : IntFile.values) {
			long mask = 0;
			int f=file;
			for (int r = 0; r<8; r++) {
				mask  |= 1L << r*8+f;
			}
			FileMask_Lut[file]= mask;
		}
	}
	
	private static final long[] RankMask_Lut = new long[8];
	static {
		for (int r : IntRank.values) {
			long mask = 0;
			for (int f = 0; f<8; f++) {
				mask  |= 1L << r*8+f;
			}
			RankMask_Lut[r] = mask;
		}
	}
	
	public Board( Map<Integer, Integer> pieceMap ) {
		allPieces = 0x0;
		whitePieces = 0x0;
		blackPieces = 0x0;
		for (int i=0; i<=INDEX_PAWN; i++) {
			pieces[i] = 0x0;
		}
		for ( Entry<Integer, Integer> nextPiece : pieceMap.entrySet() ) {
			setPieceAtSquare( nextPiece.getKey(), nextPiece.getValue() );
		}
	}
	
	public CaptureData doMove(int move) throws InvalidPieceException {
		CaptureData captureTarget = NULL_CAPTURE;
		int pieceToMove = Move.getOriginPiece(move);
		int targetSquare = Move.getTargetPosition(move);
		int targetPiece = Move.getTargetPiece(move);
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
			captureTarget = new CaptureData(pickUpPieceAtSquare(capturePos), capturePos, true);
		} else {
			// handle castling, setting en passant etc
			if (checkToSetEnPassantTargetSq(move) == IntFile.NOFILE) {
				// Handle castling secondary rook moves...
				if (Piece.isKing(pieceToMove)) {
					performSecondaryCastlingMove(move);
				}
				if (targetPiece != Piece.NONE) {
					captureTarget = new CaptureData(pickUpPieceAtSquare(targetSquare, targetPiece), targetSquare, false);
				}
			}			
		}
		movePiece(move);
		return captureTarget;
	}
	
	private void movePiece(int move) {
		int pieceToMove = Move.getOriginPiece(move);
		long initialSquareMask = BitBoard.positionToMask_Lut[Move.getOriginPosition(move)];
		long targetSquareMask = BitBoard.positionToMask_Lut[Move.getTargetPosition(move)];
		long positionsMask = initialSquareMask | targetSquareMask;
		// Switch piece-specific bitboards
		int promotedChessman = Move.getPromotion(move);
		if (promotedChessman != IntChessman.NOCHESSMAN) {
			// For a promotion, need to resolve piece-specific across multiple bitboards; can't be a king, sorted in order of likeliness.
			if ((pieces[INDEX_PAWN] & initialSquareMask) == initialSquareMask) {
				pieces[INDEX_PAWN] &= ~initialSquareMask;
			} else {
				switch(promotedChessman) {
				case IntChessman.KNIGHT:
					pieces[INDEX_KNIGHT] &= ~initialSquareMask;
					break;
				case IntChessman.BISHOP:
					pieces[INDEX_BISHOP] &= ~initialSquareMask;
					break;
				case IntChessman.ROOK:
					pieces[INDEX_ROOK] &= ~initialSquareMask;
					break;
				case IntChessman.QUEEN:
					pieces[INDEX_QUEEN] &= ~initialSquareMask;
					break;
				default:
					assert false;
					break;			
				}
			}
			pieces[pieceToMove & Piece.PIECE_NO_COLOUR_MASK] |= targetSquareMask;
		} else {
			// Piece type doesn't change across boards
			pieces[Piece.PIECE_NO_COLOUR_MASK & pieceToMove] ^= positionsMask;
		}
		// Switch colour bitboard
		if (Piece.isWhite(pieceToMove)) {
			whitePieces ^= positionsMask;
		} else {
			blackPieces ^= positionsMask;
		}
		// Switch all pieces bitboard
		allPieces ^= positionsMask;
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
	
	private static final long wksc_mask = BitBoard.positionToMask_Lut[Position.h1] | BitBoard.positionToMask_Lut[Position.f1];
	private static final long wqsc_mask = BitBoard.positionToMask_Lut[Position.a1] | BitBoard.positionToMask_Lut[Position.d1];
	private static final long bksc_mask = BitBoard.positionToMask_Lut[Position.h8] | BitBoard.positionToMask_Lut[Position.f8];
	private static final long bqsc_mask = BitBoard.positionToMask_Lut[Position.a8] | BitBoard.positionToMask_Lut[Position.d8];
	
	private void performSecondaryCastlingMove(int move) throws InvalidPieceException {
		if (Move.areEqual(move, CastlingManager.wksc)) {
			pieces[INDEX_ROOK] ^= (wksc_mask);
			whitePieces ^= (wksc_mask);
			allPieces ^= (wksc_mask);
		} else if (Move.areEqual(move, CastlingManager.wqsc)) {
			pieces[INDEX_ROOK] ^= (wqsc_mask);
			whitePieces ^= (wqsc_mask);
			allPieces ^= (wqsc_mask);
		} else if (Move.areEqual(move, CastlingManager.bksc)) {
			pieces[INDEX_ROOK] ^= (bksc_mask);
			blackPieces ^= (bksc_mask);
			allPieces ^= (bksc_mask);
		} else if (Move.areEqual(move, CastlingManager.bqsc)) {
			pieces[INDEX_ROOK] ^= (bqsc_mask);
			blackPieces ^= (bqsc_mask);
			allPieces ^= (bqsc_mask);
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
			pieces[INDEX_ROOK] ^= (wksc_mask);
			whitePieces ^= (wksc_mask);
			allPieces ^= (wksc_mask);
		} else	if (Move.areEqual(move, CastlingManager.undo_wqsc)) {
			pieces[INDEX_ROOK] ^= (wqsc_mask);
			whitePieces ^= (wqsc_mask);
			allPieces ^= (wqsc_mask);
		} else if (Move.areEqual(move, CastlingManager.undo_bksc)) {
			pieces[INDEX_ROOK] ^= (bksc_mask);
			blackPieces ^= (bksc_mask);
			allPieces ^= (bksc_mask);
		} else if (Move.areEqual(move, CastlingManager.undo_bqsc)) {
			pieces[INDEX_ROOK] ^= (bqsc_mask);
			blackPieces ^= (bqsc_mask);
			allPieces ^= (bqsc_mask);
		}
	}
	
	public List<Integer> getRegularPieceMoves(Piece.Colour side) {
		long bitBoardToIterate = Colour.isWhite(side) ? whitePieces : blackPieces;
		ArrayList<Integer> movesList = new ArrayList<Integer>(60);
		
		PrimitiveIterator.OfInt iter = BitBoard.iterator(bitBoardToIterate);
		while (iter.hasNext()) {
			int bit_index = iter.nextInt();
			int atSquare = BitBoard.bitToPosition_Lut[bit_index];
			long mask = 1L<<bit_index;
			// for efficiency sorted in order of frequency on the board
			if ((pieces[INDEX_PAWN] & mask) == mask) {
				movesList.addAll(Piece.pawn_generateMoves(this, atSquare, side));
			} else if ((pieces[INDEX_ROOK] & mask) == mask) {
				movesList.addAll(Piece.rook_generateMoves(this, atSquare, side));
			} else if ((pieces[INDEX_BISHOP] & mask) == mask) {
				movesList.addAll(Piece.bishop_generateMoves(this, atSquare, side));
			} else if ((pieces[INDEX_KNIGHT] & mask) == mask) {
				movesList.addAll(Piece.knight_generateMoves(this, atSquare, side));
			} else if ((pieces[INDEX_KING] & mask) == mask) {
				movesList.addAll(Piece.king_generateMoves(this, atSquare, side));
			} else if ((pieces[INDEX_QUEEN] & mask) == mask) {
				movesList.addAll(Piece.queen_generateMoves(this, atSquare, side));
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
		return (allPieces & BitBoard.positionToMask_Lut[atPos]) == 0;		
	}
	
	public boolean squareIsAttacked( int atPos, Piece.Colour ownColour ) {
		return SquareAttackEvaluator.isAttacked(this, atPos, ownColour);
	}
	
	public int getPieceAtSquare( int atPos ) {
		int type = Piece.NONE;
		long pieceToGet = BitBoard.positionToMask_Lut[atPos];;
		if ((allPieces & pieceToGet) != 0) {	
			if ((blackPieces & pieceToGet) != 0) {
				type |= Piece.BLACK;
			} else assert (whitePieces & pieceToGet) != 0;
			// Sorted in order of frequency of piece on the chess board, for efficiency
			if ((pieces[INDEX_PAWN] & pieceToGet) == pieceToGet) {
				type |= Piece.PAWN;
			} else if ((pieces[INDEX_ROOK] & pieceToGet) == pieceToGet) {
				type |= Piece.ROOK;
			} else if ((pieces[INDEX_BISHOP] & pieceToGet) == pieceToGet) {
				type |= Piece.BISHOP;
			} else if ((pieces[INDEX_KNIGHT] & pieceToGet) == pieceToGet) {
				type |= Piece.KNIGHT;
			} else if ((pieces[INDEX_KING] & pieceToGet) == pieceToGet) {
				type |= Piece.KING;
			} else if ((pieces[INDEX_QUEEN] & pieceToGet) == pieceToGet) {
				type |= Piece.QUEEN;
			}
		}
		return type;
	}
	
	public void setPieceAtSquare( int atPos, int pieceToPlace ) {
		assert pieceToPlace != Piece.NONE;
		long mask = BitBoard.positionToMask_Lut[atPos];
		// Set on piece-specific bitboard
		pieces[pieceToPlace & Piece.PIECE_NO_COLOUR_MASK] |= mask;
		// Set on colour bitboard
		if (Piece.isBlack(pieceToPlace)) {
			blackPieces |= (mask);
		} else {
			whitePieces |= (mask);
		}
		// Set on all pieces bitboard
		allPieces |= (mask);
	}
	
	public boolean isKingInCheck(Piece.Colour side) {
		boolean inCheck = false;
		long getFromBoard = Colour.isWhite(side) ? whitePieces : blackPieces;
		long kingMask = getFromBoard & pieces[INDEX_KING];
		if (kingMask != 0) {
			// The conditional is needed because some unit test positions don't have a king...
			int kingSquare = BitBoard.bitToPosition_Lut[Long.numberOfTrailingZeros(kingMask)];
			inCheck = squareIsAttacked(kingSquare, side);
		}
		return inCheck;
	}
	
	public int pickUpPieceAtSquare( int atPos ) {
		int type = Piece.NONE;
		long pieceToPickUp = BitBoard.positionToMask_Lut[atPos];
		if ((allPieces & pieceToPickUp) != 0) {	
			// Remove from relevant colour bitboard
			if ((blackPieces & pieceToPickUp) != 0) {
				blackPieces &= ~pieceToPickUp;
				type |= Piece.BLACK;
			} else {
				assert (whitePieces & pieceToPickUp) != 0;
				whitePieces &= ~pieceToPickUp;
			}
			// Remove from specific-piece bitboard
			if ((pieces[INDEX_KING] & pieceToPickUp) == pieceToPickUp) {
				pieces[INDEX_KING] &= ~pieceToPickUp;
				type |= Piece.KING;
			} else if ((pieces[INDEX_QUEEN] & pieceToPickUp) == pieceToPickUp) {
				pieces[INDEX_QUEEN] &= ~pieceToPickUp;
				type |= Piece.QUEEN;
			} else if ((pieces[INDEX_ROOK] & pieceToPickUp) == pieceToPickUp) {
				pieces[INDEX_ROOK] &= ~pieceToPickUp;
				type |= Piece.ROOK;
			} else if ((pieces[INDEX_BISHOP] & pieceToPickUp) == pieceToPickUp) {
				pieces[INDEX_BISHOP] &= ~pieceToPickUp;
				type |= Piece.BISHOP;
			} else if ((pieces[INDEX_KNIGHT] & pieceToPickUp) == pieceToPickUp) {
				pieces[INDEX_KNIGHT] &= ~pieceToPickUp;
				type |= Piece.KNIGHT;
			} else if ((pieces[INDEX_PAWN] & pieceToPickUp) == pieceToPickUp) {
				pieces[INDEX_PAWN] &= ~pieceToPickUp;
				type |= Piece.PAWN;
			}
			// Remove from all pieces bitboard
			allPieces &= ~pieceToPickUp;
		}
		return type;
	}
	
	public int pickUpPieceAtSquare( int atPos, int piece ) {
		long pieceToPickUp = BitBoard.positionToMask_Lut[atPos];
		if ((allPieces & pieceToPickUp) != 0) {	
			// Remove from relevant colour bitboard
			if (Piece.isBlack(piece)) {
				blackPieces &= ~pieceToPickUp;
			} else {
				whitePieces &= ~pieceToPickUp;
			}
			// remove from specific bitboard
			pieces[piece & Piece.PIECE_NO_COLOUR_MASK] &= ~pieceToPickUp;
			// Remove from all pieces bitboard
			allPieces &= ~pieceToPickUp;
		} else {
			piece = Piece.NONE;
		}
		return piece;
	}
	
	public int countDoubledPawnsForSide(Colour side) {
		int doubledCount = 0;
		long pawns = Colour.isWhite(side) ? getWhitePawns() : getBlackPawns();
		for (int file : IntFile.values) {
			long mask = FileMask_Lut[file];
			long fileMask = pawns & mask;
			int numPawnsInFile = Long.bitCount(fileMask);
			if (numPawnsInFile > 1) {
				doubledCount += numPawnsInFile-1;
			}
		}
		return doubledCount;
	}
	
	public boolean isPassedPawn(int atPos, Colour side) {
		boolean isPassed = true;
		long mask = PassedPawn_Lut[side.ordinal()][atPos];
		long otherSidePawns = Colour.isWhite(side) ? getBlackPawns() : getWhitePawns();
		if ((mask & otherSidePawns) != 0) {
			isPassed  = false;
		}
		return isPassed;
	}
	
	private static final long[][] PassedPawn_Lut = new long[2][]; 
	static {
		long[] white_map = new long[128];
		PassedPawn_Lut[Colour.white.ordinal()] = white_map;
		for (int atPos : Position.values) {
			white_map[atPos] = buildPassedPawnFileMask(Position.getFile(atPos), Position.getRank(atPos), true);
		}
		long[] black_map = new long[128];
		PassedPawn_Lut[Colour.black.ordinal()] = black_map;
		for (int atPos : Position.values) {
			black_map[atPos] = buildPassedPawnFileMask(Position.getFile(atPos), Position.getRank(atPos), false);
		}
	}
	private static long buildPassedPawnFileMask(int f, int r, boolean isWhite) {
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
		return mask;
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
			long bitBoardToIterate;
			if (typeToIterate == Piece.WHITE_PAWN) {
				bitBoardToIterate = getWhitePawns();
			} else if (typeToIterate == Piece.BLACK_PAWN) {
				bitBoardToIterate = getBlackPawns();
			} else {
				bitBoardToIterate = 0x0;
			}
			buildIterList(bitBoardToIterate);
		}

		private void buildIterList(long bitBoardToIterate) {
			PrimitiveIterator.OfInt iter = BitBoard.iterator(bitBoardToIterate);
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
	
	public long getMaskForType(int type) {
		long mask = 0x0;
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
		
	public long getBlackPawns() {
		return blackPieces & (pieces[INDEX_PAWN]);
	}
	
	public long getBlackKnights() {
		return blackPieces & (pieces[INDEX_KNIGHT]);
	}
	
	public long getBlackBishops() {
		return blackPieces & (pieces[INDEX_BISHOP]);
	}
	
	public long getBlackRooks() {
		return blackPieces & (pieces[INDEX_ROOK]);
	}
	
	public long getBlackQueens() {
		return blackPieces & (pieces[INDEX_QUEEN]);
	}
	
	public long getBlackKing() {
		return blackPieces & (pieces[INDEX_KING]);
	}
	
	public long getWhitePawns() {
		return whitePieces & (pieces[INDEX_PAWN]);
	}
	
	public long getWhiteBishops() {
		return whitePieces & (pieces[INDEX_BISHOP]);
	}
	
	public long getWhiteRooks() {
		return whitePieces & (pieces[INDEX_ROOK]);
	}
	
	public long getWhiteQueens() {
		return whitePieces & (pieces[INDEX_QUEEN]);
	}
	
	public long getWhiteKnights() {
		return whitePieces & (pieces[INDEX_KNIGHT]);
	}
	
	public long getWhiteKing() {
		return whitePieces & (pieces[INDEX_KING]);
	}
	
	public PrimitiveIterator.OfInt iterateType( int typeToIterate ) {
		try {
			return new allPiecesOnBoardIterator( typeToIterate );
		} catch (InvalidPieceException e) {
			return null;
		}
	}

	public int getNumRankFileSquaresAvailable(int atPos) {
		return getSquaresAvailableFromPosition(atPos, RankFileMask_Lut);
	}
	
	public int getNumDiagonalSquaresAvailable(int atPos) {
		return getSquaresAvailableFromPosition(atPos, DiagonalMask_Lut);
	}
	
	private int getSquaresAvailableFromPosition(int atPos, List<MobilityMask>[] maskMap ) {
		int squaresCount = 0;
		List<MobilityMask> list = maskMap[atPos];
		for (MobilityMask levelMask : list) {
			if ((allPieces & levelMask.mask) == 0) {
				squaresCount = levelMask.squares;
			} else {
				break;
			}
		}
		return squaresCount;
	}
	
	public boolean isOnHalfOpenFile(GenericPosition atPos, int type) {
		boolean isHalfOpen = false;
		long fileMask = FileMask_Lut[IntFile.valueOf(atPos.file)];
		long otherSide = Piece.getOpposite(type) == Colour.white ? whitePieces : blackPieces;
		long pawnMask = otherSide & (pieces[INDEX_PAWN]);
		boolean opponentPawnOnFile = (pawnMask & fileMask) != 0;
		if (opponentPawnOnFile) {
			long ownSide = Piece.isWhite(type) ? whitePieces : blackPieces;
			pawnMask = ownSide & (pieces[INDEX_PAWN]);
			// and no pawns of own side
			isHalfOpen = !((pawnMask & fileMask) != 0);
		}
		return isHalfOpen;
	}
	
	public boolean moveCouldLeadToOwnKingDiscoveredCheck(Integer move) {
		int piece = Move.getOriginPiece(move);
		long king = (Piece.isWhite(piece)) ? getWhiteKing() : getBlackKing();
		
		if (king == 0)  return false;
		
		int kingPosition = BitBoard.bitToPosition_Lut[Long.numberOfTrailingZeros(king)];
		
		return moveCouldLeadToDiscoveredCheck(move, kingPosition);
	}
	
	public boolean moveCouldPotentiallyCheckOtherKing(Integer move) {
		boolean isPotentialCheck = false;
		int piece = Move.getOriginPiece(move);
		long king = (Piece.isBlack(piece)) ? getWhiteKing() : getBlackKing();
		
		if (king == 0)  return false;
		
		int kingPosition = BitBoard.bitToPosition_Lut[Long.numberOfTrailingZeros(king)];
		if (moveCouldLeadToDiscoveredCheck(move, kingPosition)) {
			// Could be a discovered check, so search further
			isPotentialCheck = true;
		} else {
			int targetSquare = Move.getTargetPosition(move);
			long targetMask = BitBoard.positionToMask_Lut[targetSquare];
			
			// Establish if target square puts attacker onto a king attack square
			if ((targetMask & SquareAttackEvaluator.allAttacksOnPosition_Lut[kingPosition]) != 0) {
				// Could be either a direct or indirect attack on the King, so search further
				isPotentialCheck = true;
			}
		}
		return isPotentialCheck;
	}
	
	private boolean moveCouldLeadToDiscoveredCheck(Integer move, int kingPosition) {
		int atSquare = Move.getOriginPosition(move);
		// Establish if the initial square is on a multiple square slider mask from the king position
		long square = BitBoard.positionToMask_Lut[atSquare];
		long attackingSquares = directAttacksOnPosition_Lut[kingPosition];
		return ((square & attackingSquares) != 0);
	}
	
	private boolean isPromotionPawnBlocked(long pawns, Direction dir) {
		boolean potentialPromotion = false;
		PrimitiveIterator.OfInt iter = BitBoard.iterator(pawns);
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
			long pawns = getWhitePawns() & (RankMask_Lut[IntRank.R7]);
			potentialPromotion = isPromotionPawnBlocked(pawns, Direction.up);
		} else {
			long pawns = getBlackPawns() & (RankMask_Lut[IntRank.R2]);
			potentialPromotion = isPromotionPawnBlocked(pawns, Direction.down);
		}
		return potentialPromotion;
	}
}
