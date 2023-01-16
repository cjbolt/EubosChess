package eubos.board;

import java.util.PrimitiveIterator;

import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.position.Position;

import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.IntRank;

public final class BitBoard {
	
	public static final int INVALID = 64;

	public static final boolean USE_SEAL_NUMBER_OF_TRAILING_ZEROS = false;
	public static final boolean USE_GAUD_NUMBER_OF_TRAILING_ZEROS = false;
	
	public static final int a1 = 0;   public static final int a2 = 8;
	public static final int b1 = 1;   public static final int b2 = 9;
	public static final int c1 = 2;   public static final int c2 = 10;
	public static final int d1 = 3;   public static final int d2 = 11;
	public static final int e1 = 4;   public static final int e2 = 12;
	public static final int f1 = 5;   public static final int f2 = 13;
	public static final int g1 = 6;   public static final int g2 = 14;
	public static final int h1 = 7;   public static final int h2 = 15;

	public static final int a3 = 16;  public static final int a4 = 24;
	public static final int b3 = 17;  public static final int b4 = 25;
	public static final int c3 = 18;  public static final int c4 = 26;
	public static final int d3 = 19;  public static final int d4 = 27;
	public static final int e3 = 20;  public static final int e4 = 28;
	public static final int f3 = 21;  public static final int f4 = 29;
	public static final int g3 = 22;  public static final int g4 = 30;
	public static final int h3 = 23;  public static final int h4 = 31;

	public static final int a5 = 32;  public static final int a6 = 40;
	public static final int b5 = 33;  public static final int b6 = 41;
	public static final int c5 = 34;  public static final int c6 = 42;
	public static final int d5 = 35;  public static final int d6 = 43;
	public static final int e5 = 36;  public static final int e6 = 44;
	public static final int f5 = 37;  public static final int f6 = 45;
	public static final int g5 = 38;  public static final int g6 = 46;
	public static final int h5 = 39;  public static final int h6 = 47;

	public static final int a7 = 48;  public static final int a8 = 56;
	public static final int b7 = 49;  public static final int b8 = 57;
	public static final int c7 = 50;  public static final int c8 = 58;
	public static final int d7 = 51;  public static final int d8 = 59;
	public static final int e7 = 52;  public static final int e8 = 60;
	public static final int f7 = 53;  public static final int f8 = 61;
	public static final int g7 = 54;  public static final int g8 = 62;
	public static final int h7 = 55;  public static final int h8 = 63;
	
	public static long valueOf(int [] positions) {
		long bitboard = 0L;
		for (int pos : positions) {
			bitboard |= positionToMask_Lut[pos];
		}
		return bitboard; 
	}
	
	private static final long not_a_file = 0xfefefefefefefefeL;
	private static final long not_h_file = 0x7f7f7f7f7f7f7f7fL;
	
	public static String toString(long board) {
		int spaceCounter = 0;
		StringBuilder sb = new StringBuilder();
		for (int rank=7; rank>=0; rank--) {
			for (int file=0; file<8; file++) {
				long currentBit = positionToMask_Lut[Position.valueOf(file,rank)];
				if ((currentBit & board) == currentBit) {
					if (spaceCounter != 0)
						sb.append(spaceCounter);
					sb.append('X');
					spaceCounter=0;					
				} else {
					spaceCounter++;
				}
			}
			if (spaceCounter != 0)
				sb.append(spaceCounter);
			if (rank != 0)
				sb.append('/');
			spaceCounter=0;
		}
		sb.append(" ");
		PrimitiveIterator.OfInt iter = iterator(board);
		while (iter.hasNext()) {
			int bit_index = iter.nextInt();
			int file = bit_index%8;
			int rank = bit_index/8;
			sb.append(GenericPosition.valueOf(IntFile.toGenericFile(file),IntRank.toGenericRank(rank)));
			sb.append(", ");
		}
		return sb.toString();
	}
	
	public static final int[] bitToPosition_Lut = new int[64];
	static {
		int bit_index = 0;
		for (int square : Position.values) {
			bitToPosition_Lut[bit_index++] = square;
		}
	}
	
	public static final int[] positionToBit_Lut = new int[128];
	static {
		int bit = 0;
		for (int square : Position.values) {
			positionToBit_Lut[square] = bit++;
		}
	}
	
	public static final long[] positionToMask_Lut = new long[128];
	static {
		int bit_index = 0;
		for (int x88_square : Position.values) {
			long atPosMask = 1L << bit_index;
			positionToMask_Lut[x88_square] = atPosMask;
			bit_index++;
		}
	}
	
	public static PrimitiveIterator.OfInt iterator(Long bitBoard) {
		return new SetBitsIterator(bitBoard);
	}
	
	public static PrimitiveIterator.OfLong maskIterator(Long bitBoard) {
		return new MaskIterator(bitBoard);
	}
	
	public static long upOccludedEmpty(long board, long empty) {
	   long flood = board;
	   flood |= board = (board << 8) & empty;
	   flood |= board = (board << 8) & empty;
	   flood |= board = (board << 8) & empty;
	   flood |= board = (board << 8) & empty;
	   flood |= board = (board << 8) & empty;
	   flood |= board = (board << 8) & empty;
	   flood |=         (board << 8) & empty;
	   return flood;
	}	
	
	public static long downOccludedEmpty(long board, long empty) {
	   long flood = board;
	   flood |= board = (board >>> 8) & empty;
	   flood |= board = (board >>> 8) & empty;
	   flood |= board = (board >>> 8) & empty;
	   flood |= board = (board >>> 8) & empty;
	   flood |= board = (board >>> 8) & empty;
	   flood |= board = (board >>> 8) & empty;
	   flood |=         (board >>> 8) & empty;
	   return flood;
	}
	
	public static long rightOccludedEmpty(long board, long empty) {
	   long flood = board;
	   empty &= not_a_file; // clear a file bits so that we can't overflow into the next rank
	   flood |= board = (board << 1) & empty;
	   flood |= board = (board << 1) & empty;
	   flood |= board = (board << 1) & empty;
	   flood |= board = (board << 1) & empty;
	   flood |= board = (board << 1) & empty;
	   flood |= board = (board << 1) & empty;
	   flood |=         (board << 1) & empty;
	   return flood;
	}
	
	public static long downRightOccludedEmpty(long board, long empty) {
	   long flood = board;
	   empty &= not_a_file; // clear a file bits so that we can't overflow into the next rank
	   flood |= board = (board >>> 7) & empty;
	   flood |= board = (board >>> 7) & empty;
	   flood |= board = (board >>> 7) & empty;
	   flood |= board = (board >>> 7) & empty;
	   flood |= board = (board >>> 7) & empty;
	   flood |= board = (board >>> 7) & empty;
	   flood |=         (board >>> 7) & empty;
	   return flood;
	}
	
	public static long upRightOccludedEmpty(long board, long empty) {
	   long flood = board;
	   empty &= not_a_file; // clear a file bits so that we can't overflow into the next rank
	   flood |= board = (board << 9) & empty;
	   flood |= board = (board << 9) & empty;
	   flood |= board = (board << 9) & empty;
	   flood |= board = (board << 9) & empty;
	   flood |= board = (board << 9) & empty;
	   flood |= board = (board << 9) & empty;
	   flood |=         (board << 9) & empty;
	   return flood;
	}
	
	public static long leftOccludedEmpty(long board, long empty) {
	   long flood = board;
	   empty &= not_h_file; // clear h file bits so that we can't overflow into the next rank
	   flood |= board = (board >>> 1) & empty;
	   flood |= board = (board >>> 1) & empty;
	   flood |= board = (board >>> 1) & empty;
	   flood |= board = (board >>> 1) & empty;
	   flood |= board = (board >>> 1) & empty;
	   flood |= board = (board >>> 1) & empty;
	   flood |=         (board >>> 1) & empty;
	   return flood;
	}
	
	public static long downLeftOccludedEmpty(long board, long empty) {
	   long flood = board;
	   empty &= not_h_file; // clear h file bits so that we can't overflow into the next rank
	   flood |= board = (board >>> 9) & empty;
	   flood |= board = (board >>> 9) & empty;
	   flood |= board = (board >>> 9) & empty;
	   flood |= board = (board >>> 9) & empty;
	   flood |= board = (board >>> 9) & empty;
	   flood |= board = (board >>> 9) & empty;
	   flood |=         (board >>> 9) & empty;
	   return flood;
	}
	
	public static long upLeftOccludedEmpty(long board, long empty) {
	   long flood = board;
	   empty &= not_h_file; // clear h file bits so that we can't overflow into the next rank
	   flood |= board = (board << 7) & empty;
	   flood |= board = (board << 7) & empty;
	   flood |= board = (board << 7) & empty;
	   flood |= board = (board << 7) & empty;
	   flood |= board = (board << 7) & empty;
	   flood |= board = (board << 7) & empty;
	   flood |=         (board << 7) & empty;
	   return flood;
	}
	
	public static long upAttacks(long board, long empty) {
		return upOccludedEmpty(board, empty) << 8;
	}
	
	public static long downAttacks(long board, long empty) {
		return downOccludedEmpty(board, empty) >>> 8;
	}
	
	public static long leftAttacks(long board, long empty) {
		return (leftOccludedEmpty(board, empty) >>> 1) & not_h_file;
	}
	
	public static long rightAttacks(long board, long empty) {
		return (rightOccludedEmpty(board, empty) << 1) & not_a_file;
	}
	
	public static long upLeftAttacks(long board, long empty) {
	   return (upLeftOccludedEmpty(board, empty) << 7) & not_h_file;
	}
	
	public static long upRightAttacks(long board, long empty) {
		return (upRightOccludedEmpty(board, empty) << 9) & not_a_file;
	}
	
	public static long downLeftAttacks(long board, long empty) {
		return (downLeftOccludedEmpty(board, empty) >>> 9) & not_h_file;
	}
	
	public static long downRightAttacks(long board, long empty) {
		return (downRightOccludedEmpty(board, empty) >>> 7) & not_a_file;
	}
	
	public static long upAttacks(long mobility) {
		return mobility << 8;
	}
	
	public static long downAttacks(long mobility) {
		return mobility >>> 8;
	}
	
	public static long leftAttacks(long mobility) {
		return (mobility >>> 1) & not_h_file;
	}
	
	public static long rightAttacks(long mobility) {
		return (mobility << 1) & not_a_file;
	}
	
	public static long upLeftAttacks(long mobility) {
	   return (mobility << 7) & not_h_file;
	}
	
	public static long upRightAttacks(long mobility) {
		return (mobility << 9) & not_a_file;
	}
	
	public static long downLeftAttacks(long mobility) {
		return (mobility >>> 9) & not_h_file;
	}
	
	public static long downRightAttacks(long mobility) {
		return (mobility >>> 7) & not_a_file;
	}
	
	static final long[] FileMask_Lut = new long[8];
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
	
	static final long[] RankMask_Lut = new long[8];
	static {
		for (int rank : IntRank.values) {
			RankMask_Lut[rank] = 0xFFL << rank*8;
		}
	}
	
	public static final long[][] PawnFrontSpan_Lut = new long[2][]; 
	static {
		long[] white_map = new long[64];
		PawnFrontSpan_Lut[Colour.white.ordinal()] = white_map;
		for (int bitOffset=0; bitOffset<64; bitOffset++) {
			white_map[bitOffset] = buildFrontSpanMask(BitBoard.getFile(bitOffset), BitBoard.getRank(bitOffset), true);
		}
		long[] black_map = new long[64];
		PawnFrontSpan_Lut[Colour.black.ordinal()] = black_map;
		for (int bitOffset=0; bitOffset<64; bitOffset++) {
			black_map[bitOffset] = buildFrontSpanMask(BitBoard.getFile(bitOffset), BitBoard.getRank(bitOffset), false);
		}
	}
	private static long buildFrontSpanMask(int f, int r, boolean isWhite) {
		long mask = 0;
		if (isWhite) {
			if (r!=7 && r!=0) {
				for (r=r+1; r <= 7; r++) {
					mask |= 1L << r*8+f;
				}
			}
		} else {
			if (r!=0 && r!=7) {
				for (r=r-1; r >= 0; r--) {
					mask |= 1L << r*8+f;	
				}
			}
		}
		return mask;
	}
	
	public static final long[][] PassedPawn_Lut = new long[2][]; 
	static {
		long[] white_map = new long[64];
		PassedPawn_Lut[Colour.white.ordinal()] = white_map;
		for (int bitOffset=0; bitOffset<64; bitOffset++) {
			white_map[bitOffset] = buildPassedPawnFileMask(BitBoard.getFile(bitOffset), BitBoard.getRank(bitOffset), true);
		}
		long[] black_map = new long[64];
		PassedPawn_Lut[Colour.black.ordinal()] = black_map;
		for (int bitOffset=0; bitOffset<64; bitOffset++) {
			black_map[bitOffset] = buildPassedPawnFileMask(BitBoard.getFile(bitOffset), BitBoard.getRank(bitOffset), false);
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
	
	// Dimensions bit offset, colour of attacker, direction
	static final long[][][] IterativePassedPawnUpdateCaptures_Lut = new long[64][2][]; 
	static {
		for (int bitOffset = 0; bitOffset < 64; bitOffset++) {
			IterativePassedPawnUpdateCaptures_Lut[bitOffset][0] = addBoardForCapturingPassedPawn(bitOffset, true);
			IterativePassedPawnUpdateCaptures_Lut[bitOffset][1] = addBoardForCapturingPassedPawn(bitOffset, false);
		}
	}
	private static long[] addBoardForCapturingPassedPawn(int bitOffset, boolean isWhite) {
		long[] masks = new long[2];
		// manage file transition of capturing pawn moves
		long mask = 0L;
		int origin_file = BitBoard.getFile(bitOffset);
		if (origin_file > 0) {
			// do left capture
			int target_file = origin_file - 1;
			if (target_file > 0) {
				int left_file_from_target = target_file - 1;
				mask |= BitBoard.FileMask_Lut[left_file_from_target];
			}
			if (origin_file < 7) {
				int right_file_from_origin = origin_file + 1;
				mask |= BitBoard.FileMask_Lut[right_file_from_origin];
			}
			mask |= getCaptureTargetMask(bitOffset, isWhite, true);
			masks[0] = mask;
		}
		mask = 0L;
		if (origin_file < 7) {
			// do right capture
			int target_file = origin_file + 1;
			if (target_file < 7) {
				int right_file_from_target = target_file + 1;
				mask |= BitBoard.FileMask_Lut[right_file_from_target];
			}
			if (origin_file > 0) {
				int left_file_from_origin = origin_file - 1;
				mask |= BitBoard.FileMask_Lut[left_file_from_origin];
			}
			mask |= getCaptureTargetMask(bitOffset, isWhite, false);
			masks[1] = mask;
		}
		// Mask off bits that are behind the pawn
		if (isWhite) {
			for (int i=0; i < BitBoard.getRank(bitOffset); i++) {
				masks[0] &= ~BitBoard.RankMask_Lut[i];
				masks[1] &= ~BitBoard.RankMask_Lut[i];
			}
		} else {
			
			for (int i=7; i > BitBoard.getRank(bitOffset); i--) {
				masks[0] &= ~BitBoard.RankMask_Lut[i];
				masks[1] &= ~BitBoard.RankMask_Lut[i];
			}
		}
		return masks;
	}
	static long getCaptureTargetMask(int bitOffset, boolean isWhite, boolean isLeft) {
		long targetMask = 0;
		if (isWhite) {
			if (isLeft) {
				targetMask = Piece.pawn_genLeftCaptureTargetWhite(bitOffset);
			} else {
				targetMask = Piece.pawn_genRightCaptureTargetWhite(bitOffset);
			}
		} else {
			if (isLeft) {
				targetMask = Piece.pawn_genLeftCaptureTargetBlack(bitOffset);
			} else {
				targetMask = Piece.pawn_genRightCaptureTargetBlack(bitOffset);
			}
		}
		return targetMask;
	}
	
	static final long[][] BackwardsPawn_Lut = new long[2][]; 
	static {
		long[] white_map = new long[64];
		BackwardsPawn_Lut[Colour.white.ordinal()] = white_map;
		int bitOffset = 0;
		for (int atPos : Position.values) {
			white_map[bitOffset++] = buildBackwardPawnFileMask(Position.getFile(atPos), Position.getRank(atPos), true);
		}
		long[] black_map = new long[64];
		bitOffset = 0;
		BackwardsPawn_Lut[Colour.black.ordinal()] = black_map;
		for (int atPos : Position.values) {
			black_map[bitOffset++] = buildBackwardPawnFileMask(Position.getFile(atPos), Position.getRank(atPos), false);
		}
	}
	private static long buildBackwardPawnFileMask(int f, int r, boolean isWhite) {
		long mask = 0;
		boolean hasPrevFile = IntFile.toGenericFile(f).hasPrev();
		boolean hasNextFile = IntFile.toGenericFile(f).hasNext();
		if (isWhite) {
			for (r=r-1; r > 0; r--) {
				mask = addRankForBackwardsPawnMask(mask, r, f, hasPrevFile, hasNextFile);
			}
		} else {
			for (r=r+1; r < 7; r++) {
				mask = addRankForBackwardsPawnMask(mask, r, f, hasPrevFile,	hasNextFile);	
			}
		}
		return mask;
	}
	private static long addRankForBackwardsPawnMask(long mask, int r, int f,
			boolean hasPrevFile, boolean hasNextFile) {
		if (hasPrevFile) {
			mask |= 1L << r*8+(f-1);
		}
		if (hasNextFile) {
			mask |= 1L << r*8+(f+1);
		}
		return mask;
	}
	
	static final long[] IsolatedPawn_Lut = new long[64];
	static {
		int bitOffset = 0;
		for (int atPos : Position.values) {
			IsolatedPawn_Lut[bitOffset++] = buildIsolatedPawnFileMask(Position.getFile(atPos));
		}
	}
	private static long buildIsolatedPawnFileMask(int f) {
		long mask = 0;
		boolean hasPrevFile = IntFile.toGenericFile(f).hasPrev();
		boolean hasNextFile = IntFile.toGenericFile(f).hasNext();
		if (hasPrevFile) {
			mask |= FileMask_Lut[f-1];
		}
		if (hasNextFile) {
			mask |= FileMask_Lut[f+1];
		}
		return mask;
	}
	
	// Dimensions colour, bit offset
	static final long[][] IterativePassedPawnNonCapture = new long[2][]; 
	static {
		long[] white_map = new long[64];
		for (int atPos : Position.values) {
			int rank = Position.getRank(atPos);
			if (rank >= 1 && rank <= 6) {
				long mask = 0L;
				int sq = Direction.getDirectMoveSq(Direction.upRight, atPos);
				if (sq != Position.NOPOSITION) {
					mask |= positionToMask_Lut[sq];
				}
				sq = Direction.getDirectMoveSq(Direction.up, atPos);
				if (sq != Position.NOPOSITION) {
					mask |= positionToMask_Lut[sq];
				} 
				sq = Direction.getDirectMoveSq(Direction.upLeft, atPos);
				if (sq != Position.NOPOSITION) {
					mask |= positionToMask_Lut[sq];
				}
				if (rank == 1) {
					long temp = mask;
					temp <<= 8; // Cater for double moves for white
					mask |= temp;
				}
				white_map[BitBoard.positionToBit_Lut[atPos]] = mask;
			}
		}
		IterativePassedPawnNonCapture[Colour.white.ordinal()] = white_map; 
		long[] black_map = new long[64];
		for (int atPos : Position.values) {
			int rank = Position.getRank(atPos);
			if (rank >= 2 && rank <= 6) { 
				long mask = 0L;
				int sq = Direction.getDirectMoveSq(Direction.downRight, atPos);
				if (sq != Position.NOPOSITION) {
					mask |= positionToMask_Lut[sq];
				}
				sq = Direction.getDirectMoveSq(Direction.down, atPos);
				if (sq != Position.NOPOSITION) {
					mask |= positionToMask_Lut[sq];
				} 
				sq = Direction.getDirectMoveSq(Direction.downLeft, atPos);
				if (sq != Position.NOPOSITION) {
					mask |= positionToMask_Lut[sq];
				} 
				if (rank == 6) {
					long temp = mask;
					mask >>= 8; // cater for double moves for black
					mask |= temp;
				}
				black_map[BitBoard.positionToBit_Lut[atPos]] = mask;
			}
		}
		IterativePassedPawnNonCapture[Colour.black.ordinal()] = black_map;
	}
	
	public static final long[][] PasserSupport_Lut = new long[2][];
	static {
		long[] white_map = new long[64];
		for (int bitOffset=0; bitOffset<64; bitOffset++) {
			white_map[bitOffset] = BitBoard.PassedPawn_Lut[0][bitOffset] & ~BitBoard.PawnFrontSpan_Lut[0][bitOffset];
		}
		PasserSupport_Lut[Colour.white.ordinal()] = white_map;
		long[] black_map = new long[64];
		for (int bitOffset=0; bitOffset<64; bitOffset++) {
			black_map[bitOffset] = BitBoard.PassedPawn_Lut[1][bitOffset] & ~BitBoard.PawnFrontSpan_Lut[1][bitOffset];
		}
		PasserSupport_Lut[Colour.black.ordinal()] = black_map;
	}
	static long createAdjacentPawnsFromSq(int atPos) {
		long mask = 0;
		if (Position.getRank(atPos) != 7 && Position.getRank(atPos) != 0) {
			int sq = Direction.getDirectMoveSq(Direction.right, atPos);
			if (sq != Position.NOPOSITION) {
				mask |= BitBoard.positionToMask_Lut[sq];
			}
			sq = Direction.getDirectMoveSq(Direction.left, atPos);
			if (sq != Position.NOPOSITION) {
				mask |= BitBoard.positionToMask_Lut[sq];
			}
		}
		return mask;
	}

	public static byte getRank(int bit) {
		return (byte) (bit >> 3);
	}
	
	public static byte getFile(int bit) {
		return (byte) (bit & 0x7);
	}
	
	public static int bitValueOf(int file, int rank) {
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert file >= 0 && file < 8;
			assert rank >= 0 && rank < 8;
		}
		return (rank << 3) | file;
	}
	
	public static int[][] ManhattanDistance;
	static {
		ManhattanDistance = new int[64][64];
		int bit1 = 0;
		for (int sq1 : Position.values) {
			int bit2 = 0;
			for (int sq2 : Position.values) {
				ManhattanDistance[bit1][bit2++] = precalcDistance(sq1, sq2);
			}
			bit1++;
		}
	}
	static int precalcDistance(int sq1, int sq2) {
	   int file1, file2, rank1, rank2;
	   int rankDistance, fileDistance;
	   file1 = sq1  & 7;
	   file2 = sq2  & 7;
	   rank1 = sq1 >> 4;
	   rank2 = sq2 >> 4;
	   rankDistance = Math.abs(rank2 - rank1);
	   fileDistance = Math.abs(file2 - file1);
	   return Math.max(rankDistance, fileDistance);
	}
	
	static long generatePawnCaptureTargetBoardUpRight(int bitOffset) {
		return ((1L << bitOffset) << 9) & not_a_file;
	}
	
	static long generatePawnCaptureTargetBoardUpLeft(int bitOffset) {
 		return ((1L << bitOffset) << 7) & not_h_file;
	}
	
	static long generatePawnCaptureTargetBoardDownRight(int bitOffset) {
		return ((1L << bitOffset) >>> 7) & not_a_file;
	}
	
	static long generatePawnCaptureTargetBoardDownLeft(int bitOffset) {
		return ((1L << bitOffset) >>> 9) & not_h_file;
	}
	
    private static final int table[] = {
    	32, 0, 1, 12, 2, 6, -1, 13, 3, -1, 7, -1, -1, -1, -1, 14,
    	10, 4, -1, -1, 8, -1, -1, 25, -1, -1, -1, -1, -1, 21, 27, 15,
    	31, 11, 5, -1, -1, -1, -1 ,-1, 9, -1, -1, 24, -1, -1, 20, 26,
    	30, -1, -1, -1, -1, 23, -1, 19, 29, -1, 22, 18, 28, 17, 16, -1
    };
    
    public static int convertToBitOffset(long x) {
    	if (USE_SEAL_NUMBER_OF_TRAILING_ZEROS) {
	    	int offset = 0;
	    	int y=0;
	    	if ((x & 0xFFFF_FFFFL) == 0) {
	    		y = (int)(x >>> 32);
	    		if (y == 0x8000_0000) return 63; // deal with sign bit
	 			offset = 32;
	    	} else {
	    		y = (int) x;
	    	}
	    	y = (y & -y) * 0x0450FBAF;
	        return table[y >>> 26] + offset;
    	} else if (USE_GAUD_NUMBER_OF_TRAILING_ZEROS) {
    		long y, bz, b5, b4, b3, b2, b1, b0;
    		y = x & -x;
    		bz = y != 0 ? 0 : 1;
    		b5 = ((y & 0x0000_0000_FFFF_FFFFL) != 0) ? 0 : 32;
    		b4 = ((y & 0x0000_FFFF_0000_FFFFL) != 0) ? 0 : 16;
    		b3 = ((y & 0x00FF_00FF_00FF_00FFL) != 0) ? 0 : 8;
    		b2 = ((y & 0x0F0F_0F0F_0F0F_0F0FL) != 0) ? 0 : 4;
    		b1 = ((y & 0x3333_3333_3333_3333L) != 0) ? 0 : 2;
    		b0 = ((y & 0x5555_5555_5555_5555L) != 0) ? 0 : 1;
    		return (int)(bz + b5 + b4 + b3 + b2 + b1 + b0);
    	} else {
    		return Long.numberOfTrailingZeros(x);
    	}
    }
}
