package eubos.board;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.pieces.Bishop;
import eubos.pieces.King;
import eubos.pieces.Knight;
import eubos.pieces.Pawn;
import eubos.pieces.Piece;
import eubos.pieces.Piece.Colour;
import eubos.pieces.Queen;
import eubos.pieces.Rook;

public class MiniMaxMoveGenerator extends MoveGenerator implements
		IMoveGenerator {
	
	private static final int SEARCH_DEPTH_IN_PLY = 2;
	private ArrayList<LinkedList<GenericMove>> moves;
	private int mp[];
	private int scores[];
	private GenericMove pc[][];
	
	public MiniMaxMoveGenerator( BoardManager bm, Piece.Colour sideToMove ) {
		super( bm, sideToMove);
		moves = new ArrayList<LinkedList<GenericMove>>();
		mp = new int[SEARCH_DEPTH_IN_PLY];
		scores = new int[SEARCH_DEPTH_IN_PLY];
		pc = new GenericMove[SEARCH_DEPTH_IN_PLY][SEARCH_DEPTH_IN_PLY];
	}
	
	private int evaluatePosition(Board theBoard ) {
		// First effort does only the most simple calculation based on material
		Iterator<Piece> iter_p = theBoard.iterator();
		int materialEvaluation = 0;
		while ( iter_p.hasNext() ) {
			Piece currPiece = iter_p.next();
			int currValue = 0;
			if ( currPiece instanceof Pawn ) 
				currValue = 100;
			else if ( currPiece instanceof Rook )
				currValue = 500;
			else if ( currPiece instanceof Bishop )
				currValue = 320;
			else if ( currPiece instanceof Knight )
				currValue = 300;
			else if ( currPiece instanceof Queen )
				currValue = 900;
			else if ( currPiece instanceof King )
				currValue = 30000;
			if (currPiece.isBlack()) currValue = -currValue;
			materialEvaluation += currValue;
		}
		return materialEvaluation;
	}
	
	@Override
	public GenericMove findMove() throws NoLegalMoveException {
		GenericMove bestMove = null;
		int currPly = 0;
		Piece.Colour toPlay = onMove;
		// Initialise...
		// First generate all the moves to the required search depth
		while ( currPly < SEARCH_DEPTH_IN_PLY) {
			mp[currPly]=0;
			if (toPlay==Colour.white) {
				scores[currPly] = Integer.MIN_VALUE;
			} else {
				scores[currPly] = Integer.MAX_VALUE;
			}
			currPly++;
			toPlay=Piece.Colour.getOpposite(toPlay);
		}
		// Now descend the plies in the search tree, to the full depth, updating the board
		currPly = 0;
		toPlay = onMove;
		searchPly(currPly, toPlay);
		bestMove = pc[0][0];
		return bestMove;
	}

	private void searchPly(int currPly, Piece.Colour toPlay) {
		boolean isTerminalNode = false;
		if (currPly == (SEARCH_DEPTH_IN_PLY-1))
			isTerminalNode = true;
		moves.add(generateMovesAtPosition(toPlay));
		// Iterate through all the moves for this ply
		LinkedList<GenericMove> ml = moves.get(currPly);
		Iterator<GenericMove> move_iter = ml.iterator();
		while( move_iter.hasNext()) {
			// Apply the move
			GenericMove currMove = move_iter.next();
			System.out.println("performMove("+currMove.toString()+") at Ply="+currPly);
			bm.performMove(currMove);
			if ( isTerminalNode ) {
				boolean backUpScore = false;
				// Score the resulting position
				int positionScore = evaluatePosition(bm.getTheBoard());
				// Back-up the score if appropriate
				if (toPlay == Colour.white) {
					if (positionScore < scores[currPly-1]) {
						backUpScore = true;
					}
				} else {
					if (positionScore > scores[currPly-1]) {
						backUpScore = true;
					}
				}
				if (backUpScore) {
					scores[currPly-1]=positionScore;
					System.out.println("backedUpScore:"+positionScore+" at Ply="+currPly);
					// Update Principal Continuation
					pc[currPly][currPly]=currMove;
				}
			} else {
				// Recursive call to the next level of the search
				int nextPly = currPly+1;
				System.out.println("searchPly("+nextPly+", "+toPlay.toString()+")");
				searchPly(nextPly, Piece.Colour.getOpposite(toPlay));
			}
			// restore the position
			System.out.println("restorePosition()");
			bm.undoPreviousMove();
		}
	}

	private LinkedList<GenericMove> generateMovesAtPosition(Piece.Colour colour) {
		// First step is to generate all positions in deepest position in tree and back them up...
		LinkedList<GenericMove> entireMoveList = new LinkedList<GenericMove>();
		// For each piece of the "on Move" colour, add it's legal moves to the entire move list
		Iterator<Piece> iter_p = bm.getTheBoard().iterateColour(colour);
		while ( iter_p.hasNext() ) {
			entireMoveList.addAll( iter_p.next().generateMoves( bm ));
		}
		addCastlingMoves(entireMoveList);
		// Scratch any moves resulting in the king being in check
		Iterator<GenericMove> iter_ml = entireMoveList.iterator();
		while ( iter_ml.hasNext() ) {
			GenericMove currMove = iter_ml.next();
			bm.performMove( currMove );
			if (inCheck()) {
				iter_ml.remove();
			}
			bm.undoPreviousMove();
		}
		return entireMoveList;
	}

}
