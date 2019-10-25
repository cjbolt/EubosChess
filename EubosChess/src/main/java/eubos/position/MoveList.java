package eubos.position;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Piece.Colour;

public class MoveList implements Iterable<GenericMove> {
	private List<GenericMove> all;
	private List<GenericMove> partial;

	public MoveList(PositionManager pm) {
		partial = new ArrayList<GenericMove>();
		all = new ArrayList<GenericMove>();
		
		Colour onMove = pm.getOnMove();
		int numCaptureOrCastleMoves = 0;
		for (GenericMove currMove : getRawList(pm)) {
			try {
				pm.performMove(currMove);
				if (pm.isKingInCheck(onMove)) {
					// Scratch any moves resulting in the king being in check
				}
				// Order so that the moves expected to be best are searched first, to get max benefit from alpha beta algorithm
				else if (pm.lastMoveWasCapture() ) {
					all.add(0, currMove);
					partial.add(currMove);
					numCaptureOrCastleMoves++;
				} else if (pm.lastMoveWasCaptureOrCastle() ) {
					// Add castle move at start of list, but don't enter on partial list
					all.add(0, currMove);
					numCaptureOrCastleMoves++;
				} else if (pm.isKingInCheck(Colour.getOpposite(onMove))) {
					all.add(numCaptureOrCastleMoves, currMove);
					partial.add(currMove);
				} else if (currMove.promotion != null) {
					all.add(numCaptureOrCastleMoves, currMove);
					partial.add(currMove);
				} else {
					all.add(currMove);
				}
				pm.unperformMove();
			} catch(InvalidPieceException e) {
			}
		}
		/* Walk move list one last time re-ordering so that: 
		 * 	promotion moves are in the correct order
		 * 	then capture with check
		 * 	then captures
		 * 	then castling
		 * 	then checks
		 * 	the normal moves
		 */
	}
	
	public MoveList(PositionManager pm, GenericMove seedMove) {
		this(pm);
		seedMoveListOrder(seedMove);
	}

	public Iterator<GenericMove> getIterator(boolean getCapturesChecksAndPromotions) {
		if (getCapturesChecksAndPromotions) {
			return this.getCapturesChecksAndPromotionsIterator();
		} else {
			return this.iterator();
		}
	}
	
	public void adjustForBestMove(GenericMove best) {
		all = createCopyWithBestMoveAtHead(all, best);
		if (partial.contains(best)) {
			partial = createCopyWithBestMoveAtHead(partial, best);
		}
	}
	
	@Override
	public Iterator<GenericMove> iterator() {
		return all.iterator();
	}
	
	public boolean isMateOccurred() {
		return this.all.isEmpty();
	}
	
	public GenericMove getRandomMove() {
		GenericMove bestMove = null;
		if ( !this.all.isEmpty()) {
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(this.all.size());
			bestMove = this.all.get(indexToGet);			
		}
		return bestMove;
	}
	
	List<GenericMove> getList() {
		return this.all;
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
	
	private Iterator<GenericMove> getCapturesChecksAndPromotionsIterator() {
		return this.partial.iterator();
	}

	private ArrayList<GenericMove> createCopyWithBestMoveAtHead(List<GenericMove> listToReorder, GenericMove best) {
		// It is done in this heavyweight fashion to avoid concurrent modification issues as we adjust the ml
		// that we are currently iterating through in the ply searcher class.
		ArrayList<GenericMove> ordered_ml = new ArrayList<GenericMove>(listToReorder);
		ordered_ml.remove(best);
		ordered_ml.add(0, best);
		return ordered_ml;
	}
	
	private void seedMoveListOrder(GenericMove prevBest) {
		seedList(this.all, prevBest);
		seedList(this.partial, prevBest);
	}
	
	private void seedList(List<GenericMove> listToSeed, GenericMove prevBest) {
		if (listToSeed.contains(prevBest)) {
			listToSeed.remove(prevBest);
			listToSeed.add(0,prevBest);
		}
	}
}
