package eubos.position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import eubos.board.BitBoard;
import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.search.KillerList;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;

public class MoveList implements Iterable<Integer> {
	
	public static final boolean DISTINCT_STAGE_FOR_KILLERS = true;
	public static final boolean DEBUG_CHECK = false;
	
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
	
	public AllMovesWithKillers ma_killers;
	public AllMovesWithNoKillers ma_noKillers;
	
	public QuietMovesWithNoKillers ma_quietNoKillers;
	public QuietMovesConsumingKillers ma_quietConsumeKillers;
	public QuietMovesWithKillers ma_quietKillers;
	
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
		
		ma_killers = new AllMovesWithKillers();
		ma_noKillers = new AllMovesWithNoKillers();
		ma_capturesPromos = new MoveAdderCapturesAndPromotions();
		ma_quietNoKillers = new QuietMovesWithNoKillers();
		ma_quietConsumeKillers = new QuietMovesConsumingKillers();
		ma_quietKillers = new QuietMovesWithKillers();
	}
	
	public void initialise(int bestMove, int [] killers, boolean needToEscapeMate, int ply) {
		// Initialise working variables for building the MoveList at this ply
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
		if (DEBUG_CHECK) {
			Arrays.fill(scratchpad[ply], Move.NULL_MOVE);
		}
	}
	
	@SuppressWarnings("unused")
	public MoveListIterator createForPly(int bestMove, int [] killers, boolean capturesOnly, boolean needToEscapeMate, int ply)
	{
		// Initialise working variables for building the MoveList at this ply
		this.ply = ply; 
		initialise(bestMove, killers, needToEscapeMate, ply);
		
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
	
	public MoveListIterator stagedMoveGen(int ply)
	{
		MoveListIterator iter = null;
		this.ply = ply; 
		
		switch(nextCheckPoint[ply]) {
		case 0:
			// Return best Move if valid
			nextCheckPoint[ply] = 1;
			if (bestMove[ply] != Move.NULL_MOVE) {
				if (Move.isBest(bestMove[ply]) || bestMoveIsValid()) {
					scratchpad[ply][0] = bestMove[ply];
					iter = getBestIterator();
					nextCheckPoint[ply] = 1;
					break;
				}
				bestMove[ply] = Move.NULL_MOVE;
			}
			// Note fall-through if no valid best move
		case 1:
			// Generate all captures and promotions
			nextCheckPoint[ply] = 2;
			getCapturesAndPromotions();
			if (moveCount[ply] != 0) {
				sortPriorityList();
				iter = new MoveListIterator(priority_moves[ply], priority_fill_index[ply]);
			    if (iter.hasNext()) {
					break; // Return if there is a move in the iterator
				}
				// Note fall-through if no valid move in the iterator
			}
		case 2:
			if (DISTINCT_STAGE_FOR_KILLERS) {
				// Generate Killers
				nextCheckPoint[ply] = 3;
				if (killers[ply] != null) {
					int [] validKillers = new int[3];
					int validCount = 0;
					if (!Move.areEqualForBestKiller(bestMove[ply], killers[ply][0]) &&
						pm.getTheBoard().isPlayableMove(killers[ply][0], needToEscapeMate[ply], pm.castling)) {
						validKillers[0] = killers[ply][0];
						validCount++;
					}
					if (!Move.areEqualForBestKiller(bestMove[ply], killers[ply][1]) &&
						pm.getTheBoard().isPlayableMove(killers[ply][1], needToEscapeMate[ply], pm.castling)) {
						validKillers[validCount] = killers[ply][1];
						validCount++;
					}
					if (!Move.areEqualForBestKiller(bestMove[ply], killers[ply][2]) &&
						pm.getTheBoard().isPlayableMove(killers[ply][2], needToEscapeMate[ply], pm.castling)) {
						validKillers[validCount] = killers[ply][2];
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
			priority_fill_index[ply] = 0;
			getQuietMoves();
			if (moveCount[ply] != 0) {
				sortPriorityList();
				collateMoveList();
				iter = iterator();
				break;
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
		pm.getTheBoard().getRegularPieceMoves(ma_capturesPromos, isWhiteOnMove, true);
	}
	
	private void getQuietMoves() {
		IAddMoves moveAdder = null;
		boolean isWhiteOnMove = pm.onMoveIsWhite();
		long attackMask = pm.getTheBoard().pkaa.getAttacks(isWhiteOnMove);
		if (killers[ply] == null) {
			moveAdder = ma_quietNoKillers; 
			ma_quietNoKillers.attackMask = attackMask;
		} else {
			// Set-up move adder to filter the moves from attacked pieces into the priority part of the move list
			if (DISTINCT_STAGE_FOR_KILLERS) {
				moveAdder = ma_quietConsumeKillers;
				ma_quietConsumeKillers.attackMask = attackMask;
			} else {
				moveAdder = ma_quietKillers;
				ma_quietKillers.attackMask = attackMask;
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
		long attackMask = pm.getTheBoard().pkaa.getAttacks(isWhiteOnMove);
		if (killers[ply] == null) {
			if (!capturesOnly) {
				ma_noKillers.attackMask = attackMask;
			}
			moveAdder = ma_noKillers;
		} else {
			// Set-up move adder to filter the moves from attacked pieces into the priority part of the move list
			ma_killers.attackMask = attackMask;
			moveAdder = ma_killers;
		}
		pm.getTheBoard().getRegularPieceMoves(moveAdder, isWhiteOnMove, capturesOnly);
		if (!capturesOnly && !needToEscapeMate[ply]) {
			// Can't castle out of check and don't care in extended search
			pm.castling.addCastlingMoves(isWhiteOnMove, moveAdder);
		}
	}
		
	private void collateMoveList() {
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert scratchpad_fill_index[ply] <= 1 : "Scratchpad too long";
		}
		for (int j=0; j < priority_fill_index[ply]; j++) {
			scratchpad[ply][scratchpad_fill_index[ply]++] = priority_moves[ply][j];
		}
		// Update for number of valid priority moves, needed by lazy extended moves creation
		extendedListScopeEndpoint[ply] = scratchpad_fill_index[ply];
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
			if (DEBUG_CHECK) {
				if (ext_count == 30) {
					EubosEngineMain.logger.severe(String.format("extended moves overflowing %s", pm.getFen()));
					StringBuilder s = new StringBuilder();
					for (int j=0; j<30; j++) {
						s.append(Move.toString(extended_search_moves[ply][j]));
						s.append(' ');
					}
					EubosEngineMain.logger.severe(String.format("ext move list at ply %d is %s", ply, s.toString()));
					ext_count--;
					break;
				}
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
		
		public void addPrio(int move) {
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (Move.areEqualForBestKiller(move, bestMove[ply])) {
					// Silently consume
				} else  {
					priority_moves[ply][priority_fill_index[ply]++] = move;
					moveCount[ply]++;
				}
				handleUnderPromotions(move);
			}
		}
		
		public void addNormal(int move) {} // Doesn't deal with quiet moves by design
		
		public boolean isLegalMoveFound() { return false; }

		public void clearAttackedCache() { }
		
		protected void handleUnderPromotions(int move) {
			if (Move.isQueenPromotion(move)) {
				int under1 = Move.setPromotion(move, Piece.ROOK);
				int under2 = Move.setPromotion(move, Piece.BISHOP);
				int under3 = Move.setPromotion(move, Piece.KNIGHT);
				priority_moves[ply][priority_fill_index[ply]++] = under1;
				priority_moves[ply][priority_fill_index[ply]++] = under2;
				priority_moves[ply][priority_fill_index[ply]++] = under3;
				moveCount[ply]+=3;
			}
		}
	}
	
	public class QuietMovesWithNoKillers extends MoveAdderCapturesAndPromotions implements IAddMoves {
		
		long attackMask = 0L;
		boolean attacked = false;
		boolean attackedDetermined = false;
		
		@Override
		public void addPrio(int move) {} // Doesn't deal with tactical moves by design
		
		@Override
		public void addNormal(int move) {
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (bestMove[ply] != Move.NULL_MOVE && Move.areEqualForBestKiller(bestMove[ply], move)) {
					// Silently consume, the best move should already have been found and searched if not null
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
	
	public class QuietMovesConsumingKillers extends QuietMovesWithNoKillers implements IAddMoves {
		
		@Override
		public void addNormal(int move) {
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (bestMove[ply] != Move.NULL_MOVE && Move.areEqualForBestKiller(bestMove[ply], move)) {
					// Silently consume, the best move should already have been found and searched if not null
				} else if (KillerList.isMoveOnListAtPly(killers[ply], move)) {
					// Silently consume killers, they have already been searched
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
	}
	
	public class QuietMovesWithKillers extends QuietMovesConsumingKillers implements IAddMoves {
		
		@Override
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
	}
	
	public class AllMovesWithKillers extends QuietMovesWithKillers implements IAddMoves {
		
		@Override
		public void addPrio(int move) {
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (Move.areEqualForBestKiller(move, bestMove[ply])) {
					scratchpad[ply][scratchpad_fill_index[ply]++] = Move.setBest(move);
				} else {
					priority_moves[ply][priority_fill_index[ply]++] = move;
				}
				moveCount[ply]++;
				handleUnderPromotions(move);
			}
		}
		
		// Separate these two Move Adders into an extended search and a normal search version each...
		// Move to their own file and write unit tests for them.
		@Override
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
	}
	
	public class AllMovesWithNoKillers extends AllMovesWithKillers implements IAddMoves {
		
		@Override
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
