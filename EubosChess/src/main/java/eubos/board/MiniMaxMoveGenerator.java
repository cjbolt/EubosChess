package eubos.board;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.pieces.Bishop;
import eubos.pieces.King;
import eubos.pieces.Knight;
import eubos.pieces.Pawn;
import eubos.pieces.Piece;
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
		LinkedList<GenericMove> playMoveList = generateMovesAtPosition(onMove);
		moves.add(playMoveList);
		if ( !playMoveList.isEmpty()) {
			// For each move, apply it and go to the next ply
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(playMoveList.size());
			bestMove = playMoveList.get(indexToGet);			
		} else {
			throw new NoLegalMoveException();
		}
		return bestMove;
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
