package eubos.position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import com.fluxchess.jcpi.models.GenericMove;


import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.search.KillerList;

public class MoveList implements Iterable<Integer> {
	
	private List<Integer> normal_search_moves;
	private List<Integer> priority_moves;
	private List<Integer> extended_search_moves;
	private static final MoveTypeComparator moveTypeComparator = new MoveTypeComparator();
	
    static class MoveTypeComparator implements Comparator<Integer> {
        @Override public int compare(Integer move1, Integer move2) {
            boolean gt = Move.getType(move1) < Move.getType(move2);
            boolean eq = Move.getType(move1) == Move.getType(move2);
            return gt ? 1 : (eq ? 0 : -1);
        }
    }
	
	public MoveList(PositionManager pm)  {
		this(pm, Move.NULL_MOVE, null, 1, false, Position.NOPOSITION);
	}
	
	public MoveList(PositionManager pm, int orderMoveList)  {
		this(pm, Move.NULL_MOVE, null, orderMoveList, false, Position.NOPOSITION);
	}
	
	public MoveList(PositionManager pm, int bestMove, int [] killers, int orderMoveList)  {
		this(pm, bestMove, killers, orderMoveList, false, Position.NOPOSITION);
	}	
	
	public MoveList(PositionManager pm, int bestMove, int [] killers, int orderMoveList, boolean capturesOnly, int targetPosition)  {	
		
		normal_search_moves = new LinkedList<Integer>();
		priority_moves = new LinkedList<Integer>();
		
		Colour onMove = pm.getOnMove();
		boolean needToEscapeMate = pm.isKingInCheck(onMove);
		boolean isWhiteOnMove = Piece.Colour.isWhite(onMove);
		
		pm.getTheBoard().getRegularPieceMoves(this, isWhiteOnMove, capturesOnly, targetPosition);
		if (!capturesOnly) {
			pm.castling.addCastlingMoves(isWhiteOnMove, this);
		}
		
		removeInvalidIdentifyBestKillerMoves(pm, bestMove, killers, onMove, needToEscapeMate);
		checkToSortList(orderMoveList);
		
		normal_search_moves.addAll(0, priority_moves);
	}

	private void removeInvalidIdentifyBestKillerMoves(PositionManager pm, int bestMove, int[] killers, Colour onMove,
			boolean needToEscapeMate)  {
		boolean validBest = bestMove != Move.NULL_MOVE;
		int foundBestMove = Move.NULL_MOVE;
		
		ListIterator<Integer> it = priority_moves.listIterator();
		while (it.hasNext()) {
			int currMove = it.next();
			int originPiece = Move.getOriginPiece(currMove);
			boolean possibleDiscoveredOrMoveIntoCheck = pm.getTheBoard().moveCouldLeadToOwnKingDiscoveredCheck(currMove, originPiece) || 
														Piece.isKing(originPiece);
			pm.performMove(currMove, false);
			if ((possibleDiscoveredOrMoveIntoCheck || needToEscapeMate) && pm.isKingInCheck(onMove)) {
				// Scratch any moves resulting in the king being in check, including moves that don't escape mate!
				it.remove();
			} else {
				// Check whether to set the best move - note it could be the same as one of the killers
				boolean isBest = validBest && Move.areEqualForBestKiller(currMove, bestMove);
				if (isBest) {
					foundBestMove = Move.setBest(currMove);
					validBest = false; // as already found
					it.remove();
				}
			}
			pm.unperformMove(false);
		}
		if (foundBestMove != Move.NULL_MOVE) {
			priority_moves.add(0, foundBestMove); // add back in at the head of the list
		}
		
		it = normal_search_moves.listIterator();
		while (it.hasNext()) {
			int currMove = it.next();
			int originPiece = Move.getOriginPiece(currMove);
			boolean possibleDiscoveredOrMoveIntoCheck = pm.getTheBoard().moveCouldLeadToOwnKingDiscoveredCheck(currMove, originPiece) || 
														Piece.isKing(originPiece);
			pm.performMove(currMove, false);
			if ((possibleDiscoveredOrMoveIntoCheck || needToEscapeMate) && pm.isKingInCheck(onMove)) {
				// Scratch any moves resulting in the king being in check, including moves that don't escape mate!
				it.remove();
			} else {
				// Check whether to set the best move - note it could be the same as one of the killers, so check for best first
				boolean isBest = validBest && Move.areEqualForBestKiller(currMove, bestMove);
				if (isBest) {
					foundBestMove = Move.setBest(currMove);
					validBest = false; // as already found
					it.remove();
					priority_moves.add(0, foundBestMove); // Add at head of priority list
					
				} else if (KillerList.isMoveOnListAtPly(killers, currMove)) {
					// Move was modified, add it to the priority list, where it will be sorted (add killers at end)
					currMove = Move.setKiller(currMove);
					it.remove();
					priority_moves.add(currMove);
				}
			}
			pm.unperformMove(false);
		}
	}
	
	private void checkToSortList(int orderMoveList) {
		switch (orderMoveList) {
		case 0:
			/* Don't order the move list in this case. */
			break;
		case 1:
			Collections.sort(priority_moves, Move.mvvLvaComparator);
			break;
		case 2:
			Collections.reverse(priority_moves);
			Collections.sort(priority_moves, moveTypeComparator);
			break;
		case 3:
			Collections.reverse(priority_moves);
			Collections.sort(priority_moves, Move.mvvLvaComparator);
			break;
		case 4:
			Collections.sort(priority_moves, moveTypeComparator);
			break;
		default:
			EubosEngineMain.logger.severe(String.format("Bad move ordering scheme %d!", orderMoveList));
			if (EubosEngineMain.ENABLE_ASSERTS)
				assert false;
			break;
		}
	}
	
	@Override
	public Iterator<Integer> iterator() {
		return normal_search_moves.iterator();
	}
	
	public Iterator<Integer> getStandardIterator(boolean extended, int captureSq) {
		Iterator<Integer> it;
		if (extended) {
			// Lazy creation of extended move list
			extended_search_moves = new ArrayList<Integer>(priority_moves.size());
			for (int currMove : priority_moves) {
				boolean includeInQuiescenceSearch = Move.isQueenPromotion(currMove) || (Move.isCapture(currMove) && 
						(Move.getTargetPosition(currMove) == captureSq || EubosEngineMain.ENABLE_SEARCH_ALL_CAPTURES_IN_QUIESCENSE));
				if (includeInQuiescenceSearch) {
					extended_search_moves.add(currMove);
				}
			}
			it = extended_search_moves.iterator();
		} else {
			it = normal_search_moves.iterator();
		}
		return it; 
	}
		
	public boolean isMateOccurred() {
		return (normal_search_moves.size() == 0);
	}
	
	public GenericMove getRandomMove() {
		GenericMove bestMove = null;
		if (!isMateOccurred()) {
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(normal_search_moves.size());
			bestMove = Move.toGenericMove(normal_search_moves.get(indexToGet));		
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
		if (normal_search_moves.size() != 0) {
			return normal_search_moves.get(0);
		} else {
			return Move.NULL_MOVE;
		}
	}
	
	
	public void addNormal(int move) {
		this.normal_search_moves.add(move);
	}
	
	public void addPrio(int move) {
		this.priority_moves.add(move);
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
		return normal_search_moves;		
	}
}
