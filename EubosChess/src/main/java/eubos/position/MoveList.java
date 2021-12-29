package eubos.position;

import java.util.ArrayList;
import java.util.Arrays;
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
	
	private int[] normal_search_moves;
	private int[] priority_moves;
	
	private int normal_fill_index;
	private int priority_fill_index;
	
	private int [] extended_search_moves;
	
	private boolean isMate;
	
	private static final MoveTypeComparator moveTypeComparator = new MoveTypeComparator();
	
    static class MoveTypeComparator implements IntComparator {
        @Override public int compare(int move1, int move2) {
            boolean gt = Move.getType(move1) < Move.getType(move2);
            boolean eq = Move.getType(move1) == Move.getType(move2);
            return gt ? 1 : (eq ? 0 : -1);
        }
    }
	
	public MoveList(PositionManager pm)  {
		this(pm, Move.NULL_MOVE, null, 0, false, pm.isKingInCheck(pm.getOnMove()));
	}
	
	public MoveList(PositionManager pm, int orderMoveList)  {
		this(pm, Move.NULL_MOVE, null, orderMoveList, false, pm.isKingInCheck(pm.getOnMove()));
	}
	
	public MoveList(PositionManager pm, int orderMoveList, boolean needToEscapeMate)  {
		this(pm, Move.NULL_MOVE, null, orderMoveList, false, needToEscapeMate);
	}
	
	public MoveList(PositionManager pm, int bestMove, int [] killers, int orderMoveList, boolean capturesOnly, boolean needToEscapeMate)  {	
		
		normal_search_moves = new int[40];
		priority_moves = new int[20];
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
		int [] condensed_array = new int[valid_move_count];
		if (!isMate) {
			int i = 0;
			for (int j=0; j < priority_fill_index; j++) {
				int move = priority_moves[j];
				if (move != Move.NULL_MOVE) {
					condensed_array[i++] = move;
				}
			}
			for (int j=0; j < normal_fill_index; j++) {
				int move = normal_search_moves[j];
				if (move != Move.NULL_MOVE) {
					condensed_array[i++] = move;
				}
			}
		}
		normal_search_moves = condensed_array;
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
			int currMove = priority_moves[i];
			if (currMove != Move.NULL_MOVE) {
				int originPiece = Move.getOriginPiece(currMove);
				boolean possibleDiscoveredOrMoveIntoCheck = Piece.isKing(originPiece) || 
						                                    pm.getTheBoard().moveCouldLeadToOwnKingDiscoveredCheck(currMove, originPiece);
				pm.performMove(currMove, false);
				if ((possibleDiscoveredOrMoveIntoCheck || needToEscapeMate) && pm.isKingInCheck(onMove)) {
					// Scratch any moves resulting in the king being in check, including moves that don't escape mate!
					priority_moves[i] = Move.NULL_MOVE;
				} else {
					// Check whether to set the best move - note it could be the same as one of the killers
					boolean isBest = validBest && Move.areEqualForBestKiller(currMove, bestMove);
					if (isBest) {
						foundBestMove = Move.setBest(currMove);
						validBest = false; // as already found
						priority_moves[i] = Move.NULL_MOVE;
					}
					valid_move_count++;
				}
				pm.unperformMove(false);
			}
		}
		if (foundBestMove != Move.NULL_MOVE) {
			priority_moves[0] = foundBestMove; // add back in at the head of the list
		}
		
		for (int i=0; i<normal_fill_index; i++) {
			int currMove = normal_search_moves[i];
			if (currMove != Move.NULL_MOVE) {
				int originPiece = Move.getOriginPiece(currMove);
				boolean possibleDiscoveredOrMoveIntoCheck = Piece.isKing(originPiece) || 
															pm.getTheBoard().moveCouldLeadToOwnKingDiscoveredCheck(currMove, originPiece);
				pm.performMove(currMove, false);
				if ((possibleDiscoveredOrMoveIntoCheck || needToEscapeMate) && pm.isKingInCheck(onMove)) {
					// Scratch any moves resulting in the king being in check, including moves that don't escape mate!
					normal_search_moves[i] = Move.NULL_MOVE;
				} else {
					// Check whether to set the best move - note it could be the same as one of the killers, so check for best first
					boolean isBest = validBest && Move.areEqualForBestKiller(currMove, bestMove);
					if (isBest) {
						foundBestMove = Move.setBest(currMove);
						validBest = false; // as already found
						normal_search_moves[i] = Move.NULL_MOVE;
						priority_moves[0] = foundBestMove; // Add at head of priority list
						
					} else if (KillerList.isMoveOnListAtPly(killers, currMove)) {
						// Move was modified, add it to the priority list, where it will be sorted (add killers at end)
						currMove = Move.setKiller(currMove);
						normal_search_moves[i] = Move.NULL_MOVE;
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
			IntArrays.quickSort(priority_moves, 0, priority_fill_index, Move.mvvLvaComparator);
			break;
		case 2:
			IntArrays.reverse(priority_moves, 0, priority_fill_index);
			IntArrays.quickSort(priority_moves, 0, priority_fill_index, moveTypeComparator);
			break;
		case 3:
			IntArrays.reverse(priority_moves, 0, priority_fill_index);
			IntArrays.quickSort(priority_moves, 0, priority_fill_index, Move.mvvLvaComparator);
			break;
		case 4:
			IntArrays.quickSort(priority_moves, 0, priority_fill_index, moveTypeComparator);
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
		return new MoveListIterator(normal_search_moves);
	}
	
	public MoveListIterator getExtendedIterator() {
		// Lazy creation of extended move list
		extended_search_moves = new int [priority_fill_index];
		int i =0;
		for (int currMove : Arrays.copyOfRange(priority_moves, 0, priority_fill_index)) {
			boolean includeInQuiescenceSearch = Move.isQueenPromotion(currMove) || Move.isCapture(currMove);
			if (includeInQuiescenceSearch) {
				extended_search_moves[i++] = currMove;
			}
		}
		return new MoveListIterator(IntArrays.trim(extended_search_moves, i)); 
	}
		
	public boolean isMateOccurred() {
		return (normal_search_moves.length == 0);
	}
	
	public int getRandomMove() {
		int bestMove = Move.NULL_MOVE;
		if (!isMateOccurred()) {
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(normal_search_moves.length);
			bestMove = normal_search_moves[indexToGet];		
		}
		return bestMove;
	}
	
	@Override
	public String toString() {
		String retVal = "";
		for (int move : this.normal_search_moves) {
			retVal += Move.toString(move);
			retVal += ", ";
		}
		return retVal;
	}

	public int getBestMove() {
		if (normal_search_moves.length != 0) {
			return normal_search_moves[0];
		} else {
			return Move.NULL_MOVE;
		}
	}	
	
	public void addNormal(int move) {
		if (normal_fill_index >= normal_search_moves.length-1) {
			normal_search_moves = IntArrays.grow(normal_search_moves, normal_search_moves.length+40);
		}
		this.normal_search_moves[normal_fill_index++] = move;
	}
	
	public void addPrio(int move) {
		if (priority_fill_index >= priority_moves.length-1) {
			priority_moves = IntArrays.grow(priority_moves, priority_moves.length+20);
		}
		this.priority_moves[priority_fill_index++] = move;
	}
	
	// Test API
	boolean contains(int move) {
		for (int reg_move : normal_search_moves) {
			if (move == reg_move)
				return true;
		}
		return false;
	}
	
	public List<Integer> getList() {
		List<Integer> ml; 
		if (!isMate) {
			ml = IntStream.of(normal_search_moves).boxed().collect(Collectors.toList());
		} else {
			ml = new ArrayList<Integer>();
		}
		return ml;
	}
}
