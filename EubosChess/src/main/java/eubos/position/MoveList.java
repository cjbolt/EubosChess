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
	// If we have separate lists like this, then something needs to be responsible for sorting both of the lists accordingly
	private List<GenericMove> theList;
	private List<GenericMove> capturesChecksAndPromotions;

	public MoveList(PositionManager pm) {
		capturesChecksAndPromotions = new ArrayList<GenericMove>();
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
					capturesChecksAndPromotions.add(currMove);
					numCaptureOrCastleMoves++;
				} else if (pm.isKingInCheck(Colour.getOpposite(onMove))) {
					newMoveList.add(numCaptureOrCastleMoves, currMove);
					capturesChecksAndPromotions.add(currMove);
				} else if (currMove.promotion != null) {
					newMoveList.add(numCaptureOrCastleMoves, currMove);
					capturesChecksAndPromotions.add(currMove);
				} else {
					newMoveList.add(currMove);
				}
				pm.unperformMove();
			} catch(InvalidPieceException e) {
				
			}
		}
		List<GenericMove> ret_list = new ArrayList<GenericMove>(newMoveList);
		newMoveList.clear();
		entireMoveList.clear();
		theList = ret_list;
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
		return theList.iterator();
	}
	
	public Iterator<GenericMove> getCapturesChecksAndPromotionsIterator() {
		return this.capturesChecksAndPromotions.iterator();
	}
	
	public void adjustForBestMove(GenericMove best) {
		LinkedList<GenericMove> ordered_ml = new LinkedList<GenericMove>();
		ordered_ml.addAll(this.theList);
		ordered_ml.remove(best);
		ordered_ml.add(0, best);
		this.theList = new ArrayList<GenericMove>(ordered_ml);
	}
	
	public boolean isMateOccurred() {
		return this.theList.isEmpty();
	}
	
	public GenericMove getFirst() {
		return this.theList.get(0);
	}
	
	public void adjustForBestAlternate(GenericMove prevBest) {
		if (this.theList.contains(prevBest)) {
			this.theList.remove(prevBest);
			this.theList.add(0,prevBest);
		}
	}
	
	public GenericMove getRandomMove() {
		GenericMove bestMove = null;
		if ( !this.theList.isEmpty()) {
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(this.theList.size());
			bestMove = this.theList.get(indexToGet);			
		}
		return bestMove;
	}
	
	public List<GenericMove> getList() {
		return this.theList;
	}
}
