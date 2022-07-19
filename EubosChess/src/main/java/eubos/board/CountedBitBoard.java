package eubos.board;

import java.util.Arrays;

public final class CountedBitBoard {
	
	static void setBits(long [] cbb, long mask) {
		int i=0;
		long bitsAlreadySet = mask & cbb[i];
		cbb[i] |= mask; // Don't care if it was already set for depth 0...
		for (i=1; i < cbb.length; i++) {
			if (bitsAlreadySet != 0L) {
				long beforeSet = cbb[i];
				cbb[i] |= bitsAlreadySet;
				mask &= beforeSet; // Need to clear the bit that we just set, in the mask
			} else {
				mask = 0L;
			}
			if (mask == 0L) break;
			bitsAlreadySet = mask & cbb[i];
		}
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
