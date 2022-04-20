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
	
	public static final boolean DEBUG_CHECK = false;
	private static final boolean SINGLE_KILLER_STAGE = false;
	
	private int [][] normal_search_moves;
	private int [][] priority_moves;
	private int [][] extended_search_moves;
	private int [][] scratchpad;
	
	private int [] normal_fill_index;
	private int [] priority_fill_index;
	private int [] normal_list_length;
	private int [] nextCheckPoint;
	
	private boolean [] needToEscapeMate;
	private boolean [] extendedSearch;
	private int [] moveCount;
	private int [] scratchpad_fill_index;
	private int [] extendedListScopeEndpoint;
	private int [] bestMove;
	private int [][] killers;
	
	private int ply;
	
	PositionManager pm;
	int ordering;
	
	public MoveAdderPromotions ma_promotions;
	public MoveAdderCaptures ma_captures;
	public MoveAdderCapturesAndSomeRegularConsumeKillers ma_captures_regular_ConsumeKillers;
	public MoveAdderCapturesAndSomeRegularNoKillers ma_captures_regular_NoKillers;
	
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
		extendedSearch = new boolean [EubosEngineMain.SEARCH_DEPTH_IN_PLY];
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
		ma_promotions = new MoveAdderPromotions();
		ma_captures = new MoveAdderCaptures();
		ma_captures_regular_ConsumeKillers = new MoveAdderCapturesAndSomeRegularConsumeKillers();
		ma_captures_regular_NoKillers = new MoveAdderCapturesAndSomeRegularNoKillers();
		ma_quietNoKillers = new QuietMovesWithNoKillers();
		ma_quietConsumeKillers = new QuietMovesConsumingKillers();
		ma_quietKillers = new QuietMovesWithKillers();
	}
	
	public void initialise(int bestMove, int [] killers, boolean needToEscapeMate, boolean capturesOnly, int ply) {
		// Initialise working variables for building the MoveList at this ply
		this.needToEscapeMate[ply] = needToEscapeMate;
		this.extendedSearch[ply] = capturesOnly;
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
		initialise(bestMove, killers, needToEscapeMate, capturesOnly, ply);
		
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
	
	public MoveListIterator stagedMoveGen(int ply)
	{
		MoveListIterator iter = null;
		this.ply = ply; 
		
		switch(nextCheckPoint[ply]) {
		case 0:
			// Return best Move if valid
			nextCheckPoint[ply] = 1;
			if (bestMove[ply] != Move.NULL_MOVE) {
				if ((Move.isBest(bestMove[ply]) || bestMoveIsValid()) &&
					(!extendedSearch[ply] || 
				     (extendedSearch[ply] && (Move.isQueenPromotion(bestMove[ply]) || Move.isCapture(bestMove[ply]))))) {
					scratchpad[ply][0] = bestMove[ply];
					iter = getBestIterator();
					break;
				}
				bestMove[ply] = Move.NULL_MOVE; // If it wasn't valid, invalidate it
			}
			// Note fall-through to next stage if no valid best move
		case 1:
			// Generate pawn promotions
			nextCheckPoint[ply] = 2;
			getPawnPromotions();
			if (moveCount[ply] != 0) {
				sortPriorityList();
				iter = new MoveListIterator(priority_moves[ply], priority_fill_index[ply]);
				break; // Return if there is a move in the iterator
			}
			// Note fall-through to next stage if no promotions
		case 2:
			// Generate all captures other than pawn promotions
			nextCheckPoint[ply] = 3;
			getNonPawnPromotionCaptures();
			if ((moveCount[ply]-normal_fill_index[ply]) != 0) {
				sortPriorityList();
				iter = new MoveListIterator(priority_moves[ply], priority_fill_index[ply]);
				break; // Return if there is a move in the iterator
			}
			// Note fall-through to next stage if no captures
		case 3:
			if (extendedSearch[ply]) {
				iter = new MoveListIterator(scratchpad[ply], 0);
				break;
			}
			// Generate Killers
			nextCheckPoint[ply] = 4;
			if (SINGLE_KILLER_STAGE) {
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
				// Note fall-through to next stage if no killer moves, or no valid killers for this position
			} else {
				iter = checkKiller(0);
				if (iter != null) break;
			}
		case 4:
			nextCheckPoint[ply] = 5;
			if (!SINGLE_KILLER_STAGE) {
				iter = checkKiller(1);
				if (iter != null) break;
			}
		case 5:
			nextCheckPoint[ply] = 6;
			if (!SINGLE_KILLER_STAGE) {
				iter = checkKiller(2);
				if (iter != null) break;
			}
		case 6:
			// Lastly, generate all moves that aren't best, killers, or tactical moves
			nextCheckPoint[ply] = 7;
			getQuietMoves();
			if (moveCount[ply] != 0) {
				collateMoveList();
				iter = iterator();
				break;
			}
			// Note fall-through to empty iterator if there are no quiet moves in the position
		case 7:
		default:
			iter = new MoveListIterator(scratchpad[ply], 0); // Empty iterator
			break;
		}
		return iter;
	}
	
	MoveListIterator checkKiller(int killerNum) {
		MoveListIterator iter = null;
		if (killers[ply] != null) {
			if (!Move.areEqualForBestKiller(bestMove[ply], killers[ply][killerNum]) &&
				pm.getTheBoard().isPlayableMove(killers[ply][killerNum], needToEscapeMate[ply], pm.castling)) {
				scratchpad[ply][0] = killers[ply][killerNum];
				iter = getBestIterator();
			}
		}
		return iter;
	}
	
	private boolean bestMoveIsValid() {
		return pm.getTheBoard().isPlayableMove(bestMove[ply], needToEscapeMate[ply], pm.castling);
	}
	
	private void getPawnPromotions() {
		moveCount[ply] = 0;
		priority_fill_index[ply] = 0;
		boolean isWhiteOnMove = pm.onMoveIsWhite();
		pm.getTheBoard().getPawnPromotionMovesForSide(ma_promotions, isWhiteOnMove);
	}
	
	private void getNonPawnPromotionCaptures() {
		moveCount[ply] = 0;
		priority_fill_index[ply] = 0;
		boolean isWhiteOnMove = pm.onMoveIsWhite();
		if (extendedSearch[ply]) {
			// N.b. In extended search, we have no killers and we don't check for regular moves
			pm.getTheBoard().getCapturesExcludingPromotions(ma_captures, isWhiteOnMove);
		} else {
			if (killers[ply] == null) {
				pm.getTheBoard().getCapturesBufferRegularExcludingPromotions(ma_captures_regular_NoKillers, isWhiteOnMove);
			} else {
				pm.getTheBoard().getCapturesBufferRegularExcludingPromotions(ma_captures_regular_ConsumeKillers, isWhiteOnMove);
			}
		}
	}
	
	private void getQuietMoves() {
		moveCount[ply] = normal_fill_index[ply];
		priority_fill_index[ply] = 0;
		IAddMoves moveAdder = null;
		boolean isWhiteOnMove = pm.onMoveIsWhite();
		long attackMask = pm.getTheBoard().pkaa.getAttacks(isWhiteOnMove);
		if (killers[ply] == null) {
			moveAdder = ma_quietNoKillers; 
			ma_quietNoKillers.attackMask = attackMask;
		} else {
			// Set-up move adder to filter the moves from attacked pieces into the priority part of the move list
			moveAdder = ma_quietConsumeKillers;
			ma_quietConsumeKillers.attackMask = attackMask;
		}
		pm.getTheBoard().getLeftoverRegularExcludingPromotions(moveAdder, isWhiteOnMove);
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
		pm.getTheBoard().getRegularPieceMoves(moveAdder, isWhiteOnMove);
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
	
	public class MoveAdderPromotions implements IAddMoves {
		
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
			if (!extendedSearch[ply] && Move.isQueenPromotion(move)) {
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
	
	public class MoveAdderCaptures extends MoveAdderPromotions implements IAddMoves {
		@Override
		public void addPrio(int move) {
			if (Move.areEqualForBestKiller(move, bestMove[ply])) return;
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				priority_moves[ply][priority_fill_index[ply]++] = move;
				moveCount[ply]++;
			}
		}
	}
	
	public class MoveAdderCapturesAndSomeRegularConsumeKillers extends MoveAdderCaptures implements IAddMoves {
		@Override
		public void addNormal(int move) {
			if (Move.areEqualForBestKiller(move, bestMove[ply])) return;
			if (KillerList.isMoveOnListAtPly(killers[ply], move)) return;
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {	
				normal_search_moves[ply][normal_fill_index[ply]++] = move;
				moveCount[ply]++;
			}
		}
	}
	
	public class MoveAdderCapturesAndSomeRegularNoKillers extends MoveAdderCaptures implements IAddMoves {
		@Override
		public void addNormal(int move) {
			if (Move.areEqualForBestKiller(move, bestMove[ply])) return;
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {	
				normal_search_moves[ply][normal_fill_index[ply]++] = move;
				moveCount[ply]++;
			}
		}
	}
	
	public class QuietMovesWithNoKillers extends MoveAdderPromotions implements IAddMoves {
		
		long attackMask = 0L;
		boolean attacked = false;
		boolean attackedDetermined = false;
		
		@Override
		public void addPrio(int move) {} // Doesn't deal with tactical moves by design
		
		@Override
		public void addNormal(int move) {
			if (Move.areEqualForBestKiller(move, bestMove[ply])) return;
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (attacked || (!attackedDetermined && isMoveOriginSquareAttacked(move))) {
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
			if (Move.areEqualForBestKiller(move, bestMove[ply])) return;
			if (KillerList.isMoveOnListAtPly(killers[ply], move)) return;
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (attacked || (!attackedDetermined && isMoveOriginSquareAttacked(move))) {
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
			if (Move.areEqualForBestKiller(move, bestMove[ply])) return;
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (KillerList.isMoveOnListAtPly(killers[ply], move)) {
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
