package eubos.position;

import java.util.ArrayList;
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
	
	private MoveClassification previousBestMoveType = MoveClassification.REGULAR;
	private SortedSet<Map.Entry<GenericMove, MoveClassification>> moves;

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
					MoveClassification moveType = MoveClassification.REGULAR;
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
						// MoveClassification.REGULAR;
					}
					if (bestMove != null && currMove.equals(bestMove)) {
						moveMap.put(currMove, MoveClassification.BEST);
						previousBestMoveType = moveType;
					} else {
						moveMap.put(currMove, moveType);
					}
				}
				pm.unperformMove();
			} catch(InvalidPieceException e) {
				assert false;
			}
		}
		moves = entriesSortedByValues(moveMap);
	}
	
	static <K,V extends Comparable<? super V>>
	SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
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

	public class AllMovesIterator implements Iterator<GenericMove> {

		private LinkedList<GenericMove> moveList = null;
	
		public AllMovesIterator() {
			moveList = new LinkedList<GenericMove>();
			for (Map.Entry<GenericMove, MoveClassification> tuple : moves ) {
				moveList.add(tuple.getKey());
			}
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
	
	public class ExtendedSearchIterator implements Iterator<GenericMove> {

		private LinkedList<GenericMove> moveList = null;
	
		public ExtendedSearchIterator() {
			moveList = new LinkedList<GenericMove>();
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
		if (extended) {
			return new ExtendedSearchIterator();
		} else {
			return new AllMovesIterator();
		}
	}
	
	@Override
	public Iterator<GenericMove> iterator() {
		return new AllMovesIterator();
	}
		
	public boolean isMateOccurred() {
		return moves.isEmpty();
	}
	
	public GenericMove getRandomMove() {
		GenericMove bestMove = null;
		if ( !moves.isEmpty()) {
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(moves.size());
			Integer i = 0;
			for(Map.Entry<GenericMove, MoveClassification> tuple : moves)
			{
			    if (i == indexToGet) {
			    	bestMove=tuple.getKey();
			    	break;
			    }
			    i++;
			}			
		}
		return bestMove;
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

	public void reorderWithNewBestMove(GenericMove newBestMove) {
		// N.b. Need to use a linked hash map to ensure that the search order is deterministic.
		Map<GenericMove, MoveClassification> moveMap = new LinkedHashMap<GenericMove, MoveClassification>();
		for (Map.Entry<GenericMove, MoveClassification> tuple : moves ) {
			// Best move is first in the sorted map, so there is no defect here updating previousBestMoveType
			if (tuple.getValue() == MoveClassification.BEST)
				tuple.setValue(previousBestMoveType);
			if (tuple.getKey().equals(newBestMove)) {
				previousBestMoveType = tuple.getValue();
				tuple.setValue(MoveClassification.BEST);
			}	
			moveMap.put(tuple.getKey(), tuple.getValue());
		}
		moves = entriesSortedByValues(moveMap);
		moveMap.clear();
	}
}
