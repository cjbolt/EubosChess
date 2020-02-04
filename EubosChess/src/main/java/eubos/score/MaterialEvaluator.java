package eubos.score;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

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
	
	public static final short ROOK_OPEN_FILE_BOOST = 25;
	
	private static final Map<Integer, Integer> PAWN_WHITE_WEIGHTINGS;
    static {
    	Map<Integer, Integer> aMap = new HashMap<Integer, Integer>();
        aMap.put(Position.a1, 0);aMap.put(Position.b1, 0);aMap.put(Position.c1, 0);aMap.put(Position.d1, 0);aMap.put(Position.e1, 0);aMap.put(Position.f1, 0);aMap.put(Position.g1, 0);aMap.put(Position.h1, 0);
        aMap.put(Position.a2, 0);aMap.put(Position.b2, 0);aMap.put(Position.c2, 0);aMap.put(Position.d2, 0);aMap.put(Position.e2, 0);aMap.put(Position.f2, 0);aMap.put(Position.g2, 0);aMap.put(Position.h2, 0);
        aMap.put(Position.a3, 0);aMap.put(Position.b3, 0);aMap.put(Position.c3, 0);aMap.put(Position.d3, 5);aMap.put(Position.e3, 5);aMap.put(Position.f3, 0);aMap.put(Position.g3, 0);aMap.put(Position.h3, 0);
        aMap.put(Position.a4, 0);aMap.put(Position.b4, 0);aMap.put(Position.c4, 5);aMap.put(Position.d4, 10);aMap.put(Position.e4, 10);aMap.put(Position.f4, 5);aMap.put(Position.g4, 0);aMap.put(Position.h4, 0);
        aMap.put(Position.a5, 0);aMap.put(Position.b5, 3);aMap.put(Position.c5, 5);aMap.put(Position.d5, 15);aMap.put(Position.e5, 15);aMap.put(Position.f5, 5);aMap.put(Position.g5, 3);aMap.put(Position.h5, 0);
		aMap.put(Position.a6, 5);aMap.put(Position.b6, 25);aMap.put(Position.c6, 25);aMap.put(Position.d6, 25);aMap.put(Position.e6, 25);aMap.put(Position.f6, 25);aMap.put(Position.g6, 25);aMap.put(Position.h6, 10);
		aMap.put(Position.a7, 25);aMap.put(Position.b7, 50);aMap.put(Position.c7, 50);aMap.put(Position.d7, 50);aMap.put(Position.e7, 50);aMap.put(Position.f7, 50);aMap.put(Position.g7, 50);aMap.put(Position.h7, 25);
		aMap.put(Position.a8, 0);aMap.put(Position.b8, 0);aMap.put(Position.c8, 0);aMap.put(Position.d8, 0);aMap.put(Position.e8, 0);aMap.put(Position.f8, 0);aMap.put(Position.g8, 0);aMap.put(Position.h8, 0);
        PAWN_WHITE_WEIGHTINGS = Collections.unmodifiableMap(aMap);
    }
    
	private static final Map<Integer, Integer> PAWN_BLACK_WEIGHTINGS;
    static {
    	Map<Integer, Integer> aMap = new HashMap<Integer, Integer>();
        aMap.put(Position.a1, 0);aMap.put(Position.b1, 0);aMap.put(Position.c1, 0);aMap.put(Position.d1, 0);aMap.put(Position.e1, 0);aMap.put(Position.f1, 0);aMap.put(Position.g1, 0);aMap.put(Position.h1, 0);
        aMap.put(Position.a2, 25);aMap.put(Position.b2, 50);aMap.put(Position.c2, 50);aMap.put(Position.d2, 50);aMap.put(Position.e2, 50);aMap.put(Position.f2, 50);aMap.put(Position.g2, 50);aMap.put(Position.h2, 25);
        aMap.put(Position.a3, 5);aMap.put(Position.b3, 25);aMap.put(Position.c3, 25);aMap.put(Position.d3, 25);aMap.put(Position.e3, 25);aMap.put(Position.f3, 25);aMap.put(Position.g3, 25);aMap.put(Position.h3, 10);
        aMap.put(Position.a4, 0);aMap.put(Position.b4, 3);aMap.put(Position.c4, 5);aMap.put(Position.d4, 15);aMap.put(Position.e4, 15);aMap.put(Position.f4, 5);aMap.put(Position.g4, 3);aMap.put(Position.h4, 0);
        aMap.put(Position.a5, 0);aMap.put(Position.b5, 0);aMap.put(Position.c5, 5);aMap.put(Position.d5, 10);aMap.put(Position.e5, 10);aMap.put(Position.f5, 5);aMap.put(Position.g5, 0);aMap.put(Position.h5, 0);
		aMap.put(Position.a6, 0);aMap.put(Position.b6, 0);aMap.put(Position.c6, 0);aMap.put(Position.d6, 5);aMap.put(Position.e6, 5);aMap.put(Position.f6, 0);aMap.put(Position.g6, 0);aMap.put(Position.h6, 0);
		aMap.put(Position.a7, 0);aMap.put(Position.b7, 0);aMap.put(Position.c7, 0);aMap.put(Position.d7, 0);aMap.put(Position.e7, 0);aMap.put(Position.f7, 0);aMap.put(Position.g7, 0);aMap.put(Position.h7, 0);
		aMap.put(Position.a8, 0);aMap.put(Position.b8, 0);aMap.put(Position.c8, 0);aMap.put(Position.d8, 0);aMap.put(Position.e8, 0);aMap.put(Position.f8, 0);aMap.put(Position.g8, 0);aMap.put(Position.h8, 0);
        PAWN_BLACK_WEIGHTINGS = Collections.unmodifiableMap(aMap);
    }    
	
	private static final Map<Integer, Integer> KNIGHT_WEIGHTINGS;
    static {
    	Map<Integer, Integer> bMap = new HashMap<Integer, Integer>();
        bMap.put(Position.a1, -20);bMap.put(Position.b1, -10);bMap.put(Position.c1, -10);bMap.put(Position.d1, -10);bMap.put(Position.e1, -10);bMap.put(Position.f1, -10);bMap.put(Position.g1, -10);bMap.put(Position.h1, -20);
		bMap.put(Position.a2, -10);bMap.put(Position.b2, 0);bMap.put(Position.c2, 0);bMap.put(Position.d2, 0);bMap.put(Position.e2, 0);bMap.put(Position.f2, 0);bMap.put(Position.g2, 0);bMap.put(Position.h2, -10);
		bMap.put(Position.a3, -10);bMap.put(Position.b3, 0);bMap.put(Position.c3, 10);bMap.put(Position.d3, 10);bMap.put(Position.e3, 10);bMap.put(Position.f3, 10);bMap.put(Position.g3, 0);bMap.put(Position.h3, -10);
		bMap.put(Position.a4, -10);bMap.put(Position.b4, 0);bMap.put(Position.c4, 10);bMap.put(Position.d4, 20);bMap.put(Position.e4, 20);bMap.put(Position.f4, 10);bMap.put(Position.g4, 0);bMap.put(Position.h4, -10);
		bMap.put(Position.a5, -10);bMap.put(Position.b5, 0);bMap.put(Position.c5, 10);bMap.put(Position.d5, 20);bMap.put(Position.e5, 20);bMap.put(Position.f5, 10);bMap.put(Position.g5, 0);bMap.put(Position.h5, -10);
		bMap.put(Position.a6, -10);bMap.put(Position.b6, 0);bMap.put(Position.c6, 10);bMap.put(Position.d6, 10);bMap.put(Position.e6, 10);bMap.put(Position.f6, 10);bMap.put(Position.g6, 0);bMap.put(Position.h6, -10);
		bMap.put(Position.a7, -10);bMap.put(Position.b7, 0);bMap.put(Position.c7, 0);bMap.put(Position.d7, 0);bMap.put(Position.e7, 0);bMap.put(Position.f7, 0);bMap.put(Position.g7, 0);bMap.put(Position.h7, -10);
		bMap.put(Position.a8, -20);bMap.put(Position.b8, -10);bMap.put(Position.c8, -10);bMap.put(Position.d8, -10);bMap.put(Position.e8, -10);bMap.put(Position.f8, -10);bMap.put(Position.g8, -10);bMap.put(Position.h8, -20);
        KNIGHT_WEIGHTINGS = Collections.unmodifiableMap(bMap);
    }
    
    private static final Map<Integer, Integer> KING_ENDGAME_WEIGHTINGS;
    static {
    	Map<Integer, Integer> bMap = new HashMap<Integer, Integer>();
        bMap.put(Position.a1, -30);bMap.put(Position.b1, -30);bMap.put(Position.c1, -30);bMap.put(Position.d1, -30);bMap.put(Position.e1, -30);bMap.put(Position.f1, -30);bMap.put(Position.g1, -30);bMap.put(Position.h1, -30);
		bMap.put(Position.a2, -30);bMap.put(Position.b2, -20);bMap.put(Position.c2, -20);bMap.put(Position.d2, -20);bMap.put(Position.e2, -20);bMap.put(Position.f2, -20);bMap.put(Position.g2, -20);bMap.put(Position.h2, -30);
		bMap.put(Position.a3, -30);bMap.put(Position.b3, -10);bMap.put(Position.c3, 0);bMap.put(Position.d3, 10);bMap.put(Position.e3, 10);bMap.put(Position.f3, 0);bMap.put(Position.g3, -10);bMap.put(Position.h3, -30);
		bMap.put(Position.a4, -20);bMap.put(Position.b4, -10);bMap.put(Position.c4, 10);bMap.put(Position.d4, 20);bMap.put(Position.e4, 20);bMap.put(Position.f4, 10);bMap.put(Position.g4, -10);bMap.put(Position.h4, -20);
		bMap.put(Position.a5, -20);bMap.put(Position.b5, -10);bMap.put(Position.c5, 10);bMap.put(Position.d5, 20);bMap.put(Position.e5, 20);bMap.put(Position.f5, 10);bMap.put(Position.g5, -10);bMap.put(Position.h5, -20);
		bMap.put(Position.a6, -30);bMap.put(Position.b6, -10);bMap.put(Position.c6, 0);bMap.put(Position.d6, 10);bMap.put(Position.e6, 10);bMap.put(Position.f6, 0);bMap.put(Position.g6, -10);bMap.put(Position.h6, -30);
		bMap.put(Position.a7, -30);bMap.put(Position.b7, -20);bMap.put(Position.c7, -20);bMap.put(Position.d7, -20);bMap.put(Position.e7, -20);bMap.put(Position.f7, -20);bMap.put(Position.g7, -20);bMap.put(Position.h7, -30);
		bMap.put(Position.a8, -30);bMap.put(Position.b8, -30);bMap.put(Position.c8, -30);bMap.put(Position.d8, -30);bMap.put(Position.e8, -30);bMap.put(Position.f8, -30);bMap.put(Position.g8, -30);bMap.put(Position.h8, -30);
		KING_ENDGAME_WEIGHTINGS = Collections.unmodifiableMap(bMap);
    }
    
    private static final Map<Integer, Integer> KING_MIDGAME_WEIGHTINGS;
    static {
    	Map<Integer, Integer> bMap = new HashMap<Integer, Integer>();
        bMap.put(Position.a1, 5);bMap.put(Position.b1, 10);bMap.put(Position.c1, 5);bMap.put(Position.d1, 0);bMap.put(Position.e1, 0);bMap.put(Position.f1, 5);bMap.put(Position.g1, 10);bMap.put(Position.h1, 5);
		bMap.put(Position.a2, 0);bMap.put(Position.b2, 0);bMap.put(Position.c2, 0);bMap.put(Position.d2, 0);bMap.put(Position.e2, 0);bMap.put(Position.f2, 0);bMap.put(Position.g2, 0);bMap.put(Position.h2, 0);
		bMap.put(Position.a3, -20);bMap.put(Position.b3, -20);bMap.put(Position.c3, -30);bMap.put(Position.d3, -30);bMap.put(Position.e3, -30);bMap.put(Position.f3, -30);bMap.put(Position.g3, -20);bMap.put(Position.h3, -20);
		bMap.put(Position.a4, -30);bMap.put(Position.b4, -40);bMap.put(Position.c4, -50);bMap.put(Position.d4, -50);bMap.put(Position.e4, -50);bMap.put(Position.f4, -40);bMap.put(Position.g4, -40);bMap.put(Position.h4, -30);
		bMap.put(Position.a5, -30);bMap.put(Position.b5, -40);bMap.put(Position.c5, -50);bMap.put(Position.d5, -50);bMap.put(Position.e5, -50);bMap.put(Position.f5, -40);bMap.put(Position.g5, -40);bMap.put(Position.h5, -30);
		bMap.put(Position.a6, -20);bMap.put(Position.b6, -20);bMap.put(Position.c6, -30);bMap.put(Position.d6, -30);bMap.put(Position.e6, -30);bMap.put(Position.f6, -30);bMap.put(Position.g6, -20);bMap.put(Position.h6, -20);
		bMap.put(Position.a7, 0);bMap.put(Position.b7, 0);bMap.put(Position.c7, 0);bMap.put(Position.d7, 0);bMap.put(Position.e7, 0);bMap.put(Position.f7, 0);bMap.put(Position.g7, 0);bMap.put(Position.h7, 0);
		bMap.put(Position.a8, 5);bMap.put(Position.b8, 10);bMap.put(Position.c8, 5);bMap.put(Position.d8, 0);bMap.put(Position.e8, 0);bMap.put(Position.f8, 5);bMap.put(Position.g8, 10);bMap.put(Position.h8, 5);
		KING_MIDGAME_WEIGHTINGS = Collections.unmodifiableMap(bMap);
    }
 
	static MaterialEvaluation evaluate(Board theBoard, boolean isEndgame) {
		Iterator<Integer> iter_p = theBoard.iterator();
		// TODO this needn't be done with an iterator. We could just go through all the BitBoards, it might be faster.
		MaterialEvaluation materialEvaluation = new MaterialEvaluation();
		while ( iter_p.hasNext() ) {
			int atPos = iter_p.next();
			int currPiece = theBoard.getPieceAtSquare(atPos);
			int currValue = 0;
			if ( currPiece==Piece.WHITE_PAWN ) {
				currValue = MATERIAL_VALUE_PAWN;
				currValue += PAWN_WHITE_WEIGHTINGS.get(atPos);
			} else if ( currPiece==Piece.BLACK_PAWN ) {
				currValue = MATERIAL_VALUE_PAWN;
				currValue += PAWN_BLACK_WEIGHTINGS.get(atPos);
			} else if (Piece.isRook(currPiece)) {
				currValue = MATERIAL_VALUE_ROOK;
				currValue += theBoard.getNumRankFileSquaresAvailable(atPos)*2;
			} else if (Piece.isBishop(currPiece)) {
				currValue = MATERIAL_VALUE_BISHOP;
				currValue += theBoard.getNumDiagonalSquaresAvailable(atPos)*2;
			} else if (Piece.isKnight(currPiece)) {
				currValue = MATERIAL_VALUE_KNIGHT;
				currValue += KNIGHT_WEIGHTINGS.get(atPos);
			} else if (Piece.isQueen(currPiece)) {
				currValue = MATERIAL_VALUE_QUEEN;
			} else if (Piece.isKing(currPiece)) {
				currValue = MATERIAL_VALUE_KING;
				if (isEndgame) {
					currValue += KING_ENDGAME_WEIGHTINGS.get(atPos);
				} else {
					currValue += KING_MIDGAME_WEIGHTINGS.get(atPos);
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
