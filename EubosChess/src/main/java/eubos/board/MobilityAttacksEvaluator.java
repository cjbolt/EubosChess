package eubos.board;

import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.IntRank;

public class MobilityAttacksEvaluator {
	private Board theBoard;
	
	public MobilityAttacksEvaluator(Board board) {
		theBoard = board;		
	}
	
	public void handleDiagonalBatteriesInAttacks(long[] attacks, long diagonal_sliders, long slider_attacks) {
		long empty = theBoard.getEmpty();
		// Check for batteries
		// If one slider attacks another then this denotes a battery
		diagonal_sliders &= slider_attacks; // consider just sliders attacked by another slider
		if (diagonal_sliders != 0L) {
			for (int diag : IntUpRightDiagonal.values) {
				long sliders_in_diagonal = diagonal_sliders & IntUpRightDiagonal.upRightDiagonals[diag];
				if (sliders_in_diagonal == 0) continue;
				for (int i=1; i<Long.bitCount(sliders_in_diagonal); i++) {
					// Need to create a new mask to set here as there may be another slider in the original mask
					long new_mask = BitBoard.downLeftAttacks(sliders_in_diagonal, empty);
					new_mask |= BitBoard.upRightAttacks(sliders_in_diagonal, empty);
					CountedBitBoard.setBits(attacks, new_mask & IntUpRightDiagonal.upRightDiagonals[diag]);
				}
			}
			for (int diag : IntUpLeftDiagonal.values) {
				long sliders_in_diagonal = diagonal_sliders & IntUpLeftDiagonal.upLeftDiagonals[diag];
				if (sliders_in_diagonal == 0) continue;
				for (int i=1; i<Long.bitCount(sliders_in_diagonal); i++) {
					// Need to create a new mask to set here as there may be another slider in the original mask
					long new_mask = BitBoard.upLeftAttacks(sliders_in_diagonal, empty);
					new_mask |= BitBoard.downRightAttacks(sliders_in_diagonal, empty);
					CountedBitBoard.setBits(attacks, new_mask & IntUpLeftDiagonal.upLeftDiagonals[diag]);
				}
			}
		}
	}
	
	private int singleDiagonalHelper(long diagonal_sliders, long [] attacks, boolean useCountedAttacks) {
		long empty = theBoard.getEmpty();
		long temp = 0L;
		long mobility_mask = BitBoard.downLeftOccludedEmpty(diagonal_sliders, empty);
		long direction_attacks = BitBoard.downLeftAttacks(mobility_mask);
		long slider_attacks = direction_attacks;
		if (useCountedAttacks) {
			CountedBitBoard.setBits(attacks, direction_attacks);
		}
		
		temp = BitBoard.upRightOccludedEmpty(diagonal_sliders, empty);
		mobility_mask |= temp;
		direction_attacks = BitBoard.upRightAttacks(temp);
		slider_attacks |= direction_attacks;
		if (useCountedAttacks) {
			CountedBitBoard.setBits(attacks, direction_attacks);
		}
		
		temp = BitBoard.downRightOccludedEmpty(diagonal_sliders, empty);
		mobility_mask |= temp;
		direction_attacks = BitBoard.downRightAttacks(temp);
		slider_attacks |= direction_attacks;
		if (useCountedAttacks) {
			CountedBitBoard.setBits(attacks, direction_attacks);
		}
		
		temp = BitBoard.upLeftOccludedEmpty(diagonal_sliders, empty);
		mobility_mask |= temp;
		direction_attacks = BitBoard.upLeftAttacks(temp);
		slider_attacks |= direction_attacks;
		if (useCountedAttacks) {
			CountedBitBoard.setBits(attacks, direction_attacks);
		} else {
			attacks[0] = slider_attacks;
		}
		
		return Long.bitCount(mobility_mask ^ diagonal_sliders);
	}
	
	private int doubleDiagonalHelper(long diagonal_sliders, long [] attacks, boolean useCountedAttacks) {
		long empty = theBoard.getEmpty();
		long slider_attacks = 0L;
		long mobility_mask_1 = BitBoard.downLeftOccludedEmpty(diagonal_sliders, empty);
		long direction_attacks = BitBoard.downLeftAttacks(mobility_mask_1);
		slider_attacks |= direction_attacks;
		if (useCountedAttacks) {
			CountedBitBoard.setBits(attacks, direction_attacks);
		}
		
		long mobility_mask_2 = BitBoard.upRightOccludedEmpty(diagonal_sliders, empty);
		direction_attacks = BitBoard.upRightAttacks(mobility_mask_2);
		slider_attacks |= direction_attacks;
		if (useCountedAttacks) {
			CountedBitBoard.setBits(attacks, direction_attacks);
		}
		
		int mobility_score = getMobility(0, mobility_mask_1, mobility_mask_2, diagonal_sliders);
		
		mobility_mask_1 = BitBoard.downRightOccludedEmpty(diagonal_sliders, empty);
		direction_attacks = BitBoard.downRightAttacks(mobility_mask_1);
		slider_attacks |= direction_attacks;
		if (useCountedAttacks) {
			CountedBitBoard.setBits(attacks, direction_attacks);
		}
		
		mobility_mask_2 = BitBoard.upLeftOccludedEmpty(diagonal_sliders, empty);
		direction_attacks = BitBoard.upLeftAttacks(mobility_mask_2);
		slider_attacks |= direction_attacks;
		if (useCountedAttacks) {
			CountedBitBoard.setBits(attacks, direction_attacks);
		}
		
		mobility_score = getMobility(mobility_score, mobility_mask_1, mobility_mask_2, diagonal_sliders);

		if (useCountedAttacks) {
			handleDiagonalBatteriesInAttacks(attacks, diagonal_sliders, slider_attacks);
		} else {
			attacks[0] |= slider_attacks;
		}
		return mobility_score;
	}
	
	int calculateDiagonalMobility(long bishops, long queens) {
		return calculateDiagonalMobility(bishops, queens, theBoard.basic_attacks[0][2], false);
	}

	int calculateDiagonalMobility(long bishops, long queens, long [] attacks, boolean useCountedAttacks) {
		int mobility_score = 0x0;
		long diagonal_sliders = bishops | queens;

		if (queens != 0) {
			if (bishops != 0) {
				// Considers overlaps and batteries
				mobility_score = doubleDiagonalHelper(diagonal_sliders, attacks, useCountedAttacks);
			} else {
				// Assume that if it is just queens, then material is so unbalanced that it doesn't matter that they can intersect
				mobility_score = singleDiagonalHelper(diagonal_sliders, attacks, useCountedAttacks);
			}
		} else if (bishops != 0) {
			if (diagonal_sliders != 0) {
				// Assume that if it is just bishops, they can't intersect, which allows optimisation
				mobility_score = singleDiagonalHelper(diagonal_sliders, attacks, useCountedAttacks);
			}
		}
		return mobility_score;
	}
	
	
	public void handleRankAndFileBatteriesForAttacks(long[] attacks, long rank_file_sliders, long slider_attacks) {
		long empty = theBoard.getEmpty();
		// Check for batteries
		rank_file_sliders &= slider_attacks; // consider just sliders attacked by another slider
		if (rank_file_sliders != 0L) {
			// If one slider attacks another then this denotes a battery
			// look for attackers on the same rank or file, and, if found, add that rank/files attacked squares again
			for (int rank : IntRank.values) {
				long sliders_in_rank = rank_file_sliders & BitBoard.RankMask_Lut[rank];
				if (sliders_in_rank == 0) continue;
				for (int i=1; i<Long.bitCount(sliders_in_rank); i++) {
					// Need to create a new mask to set here as there may be another slider in the original mask
					long new_mask = BitBoard.leftAttacks(sliders_in_rank, empty);
					new_mask |= BitBoard.rightAttacks(sliders_in_rank, empty);
					CountedBitBoard.setBits(attacks, new_mask & BitBoard.RankMask_Lut[rank]);
				}
			}
			for (int file : IntFile.values) {
				long sliders_in_file = rank_file_sliders & BitBoard.FileMask_Lut[file];
				if (sliders_in_file == 0) continue;
				for (int i=1; i<Long.bitCount(sliders_in_file); i++) {
					// Need to create a new mask to set here as there may be another slider in the original mask
					long new_mask = BitBoard.upAttacks(sliders_in_file, empty);
					new_mask |= BitBoard.downAttacks(sliders_in_file, empty);
					CountedBitBoard.setBits(attacks, new_mask & BitBoard.FileMask_Lut[file]);
				}
			}
		}
	}
	
	int calculateRankFileMobility(long rooks, long queens) {
		return calculateRankFileMobility(rooks, queens, theBoard.basic_attacks[0][2], false);
	}
	
	public int getMobility(int mobility_score, long mobility_mask_1, long mobility_mask_2, long sliders) {
		mobility_mask_1 ^= sliders;
		mobility_mask_2 ^= sliders;
		if ((mobility_mask_1 & mobility_mask_2) == 0x0) {
			mobility_score += Long.bitCount(mobility_mask_1 | mobility_mask_2);
		} else {
			mobility_score += Long.bitCount(mobility_mask_1) + Long.bitCount(mobility_mask_2);
		}
		return mobility_score;
	}
	
	int calculateRankFileMobility(long rooks, long queens, long [] attacks, boolean useCountedAttacks) {
		long empty = theBoard.getEmpty();
		int mobility_score = 0x0;
		long rank_file_sliders = rooks | queens;
		
		if (rooks != 0) {
			long slider_attacks = 0L;
			long direction_attacks = 0L;
			long mobility_mask_1 = BitBoard.leftOccludedEmpty(rank_file_sliders, empty);
			direction_attacks = BitBoard.leftAttacks(mobility_mask_1);
			slider_attacks |= direction_attacks;
			if (useCountedAttacks) {
				CountedBitBoard.setBits(attacks, direction_attacks);
			}
			
			long mobility_mask_2 = BitBoard.rightOccludedEmpty(rank_file_sliders, empty);
			direction_attacks = BitBoard.rightAttacks(mobility_mask_2);
			slider_attacks |= direction_attacks;
			if (useCountedAttacks) {
				CountedBitBoard.setBits(attacks, direction_attacks);
			}
			
			mobility_score = getMobility(0, mobility_mask_1, mobility_mask_2, rank_file_sliders);
			
			mobility_mask_1 = BitBoard.upOccludedEmpty(rank_file_sliders, empty);
			direction_attacks = BitBoard.upAttacks(mobility_mask_1);
			slider_attacks |= direction_attacks;
			if (useCountedAttacks) {
				CountedBitBoard.setBits(attacks, direction_attacks);
			}
			
			mobility_mask_2 = BitBoard.downOccludedEmpty(rank_file_sliders, empty);
			direction_attacks = BitBoard.downAttacks(mobility_mask_2);
			slider_attacks |= direction_attacks;
			if (useCountedAttacks) {
				CountedBitBoard.setBits(attacks, direction_attacks);
			}
			
			mobility_score = getMobility(mobility_score, mobility_mask_1, mobility_mask_2, rank_file_sliders);
			
			if (useCountedAttacks) {
				handleRankAndFileBatteriesForAttacks(attacks, rank_file_sliders, slider_attacks);
			} else {
				attacks[0] |= slider_attacks;
			}
		}
		else if (rank_file_sliders != 0) {
			// Assume that if it is just queens, then material is so unbalanced that it doesn't matter that they can intersect
			long temp = 0L;
			long mobility_mask = 0L; 
			
			temp = BitBoard.leftOccludedEmpty(rank_file_sliders, empty);
			mobility_mask |= temp;
			long direction_attacks = BitBoard.leftAttacks(mobility_mask);
			long slider_attacks = direction_attacks;
			if (useCountedAttacks) {
				CountedBitBoard.setBits(attacks, direction_attacks);
			}
			
			temp = BitBoard.rightOccludedEmpty(rank_file_sliders, empty);
			mobility_mask |= temp;
			direction_attacks = BitBoard.rightAttacks(temp);
			slider_attacks |= direction_attacks;
			if (useCountedAttacks) {
				CountedBitBoard.setBits(attacks, direction_attacks);
			}
			
			temp = BitBoard.downOccludedEmpty(rank_file_sliders, empty);
			mobility_mask |= temp;
			direction_attacks = BitBoard.downAttacks(temp);
			slider_attacks |= direction_attacks;
			if (useCountedAttacks) {
				CountedBitBoard.setBits(attacks, direction_attacks);
			}
			
			temp = BitBoard.upOccludedEmpty(rank_file_sliders, empty);
			mobility_mask |= temp;
			direction_attacks = BitBoard.upAttacks(temp);
			slider_attacks |= direction_attacks;
			if (useCountedAttacks) {
				CountedBitBoard.setBits(attacks, direction_attacks);
			} else {
				attacks[0] |= slider_attacks;
			}
			
			mobility_score = Long.bitCount(mobility_mask ^ rank_file_sliders);
		}
		return mobility_score;
	}
	
	protected void getBasicAttacksForSide(long [][] attacks, boolean isBlack, boolean useCountedAttacks) {
		if (useCountedAttacks) {
			CountedBitBoard.clear(attacks[0]);
			CountedBitBoard.clear(attacks[1]);
			CountedBitBoard.clear(attacks[2]);
			CountedBitBoard.clear(attacks[3]);
		} else {
			attacks[0][0] = attacks[1][0] = attacks[2][0] = 0L;
		}
		// Pawns
		theBoard.paa.getPawnAttacks(attacks[0], isBlack);
		attacks[3][0] = attacks[0][0];
		if (useCountedAttacks) {
			attacks[3][1] = attacks[0][1];
		}
		// Knights
		theBoard.kaa.getAttacks(attacks[1], isBlack);
		// King
		long kingAttacks = SquareAttackEvaluator.KingMove_Lut[theBoard.pieceLists.getKingPos(!isBlack)];

		if (useCountedAttacks) {
			CountedBitBoard.setBitArrays(attacks[3], attacks[1]);
			CountedBitBoard.setBits(attacks[3], kingAttacks);
		} else {
			attacks[3][0] |= (attacks[1][0] | kingAttacks);
		}
	}

}
