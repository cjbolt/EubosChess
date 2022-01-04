package eubos.position;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.search.KillerList;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;

public class MoveList implements Iterable<Integer> {
	
	public static final int MOVE_LIST_MAX_DEPTH_IN_PLY = 150;
	static private int[][] normal_search_moves;
	static private int[][] priority_moves;
	static private int[][] extended_search_moves;
	static private int[] scratchpad;
	static {
		scratchpad = new int [100];
		
		// Create the static move list arrays
		normal_search_moves = new int [MOVE_LIST_MAX_DEPTH_IN_PLY][];
		priority_moves = new int [MOVE_LIST_MAX_DEPTH_IN_PLY][];
		extended_search_moves = new int [MOVE_LIST_MAX_DEPTH_IN_PLY][];
		
		// Create the list at each ply
		for (int i=0; i < MOVE_LIST_MAX_DEPTH_IN_PLY; i++) {
			normal_search_moves[i] = new int[110];
			priority_moves[i] = new int[50];
			extended_search_moves[i] = new int[30];
		}
	}
	
	private int normal_fill_index;
	private int priority_fill_index;
	
	private int normal_list_length;
	
	private boolean isMate;
	private int ply;
	
	private static final MoveTypeComparator moveTypeComparator = new MoveTypeComparator();
	
    static class MoveTypeComparator implements IntComparator {
        @Override public int compare(int move1, int move2) {
            boolean gt = Move.getType(move1) < Move.getType(move2);
            boolean eq = Move.getType(move1) == Move.getType(move2);
            return gt ? 1 : (eq ? 0 : -1);
        }
    }
	
	public MoveList(PositionManager pm)  {
		this(pm, Move.NULL_MOVE, null, 1, false, pm.isKingInCheck(pm.getOnMove()), 0);
	}
	
	public MoveList(PositionManager pm, int ply)  {
		this(pm, Move.NULL_MOVE, null, 0, false, pm.isKingInCheck(pm.getOnMove()), ply);
	}
	
	public MoveList(PositionManager pm, int orderMoveList, boolean needToEscapeMate, int ply)  {
		this(pm, Move.NULL_MOVE, null, orderMoveList, false, needToEscapeMate, ply);
	}
	
	public MoveList(PositionManager pm, int bestMove, int [] killers, int orderMoveList, boolean capturesOnly, boolean needToEscapeMate, int ply)  {	
		
		this.ply = ply;
		priority_moves[ply][0] = Move.NULL_MOVE; // clear previous best move in the static movelist 
		
		normal_fill_index = 0;
		priority_fill_index = (bestMove != Move.NULL_MOVE) ? 1 : 0; // reserve space for the best move!
		
		Colour onMove = pm.getOnMove();
		boolean isWhiteOnMove = Piece.Colour.isWhite(onMove);
		
		pm.getTheBoard().getRegularPieceMoves(this, isWhiteOnMove, capturesOnly);
		if (!capturesOnly && !needToEscapeMate) {
			// Can't castle out of check and don't care in extended search
			pm.castling.addCastlingMoves(isWhiteOnMove, this);
		}
		
		int valid_move_count = removeInvalidIdentifyBestKillerMoves(pm, bestMove, killers, onMove, needToEscapeMate);
		isMate = (valid_move_count == 0);
		checkToSortList(orderMoveList);
		addPriorityMovesAtFrontAndRemoveNullMoves(valid_move_count);
	}
	
	private void addPriorityMovesAtFrontAndRemoveNullMoves(int valid_move_count) {
		if (!isMate) {
			int i = 0;
			for (int j=0; j < priority_fill_index; j++) {
				int move = priority_moves[ply][j];
				if (move != Move.NULL_MOVE) {
					scratchpad[i++] = move;
				}
			}
			// Update for number of valid priority moves, needed by lazy extended moves creation
			priority_fill_index = i;
			for (int j=0; j < normal_fill_index; j++) {
				int move = normal_search_moves[ply][j];
				if (move != Move.NULL_MOVE) {
					scratchpad[i++] = move;
				}
			}
			// Copy to existing array, without re-allocating static array
			int k=0;
			for (; k<i; k++) {
				normal_search_moves[ply][k] = scratchpad[k];
			}
			normal_list_length = k;
		} else {
			// There are no moves
			priority_fill_index = 0;
			normal_list_length = 0;
		}
	}

	private int removeInvalidIdentifyBestKillerMoves(PositionManager pm, int bestMove, int[] killers, Colour onMove,
			boolean needToEscapeMate)  {
		boolean validBest = bestMove != Move.NULL_MOVE;
		int foundBestMove = Move.NULL_MOVE;
		int valid_move_count = 0;
		
		if (validBest) {
			// Setup best move check on origin and target sq, not in transpo table
			int bestOriginPiece = pm.getTheBoard().getPieceAtSquare(Move.getOriginPosition(bestMove));
			bestMove = Move.setOriginPiece(bestMove, bestOriginPiece);
			int targetPiece = pm.getTheBoard().getPieceAtSquare(Move.getTargetPosition(bestMove));
			bestMove = Move.setTargetPiece(bestMove, targetPiece);
		}
		
		for (int i=0; i<priority_fill_index; i++) {
			int currMove = priority_moves[ply][i];
			if (currMove != Move.NULL_MOVE) {
				int originPiece = Move.getOriginPiece(currMove);
				boolean possibleDiscoveredOrMoveIntoCheck = Piece.isKing(originPiece) || 
						                                    pm.getTheBoard().moveCouldLeadToOwnKingDiscoveredCheck(currMove, originPiece);
				pm.performMove(currMove, false);
				if ((possibleDiscoveredOrMoveIntoCheck || needToEscapeMate) && pm.isKingInCheck(onMove)) {
					// Scratch any moves resulting in the king being in check, including moves that don't escape mate!
					priority_moves[ply][i] = Move.NULL_MOVE;
				} else {
					// Check whether to set the best move - note it could be the same as one of the killers
					boolean isBest = validBest && Move.areEqualForBestKiller(currMove, bestMove);
					if (isBest) {
						foundBestMove = Move.setBest(currMove);
						validBest = false; // as already found
						priority_moves[ply][i] = Move.NULL_MOVE;
					}
					valid_move_count++;
				}
				pm.unperformMove(false);
			}
		}
		if (foundBestMove != Move.NULL_MOVE) {
			priority_moves[ply][0] = foundBestMove; // add back in at the head of the list
		}
		
		for (int i=0; i<normal_fill_index; i++) {
			int currMove = normal_search_moves[ply][i];
			if (currMove != Move.NULL_MOVE) {
				int originPiece = Move.getOriginPiece(currMove);
				boolean possibleDiscoveredOrMoveIntoCheck = Piece.isKing(originPiece) || 
															pm.getTheBoard().moveCouldLeadToOwnKingDiscoveredCheck(currMove, originPiece);
				pm.performMove(currMove, false);
				if ((possibleDiscoveredOrMoveIntoCheck || needToEscapeMate) && pm.isKingInCheck(onMove)) {
					// Scratch any moves resulting in the king being in check, including moves that don't escape mate!
					normal_search_moves[ply][i] = Move.NULL_MOVE;
				} else {
					// Check whether to set the best move - note it could be the same as one of the killers, so check for best first
					boolean isBest = validBest && Move.areEqualForBestKiller(currMove, bestMove);
					if (isBest) {
						foundBestMove = Move.setBest(currMove);
						validBest = false; // as already found
						normal_search_moves[ply][i] = Move.NULL_MOVE;
						priority_moves[ply][0] = foundBestMove; // Add at head of priority list
						
					} else if (KillerList.isMoveOnListAtPly(killers, currMove)) {
						// Move was modified, add it to the priority list, where it will be sorted (add killers at end)
						currMove = Move.setKiller(currMove);
						normal_search_moves[ply][i] = Move.NULL_MOVE;
						addPrio(currMove); // Add to tail of prio
					}
					valid_move_count++;
				}
			}
			pm.unperformMove(false);
		}
		
		return valid_move_count;
	}
	
	private void checkToSortList(int orderMoveList) {
		switch (orderMoveList) {
		case 0:
			/* Don't order the move list in this case. */
			break;
		case 1:
			IntArrays.quickSort(priority_moves[ply], 0, priority_fill_index, Move.mvvLvaComparator);
			break;
		case 2:
			IntArrays.reverse(priority_moves[ply], 0, priority_fill_index);
			IntArrays.quickSort(priority_moves[ply], 0, priority_fill_index, moveTypeComparator);
			break;
		case 3:
			IntArrays.reverse(priority_moves[ply], 0, priority_fill_index);
			IntArrays.quickSort(priority_moves[ply], 0, priority_fill_index, Move.mvvLvaComparator);
			break;
		case 4:
			IntArrays.quickSort(priority_moves[ply], 0, priority_fill_index, moveTypeComparator);
			break;
		default:
			EubosEngineMain.logger.severe(String.format("Bad move ordering scheme %d!", orderMoveList));
			if (EubosEngineMain.ENABLE_ASSERTS)
				assert false;
			break;
		}
	}
	
	@Override
	public MoveListIterator iterator() {
		return new MoveListIterator(normal_search_moves[ply], normal_list_length);
	}
	
	public MoveListIterator getExtendedIterator() {
		// Lazy creation of extended moves
		int ext_count = 0;
		for (int i=0; i < priority_fill_index; i++) {
			int move = normal_search_moves[ply][i];
			boolean includeInQuiescenceSearch = Move.isCapture(move) || Move.isQueenPromotion(move);
			if (includeInQuiescenceSearch) {
				extended_search_moves[ply][ext_count++] = move;
			}
		}
		return new MoveListIterator(extended_search_moves[ply], ext_count);
	}
		
	public int getRandomMove() {
		int randomMove = Move.NULL_MOVE;
		if (!isMate) {
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(normal_list_length);
			randomMove = normal_search_moves[ply][indexToGet];		
		}
		return randomMove;
	}
	
	@Override
	public String toString() {
		String retVal = "";
		for (int move : normal_search_moves[ply]) {
			retVal += Move.toString(move);
			retVal += ", ";
		}
		return retVal;
	}

	public int getBestMove() {
		if (!isMate) {
			return normal_search_moves[ply][0];
		} else {
			return Move.NULL_MOVE;
		}
	}	
	
	public void addNormal(int move) {
		normal_search_moves[ply][normal_fill_index++] = move;
	}
	
	public void addPrio(int move) {
		priority_moves[ply][priority_fill_index++] = move;
	}
	
	// Test API
	boolean contains(int move) {
		for (int reg_move : IntArrays.trim(normal_search_moves[ply], normal_list_length)) {
			if (move == reg_move)
				return true;
		}
		return false;
	}
	
	public List<Integer> getList() {
		List<Integer> ml; 
		if (!isMate) {
			ml = IntStream.of(IntArrays.trim(normal_search_moves[ply], normal_list_length)).boxed().collect(Collectors.toList());
		} else {
			ml = new ArrayList<Integer>();
		}
		return ml;
	}
}
