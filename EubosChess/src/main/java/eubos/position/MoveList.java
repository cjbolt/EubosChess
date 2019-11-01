package eubos.position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import com.fluxchess.jcpi.models.GenericChessman;
import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Piece.Colour;

public class MoveList implements Iterable<GenericMove> {
	
	private enum MoveClassification {
		// In order of natural priority
		BEST,
		PROMOTION_AND_CAPTURE_WITH_CHECK,
		PROMOTION_AND_CAPTURE,
		PROMOTION,
		CAPTURE_WITH_CHECK,
		CAPTURE,
		CASTLE,
		CHECK,
		REGULAR
	};
	
	private List<GenericMove> normal_search_moves;
	private List<GenericMove> extended_search_moves;
	private int normalSearchBestMovePreviousIndex = -1;
	private int extendedSearchListBestMovePreviousIndex = -1;
	
	public MoveList(PositionManager pm) {
		this(pm, null);
	}
	
	public MoveList(PositionManager pm, GenericMove bestMove) {
		// N.b. Need to use a linked hash map to ensure that the search order is deterministic.
		Map<GenericMove, MoveClassification> moveMap = new LinkedHashMap<GenericMove, MoveClassification>();
		Colour onMove = pm.getOnMove();
		for (GenericMove currMove : getRawList(pm)) {
			try {
				pm.performMove(currMove);
				if (pm.isKingInCheck(onMove)) {
					// Scratch any moves resulting in the king being in check
				} else {
					MoveClassification moveType;
					boolean isQueenPromotion = (currMove.promotion == GenericChessman.QUEEN);
					boolean isCapture = pm.lastMoveWasCapture();
					boolean isCheck = pm.isKingInCheck(Colour.getOpposite(onMove));
					boolean isCastle = (!isCapture) ? pm.lastMoveWasCaptureOrCastle() : false;
					
					if (isQueenPromotion && isCapture && isCheck) {
						moveType = MoveClassification.PROMOTION_AND_CAPTURE_WITH_CHECK;
					} else if (isQueenPromotion && isCapture) {
						moveType = MoveClassification.PROMOTION_AND_CAPTURE;
					} else if (isQueenPromotion) {
						moveType = MoveClassification.PROMOTION;
					} else if (isCapture && isCheck) {
						moveType = MoveClassification.CAPTURE_WITH_CHECK;
					} else if (isCapture) {
						moveType = MoveClassification.CAPTURE;
					} else if (isCastle) {
						moveType = MoveClassification.CASTLE;
					} else if (isCheck) {
						moveType = MoveClassification.CHECK;
					}  else {
						moveType = MoveClassification.REGULAR;
					}
					moveMap.put(currMove, moveType);
				}
				pm.unperformMove();
			} catch(InvalidPieceException e) {
				assert false;
			}
		}
		
		SortedSet<Map.Entry<GenericMove, MoveClassification>> moves = entriesSortedByValues(moveMap);
		normal_search_moves = create_normal_list(moves);
		extended_search_moves = create_extended_list(moves);
		
		if (bestMove != null) {
			seedListWithBestMove(normal_search_moves, bestMove);
			seedListWithBestMove(extended_search_moves, bestMove);
		}
	}
	
	private void seedListWithBestMove(List<GenericMove> list, GenericMove newBestMove) {
		assert list != null;
		if (list.contains(newBestMove)) {
			list.remove(newBestMove);
			list.add(0, newBestMove);
		}
	}
	
	static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
	    SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
	        new Comparator<Map.Entry<K,V>>() {
	            @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
	                int res = e1.getValue().compareTo(e2.getValue());
	                return res != 0 ? res : 1;
	            }
	        }
	    );
	    sortedEntries.addAll(map.entrySet());
	    return sortedEntries;
	}
	
	private List<GenericMove> getRawList(PositionManager pm) {
		ArrayList<GenericMove> entireMoveList = new ArrayList<GenericMove>();
		Iterator<Piece> iter_p = pm.getTheBoard().iterateColour(pm.getOnMove());
		while ( iter_p.hasNext() ) {
			Piece currPiece = iter_p.next();
			entireMoveList.addAll( currPiece.generateMoves( pm.getTheBoard() ));
		}
		pm.castling.addCastlingMoves(entireMoveList);
		return entireMoveList;
	}
	
	List<GenericMove> create_normal_list(SortedSet<Map.Entry<GenericMove, MoveClassification>> moves) {
		List<GenericMove> moveList = new ArrayList<GenericMove>();
		for (Map.Entry<GenericMove, MoveClassification> tuple : moves ) {
			moveList.add(tuple.getKey());
		}
		return moveList;
	}
	
	List<GenericMove> create_extended_list(SortedSet<Map.Entry<GenericMove, MoveClassification>> moves) {
		List<GenericMove> moveList = new ArrayList<GenericMove>();
		for (Map.Entry<GenericMove, MoveClassification> tuple : moves ) {
			switch(tuple.getValue()) {
			case BEST:
			case PROMOTION:
			case PROMOTION_AND_CAPTURE_WITH_CHECK:
			case PROMOTION_AND_CAPTURE:
			case CAPTURE_WITH_CHECK:
			case CAPTURE:
			case CHECK:
				moveList.add(tuple.getKey());
				break;
			default:
				break;
			}
		}
		return moveList;
	}
	
	public class MovesIterator implements Iterator<GenericMove> {

		private LinkedList<GenericMove> moveList = null;
	
		public MovesIterator(List<GenericMove> list) {
			moveList = new LinkedList<GenericMove>(list);
		}

		public boolean hasNext() {
			if (!moveList.isEmpty()) {
				return true;
			} else {
				return false;
			}
		}

		public GenericMove next() {
			return moveList.remove();
		}

		@Override
		public void remove() {
			moveList.remove();
		}
	}
	
	public Iterator<GenericMove> getIterator(boolean extended) {
		return new MovesIterator(extended ? extended_search_moves : normal_search_moves);
	}
	
	@Override
	public Iterator<GenericMove> iterator() {
		return new MovesIterator(normal_search_moves);
	}
		
	public boolean isMateOccurred() {
		return normal_search_moves.isEmpty();
	}
	
	public GenericMove getRandomMove() {
		GenericMove bestMove = null;
		if (!normal_search_moves.isEmpty()) {
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(normal_search_moves.size());
			bestMove = normal_search_moves.get(indexToGet);		
		}
		return bestMove;
	}

	public void reorderWithNewBestMove(GenericMove newBestMove) {
		normalSearchBestMovePreviousIndex = reorderList(normal_search_moves, newBestMove, normalSearchBestMovePreviousIndex);
		extendedSearchListBestMovePreviousIndex = reorderList(extended_search_moves, newBestMove, extendedSearchListBestMovePreviousIndex);
	}
	
	private int reorderList(List<GenericMove> list, GenericMove newBestMove, int prevBestOriginalIndex) {
		int index = list.indexOf(newBestMove);
		if (isMovePresent(index) && !isMoveAlreadyBest(index)) {
			
			if (wasPreviouslyModified(prevBestOriginalIndex)) {
				// Swap back the previous best move into its previous position, if this isn't a direct swap
				if (prevBestOriginalIndex == index) {
					// It is a direct swap back, set to -1 as the list is restored to its initial state
					prevBestOriginalIndex = -1;
				} else {
					Collections.swap(list, 0, prevBestOriginalIndex);
					prevBestOriginalIndex = index;
				}
			} else {
				// Initialise the previous best index the first time the best move is altered
				prevBestOriginalIndex = index;
			}
			
			// Swap in the new best move
			Collections.swap(list, 0, index);
		}
		return prevBestOriginalIndex;
	}
	
	private boolean isMovePresent(int index) { return (index != -1); }
	
	private boolean isMoveAlreadyBest(int index) { return (index == 0); }
	
	private boolean wasPreviouslyModified(int index) { return index != -1; }
}
