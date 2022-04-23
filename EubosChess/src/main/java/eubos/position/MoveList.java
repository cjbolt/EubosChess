package eubos.position;

import java.util.ArrayList;
import java.util.List;

import eubos.board.BitBoard;
import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.search.KillerList;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;

public class MoveList implements Iterable<Integer> {
	
	public static final boolean ALTERNATE = false;

	private int[][] normal_search_moves;
	private int[][] priority_moves;
	private int[][] extended_search_moves;
	private int[][] scratchpad;

	private int[] normal_fill_index;
	private int[] priority_fill_index;
	private int[] normal_list_length;
	private int[] nextCheckPoint;

	private boolean[] needToEscapeMate;
	private boolean[] extendedSearch;
	private boolean[] isWhite;
	private int[] moveCount;
	private int[] scratchpad_fill_index;
	private int[] bestMove;
	private int[][] killers;
	private long[] attackMask;

	private int ply;

	PositionManager pm;
	int ordering;

	public MoveAdderPromotions ma_promotions;
	public MoveAdderCaptures ma_captures;
	public MoveAdderCapturesAndSomeRegularConsumeKillers ma_captures_regular_ConsumeKillers;
	public MoveAdderCapturesAndSomeRegularNoKillers ma_captures_regular_NoKillers;

	public QuietMovesWithNoKillers ma_quietNoKillers;
	public QuietMovesConsumingKillers ma_quietConsumeKillers;

	private static final MoveTypeComparator moveTypeComparator = new MoveTypeComparator();

	static class MoveTypeComparator implements IntComparator {
		@Override
		public int compare(int move1, int move2) {
			boolean gt = Move.getType(move1) < Move.getType(move2);
			boolean eq = Move.getType(move1) == Move.getType(move2);
			return gt ? 1 : (eq ? 0 : -1);
		}
	}

	public MoveList(PositionManager pm, int orderMoveList) {

		scratchpad = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY][];

		// Create the move list arrays for this threads move list
		normal_search_moves = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY][];
		priority_moves = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY][];
		extended_search_moves = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY][];

		normal_fill_index = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		priority_fill_index = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		normal_list_length = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY];

		moveCount = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		scratchpad_fill_index = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		bestMove = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		needToEscapeMate = new boolean[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		extendedSearch = new boolean[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		isWhite = new boolean[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		killers = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY][3];
		attackMask = new long[EubosEngineMain.SEARCH_DEPTH_IN_PLY];

		nextCheckPoint = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY];

		// Create the list at each ply
		for (int i = 0; i < EubosEngineMain.SEARCH_DEPTH_IN_PLY; i++) {
			normal_search_moves[i] = new int[110];
			priority_moves[i] = new int[100];
			extended_search_moves[i] = new int[30];
			scratchpad[i] = new int[100];
		}

		this.pm = pm;
		this.ordering = orderMoveList;

		ma_promotions = new MoveAdderPromotions();
		ma_captures = new MoveAdderCaptures();
		ma_captures_regular_ConsumeKillers = new MoveAdderCapturesAndSomeRegularConsumeKillers();
		ma_captures_regular_NoKillers = new MoveAdderCapturesAndSomeRegularNoKillers();
		ma_quietNoKillers = new QuietMovesWithNoKillers();
		ma_quietConsumeKillers = new QuietMovesConsumingKillers();
	}

	public void initialise(int bestMove, int[] killers, boolean needToEscapeMate, boolean capturesOnly, int ply) {
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
	}
	
	public MoveListIterator stagedMoveGen(int ply) {
		MoveListIterator iter = null;
		this.ply = ply;

		switch (nextCheckPoint[ply]) {
		case 0:
			// Return best Move if valid
			nextCheckPoint[ply] = 1;
			if (bestMove[ply] != Move.NULL_MOVE) {
				if ((Move.isBest(bestMove[ply]) || bestMoveIsValid()) && (!extendedSearch[ply] || (extendedSearch[ply]
						&& (Move.isQueenPromotion(bestMove[ply]) || Move.isCapture(bestMove[ply]))))) {
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
			isWhite[ply] = pm.onMoveIsWhite();
			attackMask[ply] = extendedSearch[ply] ? 0L : pm.getTheBoard().pkaa.getAttacks(isWhite[ply]);
			getNonPawnPromotionCaptures();
			if ((moveCount[ply] - normal_fill_index[ply] - scratchpad_fill_index[ply]) != 0) {
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
			nextCheckPoint[ply] = 4;
			iter = checkKiller(0);
			if (iter != null)
				break;
		case 4:
			nextCheckPoint[ply] = 5;
			iter = checkKiller(1);
			if (iter != null)
				break;
		case 5:
			nextCheckPoint[ply] = 6;
			iter = checkKiller(2);
			if (iter != null)
				break;
		case 6:
			// Lastly, generate all moves that aren't best, killers, or tactical moves
			nextCheckPoint[ply] = 7;
			getQuietMoves();
			if (moveCount[ply] != 0) {
				collateMoveList();
				iter = iterator();
				break;
			}
			// Note fall-through to empty iterator if there are no quiet moves in the
			// position
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
			if (!Move.areEqualForBestKiller(bestMove[ply], killers[ply][killerNum])
					&& pm.getTheBoard().isPlayableMove(killers[ply][killerNum], needToEscapeMate[ply], pm.castling)) {
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
		boolean isWhiteOnMove = pm.onMoveIsWhite();
		pm.getTheBoard().getPawnPromotionMovesForSide(ma_promotions, isWhiteOnMove);
	}

	private void getNonPawnPromotionCaptures() {
		moveCount[ply] = 0;
		priority_fill_index[ply] = 0;
		if (ALTERNATE) {
			if (extendedSearch[ply]) {
				// N.b. In extended search, we have no killers and we don't check for regular
				// moves
				pm.getTheBoard().getCapturesExcludingPromotions(ma_captures, isWhite[ply]);
			} else if (killers[ply] == null) {
				ma_captures_regular_NoKillers.attackMask = attackMask[ply];
				pm.getTheBoard().getCapturesBufferRegularExcludingPromotions(ma_captures_regular_NoKillers,
						isWhite[ply]);
			} else {
				ma_captures_regular_ConsumeKillers.attackMask = attackMask[ply];
				pm.getTheBoard().getCapturesBufferRegularExcludingPromotions(ma_captures_regular_ConsumeKillers,
						isWhite[ply]);
			}
		} else {
			pm.getTheBoard().getCapturesExcludingPromotions(ma_captures, isWhite[ply]);
		}
	}

	private void getQuietMoves() {
		moveCount[ply] = normal_fill_index[ply] + scratchpad_fill_index[ply];
		priority_fill_index[ply] = 0;
		IAddMoves moveAdder = null;
		if (killers[ply] == null) {
			moveAdder = ma_quietNoKillers;
			ma_quietNoKillers.attackMask = attackMask[ply];
		} else {
			// Set-up move adder to filter the moves from attacked pieces into the priority
			// part of the move list
			moveAdder = ma_quietConsumeKillers;
			ma_quietConsumeKillers.attackMask = attackMask[ply];
		}
		if (ALTERNATE) {
			pm.getTheBoard().getLeftoverRegularExcludingPromotions(moveAdder, isWhite[ply]);
		} else {
			pm.getTheBoard().getRegularPieceMoves(moveAdder, isWhite[ply]);
		}
		if (!needToEscapeMate[ply]) {
			// Can't castle out of check and don't care in extended search
			pm.castling.addCastlingMoves(isWhite[ply], moveAdder);
		}
	}

	private void collateMoveList() {
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert scratchpad_fill_index[ply] <= 1 : "Scratchpad too long";
		}
		for (int j = 0; j < priority_fill_index[ply]; j++) {
			scratchpad[ply][scratchpad_fill_index[ply]++] = priority_moves[ply][j];
		}
		for (int j = 0; j < normal_fill_index[ply]; j++) {
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
				} else {
					priority_moves[ply][priority_fill_index[ply]++] = move;
					moveCount[ply]++;
				}
				handleUnderPromotions(move);
			}
		}

		public void addNormal(int move) {
		} // Doesn't deal with quiet moves by design

		public boolean isLegalMoveFound() {
			return false;
		}

		public void clearAttackedCache() {
		}

		protected void handleUnderPromotions(int move) {
			if (ply == 0 && Move.isQueenPromotion(move)) {
				int under1 = Move.setPromotion(move, Piece.ROOK);
				int under2 = Move.setPromotion(move, Piece.BISHOP);
				int under3 = Move.setPromotion(move, Piece.KNIGHT);
				priority_moves[ply][priority_fill_index[ply]++] = under1;
				priority_moves[ply][priority_fill_index[ply]++] = under2;
				priority_moves[ply][priority_fill_index[ply]++] = under3;
				moveCount[ply] += 3;
			}
		}
	}

	public class MoveAdderCaptures extends MoveAdderPromotions implements IAddMoves {
		@Override
		public void addPrio(int move) {
			if (Move.areEqualForBestKiller(move, bestMove[ply]))
				return;
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				priority_moves[ply][priority_fill_index[ply]++] = move;
				moveCount[ply]++;
			}
		}
	}

	public class MoveAdderCapturesAndSomeRegularConsumeKillers extends MoveAdderPromotions implements IAddMoves {
		long attackMask = 0L;
		boolean attacked = false;
		boolean attackedDetermined = false;

		@Override
		public void addPrio(int move) {
			if (Move.areEqualForBestKiller(move, bestMove[ply]))
				return;
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				priority_moves[ply][priority_fill_index[ply]++] = move;
				moveCount[ply]++;
			}
		}

		@Override
		public void addNormal(int move) {
			if (Move.areEqualForBestKiller(move, bestMove[ply]))
				return;
			if (KillerList.isMoveOnListAtPly(killers[ply], move))
				return;
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (attacked || (!attackedDetermined && isMoveOriginSquareAttacked(move))) {
					scratchpad[ply][scratchpad_fill_index[ply]++] = move;
					attacked = true;
				} else {
					normal_search_moves[ply][normal_fill_index[ply]++] = move;
				}
				attackedDetermined = true;
				moveCount[ply]++;
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

	public class MoveAdderCapturesAndSomeRegularNoKillers extends MoveAdderCapturesAndSomeRegularConsumeKillers implements IAddMoves {
		@Override
		public void addPrio(int move) {
			if (Move.areEqualForBestKiller(move, bestMove[ply]))
				return;
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				priority_moves[ply][priority_fill_index[ply]++] = move;
				moveCount[ply]++;
			}
		}

		@Override
		public void addNormal(int move) {
			if (Move.areEqualForBestKiller(move, bestMove[ply]))
				return;
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (attacked || (!attackedDetermined && isMoveOriginSquareAttacked(move))) {
					scratchpad[ply][scratchpad_fill_index[ply]++] = move;
					attacked = true;
				} else {
					normal_search_moves[ply][normal_fill_index[ply]++] = move;
				}
				attackedDetermined = true;
				moveCount[ply]++;
			}
		}
	}

	public class QuietMovesWithNoKillers extends MoveAdderCapturesAndSomeRegularConsumeKillers implements IAddMoves {
		@Override
		public void addPrio(int move) {
		} // Doesn't deal with tactical moves by design

		@Override
		public void addNormal(int move) {
			if (Move.areEqualForBestKiller(move, bestMove[ply]))
				return;
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (attacked || (!attackedDetermined && isMoveOriginSquareAttacked(move))) {
					priority_moves[ply][priority_fill_index[ply]++] = move;
					attacked = true;
				} else {
					normal_search_moves[ply][normal_fill_index[ply]++] = move;
				}
				attackedDetermined = true;
				moveCount[ply]++;
			}
		}
	}

	public class QuietMovesConsumingKillers extends QuietMovesWithNoKillers implements IAddMoves {
		@Override
		public void addNormal(int move) {
			if (Move.areEqualForBestKiller(move, bestMove[ply]))
				return;
			if (KillerList.isMoveOnListAtPly(killers[ply], move))
				return;
			if (!pm.getTheBoard().isIllegalMove(move, needToEscapeMate[ply])) {
				if (attacked || (!attackedDetermined && isMoveOriginSquareAttacked(move))) {
					priority_moves[ply][priority_fill_index[ply]++] = move;
					attacked = true;
				} else {
					normal_search_moves[ply][normal_fill_index[ply]++] = move;
				}
				attackedDetermined = true;
				moveCount[ply]++;
			}
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Test APIs
	//
	public List<Integer> getList() {
		List<Integer> ml = new ArrayList<Integer>();
		MoveListIterator it = stagedMoveGen(0);
		do {
			while (it.hasNext()) {
				ml.add(it.nextInt());
			}
			it = stagedMoveGen(0);
		} while (it.hasNext());
		return ml;
	}
}
