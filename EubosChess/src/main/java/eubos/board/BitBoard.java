package eubos.board;

import java.util.PrimitiveIterator;

import eubos.board.Piece.Colour;
import eubos.position.Position;

import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.IntRank;

public final class BitBoard {
	
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
			mask |= getCaptureTargetMask(BitBoard.bitToPosition_Lut[bitOffset], isWhite, true);
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
			mask |= getCaptureTargetMask(BitBoard.bitToPosition_Lut[bitOffset], isWhite, false);
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
	static long getCaptureTargetMask(int position, boolean isWhite, boolean isLeft) {
		int targetPos = 0;
		if (isWhite) {
			if (isLeft) {
				targetPos = Piece.pawn_genLeftCaptureTargetWhite(position);
			} else {
				targetPos = Piece.pawn_genRightCaptureTargetWhite(position);
			}
		} else {
			if (isLeft) {
				targetPos = Piece.pawn_genLeftCaptureTargetBlack(position);
			} else {
				targetPos = Piece.pawn_genRightCaptureTargetBlack(position);
			}
		}
		if (targetPos != Position.NOPOSITION) {
			return BitBoard.positionToMask_Lut[targetPos];
		} else {
			return 0L;
		}
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

	public static int getRank(int bit) {
		return bit/8;
	}
	
	public static int getFile(int bit) {
		return bit%8;
	}
}
