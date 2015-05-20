package eubos.board;

import java.util.Iterator;
import java.util.LinkedList;

public class BitBoard implements Iterable<Integer> {
	
	private long squareOccupied = 0x0L;
	
	public long getSquareOccupied() {
		return squareOccupied;
	}

	public void set(int rank, int file) {
		squareOccupied |= (1L << ((rank*8)+file));
	}
	
	public void clear(int rank, int file) {
		squareOccupied &= ~(1L << ((rank*8)+file));
	}
	
	public boolean isSet(int rank, int file) {
		return ((squareOccupied & (1L<<((rank*8)+file))) != 0) ? true : false;
	}

	public class SetBitsIterator implements Iterator<Integer> {

		private LinkedList<Integer> setBitsIndexes = null;
		
		private final int Mod37BitPosition[] = {
		  32, 0, 1, 26, 2, 23, 27, 0, 3, 16, 24, 30, 28, 11, 0, 13, 4,
		  7, 17, 0, 25, 22, 31, 15, 29, 10, 12, 6, 0, 21, 14, 9, 5,
		  20, 8, 19, 18
		};
		
		public SetBitsIterator() {
			setBitsIndexes = new LinkedList<Integer>();
			// First do ls 32bits
			int tempBB = (int) (squareOccupied & 0xffffffff);
			// handle sign bit being set.
			if (tempBB < 0) {
				setBitsIndexes.add(31);
				tempBB = (int) (squareOccupied & 0x7fffffff);
			}
			while( tempBB != 0x0L ) {
				int lowest_set_bit = Integer.lowestOneBit(tempBB);
				int mod_value = lowest_set_bit % 37;
				setBitsIndexes.add(Mod37BitPosition[mod_value]);
				// clears the lsb
				tempBB &= tempBB-1;
			}
			// Then do the ms 32bits
			tempBB = (int) ((squareOccupied >> 32) & 0xffffffff);
			// handle sign bit being set.
			if (tempBB < 0) {
				setBitsIndexes.add(31+32);
				tempBB = (int) ((squareOccupied >> 32) & 0x7fffffff);
			}
			while( tempBB != 0x0L ) {
				int lowest_set_bit = Integer.lowestOneBit(tempBB);
				int mod_value = lowest_set_bit % 37;
				setBitsIndexes.add(Mod37BitPosition[mod_value]+32);
				// clears the lsb
				tempBB &= tempBB-1;
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
