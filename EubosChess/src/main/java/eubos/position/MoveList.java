package eubos.position;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import eubos.board.BitBoard;
import eubos.main.EubosEngineMain;
import eubos.search.KillerList;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;

public class MoveList implements Iterable<Integer> {
	
	public static final boolean DISTINCT_STAGE_FOR_KILLERS = false;
	
	private int [][] normal_search_moves;
	private int [][] priority_moves;
	private int [][] extended_search_moves;
	private int [][] scratchpad;
	
	private int [] normal_fill_index;
	private int [] priority_fill_index;
	private int [] normal_list_length;
	private int [] nextCheckPoint;
	
	private boolean [] needToEscapeMate;
	private int [] moveCount;
	private int [] scratchpad_fill_index;
	private int [] extendedListScopeEndpoint;
	private int [] bestMove;
	private int [][] killers;
	
	private int ply;
	
	PositionManager pm;
	int ordering;
	
	public MoveAdderCapturesAndPromotions ma_capturesPromos;
	public MoveAdderWithKillers ma_killers;
	public MoveAdderWithNoKillers ma_noKillers;
	public MoveAdderQuietMovesNoKillers ma_quietNoKillers;
	public MoveAdderQuietMovesKillers ma_quietKillers;
	public MoveAdderWithHandlingKillers ma_quietHandleKillers;
	
	private static final MoveTypeComparator moveTypeComparator = new MoveTypeComparator();
	
    static class MoveTypeComparator implements IntComparator {
        @Override public int compare(int move1, int move2) {
            boolean gt = Move.getType(move1) < Move.getType(move2);
            boolean eq = Move.getType(move1) == Move.getType(move2);
            return gt ? 1 : (eq ? 0 : -1);
        }
    }
	
	public MoveList(PositionManager pm, int orderMoveList)  {	
		
		scratchpad = new int [EubosEngineMain.SEARCH_DEPTH_IN_PLY][];
		
		// Create the move list arrays for this threads move list
		normal_search_moves = new int [EubosEngineMain.SEARCH_DEPTH_IN_PLY][];
		priority_moves = new int [EubosEngineMain.SEARCH_DEPTH_IN_PLY][];
		extended_search_moves = new int [EubosEngineMain.SEARCH_DEPTH_IN_PLY][];
		
		normal_fill_index = new int [EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		priority_fill_index = new int [EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		normal_list_length = new int [EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		
		moveCount = new int [EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		scratchpad_fill_index = new int [EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		extendedListScopeEndpoint = new int [EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		bestMove = new int [EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		needToEscapeMate = new boolean [EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		killers = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY][3];
		
		nextCheckPoint = new int [EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		
		// Create the list at each ply
		for (int i=0; i < EubosEngineMain.SEARCH_DEPTH_IN_PLY; i++) {
			normal_search_moves[i] = new int[110];
			priority_moves[i] = new int[100];
			extended_search_moves[i] = new int[30];
			scratchpad[i] = new int [100];
		}
		
		this.pm = pm;
		this.ordering = orderMoveList;
		
		ma_killers = new MoveAdderWithKillers();
		ma_noKillers = new MoveAdderWithNoKillers();
		ma_capturesPromos = new MoveAdderCapturesAndPromotions();
		ma_quietNoKillers = new MoveAdderQuietMovesNoKillers();
		ma_quietKillers = new MoveAdderQuietMovesKillers();
		ma_quietHandleKillers = new MoveAdderWithHandlingKillers();
	}
	
	@SuppressWarnings("unused")
	public MoveListIterator createForPly(int bestMove, int [] killers, boolean capturesOnly, boolean needToEscapeMate, int ply)
	{
		// Initialise working variables for building the MoveList at this ply
		this.ply = ply; 
		this.needToEscapeMate[ply] = needToEscapeMate;
		this.killers[ply] = killers;
		this.bestMove[ply] = bestMove;
		moveCount[ply] = 0;
		normal_fill_index[ply] = 0;
		priority_fill_index[ply] = 0;
		normal_list_length[ply] = 0;
		scratchpad_fill_index[ply] = 0;
		extendedListScopeEndpoint[ply] = 0;
		nextCheckPoint[ply] = 0;
		
		getMoves(capturesOnly);
		if (moveCount[ply] != 0) {
			if (EubosEngineMain.ENABLE_ASSERTS && bestMove != Move.NULL_MOVE) {
				if (scratchpad_fill_index[ply] == 1) {
					// Only check the best move is present if it is a hash table move, if it is from the principal 
					// continuation it shall only be guaranteed valid for ply 0. 
					assert Move.areEqualForBestKiller(bestMove, scratchpad[ply][0]) : 
						String.format("When creating MoveList, bestMove=%s was not found amongst available moves", Move.toString(bestMove));
				}
			}
			sortPriorityList();
			collateMoveList();
		}
		
		return iterator();
	}
	
	public MoveListIterator createForPly(int bestMove, boolean needToEscapeMate, int ply) {
		createForPly(bestMove, null, true, needToEscapeMate, ply);
		return getExtendedIterator(); 
	}
	
	public MoveListIterator stagedMoveGen(boolean firstMoveAtPly, int bestMove, int [] killers, boolean needToEscapeMate, int ply)
	{
		MoveListIterator iter = null;
			
		this.ply = ply; 
		
		// Initialise working variables for building the MoveList at this ply
		if (firstMoveAtPly) {
			this.needToEscapeMate[ply] = needToEscapeMate;
			this.killers[ply] = killers;
			this.bestMove[ply] = bestMove;
			nextCheckPoint[ply] = 0;
			moveCount[ply] = 0;
			normal_fill_index[ply] = 0;
			priority_fill_index[ply] = 0;
			normal_list_length[ply] = 0;
			scratchpad_fill_index[ply] = 0;
			extendedListScopeEndpoint[ply] = 0;
		}
		
		switch(nextCheckPoint[ply]) {
		case 0:
			// Return best Move if valid
			nextCheckPoint[ply] = 1;
			if (Move.isBest(this.bestMove[ply]) || (this.bestMove[ply] != Move.NULL_MOVE && bestMoveIsValid())) {
				scratchpad[ply][0] = this.bestMove[ply];
				iter = getBestIterator();
				nextCheckPoint[ply] = 1;
				break;
			}
			this.bestMove[ply] = Move.NULL_MOVE;
			// Note fall-through if no valid best move
		case 1:
			// Generate all captures and promotions
			nextCheckPoint[ply] = 2;
			getCapturesAndPromotions();
			if (moveCount[ply] != 0) {
				sortPriorityList();
				iter = new MoveListIterator(priority_moves[ply], priority_fill_index[ply]);
				if (Move.areEqualForBestKiller(this.bestMove[ply], priority_moves[ply][0])) {
					// Step passed best move that we returned already
					iter.nextInt();
					if (iter.hasNext()) {
						break;
					} else {
						// If the only move in the prio list was the previous best move, drop-through and look for killers
					}
				} else {
					break;
				}
			}
		case 2:
			if (DISTINCT_STAGE_FOR_KILLERS) {
				// Generate Killers
				nextCheckPoint[ply] = 3;
				if (this.killers[ply] != null) {
					int [] validKillers = new int[3];
					int validCount = 0;
					if (!Move.areEqualForBestKiller(this.bestMove[ply], this.killers[ply][0]) &&
						pm.getTheBoard().isPlayableMove(this.killers[ply][0], this.needToEscapeMate[ply], pm.castling)) {
						validKillers[0] = this.killers[ply][0];
						validCount++;
					}
					if (!Move.areEqualForBestKiller(this.bestMove[ply], this.killers[ply][1]) &&
						pm.getTheBoard().isPlayableMove(this.killers[ply][1], this.needToEscapeMate[ply], pm.castling)) {
						validKillers[validCount] = this.killers[ply][1];
						validCount++;
					}
					if (!Move.areEqualForBestKiller(this.bestMove[ply], this.killers[ply][2]) &&
						pm.getTheBoard().isPlayableMove(this.killers[ply][2], this.needToEscapeMate[ply], pm.castling)) {
						validKillers[validCount] = this.killers[ply][2];
						validCount++;
					}
					if (validCount > 0) {
						iter = new MoveListIterator(validKillers, validCount);
						break;
					}
				}
			}
		case 3:
			// Lastly, generate all moves that aren't best, killers, or tactical moves
			nextCheckPoint[ply] = 4;
			moveCount[ply] = 0;
			if (DISTINCT_STAGE_FOR_KILLERS) {
				getQuietMoves();
				if (moveCount[ply] != 0) {
					iter = new MoveListIterator(normal_search_moves[ply], normal_fill_index[ply]);
					break;
				}
			} else {
				priority_fill_index[ply] = 0;
				getQuietMoves();
				if (moveCount[ply] != 0) {
					sortPriorityList();
					collateMoveList();
					iter = iterator();
					break;
				}
			}
		case 4:
		default:
			iter = new MoveListIterator(scratchpad[ply], 0); // Empty iterator
			break;
		}
		return iter;
	}
	
	private boolean bestMoveIsValid() {
		return pm.getTheBoard().isPlayableMove(bestMove[ply], needToEscapeMate[ply], pm.castling);
	}
	
	private void getCapturesAndPromotions() {
		// Set-up move adder to filter the moves from attacked pieces into the priority part of the move list
		boolean isWhiteOnMove = pm.onMoveIsWhite();
		ma_capturesPromos.attackMask = pm.getTheBoard().pkaa.getAttacks(isWhiteOnMove);
		pm.getTheBoard().getRegularPieceMoves(ma_capturesPromos, isWhiteOnMove, true);
	}
	
	private void getQuietMoves() {
		IAddMoves moveAdder = null;
		boolean isWhiteOnMove = pm.onMoveIsWhite();
		if (killers[ply] == null) {
			moveAdder = ma_quietNoKillers;
		} else {
			// Set-up move adder to filter the moves from attacked pieces into the priority part of the move list
			if (DISTINCT_STAGE_FOR_KILLERS) {
				moveAdder = ma_quietKillers;
			} else {
				moveAdder = ma_quietHandleKillers;
			}
		}
		pm.getTheBoard().getRegularPieceMoves(moveAdder, isWhiteOnMove, false);
		if (!needToEscapeMate[ply]) {
			// Can't castle out of check and don't care in extended search
			pm.castling.addCastlingMoves(isWhiteOnMove, moveAdder);
		}
	}
	
	private void getMoves(boolean capturesOnly) {
		IAddMoves moveAdder = null;
		boolean isWhiteOnMove = pm.onMoveIsWhite();
		if (killers[ply] == null) {
			if (!capturesOnly) {
				ma_noKillers.attackMask = pm.getTheBoard().pkaa.getAttacks(isWhiteOnMove);
			}
			moveAdder = ma_noKillers;
		} else {
			// Set-up move adder to filter the moves from attacked pieces into the priority part of the move list
			ma_killers.attackMask = pm.getTheBoard().pkaa.getAttacks(isWhiteOnMove);
			moveAdder = ma_killers;
		}
		pm.getTheBoard().getRegularPieceMoves(moveAdder, isWhiteOnMove, capturesOnly);
		if (!capturesOnly && !needToEscapeMate[ply]) {
			// Can't castle out of check and don't care in extended search
			pm.castling.addCastlingMoves(isWhiteOnMove, moveAdder);
		}
	}
		
	private void collateMoveList() {
		for (int j=0; j < priority_fill_index[ply]; j++) {
			scratchpad[ply][scratchpad_fill_index[ply]++] = priority_moves[ply][j];
		}
		// Update for number of valid priority moves, needed by lazy extended moves creation
		extendedListScopeEndpoint = scratchpad_fill_index;
		for (int j=0; j < normal_fill_index[ply]; j++) {
			scratchpad[ply][scratchpad_fill_index[ply]++] = normal_search_moves[ply][j];
		}
		normal_list_length[ply] = moveCount[ply];
	}
	
	private void sortPriorityList() {
		switch (ordering) {
		case 0:
			/* Don't order the move list in this case. */
			break;
		case 1:
			IntArrays.quickSort(priority_moves[ply], 0, priority_fill_index[ply], Move.mvvLvaComparator);
			break;
		case 2:
			IntArrays.reverse(priority_moves[ply], 0, priority_fill_index[ply]);
			IntArrays.quickSort(priority_moves[ply], 0, priority_fill_index[ply], moveTypeComparator);
			break;
		case 3:
			IntArrays.reverse(priority_moves[ply], 0, priority_fill_index[ply]);
			IntArrays.quickSort(priority_moves[ply], 0, priority_fill_index[ply], Move.mvvLvaComparator);
			break;
		case 4:
			IntArrays.quickSort(priority_moves[ply], 0, priority_fill_index[ply], moveTypeComparator);
			break;
		default:
			EubosEngineMain.logger.severe(String.format("Bad move ordering scheme %d!", ordering));
			if (EubosEngineMain.ENABLE_ASSERTS)
				assert false;
			break;
		}
	}
	
	@Override
	public MoveListIterator iterator() {
		MoveListIterator iter = new MoveListIterator(scratchpad[ply], normal_list_length[ply]);
		return iter;
	}
	
	public MoveListIterator getBestIterator() {
		return new MoveListIterator(scratchpad[ply], 1);
	}
	
	public MoveListIterator getExtendedIterator() {
		// Lazy creation of extended moves
		int ext_count = 0;
		for (int i=0; i < extendedListScopeEndpoint[ply]; i++) {
			int move = scratchpad[ply][i];
			boolean includeInQuiescenceSearch = Move.isCapture(move) || Move.isQueenPromotion(move);
			if (includeInQuiescenceSearch) {
				extended_search_moves[ply][ext_count++] = move;
			}
		}
		return new MoveListIterator(extended_search_moves[ply], ext_count);
	}
		
	public int getRandomMove() {
		int randomMove = Move.NULL_MOVE;
		if (normal_list_length[ply] != 0) {
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(normal_list_length[ply]);
			randomMove = scratchpad[ply][indexToGet];		
		}
		return randomMove;
	}
	
	@Override
	public String toString() {
		String retVal = "";
		for (int move : IntArrays.trim(scratchpad[ply], normal_list_length[ply])) {
			retVal += Move.toString(move);
			retVal += ", ";
		}
		return retVal;
	}

	public int getBestMove() {
		if (normal_list_length[ply] != 0) {
			return scratchpad[ply][0];
		} else {
			return Move.NULL_MOVE;
		}
	}
	
	public class MoveAdderCapturesAndPromotions implements IAddMoves {
		
		long attackMask = 0L;
		boolean attacked = false;
		boolean attackedDetermined = false;
		
		public void addPrio(int move) {
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				priority_moves[ply][priority_fill_index[ply]++] = move;
				moveCount[ply]++;
			}
		}
		
		public void addNormal(int move) {} // Doesn't deal with quiet moves by design
		
		public boolean isLegalMoveFound() {
			return false;
		}
		
		protected boolean isMoveOriginSquareAttacked(int move) {
			long orginSquare = BitBoard.positionToMask_Lut[Move.getOriginPosition(move)];
			if ((orginSquare & attackMask) == orginSquare)
				return true;
			return false;
		}

		@Override
		public void clearAttackedCache() {
			attackedDetermined = false;
			attacked = false;			
		}
	}
	
	public class MoveAdderQuietMovesNoKillers extends MoveAdderCapturesAndPromotions implements IAddMoves {
		
		public void addPrio(int move) {} // Doesn't deal with tactical moves by design
		
		public void addNormal(int move) {
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (bestMove[ply] != Move.NULL_MOVE && Move.areEqualForBestKiller(bestMove[ply], move)) {
					// Silently consume, the best move should already have been found and searched if not null
				} else {
					normal_search_moves[ply][normal_fill_index[ply]++] = move;
					moveCount[ply]++;
				}
			}
		}
		
		public boolean isLegalMoveFound() {
			return false;
		}
		
		protected boolean isMoveOriginSquareAttacked(int move) {
			return false;
		}

		public void clearAttackedCache() {}
	}
	
	public class MoveAdderQuietMovesKillers extends MoveAdderQuietMovesNoKillers implements IAddMoves {
		
		public void addNormal(int move) {
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (bestMove[ply] != Move.NULL_MOVE && Move.areEqualForBestKiller(bestMove[ply], move)) {
					// Silently consume, the best move should already have been found and searched if not null
				} else if (KillerList.isMoveOnListAtPly(killers[ply], move)) {
					// Silently consume killers, they have already been searched
				} else {
					normal_search_moves[ply][normal_fill_index[ply]++] = move;
					moveCount[ply]++;
				}
			}
		}
	}
	
	public class MoveAdderWithHandlingKillers extends MoveAdderCapturesAndPromotions implements IAddMoves {
		
		long attackMask = 0L;
		boolean attacked = false;
		boolean attackedDetermined = false;
		
		public void addPrio(int move) {}
		
		public void addNormal(int move) {
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (bestMove[ply] != Move.NULL_MOVE && Move.areEqualForBestKiller(bestMove[ply], move)) {
					// Silently consume, the best move should already have been found and searched if not null
				} else if (KillerList.isMoveOnListAtPly(killers[ply], move)) {
					priority_moves[ply][priority_fill_index[ply]++] = Move.setKiller(move);
					moveCount[ply]++;
				} else if (attacked || (!attackedDetermined && isMoveOriginSquareAttacked(move))) {
					priority_moves[ply][priority_fill_index[ply]++] = move;
					attackedDetermined = true;
					attacked = true;
					moveCount[ply]++;
				} else {
					normal_search_moves[ply][normal_fill_index[ply]++] = move;
					attackedDetermined = true;
					moveCount[ply]++;
				}
			}
		}
		
		public boolean isLegalMoveFound() {
			return false;
		}
		
		protected boolean isMoveOriginSquareAttacked(int move) {
			long orginSquare = BitBoard.positionToMask_Lut[Move.getOriginPosition(move)];
			if ((orginSquare & attackMask) == orginSquare)
				return true;
			return false;
		}

		@Override
		public void clearAttackedCache() {
			attackedDetermined = false;
			attacked = false;			
		}
	}
	
	public class MoveAdderWithKillers extends MoveAdderCapturesAndPromotions implements IAddMoves {
		
		long attackMask = 0L;
		boolean attacked = false;
		boolean attackedDetermined = false;
		
		public void addPrio(int move) {
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (Move.areEqualForBestKiller(move, bestMove[ply])) {
					scratchpad[ply][scratchpad_fill_index[ply]++] = Move.setBest(move);
				} else {
					priority_moves[ply][priority_fill_index[ply]++] = move;
				}
				moveCount[ply]++;
			}
		}
		
		public void addNormal(int move) {
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (Move.areEqualForBestKiller(move, bestMove[ply])) {
					scratchpad[ply][scratchpad_fill_index[ply]++] = Move.setBest(move);
				} else if (KillerList.isMoveOnListAtPly(killers[ply], move)) {
					priority_moves[ply][priority_fill_index[ply]++] = Move.setKiller(move);
				} else if (attacked || (!attackedDetermined && isMoveOriginSquareAttacked(move))) {
					priority_moves[ply][priority_fill_index[ply]++] = move;
					attackedDetermined = true;
					attacked = true;
				} else {
					normal_search_moves[ply][normal_fill_index[ply]++] = move;
					attackedDetermined = true;
				}
				moveCount[ply]++;
			}
		}
		
		public boolean isLegalMoveFound() {
			return false;
		}
		
		protected boolean isMoveOriginSquareAttacked(int move) {
			long orginSquare = BitBoard.positionToMask_Lut[Move.getOriginPosition(move)];
			if ((orginSquare & attackMask) == orginSquare)
				return true;
			return false;
		}

		@Override
		public void clearAttackedCache() {
			attackedDetermined = false;
			attacked = false;			
		}
	}
	
	public class MoveAdderWithNoKillers extends MoveAdderWithKillers implements IAddMoves {
		public void addNormal(int move) {
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (Move.areEqualForBestKiller(move, bestMove[ply])) {
					scratchpad[ply][scratchpad_fill_index[ply]++] = Move.setBest(move);
				} else if (attacked || (!attackedDetermined && isMoveOriginSquareAttacked(move))) {
					priority_moves[ply][priority_fill_index[ply]++] = move;
					attackedDetermined = true;
					attacked = true;
				} else {
					normal_search_moves[ply][normal_fill_index[ply]++] = move;
					attackedDetermined = true;
				}
				moveCount[ply]++;
			}
		}
	}
	
	//---------------------------------------------------------------------------------------------
	// Test APIs
	boolean contains(int move) {
		for (int reg_move : IntArrays.trim(scratchpad[ply], normal_list_length[ply])) {
			if (move == reg_move)
				return true;
		}
		return false;
	}
	
	public List<Integer> getList(int currPly) {
		List<Integer> ml; 
		if (normal_list_length[currPly] != 0) {
			ml = IntStream.of(IntArrays.trim(scratchpad[currPly], normal_list_length[currPly]))
					.boxed().collect(Collectors.toList());
		} else {
			ml = new ArrayList<Integer>();
		}
		return ml;
	}
}
