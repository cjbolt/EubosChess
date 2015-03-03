package eubos.board;

import java.util.LinkedList;
import java.util.Random;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.pieces.Piece;

public class MoveGenerator {
	
	private Board theBoard;
	
	public MoveGenerator( Board inputBoard ) { theBoard = inputBoard; }

	public GenericMove findBestMove() throws IllegalNotationException {
		// TODO: for now find a random legal move for the side indicated
		// Generate the entire move list
		GenericMove bestMove = null;
		LinkedList<GenericMove> entireMoveList = new LinkedList<GenericMove>();
		for (Piece currentBlackPiece: theBoard) {
			// append this piece's legal moves to the entire move list
			entireMoveList.addAll( currentBlackPiece.generateMoveList(theBoard));
		}
		if ( !entireMoveList.isEmpty()) {
			// once the move list has been generated, remove any moves that would place
			// the king in check from consideration.
			//for ( GenericMove currMove : entireMoveList) {
				// test if places king in check...
			//}
			// For the time-being, return a valid move at random
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(entireMoveList.size());
			bestMove = entireMoveList.get(indexToGet);			
		}
		// TODO: This exception is when there is no valid move - it is temporary,
		// when implementation is complete this case would actually mean stalemate.
		else throw new IllegalNotationException();
		return bestMove;
	}
}
