package eubos.board;

import java.util.PrimitiveIterator;

import eubos.position.Position;

import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.IntRank;

public final class BitBoard {
	
	private static final long not_a_file = 0xfefefefefefefefeL;
	private static final long not_h_file = 0x7f7f7f7f7f7f7f7fL;
	
	static final int[] bitToPosition_Lut = new int[64];
	static {
		int bit_index = 0;
		for (int square : Position.values) {
			bitToPosition_Lut[bit_index++] = square;
		}
	}
	
	static final long[] positionToMask_Lut = new long[128];
	static {
		int bit_index = 0;
		for (int x88_square : Position.values) {
			long atPosMask = 1L << bit_index;
			positionToMask_Lut[x88_square] = atPosMask;
			bit_index++;
		}
	}
			
	public static String toString(long bitBoard) {
		StringBuilder sb = new StringBuilder();
		PrimitiveIterator.OfInt iter = iterator(bitBoard);
		while (iter.hasNext()) {
			int bit_index = iter.nextInt();
			int file = bit_index%8;
			int rank = bit_index/8;
			sb.append(GenericPosition.valueOf(IntFile.toGenericFile(file),IntRank.toGenericRank(rank)));
			sb.append(", ");
		}
		return sb.toString();
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
}
