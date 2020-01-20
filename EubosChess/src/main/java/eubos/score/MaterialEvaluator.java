package eubos.score;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.Piece.PieceType;
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
    
	private static final Map<GenericPosition, Integer> PAWN_BLACK_WEIGHTINGS;
    static {
    	Map<GenericPosition, Integer> aMap = new EnumMap<GenericPosition, Integer>(GenericPosition.class);
        aMap.put(GenericPosition.a1, 0);aMap.put(GenericPosition.b1, 0);aMap.put(GenericPosition.c1, 0);aMap.put(GenericPosition.d1, 0);aMap.put(GenericPosition.e1, 0);aMap.put(GenericPosition.f1, 0);aMap.put(GenericPosition.g1, 0);aMap.put(GenericPosition.h1, 0);
        aMap.put(GenericPosition.a2, 25);aMap.put(GenericPosition.b2, 50);aMap.put(GenericPosition.c2, 50);aMap.put(GenericPosition.d2, 50);aMap.put(GenericPosition.e2, 50);aMap.put(GenericPosition.f2, 50);aMap.put(GenericPosition.g2, 50);aMap.put(GenericPosition.h2, 25);
        aMap.put(GenericPosition.a3, 5);aMap.put(GenericPosition.b3, 25);aMap.put(GenericPosition.c3, 25);aMap.put(GenericPosition.d3, 25);aMap.put(GenericPosition.e3, 25);aMap.put(GenericPosition.f3, 25);aMap.put(GenericPosition.g3, 25);aMap.put(GenericPosition.h3, 10);
        aMap.put(GenericPosition.a4, 0);aMap.put(GenericPosition.b4, 3);aMap.put(GenericPosition.c4, 5);aMap.put(GenericPosition.d4, 15);aMap.put(GenericPosition.e4, 15);aMap.put(GenericPosition.f4, 5);aMap.put(GenericPosition.g4, 3);aMap.put(GenericPosition.h4, 0);
        aMap.put(GenericPosition.a5, 0);aMap.put(GenericPosition.b5, 0);aMap.put(GenericPosition.c5, 5);aMap.put(GenericPosition.d5, 10);aMap.put(GenericPosition.e5, 10);aMap.put(GenericPosition.f5, 5);aMap.put(GenericPosition.g5, 0);aMap.put(GenericPosition.h5, 0);
		aMap.put(GenericPosition.a6, 0);aMap.put(GenericPosition.b6, 0);aMap.put(GenericPosition.c6, 0);aMap.put(GenericPosition.d6, 5);aMap.put(GenericPosition.e6, 5);aMap.put(GenericPosition.f6, 0);aMap.put(GenericPosition.g6, 0);aMap.put(GenericPosition.h6, 0);
		aMap.put(GenericPosition.a7, 0);aMap.put(GenericPosition.b7, 0);aMap.put(GenericPosition.c7, 0);aMap.put(GenericPosition.d7, 0);aMap.put(GenericPosition.e7, 0);aMap.put(GenericPosition.f7, 0);aMap.put(GenericPosition.g7, 0);aMap.put(GenericPosition.h7, 0);
		aMap.put(GenericPosition.a8, 0);aMap.put(GenericPosition.b8, 0);aMap.put(GenericPosition.c8, 0);aMap.put(GenericPosition.d8, 0);aMap.put(GenericPosition.e8, 0);aMap.put(GenericPosition.f8, 0);aMap.put(GenericPosition.g8, 0);aMap.put(GenericPosition.h8, 0);
        PAWN_BLACK_WEIGHTINGS = Collections.unmodifiableMap(aMap);
    }    
	
	private static final Map<GenericPosition, Integer> KNIGHT_WEIGHTINGS;
    static {
    	Map<GenericPosition, Integer> bMap = new EnumMap<GenericPosition, Integer>(GenericPosition.class);
        bMap.put(GenericPosition.a1, -20);bMap.put(GenericPosition.b1, -10);bMap.put(GenericPosition.c1, -10);bMap.put(GenericPosition.d1, -10);bMap.put(GenericPosition.e1, -10);bMap.put(GenericPosition.f1, -10);bMap.put(GenericPosition.g1, -10);bMap.put(GenericPosition.h1, -20);
		bMap.put(GenericPosition.a2, -10);bMap.put(GenericPosition.b2, 0);bMap.put(GenericPosition.c2, 0);bMap.put(GenericPosition.d2, 0);bMap.put(GenericPosition.e2, 0);bMap.put(GenericPosition.f2, 0);bMap.put(GenericPosition.g2, 0);bMap.put(GenericPosition.h2, -10);
		bMap.put(GenericPosition.a3, -10);bMap.put(GenericPosition.b3, 0);bMap.put(GenericPosition.c3, 10);bMap.put(GenericPosition.d3, 10);bMap.put(GenericPosition.e3, 10);bMap.put(GenericPosition.f3, 10);bMap.put(GenericPosition.g3, 0);bMap.put(GenericPosition.h3, -10);
		bMap.put(GenericPosition.a4, -10);bMap.put(GenericPosition.b4, 0);bMap.put(GenericPosition.c4, 10);bMap.put(GenericPosition.d4, 20);bMap.put(GenericPosition.e4, 20);bMap.put(GenericPosition.f4, 10);bMap.put(GenericPosition.g4, 0);bMap.put(GenericPosition.h4, -10);
		bMap.put(GenericPosition.a5, -10);bMap.put(GenericPosition.b5, 0);bMap.put(GenericPosition.c5, 10);bMap.put(GenericPosition.d5, 20);bMap.put(GenericPosition.e5, 20);bMap.put(GenericPosition.f5, 10);bMap.put(GenericPosition.g5, 0);bMap.put(GenericPosition.h5, -10);
		bMap.put(GenericPosition.a6, -10);bMap.put(GenericPosition.b6, 0);bMap.put(GenericPosition.c6, 10);bMap.put(GenericPosition.d6, 10);bMap.put(GenericPosition.e6, 10);bMap.put(GenericPosition.f6, 10);bMap.put(GenericPosition.g6, 0);bMap.put(GenericPosition.h6, -10);
		bMap.put(GenericPosition.a7, -10);bMap.put(GenericPosition.b7, 0);bMap.put(GenericPosition.c7, 0);bMap.put(GenericPosition.d7, 0);bMap.put(GenericPosition.e7, 0);bMap.put(GenericPosition.f7, 0);bMap.put(GenericPosition.g7, 0);bMap.put(GenericPosition.h7, -10);
		bMap.put(GenericPosition.a8, -20);bMap.put(GenericPosition.b8, -10);bMap.put(GenericPosition.c8, -10);bMap.put(GenericPosition.d8, -10);bMap.put(GenericPosition.e8, -10);bMap.put(GenericPosition.f8, -10);bMap.put(GenericPosition.g8, -10);bMap.put(GenericPosition.h8, -20);
        KNIGHT_WEIGHTINGS = Collections.unmodifiableMap(bMap);
    }	
 
	static MaterialEvaluation evaluate(Board theBoard) {
		Iterator<Integer> iter_p = theBoard.iterator();
		// TODO this needn't be done with an iterator. We could just go through all the BitBoards, it might be faster.
		MaterialEvaluation materialEvaluation = new MaterialEvaluation();
		while ( iter_p.hasNext() ) {
			int atPos = iter_p.next();
			PieceType currPiece = theBoard.getPieceAtSquare(atPos);
			int currValue = 0;
			if ( currPiece==PieceType.WhitePawn ) {
				currValue = MATERIAL_VALUE_PAWN;
				currValue += PAWN_WHITE_WEIGHTINGS.get(atPos);
			} else if ( currPiece==PieceType.BlackPawn ) {
				currValue = MATERIAL_VALUE_PAWN;
				currValue += PAWN_BLACK_WEIGHTINGS.get(atPos);
			} else if (PieceType.isRook(currPiece)) {
				currValue = MATERIAL_VALUE_ROOK;
				currValue += theBoard.getNumRankFileSquaresAvailable(atPos)*2;
			} else if (PieceType.isBishop(currPiece)) {
				currValue = MATERIAL_VALUE_BISHOP;
				currValue += theBoard.getNumDiagonalSquaresAvailable(atPos)*2;
			} else if (PieceType.isKnight(currPiece)) {
				currValue = MATERIAL_VALUE_KNIGHT;
				currValue += KNIGHT_WEIGHTINGS.get(atPos);
			} else if (PieceType.isQueen(currPiece)) {
				currValue = MATERIAL_VALUE_QUEEN;
			} else if (PieceType.isKing(currPiece)) {
				currValue = MATERIAL_VALUE_KING;
			}
			if (PieceType.isWhite(currPiece)) {
				materialEvaluation.addWhite(currValue);
			} else { 
				materialEvaluation.addBlack(currValue);
			}
		}
		return materialEvaluation;
	}
}
