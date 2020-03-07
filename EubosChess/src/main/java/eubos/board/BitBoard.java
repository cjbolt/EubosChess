package eubos.board;

import java.util.HashMap;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;

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
	
	static int[] positionToBit_Lut = new int[256];
	static {
		Integer bit_index = 0;
		for (int x88_square : Position.values) {
			positionToBit_Lut[x88_square] = bit_index;
			bit_index++;
		}
	}
	
	static BitBoard[] positionToMask_Lut = new BitBoard[256];
	static {
		Integer bit_index = 0;
		for (int x88_square : Position.values) {
			BitBoard atPosMask = new BitBoard(1L << bit_index);
			assert atPosMask != null;
			positionToMask_Lut[x88_square] = atPosMask;
			bit_index++;
		}
	}
	
	static Map<Long, Integer> maskToPosition_Lut = new HashMap<Long, Integer>();
	static {
		int atPos = 0;
		for (int bit_index=0; bit_index<64; bit_index++) {
			atPos = Position.values[bit_index];
			BitBoard atPosMask = new BitBoard(1L << bit_index);
			maskToPosition_Lut.put(atPosMask.getValue(), atPos);
		}
	}
	
	private int numBitsSet = 0;
	private long bitBoard = 0x0L;
	
	public BitBoard(long board) {
		bitBoard = board;
	}
	
	public BitBoard() { this(0); }
	
	public long getValue() {
		return bitBoard;
	}
	
	public boolean isNonZero() {
		return bitBoard != 0;
	}
	
	public void setNumBits() {
		numBitsSet = Long.bitCount(this.bitBoard);
	}
	
	public int getNumBits() {
		return numBitsSet;
	}
	
	public BitBoard and(BitBoard other) {
		return new BitBoard(this.bitBoard & other.bitBoard);
	}
	
	public void xor(BitBoard other) {
		this.bitBoard ^= other.bitBoard;
	}
	
	public BitBoard or(BitBoard other) {
		return new BitBoard(this.bitBoard | other.bitBoard);
	}

	public void set(int bit) {
		bitBoard |= (1L << bit);
	}
	
	public void set(long mask) {
		bitBoard |= mask;
	}
	
	public void clear(int bit) {
		bitBoard &= ~(1L << bit);
	}
	
	public void clear(long mask) {
		bitBoard &= ~mask;
	}
	
	private static int getBitFromRankAndFile(int rank, int file) {
		return(rank*8+file);
	}
	
	public boolean isSet(int rank, int file) {
		return this.isSet(getBitFromRankAndFile(rank,file));
	}
	
	public boolean isSet(int bit) {
		return (bitBoard & (1L<<bit)) != 0;
	}
	
	public boolean isSet(long mask) {
		return (bitBoard & mask) != 0;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		PrimitiveIterator.OfInt iter = this.iterator();
		while (iter.hasNext()) {
			int bit_index = iter.nextInt();
			int file = bit_index%8;
			int rank = bit_index/8;
			sb.append(GenericPosition.valueOf(IntFile.toGenericFile(file),IntRank.toGenericRank(rank)));
			sb.append(", ");
		}
		return sb.toString();
	}

	public class SetBitsIterator implements PrimitiveIterator.OfInt {

		private int [] setBitsIndexes = null;
		private int count = 0;
		private int next = 0;
	
		public SetBitsIterator() {
			setBitsIndexes = new int[64];
			long scratchBitBoard = bitBoard;
			while ( scratchBitBoard != 0x0L ) {
				setBitsIndexes[count++] = Long.numberOfTrailingZeros(scratchBitBoard);
				// clear the lssb
				scratchBitBoard &= scratchBitBoard-1;
			}
		}

		public boolean hasNext() {
			return next < setBitsIndexes.length && next < count;
		}

		public Integer next() {
			assert false; // use nextInt()
			return setBitsIndexes[next++];
		}

		@Override
		public void remove() {
		}

		@Override
		public void forEachRemaining(IntConsumer action) {
		}

		@Override
		public int nextInt() {
			return setBitsIndexes[next++];
		}
	}
	
	public PrimitiveIterator.OfInt iterator() {
		return new SetBitsIterator();
	}

	public boolean isZero() {
		return !isNonZero();
	}
}
