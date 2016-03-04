package eubos.search;

import java.util.Iterator;

import eubos.board.Board;
import eubos.board.pieces.Bishop;
import eubos.board.pieces.King;
import eubos.board.pieces.Knight;
import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Queen;
import eubos.board.pieces.Rook;

public class ScoreGenerator {
	
	int searchDepth;
	
	static final int KING_VALUE = 300000;
	static final int QUEEN_VALUE = 900;
	static final int ROOK_VALUE = 500;
	static final int BISHOP_VALUE = 320;
	static final int KNIGHT_VALUE = 300;
	static final int PAWN_VALUE = 100;
	
	static final int PLIES_PER_MOVE = 2;
	
	ScoreGenerator(int searchDepth) {
		this.searchDepth = searchDepth;		
	}
	
	int getScoreForStalemate() {
		// Avoid stalemates by giving them a large penalty score.
		return -KING_VALUE;
	}
	
	int generateScoreForCheckmate(int currPly) {
		// Favour earlier mates (i.e. Mate-in-one over mate-in-three) by giving them a larger score.
		int totalMovesSearched = searchDepth/PLIES_PER_MOVE;
		int mateMoveNum = (currPly-1)/PLIES_PER_MOVE; // currPly-1 because mate was caused by the move from the previousPly
		int multiplier = totalMovesSearched-mateMoveNum;
		return multiplier*KING_VALUE;
	}
	
	int generateScoreForPosition(Board theBoard) {
		// First effort does only the most simple calculation based on material
		Iterator<Piece> iter_p = theBoard.iterator();
		int materialEvaluation = 0;
		while ( iter_p.hasNext() ) {
			Piece currPiece = iter_p.next();
			int currValue = 0;
			if ( currPiece instanceof Pawn ) 
				currValue = PAWN_VALUE;
			else if ( currPiece instanceof Rook )
				currValue = ROOK_VALUE;
			else if ( currPiece instanceof Bishop )
				currValue = BISHOP_VALUE;
			else if ( currPiece instanceof Knight )
				currValue = KNIGHT_VALUE;
			else if ( currPiece instanceof Queen )
				currValue = QUEEN_VALUE;
			else if ( currPiece instanceof King )
				currValue = KING_VALUE;
			if (currPiece.isBlack()) currValue = -currValue;
			materialEvaluation += currValue;
		}
		return materialEvaluation;
	}
}
