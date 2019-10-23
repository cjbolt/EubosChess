package eubos.position;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
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
		List<GenericMove> entireMoveList = new LinkedList<GenericMove>();
		Colour onMove = pm.getOnMove();
		// For each piece of the "on Move" colour, add it's legal moves to the entire move list
		Iterator<Piece> iter_p = pm.getTheBoard().iterateColour(onMove);
		while ( iter_p.hasNext() ) {
			Piece currPiece = iter_p.next();
			entireMoveList.addAll( currPiece.generateMoves( pm.getTheBoard() ));
		}
		pm.castling.addCastlingMoves(entireMoveList);
		List<GenericMove> newMoveList = new LinkedList<GenericMove>();
		Iterator<GenericMove> iter_ml = entireMoveList.iterator();
		int numCaptureOrCastleMoves = 0;
		while ( iter_ml.hasNext() ) {
			GenericMove currMove = iter_ml.next();
			try {
				pm.performMove(currMove);
				// Scratch any moves resulting in the king being in check
				if (pm.isKingInCheck(onMove))
					iter_ml.remove();
				// Groom the movelist so that the moves expected to be best are searched first.
				// This is to get max benefit form alpha beta algorithm
				else if (pm.lastMoveWasCaptureOrCastle() ) {
					newMoveList.add(0, currMove);
					partial.add(currMove);
					numCaptureOrCastleMoves++;
				} else if (pm.isKingInCheck(Colour.getOpposite(onMove))) {
					newMoveList.add(numCaptureOrCastleMoves, currMove);
					partial.add(currMove);
				} else if (currMove.promotion != null) {
					newMoveList.add(numCaptureOrCastleMoves, currMove);
					partial.add(currMove);
				} else {
					newMoveList.add(currMove);
				}
				pm.unperformMove();
			} catch(InvalidPieceException e) {
				
			}
		}
		all = new ArrayList<GenericMove>(newMoveList);
	}

	public Iterator<GenericMove> getIterator(boolean getCapturesChecksAndPromotions) {
		if (getCapturesChecksAndPromotions) {
			return this.getCapturesChecksAndPromotionsIterator();
		} else {
			return this.iterator();
		}
	}
	@Override
	public Iterator<GenericMove> iterator() {
		return all.iterator();
	}
	
	public Iterator<GenericMove> getCapturesChecksAndPromotionsIterator() {
		return this.partial.iterator();
	}
	
	public void adjustForBestMove(GenericMove best) {
		all = createCopyWithBestMoveAtHead(all, best);
		if (partial.indexOf(best) != -1) {
			partial = createCopyWithBestMoveAtHead(partial, best);
		}
	}

	private ArrayList<GenericMove> createCopyWithBestMoveAtHead(List<GenericMove> listToReorder, GenericMove best) {
		// It is done in this heavyweight fashion to avoid concurrent modification issues as we adjust the ml
		// that we are currently iterating through in the ply searcher class.
		LinkedList<GenericMove> ordered_ml = new LinkedList<GenericMove>();
		ordered_ml.addAll(listToReorder);
		ordered_ml.remove(best);
		ordered_ml.add(0, best);
		return new ArrayList<GenericMove>(ordered_ml);
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
	
	public List<GenericMove> getList() {
		return this.all;
	}
	
	public void adjustForBestAlternate(GenericMove prevBest) {
		if (this.all.contains(prevBest)) {
			this.all.remove(prevBest);
			this.all.add(0,prevBest);
		}
	}
}
