package eubos.board;

import java.util.Arrays;

public final class CountedBitBoard {
	
	static void setBits(long [] cbb, long mask) {
		int i=0;
		do {
			long bitsAlreadySet = mask & cbb[i];
			cbb[i] |= mask; // Don't care if already set...
			i += 1;
			if (bitsAlreadySet != 0L) {
				long beforeSet = cbb[i];
				cbb[i] |= bitsAlreadySet;
				// Need to clear the bit that we just set, in the mask
				mask &= beforeSet;
			} else {
				mask = 0L;
			}
		} while (mask != 0L && i < (cbb.length-1));
	}
	
	static void setBitArrays(long [] cbb, long[] masks) {
		for (long mask : masks) {
			setBits(cbb, mask);
		}
	}
	
	static int count(long [] cbb, int pos) {
		long mask = BitBoard.positionToMask_Lut[pos];
		int count = 0;
		while (count < cbb.length) {
			if ((cbb[count] & mask) != 0L) {
				count++;
			} else { 
				break; 
			}
		}
		return count;
	}
	
	static void clear(long[] cbb) {
		Arrays.fill(cbb, 0L);
	}
}
