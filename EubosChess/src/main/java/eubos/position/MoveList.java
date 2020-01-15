package eubos.position;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.IntConsumer;

import com.fluxchess.jcpi.models.GenericChessman;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IntChessman;

import eubos.board.InvalidPieceException;
import eubos.board.Piece.Colour;

public class MoveList implements Iterable<Integer> {
	
	public enum MoveClassification {
		// In order of natural priority
		PROMOTION_AND_CAPTURE_WITH_CHECK,
		PROMOTION_AND_CAPTURE,
		PROMOTION,
		OTHER_PROMOTION,
		CAPTURE_WITH_CHECK,
		CAPTURE,
		CASTLE,
		CHECK,
		REGULAR
	};
	
	private int[] normal_search_moves;
	private int[] extended_search_moves;
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
					boolean isPromotion = (currMove.promotion == GenericChessman.BISHOP
							|| currMove.promotion == GenericChessman.ROOK
							|| currMove.promotion == GenericChessman.KNIGHT);
					boolean isCapture = pm.lastMoveWasCapture();
					boolean isCheck = pm.isKingInCheck(Colour.getOpposite(onMove));
					boolean isCastle = pm.lastMoveWasCastle();
					
					if (isQueenPromotion && isCapture && isCheck) {
						moveType = MoveClassification.PROMOTION_AND_CAPTURE_WITH_CHECK;
					} else if (isQueenPromotion && isCapture) {
						moveType = MoveClassification.PROMOTION_AND_CAPTURE;
					} else if (isQueenPromotion) {
						moveType = MoveClassification.PROMOTION;
					} else if (isPromotion) {
						moveType = MoveClassification.OTHER_PROMOTION;
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
		
		int intBestMove = 0;
		if (bestMove != null) {
			for (Map.Entry<GenericMove, MoveClassification> tuple : moves ) {
				GenericMove currMove = tuple.getKey();
				if (currMove.equals(bestMove)) {
					intBestMove = Move.toMove(bestMove, tuple.getValue());
					break;
				}
			}
			seedListWithBestMove(normal_search_moves, intBestMove);
			seedListWithBestMove(extended_search_moves, intBestMove);
		}
	}
	
	private int getIndex(int[] moveArray, int move) {
		int index = 0;
		boolean found = false;
		for (int current : moveArray) {
			if (move == current) {
				found = true;
				break;	
			}
			index++;
		}
		if (!found) {
			index = -1;
		}
		return index;
	}
	
	private void swapWithFirst(int[] moveArray, int index) {
		if (isMovePresent(index) && index < moveArray.length) {
			int temp = moveArray[0];
			moveArray[0] = moveArray[index];
			moveArray[index] = temp;
		}
	}
	
	private void seedListWithBestMove(int[] moveArray, int newBestMove) {
		int index = getIndex(moveArray, newBestMove);
	    swapWithFirst(moveArray, index);
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
		List<GenericMove> entireMoveList = pm.generateMoves();
		return entireMoveList;
	}
	
	int [] create_normal_list(SortedSet<Map.Entry<GenericMove, MoveClassification>> moves) {
		int [] moveArray = new int[moves.size()];
		int index = 0;
		for (Map.Entry<GenericMove, MoveClassification> tuple : moves ) {
			GenericMove currMove = tuple.getKey();
			MoveClassification type = tuple.getValue();
			moveArray[index++] = Move.toMove(currMove, type);
		}
		return moveArray;
	}
	
	int[] create_extended_list(SortedSet<Map.Entry<GenericMove, MoveClassification>> moves) {
		List<Integer> list = new ArrayList<Integer>();
		for (Map.Entry<GenericMove, MoveClassification> tuple : moves ) {
			switch(tuple.getValue()) {
			case PROMOTION:
			case PROMOTION_AND_CAPTURE_WITH_CHECK:
			case PROMOTION_AND_CAPTURE:
			case OTHER_PROMOTION:
			case CAPTURE_WITH_CHECK:
			case CAPTURE:
			case CHECK:
				list.add(Move.toMove(tuple.getKey(), tuple.getValue()));
				break;
			default:
				break;
			}
		}
		int[] array = new int[list.size()];
		for (int i=0; i<array.length; i++) {
			array[i] = list.get(i);
		}
		return array;
	}
	
	public class MovesIterator implements PrimitiveIterator.OfInt {

		private int[] moveList = null;
		private int next = 0;
	
		public MovesIterator(int[] array) {
			moveList = array.clone();
			next = 0;
		}

		public boolean hasNext() {
			return next != moveList.length;
		}

		public Integer next() {
			return moveList[next++];
		}

		@Override
		public void remove() {
		}

		@Override
		public void forEachRemaining(IntConsumer action) {
			
		}

		@Override
		public int nextInt() {
			return moveList[next++];
		}
	}
	
	public PrimitiveIterator.OfInt getIterator(boolean extended) {
		return new MovesIterator(extended ? extended_search_moves : normal_search_moves);
	}
	
	@Override
	public PrimitiveIterator.OfInt iterator() {
		return new MovesIterator(normal_search_moves);
	}
		
	public boolean isMateOccurred() {
		return (normal_search_moves.length == 0);
	}
	
	public GenericMove getRandomMove() {
		GenericMove bestMove = null;
		if (!isMateOccurred()) {
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(normal_search_moves.length);
			bestMove = Move.toGenericMove(normal_search_moves[indexToGet]);		
		}
		return bestMove;
	}
	
	public MoveClassification getMoveTypeFromNormalList(GenericMove genericMove) {
		MoveClassification type = MoveClassification.OTHER_PROMOTION;
		boolean found = false;
		for (int move : normal_search_moves) {
			if (Move.getOriginPosition(move) == Position.valueOf(genericMove.from)
				&& Move.getTargetPosition(move) == Position.valueOf(genericMove.to)) {
				if (genericMove.promotion != null && genericMove.promotion.isLegalPromotion()) {
					if (IntChessman.valueOfPromotion(genericMove.promotion) == Move.getPromotion(move)) {
						type = MoveClassification.values()[Move.getType(move)];
						found = true;
						break;
					}
				} else {
					type = MoveClassification.values()[Move.getType(move)];
					found = true;
					break;
				}
			}
		}
		assert found : genericMove;
		return type;
	}
	
	public void reorderWithNewBestMove(GenericMove newBestMove) {
		int move = Move.toMove(newBestMove, getMoveTypeFromNormalList(newBestMove));
		reorderWithNewBestMove(move);
	}

	public void reorderWithNewBestMove(int newBestMove) {
		normalSearchBestMovePreviousIndex = reorderList(normal_search_moves, newBestMove, normalSearchBestMovePreviousIndex);
		extendedSearchListBestMovePreviousIndex = reorderList(extended_search_moves, newBestMove, extendedSearchListBestMovePreviousIndex);
	}
	
	private int reorderList(int[] moveArray, int newBestMove, int prevBestOriginalIndex) {
		int index = getIndex(moveArray, newBestMove);
		if (isMovePresent(index) && !isMoveAlreadyBest(index)) {
			
			if (wasPreviouslyModified(prevBestOriginalIndex)) {
				// Swap back the previous best move into its previous position, if this isn't a direct swap
				if (prevBestOriginalIndex == index) {
					// It is a direct swap back, set to -1 as the list is restored to its initial state
					prevBestOriginalIndex = -1;
				} else {
					swapWithFirst(moveArray, prevBestOriginalIndex);
					prevBestOriginalIndex = index;
				}
			} else {
				// Initialise the previous best index the first time the best move is altered
				prevBestOriginalIndex = index;
			}
			
			// Swap in the new best move
			swapWithFirst(moveArray, index);
		}
		return prevBestOriginalIndex;
	}
	
	private boolean isMovePresent(int index) { return (index != -1); }
	
	private boolean isMoveAlreadyBest(int index) { return (index == 0); }
	
	private boolean wasPreviouslyModified(int index) { return index != -1; }
}
