package eubos.board;

import java.util.Arrays;

import eubos.main.EubosEngineMain;

public final class CountedBitBoard {

	static void setBits(long [] cbb, long mask) {
		int i=0;
		while (mask != 0 && i < cbb.length) {
			long bitsAlreadySetInCbbAtLevel = mask & cbb[i];
			cbb[i++] |= mask;
			mask = bitsAlreadySetInCbbAtLevel;
		}
	}
	
	static void setBitArrays(long [] cbb, long[] masks) {
		for (long mask : masks) {
			if (mask == 0) break;
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
	
	static boolean weControlContestedSquares(long[] own, long[] enemy, long contestedMask) {
		boolean we_have_control = true;
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert own.length == enemy.length;
		}
		for (int i=0; i < own.length; i++) {
			long enemy_control_at_count = enemy[i] & contestedMask;
			if (enemy_control_at_count != 0L) {
				long own_control_at_count = own[i] & contestedMask;
				if ((own_control_at_count & enemy_control_at_count) != enemy_control_at_count) {
					// Then we don't have the same control over the contested squares as the enemy
					we_have_control = false;
					break;
				}
			} else {
				break;
			}
		}
		return we_have_control;
	}

	public static int evaluate(long[] own, long[] enemy, long areaMask) {
		long enemy_control_mask = 0L;
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert own.length == enemy.length;
		}
		for (int i=0; i < own.length; i++) {
			long enemy_control_at_count = enemy[i] & areaMask;
			if (enemy_control_at_count == 0L) {
				break;
			}
			long own_control_at_count = own[i] & areaMask;
			enemy_control_mask |= (own_control_at_count ^ enemy_control_at_count) & enemy_control_at_count; 
		}
		return Long.bitCount(enemy_control_mask);
	}
}
