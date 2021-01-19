package eubos.position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.search.KillerList;

public class MoveList implements Iterable<Integer> {
	
	private List<Integer> normal_search_moves;
	private List<Integer> extended_search_moves;
	private static final MoveTypeComparator moveTypeComparator = new MoveTypeComparator();
	
    static class MoveTypeComparator implements Comparator<Integer> {
        @Override public int compare(Integer move1, Integer move2) {
            boolean gt = Move.getType(move1) < Move.getType(move2);
            boolean eq = Move.getType(move1) == Move.getType(move2);
            return gt ? 1 : (eq ? 0 : -1);
        }
    }
	
	public MoveList(PositionManager pm) throws InvalidPieceException {
		this(pm, Move.NULL_MOVE, Move.NULL_MOVE, Move.NULL_MOVE, 1 );
	}
	
	public MoveList(PositionManager pm, int orderMoveList) throws InvalidPieceException {
		this(pm, Move.NULL_MOVE, Move.NULL_MOVE, Move.NULL_MOVE, orderMoveList );
	}
	
	public MoveList(PositionManager pm, int bestMove, int killer1, int killer2, int orderMoveList) throws InvalidPieceException {
		this(pm, bestMove, killer1, killer2, orderMoveList, Position.NOPOSITION);
	}
	
	public MoveList(PositionManager pm, int bestMove, int killer1, int killer2, int orderMoveList, int targetSquare) throws InvalidPieceException {	
		Colour onMove = pm.getOnMove();
		boolean needToEscapeMate = pm.isKingInCheck(onMove);

		normal_search_moves = pm.generateMoves(targetSquare);
		int foundBestMove = removeInvalidIdentifyBestKillerMoves(pm, bestMove, killer1, killer2, onMove, needToEscapeMate);
		if (foundBestMove != Move.NULL_MOVE) {
			normal_search_moves.add(0, foundBestMove);
		}
		checkToSortList(orderMoveList);
	}

	private int removeInvalidIdentifyBestKillerMoves(PositionManager pm, int bestMove, int killer1, int killer2, Colour onMove,
			boolean needToEscapeMate) throws InvalidPieceException {
		boolean validBest = bestMove != Move.NULL_MOVE;
		boolean validKillerMove1 = killer1 != Move.NULL_MOVE;
		boolean validKillerMove2 = killer2 != Move.NULL_MOVE;
		int foundBestMove = Move.NULL_MOVE;
		
		ListIterator<Integer> it = normal_search_moves.listIterator();
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
				
				if (KillerList.ENABLE_KILLER_MOVES) {
					// Check whether to set Killer flags
					boolean isKiller1 = validKillerMove1 && Move.areEqualForBestKiller(currMove, killer1);
					if (isKiller1) {
						validKillerMove1 = false; // as already found
					}
					boolean isKiller2 = validKillerMove2 && Move.areEqualForBestKiller(currMove, killer2);
					if (isKiller2) {
						validKillerMove2 = false; // as already found
					}
					if (!isBest && (isKiller1 || isKiller2)) {
						// Move was modified, update it using the iterator
						currMove = Move.setKiller(currMove);
						it.set(currMove);
					}
				}
			}
			pm.unperformMove(false);
		}
		return foundBestMove;
	}

	private void checkToSortList(int orderMoveList) {
		switch (orderMoveList) {
		case 0:
			/* Don't order the move list in this case. */
			break;
		case 1:
			Collections.sort(normal_search_moves, Move.mvvLvaComparator);
			break;
		case 2:
			Collections.reverse(normal_search_moves);
			Collections.sort(normal_search_moves, moveTypeComparator);
			break;
		case 3:
			Collections.reverse(normal_search_moves);
			Collections.sort(normal_search_moves, Move.mvvLvaComparator);
			break;
		case 4:
			Collections.sort(normal_search_moves, moveTypeComparator);
			break;
		default:
			EubosEngineMain.logger.severe(String.format("Bad move ordering scheme %d!", orderMoveList));
			if (EubosEngineMain.ASSERTS_ENABLED)
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
			extended_search_moves = new ArrayList<Integer>(normal_search_moves.size());
			for (int currMove : normal_search_moves) {
				if ((Move.isCapture(currMove) && (Move.getTargetPosition(currMove) == captureSq)) || Move.isQueenPromotion(currMove)) {
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

	public boolean hasMultipleRegularMoves() {
		int count = 0;
		for (int move : normal_search_moves) {
			if (Move.isRegular(move)) {
				count++;
				if (count == 2 )
					return true;
			}
		}
		return false;
	}

	public int getBestMove() {
		if (normal_search_moves.size() != 0) {
			return normal_search_moves.get(0);
		} else {
			return Move.NULL_MOVE;
		}
	}
	
	public int getSafestMove() {
		if (normal_search_moves.size() != 0) {
			/* The logic is to avoid checks, which will be in the highest prio indexes of the ml */
			return normal_search_moves.get(normal_search_moves.size()-1);
		} else {
			return Move.NULL_MOVE;
		}
	}
	
	// Test API
	boolean contains(int move) {
		for (int reg_move : normal_search_moves) {
			if (move == reg_move)
				return true;
		}
		return false;
	}
}
