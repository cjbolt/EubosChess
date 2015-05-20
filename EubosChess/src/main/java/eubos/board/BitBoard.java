package eubos.board;

//import java.math.BigInteger;
//import java.nio.ByteBuffer;
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

		/**
		 * bitScanForward
		 * @author Martin Läuter (1997)
		 *         Charles E. Leiserson
		 *         Harald Prokop
		 *         Keith H. Randall
		 * "Using de Bruijn Sequences to Index a 1 in a Computer Word"
		 * @param bb bitboard to scan
		 * @precondition bb != 0
		 * @return index (0..63) of least significant one bit
		 */	
		private final int index64[] = {
			0,  1, 48,  2, 57, 49, 28,  3,
			61, 58, 50, 42, 38, 29, 17,  4,
			62, 55, 59, 36, 53, 51, 43, 22,
			45, 39, 33, 30, 24, 18, 12,  5,
			63, 47, 56, 27, 60, 41, 37, 16,
			54, 35, 52, 21, 44, 32, 23, 11,
			46, 26, 40, 15, 34, 20, 31, 10,
			25, 14, 19,  9, 13,  8,  7,  6
		};
		private static final long debruijn64 = 0x03f79d71b4cb0a89L;

//		public SetBitsIterator() {
//			setBitsIndexes = new LinkedList<Integer>();
//			// First do ls 32bits
//			int tempBB = (int) (squareOccupied & 0xffffffff);
//			// handle sign bit being set.
//			if (tempBB < 0) {
//				setBitsIndexes.add(31);
//				tempBB = (int) (squareOccupied & 0x7fffffff);
//			}
//			while( tempBB != 0x0L ) {
//				// Do multiply - doesn't work for bit 63 because in Java this is the sign bit :(
//				int debruijnResult = (int) (((tempBB & -tempBB) * debruijn32) >> (27));
//				// Do table lookup
//				setBitsIndexes.add(index32[debruijnResult]);
//				// clears the lsb
//				tempBB &= tempBB-1;
//			}
//			// Then do the ms 32bits
//			tempBB = (int) ((squareOccupied >> 32) & 0xffffffff);
//			// handle sign bit being set.
//			if (tempBB < 0) {
//				setBitsIndexes.add(31+32);
//				tempBB = (int) ((squareOccupied >> 32) & 0x7fffffff);
//			}
//			while( tempBB != 0x0L ) {
//				// Do multiply - doesn't work for bit 63 because in Java this is the sign bit :(
//				int debruijnResult = (int) (((tempBB & -tempBB) * debruijn32) >> (27));
//				// Do table lookup
//				setBitsIndexes.add((index32[debruijnResult]+32));
//				// clears the lsb
//				tempBB &= tempBB-1;
//			}
//		}
//
//		public SetBitsIterator() {
//			setBitsIndexes = new LinkedList<Integer>();
//			// First do ls 32bits
//			int tempBB = (int) (squareOccupied & 0xffffffff);
//			// handle sign bit being set.
//			if (tempBB < 0) {
//				setBitsIndexes.add(31);
//				tempBB = (int) (squareOccupied & 0x7fffffff);
//			}
//			while( tempBB != 0x0L ) {
////				int negated_tempBB = -tempBB; // Wrong
////				int xor_tempBB = tempBB ^ 0xffffffff; // correct
//				int lowest_set_bit = Integer.lowestOneBit(tempBB);
//				int debruijnResult = (int) ((lowest_set_bit * debruijn32) >> (27));
//				// Do table lookup
//				setBitsIndexes.add(index32[debruijnResult]);
//				// clears the lsb
//				tempBB &= tempBB-1;
//			}
//			// Then do the ms 32bits
//			tempBB = (int) ((squareOccupied >> 32) & 0xffffffff);
//			// handle sign bit being set.
//			if (tempBB < 0) {
//				setBitsIndexes.add(31+32);
//				tempBB = (int) ((squareOccupied >> 32) & 0x7fffffff);
//			}
//			while( tempBB != 0x0L ) {
//				// Do multiply - doesn't work for bit 63 because in Java this is the sign bit :(
//				//int negated_tempBB = -tempBB; // wrong
//				//int unary_tempBB = ~tempBB; // correct
//				//int xor_tempBB = tempBB ^ 0xffffffff; // correct
//				int lowest_set_bit = Integer.lowestOneBit(tempBB);
//				int debruijnResult = (int) ((lowest_set_bit * debruijn32) >> (27));
//				// Do table lookup
//				setBitsIndexes.add((index32[debruijnResult]+32));
//				// clears the lsb
//				tempBB &= tempBB-1;
//			}
//		}		

		private final int index32[] = 
		{
		  0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 
		  31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9
		};
		private static final int debruijn32 = 0x077CB531;		
		
//		public SetBitsIterator() {
//			setBitsIndexes = new LinkedList<Integer>();
//			// First do ls 32bits
//			int tempBB = (int) (squareOccupied & 0xffffffff);
//			// handle sign bit being set.
//			if (tempBB < 0) {
//				setBitsIndexes.add(31);
//				tempBB = (int) (squareOccupied & 0x7fffffff);
//			}
//			while( tempBB != 0x0L ) {
//				int lowest_set_bit = Integer.lowestOneBit(tempBB);
//				long long_lowest_set_bit = (long) lowest_set_bit;
//				// Problem it is sign extending the multiply, which I don't think it should: int * int cast to long was overflowed.
//				long multiply_result = long_lowest_set_bit * debruijn32;
//				long scaled_multiply_result = multiply_result >> 27;
//				//scaled_multiply_result >>= 7;
//				int debruijnResult = (int) scaled_multiply_result;
//				//int debruijnResult = (int) ((lowest_set_bit * debruijn32) >> (27));
//				// Do table lookup
//				setBitsIndexes.add(index32[debruijnResult]);
//				// clears the lsb
//				tempBB &= tempBB-1;
//			}
//			// Then do the ms 32bits
//			tempBB = (int) ((squareOccupied >> 32) & 0xffffffff);
//			// handle sign bit being set.
//			if (tempBB < 0) {
//				setBitsIndexes.add(31+32);
//				tempBB = (int) ((squareOccupied >> 32) & 0x7fffffff);
//			}
//			while( tempBB != 0x0L ) {
//				int lowest_set_bit = Integer.lowestOneBit(tempBB);
//				long long_lowest_set_bit = (long) lowest_set_bit;
//				long multiply_result = long_lowest_set_bit * debruijn32;
//				long scaled_multiply_result = multiply_result >> 27;
//				int debruijnResult = (int) scaled_multiply_result;
//				// Do table lookup
//				setBitsIndexes.add((index32[debruijnResult]+32));
//				// clears the lsb
//				tempBB &= tempBB-1;
//			}
//		}
		
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

//		public SetBitsIterator() {
//			setBitsIndexes = new LinkedList<Integer>();
//			Long tempBB = new Long(squareOccupied);
//			tempBB.l);
//		}
		
//		// TODO consider doing two 32 bit searches?
//		public SetBitsIterator() {
//			setBitsIndexes = new LinkedList<Integer>();
//			long tempBB = squareOccupied;
//			if (tempBB < 0L) {
//				setBitsIndexes.add(63);
//				tempBB &= 0x7fffffffffffffffL;
//			}
//			while( tempBB != 0x0L ) {
//				// Do multiply - doesn't work for bit 63 because in Java this is the sign bit :(
//				int debruijnResult = (int) (((tempBB & -tempBB) * debruijn64) >> 58);
//				// Do table lookup
//				setBitsIndexes.add(index64[debruijnResult]);
//				// clears the lsb
//				tempBB &= tempBB-1;
//			}
//		}
		
//		public SetBitsIterator() {
//			setBitsIndexes = new LinkedList<Integer>();
//			ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
//			buffer.putLong(squareOccupied);
//			BigInteger tempBB = new BigInteger(1, buffer.array());
//			long test = tempBB.longValue();
//			//long tempBB = squareOccupied;
//			while( tempBB != BigInteger.ZERO ) {
//				//int debruijnResult = (int) (((tempBB.and(tempBB.negate())).multiply(BigInteger(debruijn64))).shiftLeft(58));
//				BigInteger i = (((tempBB.and(tempBB.negate())).multiply(BigInteger.valueOf(debruijn64))).shiftLeft(58));
//				int debruijnResult = i.intValue();
//				// Do table lookup
//				setBitsIndexes.add(index64[debruijnResult]);
//				// clears the lsb
//				tempBB = tempBB.and(tempBB.subtract(BigInteger.valueOf(-1)));
//				test &= test-1;
//			}
//		}

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
