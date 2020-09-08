package eubos.board;

import java.util.PrimitiveIterator;

import eubos.position.Position;

import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.IntRank;

public class BitBoard {
	
	static int[] bitToPosition_Lut = new int[64];
	static {
		int bit_index = 0;
		for (int square : Position.values) {
			bitToPosition_Lut[bit_index++] = square;
		}
	}
	
	static long[] positionToMask_Lut = new long[128];
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
}
