package eubos.position;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
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
		BEST,
		OLD_BEST,
		PROMOTION,
		CAPTURE,
		CASTLE,
		CHECK,
		REGULAR
	};
	
	private SortedSet<Map.Entry<GenericMove, MoveClassification>> moves;

	public MoveList(PositionManager pm) {
		this(pm, null);
	}
	
	public MoveList(PositionManager pm, GenericMove bestMove) {
		Map<GenericMove, MoveClassification> moveMap = new HashMap<GenericMove, MoveClassification>();
		Colour onMove = pm.getOnMove();
		for (GenericMove currMove : getRawList(pm)) {
			try {
				pm.performMove(currMove);
				if (pm.isKingInCheck(onMove)) {
					// Scratch any moves resulting in the king being in check
				} else if (bestMove != null && currMove.equals(bestMove)) {
					moveMap.put(currMove, MoveClassification.BEST);
				} else if (pm.lastMoveWasCapture() ) {
					moveMap.put(currMove, MoveClassification.CAPTURE);
				} else if (pm.lastMoveWasCaptureOrCastle() ) {
					moveMap.put(currMove, MoveClassification.CASTLE);
				} else if (pm.isKingInCheck(Colour.getOpposite(onMove))) {
					moveMap.put(currMove, MoveClassification.CHECK);
				} else if (currMove.promotion == GenericChessman.QUEEN) {
					moveMap.put(currMove, MoveClassification.PROMOTION);
				} else {
					moveMap.put(currMove, MoveClassification.REGULAR);
				}
				pm.unperformMove();
			} catch(InvalidPieceException e) {
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
				case OLD_BEST:
				case CAPTURE:
				case PROMOTION:
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
	
	List<GenericMove> getList() {
		List<GenericMove> moveList = new ArrayList<GenericMove>();
		for (Map.Entry<GenericMove, MoveClassification> tuple : moves ) {
			moveList.add(tuple.getKey());
		}
		return moveList;
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

	public void adjustForBestMove(GenericMove newBestMove) {
		Map<GenericMove, MoveClassification> moveMap = new HashMap<GenericMove, MoveClassification>();
		for (Map.Entry<GenericMove, MoveClassification> tuple : moves ) {
			if (tuple.getValue() == MoveClassification.BEST)
				tuple.setValue(MoveClassification.OLD_BEST);
			if (tuple.getKey().equals(newBestMove))
				tuple.setValue(MoveClassification.BEST);
			moveMap.put(tuple.getKey(), tuple.getValue());
		}
		moves = entriesSortedByValues(moveMap);
	}
}
