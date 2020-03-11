package eubos.score;

import java.util.PrimitiveIterator;

import eubos.board.Board;
import eubos.board.Piece;
import eubos.position.Position;

public class MaterialEvaluator {
	
	public static final short MATERIAL_VALUE_KING = 4000;
	public static final short MATERIAL_VALUE_QUEEN = 950;
	public static final short MATERIAL_VALUE_ROOK = 490;
	public static final short MATERIAL_VALUE_BISHOP = 320;
	public static final short MATERIAL_VALUE_KNIGHT = 290;
	public static final short MATERIAL_VALUE_PAWN = 100;
	
	private static final int[] PAWN_WHITE_WEIGHTINGS;
    static {
    	PAWN_WHITE_WEIGHTINGS = new int[128];
        PAWN_WHITE_WEIGHTINGS[Position.a1] = 0; PAWN_WHITE_WEIGHTINGS[Position.b1] = 0; PAWN_WHITE_WEIGHTINGS[Position.c1] = 0; PAWN_WHITE_WEIGHTINGS[Position.d1] = 0; PAWN_WHITE_WEIGHTINGS[Position.e1] = 0; PAWN_WHITE_WEIGHTINGS[Position.f1] = 0; PAWN_WHITE_WEIGHTINGS[Position.g1] = 0; PAWN_WHITE_WEIGHTINGS[Position.h1] = 0;
        PAWN_WHITE_WEIGHTINGS[Position.a2] = 0; PAWN_WHITE_WEIGHTINGS[Position.b2] = 0; PAWN_WHITE_WEIGHTINGS[Position.c2] = 0; PAWN_WHITE_WEIGHTINGS[Position.d2] = 0; PAWN_WHITE_WEIGHTINGS[Position.e2] = 0; PAWN_WHITE_WEIGHTINGS[Position.f2] = 0; PAWN_WHITE_WEIGHTINGS[Position.g2] = 0; PAWN_WHITE_WEIGHTINGS[Position.h2] = 0;
        PAWN_WHITE_WEIGHTINGS[Position.a3] = 0; PAWN_WHITE_WEIGHTINGS[Position.b3] = 0; PAWN_WHITE_WEIGHTINGS[Position.c3] = 0; PAWN_WHITE_WEIGHTINGS[Position.d3] = 5; PAWN_WHITE_WEIGHTINGS[Position.e3] = 5; PAWN_WHITE_WEIGHTINGS[Position.f3] = 0; PAWN_WHITE_WEIGHTINGS[Position.g3] = 0; PAWN_WHITE_WEIGHTINGS[Position.h3] = 0;
        PAWN_WHITE_WEIGHTINGS[Position.a4] = 0; PAWN_WHITE_WEIGHTINGS[Position.b4] = 0; PAWN_WHITE_WEIGHTINGS[Position.c4] = 5; PAWN_WHITE_WEIGHTINGS[Position.d4] = 10; PAWN_WHITE_WEIGHTINGS[Position.e4] = 10;PAWN_WHITE_WEIGHTINGS[Position.f4] = 5; PAWN_WHITE_WEIGHTINGS[Position.g4] = 0; PAWN_WHITE_WEIGHTINGS[Position.h4] = 0;
        PAWN_WHITE_WEIGHTINGS[Position.a5] = 0; PAWN_WHITE_WEIGHTINGS[Position.b5] = 3; PAWN_WHITE_WEIGHTINGS[Position.c5] = 5; PAWN_WHITE_WEIGHTINGS[Position.d5] = 15; PAWN_WHITE_WEIGHTINGS[Position.e5] = 15; PAWN_WHITE_WEIGHTINGS[Position.f5] = 5; PAWN_WHITE_WEIGHTINGS[Position.g5] = 3; PAWN_WHITE_WEIGHTINGS[Position.h5] = 0;
		PAWN_WHITE_WEIGHTINGS[Position.a6] = 5; PAWN_WHITE_WEIGHTINGS[Position.b6] = 25; PAWN_WHITE_WEIGHTINGS[Position.c6] = 25; PAWN_WHITE_WEIGHTINGS[Position.d6] = 25; PAWN_WHITE_WEIGHTINGS[Position.e6] = 25; PAWN_WHITE_WEIGHTINGS[Position.f6] = 25; PAWN_WHITE_WEIGHTINGS[Position.g6] = 25; PAWN_WHITE_WEIGHTINGS[Position.h6] = 10;
		PAWN_WHITE_WEIGHTINGS[Position.a7] = 25; PAWN_WHITE_WEIGHTINGS[Position.b7] = 50; PAWN_WHITE_WEIGHTINGS[Position.c7] = 50; PAWN_WHITE_WEIGHTINGS[Position.d7] = 50; PAWN_WHITE_WEIGHTINGS[Position.e7] = 50; PAWN_WHITE_WEIGHTINGS[Position.f7] = 50; PAWN_WHITE_WEIGHTINGS[Position.g7] = 50; PAWN_WHITE_WEIGHTINGS[Position.h7] = 25;
		PAWN_WHITE_WEIGHTINGS[Position.a8] = 0; PAWN_WHITE_WEIGHTINGS[Position.b8] = 0; PAWN_WHITE_WEIGHTINGS[Position.c8] = 0; PAWN_WHITE_WEIGHTINGS[Position.d8] = 0; PAWN_WHITE_WEIGHTINGS[Position.e8] = 0; PAWN_WHITE_WEIGHTINGS[Position.f8] = 0; PAWN_WHITE_WEIGHTINGS[Position.g8] = 0; PAWN_WHITE_WEIGHTINGS[Position.h8] = 0;
    }
    
	private static final int[] PAWN_BLACK_WEIGHTINGS;
    static {
    	PAWN_BLACK_WEIGHTINGS = new int[128];
        PAWN_BLACK_WEIGHTINGS[Position.a1] = 0; PAWN_BLACK_WEIGHTINGS[Position.b1] = 0; PAWN_BLACK_WEIGHTINGS[Position.c1] = 0; PAWN_BLACK_WEIGHTINGS[Position.d1] = 0; PAWN_BLACK_WEIGHTINGS[Position.e1] = 0; PAWN_BLACK_WEIGHTINGS[Position.f1] = 0; PAWN_BLACK_WEIGHTINGS[Position.g1] = 0; PAWN_BLACK_WEIGHTINGS[Position.h1] = 0;
        PAWN_BLACK_WEIGHTINGS[Position.a2] = 25; PAWN_BLACK_WEIGHTINGS[Position.b2] = 50; PAWN_BLACK_WEIGHTINGS[Position.c2] = 50; PAWN_BLACK_WEIGHTINGS[Position.d2] = 50;PAWN_BLACK_WEIGHTINGS[Position.e2] = 50;PAWN_BLACK_WEIGHTINGS[Position.f2] = 50; PAWN_BLACK_WEIGHTINGS[Position.g2] = 50; PAWN_BLACK_WEIGHTINGS[Position.h2] = 25;
        PAWN_BLACK_WEIGHTINGS[Position.a3] = 5; PAWN_BLACK_WEIGHTINGS[Position.b3] = 25; PAWN_BLACK_WEIGHTINGS[Position.c3] = 25; PAWN_BLACK_WEIGHTINGS[Position.d3] = 25;PAWN_BLACK_WEIGHTINGS[Position.e3] = 25;PAWN_BLACK_WEIGHTINGS[Position.f3] = 25; PAWN_BLACK_WEIGHTINGS[Position.g3] = 25; PAWN_BLACK_WEIGHTINGS[Position.h3] = 10;
        PAWN_BLACK_WEIGHTINGS[Position.a4] = 0; PAWN_BLACK_WEIGHTINGS[Position.b4] = 3; PAWN_BLACK_WEIGHTINGS[Position.c4] = 5; PAWN_BLACK_WEIGHTINGS[Position.d4] = 15;PAWN_BLACK_WEIGHTINGS[Position.e4] = 15;PAWN_BLACK_WEIGHTINGS[Position.f4] = 5;PAWN_BLACK_WEIGHTINGS[Position.g4] = 3; PAWN_BLACK_WEIGHTINGS[Position.h4] = 0;
        PAWN_BLACK_WEIGHTINGS[Position.a5] = 0; PAWN_BLACK_WEIGHTINGS[Position.b5] = 0; PAWN_BLACK_WEIGHTINGS[Position.c5] = 5; PAWN_BLACK_WEIGHTINGS[Position.d5] = 10;PAWN_BLACK_WEIGHTINGS[Position.e5] = 10;PAWN_BLACK_WEIGHTINGS[Position.f5] = 5;PAWN_BLACK_WEIGHTINGS[Position.g5] = 0; PAWN_BLACK_WEIGHTINGS[Position.h5] = 0;
		PAWN_BLACK_WEIGHTINGS[Position.a6] = 0; PAWN_BLACK_WEIGHTINGS[Position.b6] = 0; PAWN_BLACK_WEIGHTINGS[Position.c6] = 0; PAWN_BLACK_WEIGHTINGS[Position.d6] = 5;PAWN_BLACK_WEIGHTINGS[Position.e6] = 5;PAWN_BLACK_WEIGHTINGS[Position.f6] = 0; PAWN_BLACK_WEIGHTINGS[Position.g6] = 0; PAWN_BLACK_WEIGHTINGS[Position.h6] = 0;
		PAWN_BLACK_WEIGHTINGS[Position.a7] = 0; PAWN_BLACK_WEIGHTINGS[Position.b7] = 0; PAWN_BLACK_WEIGHTINGS[Position.c7] = 0; PAWN_BLACK_WEIGHTINGS[Position.d7] = 0; PAWN_BLACK_WEIGHTINGS[Position.e7] = 0; PAWN_BLACK_WEIGHTINGS[Position.f7] = 0; PAWN_BLACK_WEIGHTINGS[Position.g7] = 0; PAWN_BLACK_WEIGHTINGS[Position.h7] = 0;
		PAWN_BLACK_WEIGHTINGS[Position.a8] = 0; PAWN_BLACK_WEIGHTINGS[Position.b8] = 0; PAWN_BLACK_WEIGHTINGS[Position.c8] = 0; PAWN_BLACK_WEIGHTINGS[Position.d8] = 0; PAWN_BLACK_WEIGHTINGS[Position.e8] = 0; PAWN_BLACK_WEIGHTINGS[Position.f8] = 0; PAWN_BLACK_WEIGHTINGS[Position.g8] = 0; PAWN_BLACK_WEIGHTINGS[Position.h8] = 0;
    }    
	
	private static final int[] KNIGHT_WEIGHTINGS;
    static {
    	KNIGHT_WEIGHTINGS = new int[128];
        KNIGHT_WEIGHTINGS[Position.a1] = -20;KNIGHT_WEIGHTINGS[Position.b1] = -10;KNIGHT_WEIGHTINGS[Position.c1] = -10;KNIGHT_WEIGHTINGS[Position.d1] = -10;KNIGHT_WEIGHTINGS[Position.e1] = -10;KNIGHT_WEIGHTINGS[Position.f1] = -10;KNIGHT_WEIGHTINGS[Position.g1] = -10;KNIGHT_WEIGHTINGS[Position.h1] = -20;
		KNIGHT_WEIGHTINGS[Position.a2] = -10;KNIGHT_WEIGHTINGS[Position.b2] = 0;KNIGHT_WEIGHTINGS[Position.c2] = 0;KNIGHT_WEIGHTINGS[Position.d2] = 0;KNIGHT_WEIGHTINGS[Position.e2] = 0;KNIGHT_WEIGHTINGS[Position.f2] = 0;KNIGHT_WEIGHTINGS[Position.g2] = 0;KNIGHT_WEIGHTINGS[Position.h2] = -10;
		KNIGHT_WEIGHTINGS[Position.a3] = -10;KNIGHT_WEIGHTINGS[Position.b3] = 0;KNIGHT_WEIGHTINGS[Position.c3] = 10;KNIGHT_WEIGHTINGS[Position.d3] = 10;KNIGHT_WEIGHTINGS[Position.e3] = 10;KNIGHT_WEIGHTINGS[Position.f3] = 10;KNIGHT_WEIGHTINGS[Position.g3] = 0;KNIGHT_WEIGHTINGS[Position.h3] = -10;
		KNIGHT_WEIGHTINGS[Position.a4] = -10;KNIGHT_WEIGHTINGS[Position.b4] = 0;KNIGHT_WEIGHTINGS[Position.c4] = 10;KNIGHT_WEIGHTINGS[Position.d4] = 20;KNIGHT_WEIGHTINGS[Position.e4] = 20;KNIGHT_WEIGHTINGS[Position.f4] = 10;KNIGHT_WEIGHTINGS[Position.g4] = 0;KNIGHT_WEIGHTINGS[Position.h4] = -10;
		KNIGHT_WEIGHTINGS[Position.a5] = -10;KNIGHT_WEIGHTINGS[Position.b5] = 0;KNIGHT_WEIGHTINGS[Position.c5] = 10;KNIGHT_WEIGHTINGS[Position.d5] = 20;KNIGHT_WEIGHTINGS[Position.e5] = 20;KNIGHT_WEIGHTINGS[Position.f5] = 10;KNIGHT_WEIGHTINGS[Position.g5] = 0;KNIGHT_WEIGHTINGS[Position.h5] = -10;
		KNIGHT_WEIGHTINGS[Position.a6] = -10;KNIGHT_WEIGHTINGS[Position.b6] = 0;KNIGHT_WEIGHTINGS[Position.c6] = 10;KNIGHT_WEIGHTINGS[Position.d6] = 10;KNIGHT_WEIGHTINGS[Position.e6] = 10;KNIGHT_WEIGHTINGS[Position.f6] = 10;KNIGHT_WEIGHTINGS[Position.g6] = 0;KNIGHT_WEIGHTINGS[Position.h6] = -10;
		KNIGHT_WEIGHTINGS[Position.a7] = -10;KNIGHT_WEIGHTINGS[Position.b7] = 0;KNIGHT_WEIGHTINGS[Position.c7] = 0;KNIGHT_WEIGHTINGS[Position.d7] = 0;KNIGHT_WEIGHTINGS[Position.e7] = 0;KNIGHT_WEIGHTINGS[Position.f7] = 0;KNIGHT_WEIGHTINGS[Position.g7] = 0;KNIGHT_WEIGHTINGS[Position.h7] = -10;
		KNIGHT_WEIGHTINGS[Position.a8] = -20;KNIGHT_WEIGHTINGS[Position.b8] = -10;KNIGHT_WEIGHTINGS[Position.c8] = -10;KNIGHT_WEIGHTINGS[Position.d8] = -10;KNIGHT_WEIGHTINGS[Position.e8] = -10;KNIGHT_WEIGHTINGS[Position.f8] = -10;KNIGHT_WEIGHTINGS[Position.g8] = -10;KNIGHT_WEIGHTINGS[Position.h8] = -20;
    }
    
    private static final int[] KING_ENDGAME_WEIGHTINGS;
    static {
    	KING_ENDGAME_WEIGHTINGS = new int[128];
        KING_ENDGAME_WEIGHTINGS[Position.a1] = -30;KING_ENDGAME_WEIGHTINGS[Position.b1] = -30;KING_ENDGAME_WEIGHTINGS[Position.c1] = -30;KING_ENDGAME_WEIGHTINGS[Position.d1] = -30;KING_ENDGAME_WEIGHTINGS[Position.e1] = -30;KING_ENDGAME_WEIGHTINGS[Position.f1] = -30;KING_ENDGAME_WEIGHTINGS[Position.g1] = -30;KING_ENDGAME_WEIGHTINGS[Position.h1] = -30;
		KING_ENDGAME_WEIGHTINGS[Position.a2] = -30;KING_ENDGAME_WEIGHTINGS[Position.b2] = -20;KING_ENDGAME_WEIGHTINGS[Position.c2] = -20;KING_ENDGAME_WEIGHTINGS[Position.d2] = -20;KING_ENDGAME_WEIGHTINGS[Position.e2] = -20;KING_ENDGAME_WEIGHTINGS[Position.f2] = -20;KING_ENDGAME_WEIGHTINGS[Position.g2] = -20;KING_ENDGAME_WEIGHTINGS[Position.h2] = -30;
		KING_ENDGAME_WEIGHTINGS[Position.a3] = -30;KING_ENDGAME_WEIGHTINGS[Position.b3] = -10;KING_ENDGAME_WEIGHTINGS[Position.c3] = 0;KING_ENDGAME_WEIGHTINGS[Position.d3] = 10;KING_ENDGAME_WEIGHTINGS[Position.e3] = 10;KING_ENDGAME_WEIGHTINGS[Position.f3] = 0;KING_ENDGAME_WEIGHTINGS[Position.g3] = -10;KING_ENDGAME_WEIGHTINGS[Position.h3] = -30;
		KING_ENDGAME_WEIGHTINGS[Position.a4] = -20;KING_ENDGAME_WEIGHTINGS[Position.b4] = -10;KING_ENDGAME_WEIGHTINGS[Position.c4] = 10;KING_ENDGAME_WEIGHTINGS[Position.d4] = 20;KING_ENDGAME_WEIGHTINGS[Position.e4] = 20;KING_ENDGAME_WEIGHTINGS[Position.f4] = 10;KING_ENDGAME_WEIGHTINGS[Position.g4] = -10;KING_ENDGAME_WEIGHTINGS[Position.h4] = -20;
		KING_ENDGAME_WEIGHTINGS[Position.a5] = -20;KING_ENDGAME_WEIGHTINGS[Position.b5] = -10;KING_ENDGAME_WEIGHTINGS[Position.c5] = 10;KING_ENDGAME_WEIGHTINGS[Position.d5] = 20;KING_ENDGAME_WEIGHTINGS[Position.e5] = 20;KING_ENDGAME_WEIGHTINGS[Position.f5] = 10;KING_ENDGAME_WEIGHTINGS[Position.g5] = -10;KING_ENDGAME_WEIGHTINGS[Position.h5] = -20;
		KING_ENDGAME_WEIGHTINGS[Position.a6] = -30;KING_ENDGAME_WEIGHTINGS[Position.b6] = -10;KING_ENDGAME_WEIGHTINGS[Position.c6] = 0;KING_ENDGAME_WEIGHTINGS[Position.d6] = 10;KING_ENDGAME_WEIGHTINGS[Position.e6] = 10;KING_ENDGAME_WEIGHTINGS[Position.f6] = 0;KING_ENDGAME_WEIGHTINGS[Position.g6] = -10;KING_ENDGAME_WEIGHTINGS[Position.h6] = -30;
		KING_ENDGAME_WEIGHTINGS[Position.a7] = -30;KING_ENDGAME_WEIGHTINGS[Position.b7] = -20;KING_ENDGAME_WEIGHTINGS[Position.c7] = -20;KING_ENDGAME_WEIGHTINGS[Position.d7] = -20;KING_ENDGAME_WEIGHTINGS[Position.e7] = -20;KING_ENDGAME_WEIGHTINGS[Position.f7] = -20;KING_ENDGAME_WEIGHTINGS[Position.g7] = -20;KING_ENDGAME_WEIGHTINGS[Position.h7] = -30;
		KING_ENDGAME_WEIGHTINGS[Position.a8] = -30;KING_ENDGAME_WEIGHTINGS[Position.b8] = -30;KING_ENDGAME_WEIGHTINGS[Position.c8] = -30;KING_ENDGAME_WEIGHTINGS[Position.d8] = -30;KING_ENDGAME_WEIGHTINGS[Position.e8] = -30;KING_ENDGAME_WEIGHTINGS[Position.f8] = -30;KING_ENDGAME_WEIGHTINGS[Position.g8] = -30;KING_ENDGAME_WEIGHTINGS[Position.h8] = -30;
    }
    
    private static final int[] KING_MIDGAME_WEIGHTINGS;
    static {
    	KING_MIDGAME_WEIGHTINGS = new int[128];
        KING_MIDGAME_WEIGHTINGS[Position.a1] = 5;KING_MIDGAME_WEIGHTINGS[Position.b1] = 10;KING_MIDGAME_WEIGHTINGS[Position.c1] = 5;KING_MIDGAME_WEIGHTINGS[Position.d1] = 0;KING_MIDGAME_WEIGHTINGS[Position.e1] = 0;KING_MIDGAME_WEIGHTINGS[Position.f1] = 5;KING_MIDGAME_WEIGHTINGS[Position.g1] = 10;KING_MIDGAME_WEIGHTINGS[Position.h1] = 5;
		KING_MIDGAME_WEIGHTINGS[Position.a2] = 0;KING_MIDGAME_WEIGHTINGS[Position.b2] = 0;KING_MIDGAME_WEIGHTINGS[Position.c2] = 0;KING_MIDGAME_WEIGHTINGS[Position.d2] = 0;KING_MIDGAME_WEIGHTINGS[Position.e2] = 0;KING_MIDGAME_WEIGHTINGS[Position.f2] = 0;KING_MIDGAME_WEIGHTINGS[Position.g2] = 0;KING_MIDGAME_WEIGHTINGS[Position.h2] = 0;
		KING_MIDGAME_WEIGHTINGS[Position.a3] = -20;KING_MIDGAME_WEIGHTINGS[Position.b3] = -20;KING_MIDGAME_WEIGHTINGS[Position.c3] = -30;KING_MIDGAME_WEIGHTINGS[Position.d3] = -30;KING_MIDGAME_WEIGHTINGS[Position.e3] = -30;KING_MIDGAME_WEIGHTINGS[Position.f3] = -30;KING_MIDGAME_WEIGHTINGS[Position.g3] = -20;KING_MIDGAME_WEIGHTINGS[Position.h3] = -20;
		KING_MIDGAME_WEIGHTINGS[Position.a4] = -30;KING_MIDGAME_WEIGHTINGS[Position.b4] = -40;KING_MIDGAME_WEIGHTINGS[Position.c4] = -50;KING_MIDGAME_WEIGHTINGS[Position.d4] = -50;KING_MIDGAME_WEIGHTINGS[Position.e4] = -50;KING_MIDGAME_WEIGHTINGS[Position.f4] = -40;KING_MIDGAME_WEIGHTINGS[Position.g4] = -40;KING_MIDGAME_WEIGHTINGS[Position.h4] = -30;
		KING_MIDGAME_WEIGHTINGS[Position.a5] = -30;KING_MIDGAME_WEIGHTINGS[Position.b5] = -40;KING_MIDGAME_WEIGHTINGS[Position.c5] = -50;KING_MIDGAME_WEIGHTINGS[Position.d5] = -50;KING_MIDGAME_WEIGHTINGS[Position.e5] = -50;KING_MIDGAME_WEIGHTINGS[Position.f5] = -40;KING_MIDGAME_WEIGHTINGS[Position.g5] = -40;KING_MIDGAME_WEIGHTINGS[Position.h5] = -30;
		KING_MIDGAME_WEIGHTINGS[Position.a6] = -20;KING_MIDGAME_WEIGHTINGS[Position.b6] = -20;KING_MIDGAME_WEIGHTINGS[Position.c6] = -30;KING_MIDGAME_WEIGHTINGS[Position.d6] = -30;KING_MIDGAME_WEIGHTINGS[Position.e6] = -30;KING_MIDGAME_WEIGHTINGS[Position.f6] = -30;KING_MIDGAME_WEIGHTINGS[Position.g6] = -20;KING_MIDGAME_WEIGHTINGS[Position.h6] = -20;
		KING_MIDGAME_WEIGHTINGS[Position.a7] = 0;KING_MIDGAME_WEIGHTINGS[Position.b7] = 0;KING_MIDGAME_WEIGHTINGS[Position.c7] = 0;KING_MIDGAME_WEIGHTINGS[Position.d7] = 0;KING_MIDGAME_WEIGHTINGS[Position.e7] = 0;KING_MIDGAME_WEIGHTINGS[Position.f7] = 0;KING_MIDGAME_WEIGHTINGS[Position.g7] = 0;KING_MIDGAME_WEIGHTINGS[Position.h7] = 0;
		KING_MIDGAME_WEIGHTINGS[Position.a8] = 5;KING_MIDGAME_WEIGHTINGS[Position.b8] = 10;KING_MIDGAME_WEIGHTINGS[Position.c8] = 5;KING_MIDGAME_WEIGHTINGS[Position.d8] = 0;KING_MIDGAME_WEIGHTINGS[Position.e8] = 0;KING_MIDGAME_WEIGHTINGS[Position.f8] = 5;KING_MIDGAME_WEIGHTINGS[Position.g8] = 10;KING_MIDGAME_WEIGHTINGS[Position.h8] = 5;
    }
 
	static MaterialEvaluation evaluate(Board theBoard, boolean isEndgame) {
		PrimitiveIterator.OfInt iter_p = theBoard.iterator();
		// TODO this needn't be done with an iterator. We could just go through all the BitBoards, it might be faster.
		MaterialEvaluation materialEvaluation = new MaterialEvaluation();
		while ( iter_p.hasNext() ) {
			int atPos = iter_p.nextInt();
			int currPiece = theBoard.getPieceAtSquare(atPos);
			int currValue = 0;
			if ( currPiece==Piece.WHITE_PAWN ) {
				currValue = MATERIAL_VALUE_PAWN;
				currValue += PAWN_WHITE_WEIGHTINGS[atPos];
			} else if ( currPiece==Piece.BLACK_PAWN ) {
				currValue = MATERIAL_VALUE_PAWN;
				currValue += PAWN_BLACK_WEIGHTINGS[atPos];
			} else if (Piece.isRook(currPiece)) {
				currValue = MATERIAL_VALUE_ROOK;
				// TODO This is quite costly, the numSetBits is calculated since making BitBoard static
				currValue += theBoard.getNumRankFileSquaresAvailable(atPos)*2;
			} else if (Piece.isBishop(currPiece)) {
				currValue = MATERIAL_VALUE_BISHOP;
				// TODO This is quite costly, the numSetBits is calculated since making BitBoard static
				currValue += theBoard.getNumDiagonalSquaresAvailable(atPos)*2;
			} else if (Piece.isKnight(currPiece)) {
				currValue = MATERIAL_VALUE_KNIGHT;
				currValue += KNIGHT_WEIGHTINGS[atPos];
			} else if (Piece.isQueen(currPiece)) {
				currValue = MATERIAL_VALUE_QUEEN;
			} else if (Piece.isKing(currPiece)) {
				currValue = MATERIAL_VALUE_KING;
				if (isEndgame) {
					currValue += KING_ENDGAME_WEIGHTINGS[atPos];
				} else {
					currValue += KING_MIDGAME_WEIGHTINGS[atPos];
				}
			}
			if (Piece.isWhite(currPiece)) {
				materialEvaluation.addWhite(currValue);
			} else { 
				materialEvaluation.addBlack(currValue);
			}
		}
		return materialEvaluation;
	}
}
