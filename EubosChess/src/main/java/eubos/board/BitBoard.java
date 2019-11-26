package eubos.board;

import java.util.Iterator;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.IntRank;

public class BitBoard implements Iterable<Integer> {
	
	private long bitBoard = 0x0L;
	
	public BitBoard(long board) {
		bitBoard = board;
	}
	
	public BitBoard() { this(0); }
	
	public long getSquareOccupied() {
		return bitBoard;
	}
	
	private static int getBitFromRankAndFile(int rank, int file) {
		return(rank*8+file);
	}
	
	public BitBoard and(BitBoard other) {
		return new BitBoard(this.bitBoard & other.bitBoard);
	}

	public void set(int rank, int file) {
		this.set(getBitFromRankAndFile(rank,file));
	}
	
	public void set(int bit) {
		bitBoard |= (1L << bit);
	}
	
	public void clear(int rank, int file) {
		this.clear(getBitFromRankAndFile(rank,file));
	}
	
	public void clear(int bit) {
		bitBoard &= ~(1L << bit);
	}
	
	public boolean isSet(int rank, int file) {
		return this.isSet(getBitFromRankAndFile(rank,file));
	}
	
	public boolean isSet(int bit) {
		return (bitBoard & (1L<<bit)) != 0;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int bit_index: this) {
			int file = bit_index%8;
			int rank = bit_index/8;
			sb.append(GenericPosition.valueOf(IntFile.toGenericFile(file),IntRank.toGenericRank(rank)));
			sb.append(", ");
		}
		return sb.toString();
	}

	public class SetBitsIterator implements Iterator<Integer> {

		private LinkedList<Integer> setBitsIndexes = null;
	
		public SetBitsIterator() {
			setBitsIndexes = new LinkedList<Integer>();
			long scratchBitBoard = bitBoard;
			while ( scratchBitBoard != 0x0L ) {
				setBitsIndexes.add(Long.numberOfTrailingZeros(scratchBitBoard));
				// clear the lssb
				scratchBitBoard &= scratchBitBoard-1;
			}
		}

		public boolean hasNext() {
			if (!setBitsIndexes.isEmpty()) {
				return true;
			} else {
				return false;
			}
		}

		public Integer next() {
			return setBitsIndexes.remove();
		}

		@Override
		public void remove() {
			setBitsIndexes.remove();
		}
	}
	
	public Iterator<Integer> iterator() {
		return new SetBitsIterator();
	}
}
