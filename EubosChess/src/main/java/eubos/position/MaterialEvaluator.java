package eubos.position;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.pieces.Bishop;
import eubos.board.pieces.King;
import eubos.board.pieces.Knight;
import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Queen;
import eubos.board.pieces.Rook;

public class MaterialEvaluator {
	
	private static final Map<GenericPosition, Integer> PAWN_WHITE_WEIGHTINGS;
    static {
    	Map<GenericPosition, Integer> aMap = new HashMap<GenericPosition, Integer>();
        aMap.put(GenericPosition.a1, 0);aMap.put(GenericPosition.b1, 0);aMap.put(GenericPosition.c1, 0);aMap.put(GenericPosition.d1, 0);aMap.put(GenericPosition.e1, 0);aMap.put(GenericPosition.f1, 0);aMap.put(GenericPosition.g1, 0);aMap.put(GenericPosition.h1, 0);
        aMap.put(GenericPosition.a2, 0);aMap.put(GenericPosition.b2, 0);aMap.put(GenericPosition.c2, 0);aMap.put(GenericPosition.d2, 0);aMap.put(GenericPosition.e2, 0);aMap.put(GenericPosition.f2, 0);aMap.put(GenericPosition.g2, 0);aMap.put(GenericPosition.h2, 0);
        aMap.put(GenericPosition.a3, 0);aMap.put(GenericPosition.b3, 0);aMap.put(GenericPosition.c3, 0);aMap.put(GenericPosition.d3, 5);aMap.put(GenericPosition.e3, 5);aMap.put(GenericPosition.f3, 0);aMap.put(GenericPosition.g3, 0);aMap.put(GenericPosition.h3, 0);
        aMap.put(GenericPosition.a4, 3);aMap.put(GenericPosition.b4, 3);aMap.put(GenericPosition.c4, 5);aMap.put(GenericPosition.d4, 10);aMap.put(GenericPosition.e4, 10);aMap.put(GenericPosition.f4, 5);aMap.put(GenericPosition.g4, 3);aMap.put(GenericPosition.h4, 3);
        aMap.put(GenericPosition.a5, 5);aMap.put(GenericPosition.b5, 5);aMap.put(GenericPosition.c5, 5);aMap.put(GenericPosition.d5, 15);aMap.put(GenericPosition.e5, 15);aMap.put(GenericPosition.f5, 5);aMap.put(GenericPosition.g5, 5);aMap.put(GenericPosition.h5, 5);
		aMap.put(GenericPosition.a6, 10);aMap.put(GenericPosition.b6, 25);aMap.put(GenericPosition.c6, 25);aMap.put(GenericPosition.d6, 25);aMap.put(GenericPosition.e6, 25);aMap.put(GenericPosition.f6, 25);aMap.put(GenericPosition.g6, 25);aMap.put(GenericPosition.h6, 10);
		aMap.put(GenericPosition.a7, 25);aMap.put(GenericPosition.b7, 50);aMap.put(GenericPosition.c7, 50);aMap.put(GenericPosition.d7, 50);aMap.put(GenericPosition.e7, 50);aMap.put(GenericPosition.f7, 50);aMap.put(GenericPosition.g7, 50);aMap.put(GenericPosition.h7, 25);
		aMap.put(GenericPosition.a8, 0);aMap.put(GenericPosition.b8, 0);aMap.put(GenericPosition.c8, 0);aMap.put(GenericPosition.d8, 0);aMap.put(GenericPosition.e8, 0);aMap.put(GenericPosition.f8, 0);aMap.put(GenericPosition.g8, 0);aMap.put(GenericPosition.h8, 0);
        PAWN_WHITE_WEIGHTINGS = Collections.unmodifiableMap(aMap);
    }
    
	private static final Map<GenericPosition, Integer> PAWN_BLACK_WEIGHTINGS;
    static {
    	Map<GenericPosition, Integer> aMap = new HashMap<GenericPosition, Integer>();
        aMap.put(GenericPosition.a1, 0);aMap.put(GenericPosition.b1, 0);aMap.put(GenericPosition.c1, 0);aMap.put(GenericPosition.d1, 0);aMap.put(GenericPosition.e1, 0);aMap.put(GenericPosition.f1, 0);aMap.put(GenericPosition.g1, 0);aMap.put(GenericPosition.h1, 0);
        aMap.put(GenericPosition.a2, 25);aMap.put(GenericPosition.b2, 50);aMap.put(GenericPosition.c2, 50);aMap.put(GenericPosition.d2, 50);aMap.put(GenericPosition.e2, 50);aMap.put(GenericPosition.f2, 50);aMap.put(GenericPosition.g2, 50);aMap.put(GenericPosition.h2, 25);
        aMap.put(GenericPosition.a3, 10);aMap.put(GenericPosition.b3, 25);aMap.put(GenericPosition.c3, 25);aMap.put(GenericPosition.d3, 25);aMap.put(GenericPosition.e3, 25);aMap.put(GenericPosition.f3, 25);aMap.put(GenericPosition.g3, 25);aMap.put(GenericPosition.h3, 10);
        aMap.put(GenericPosition.a4, 5);aMap.put(GenericPosition.b4, 5);aMap.put(GenericPosition.c4, 5);aMap.put(GenericPosition.d4, 15);aMap.put(GenericPosition.e4, 15);aMap.put(GenericPosition.f4, 5);aMap.put(GenericPosition.g4, 5);aMap.put(GenericPosition.h4, 5);
        aMap.put(GenericPosition.a5, 3);aMap.put(GenericPosition.b5, 3);aMap.put(GenericPosition.c5, 5);aMap.put(GenericPosition.d5, 10);aMap.put(GenericPosition.e5, 10);aMap.put(GenericPosition.f5, 5);aMap.put(GenericPosition.g5, 3);aMap.put(GenericPosition.h5, 3);
		aMap.put(GenericPosition.a6, 0);aMap.put(GenericPosition.b6, 0);aMap.put(GenericPosition.c6, 0);aMap.put(GenericPosition.d6, 5);aMap.put(GenericPosition.e6, 5);aMap.put(GenericPosition.f6, 0);aMap.put(GenericPosition.g6, 0);aMap.put(GenericPosition.h6, 0);
		aMap.put(GenericPosition.a7, 0);aMap.put(GenericPosition.b7, 0);aMap.put(GenericPosition.c7, 0);aMap.put(GenericPosition.d7, 0);aMap.put(GenericPosition.e7, 0);aMap.put(GenericPosition.f7, 0);aMap.put(GenericPosition.g7, 0);aMap.put(GenericPosition.h7, 0);
		aMap.put(GenericPosition.a8, 0);aMap.put(GenericPosition.b8, 0);aMap.put(GenericPosition.c8, 0);aMap.put(GenericPosition.d8, 0);aMap.put(GenericPosition.e8, 0);aMap.put(GenericPosition.f8, 0);aMap.put(GenericPosition.g8, 0);aMap.put(GenericPosition.h8, 0);
        PAWN_BLACK_WEIGHTINGS = Collections.unmodifiableMap(aMap);
    }    
	
	private static final Map<GenericPosition, Integer> KNIGHT_WEIGHTINGS;
    static {
    	Map<GenericPosition, Integer> bMap = new HashMap<GenericPosition, Integer>();
        bMap.put(GenericPosition.a1, 0);bMap.put(GenericPosition.b1, 0);bMap.put(GenericPosition.c1, 0);bMap.put(GenericPosition.d1, 0);bMap.put(GenericPosition.e1, 0);bMap.put(GenericPosition.f1, 0);bMap.put(GenericPosition.g1, 0);bMap.put(GenericPosition.h1, 0);
		bMap.put(GenericPosition.a2, 0);bMap.put(GenericPosition.b2, 0);bMap.put(GenericPosition.c2, 0);bMap.put(GenericPosition.d2, 0);bMap.put(GenericPosition.e2, 0);bMap.put(GenericPosition.f2, 0);bMap.put(GenericPosition.g2, 0);bMap.put(GenericPosition.h2, 0);
		bMap.put(GenericPosition.a3, 0);bMap.put(GenericPosition.b3, 0);bMap.put(GenericPosition.c3, 10);bMap.put(GenericPosition.d3, 10);bMap.put(GenericPosition.e3, 10);bMap.put(GenericPosition.f3, 10);bMap.put(GenericPosition.g3, 0);bMap.put(GenericPosition.h3, 0);
		bMap.put(GenericPosition.a4, 0);bMap.put(GenericPosition.b4, 0);bMap.put(GenericPosition.c4, 10);bMap.put(GenericPosition.d4, 20);bMap.put(GenericPosition.e4, 20);bMap.put(GenericPosition.f4, 10);bMap.put(GenericPosition.g4, 0);bMap.put(GenericPosition.h4, 0);
		bMap.put(GenericPosition.a5, 0);bMap.put(GenericPosition.b5, 0);bMap.put(GenericPosition.c5, 10);bMap.put(GenericPosition.d5, 20);bMap.put(GenericPosition.e5, 20);bMap.put(GenericPosition.f5, 10);bMap.put(GenericPosition.g5, 0);bMap.put(GenericPosition.h5, 0);
		bMap.put(GenericPosition.a6, 0);bMap.put(GenericPosition.b6, 0);bMap.put(GenericPosition.c6, 10);bMap.put(GenericPosition.d6, 10);bMap.put(GenericPosition.e6, 10);bMap.put(GenericPosition.f6, 10);bMap.put(GenericPosition.g6, 0);bMap.put(GenericPosition.h6, 0);
		bMap.put(GenericPosition.a7, 0);bMap.put(GenericPosition.b7, 0);bMap.put(GenericPosition.c7, 0);bMap.put(GenericPosition.d7, 0);bMap.put(GenericPosition.e7, 0);bMap.put(GenericPosition.f7, 0);bMap.put(GenericPosition.g7, 0);bMap.put(GenericPosition.h7, 0);
		bMap.put(GenericPosition.a8, 0);bMap.put(GenericPosition.b8, 0);bMap.put(GenericPosition.c8, 0);bMap.put(GenericPosition.d8, 0);bMap.put(GenericPosition.e8, 0);bMap.put(GenericPosition.f8, 0);bMap.put(GenericPosition.g8, 0);bMap.put(GenericPosition.h8, 0);
        KNIGHT_WEIGHTINGS = Collections.unmodifiableMap(bMap);
    }	

	private static final Map<GenericPosition, Integer>BISHOP_WEIGHTINGS;
    static {
    	Map<GenericPosition, Integer> cMap = new HashMap<GenericPosition, Integer>();
		cMap.put(GenericPosition.a1, 5);cMap.put(GenericPosition.b1, 0);cMap.put(GenericPosition.c1, 0);cMap.put(GenericPosition.d1, 0);cMap.put(GenericPosition.e1, 0);cMap.put(GenericPosition.f1, 0);cMap.put(GenericPosition.g1, 0);cMap.put(GenericPosition.h1, 5);
		cMap.put(GenericPosition.a2, 0);cMap.put(GenericPosition.b2, 5);cMap.put(GenericPosition.c2, 0);cMap.put(GenericPosition.d2, 0);cMap.put(GenericPosition.e2, 0);cMap.put(GenericPosition.f2, 0);cMap.put(GenericPosition.g2, 5);cMap.put(GenericPosition.h2, 0);
		cMap.put(GenericPosition.a3, 0);cMap.put(GenericPosition.b3, 0);cMap.put(GenericPosition.c3, 5);cMap.put(GenericPosition.d3, 0);cMap.put(GenericPosition.e3, 0);cMap.put(GenericPosition.f3, 5);cMap.put(GenericPosition.g3, 0);cMap.put(GenericPosition.h3, 0);
		cMap.put(GenericPosition.a4, 0);cMap.put(GenericPosition.b4, 0);cMap.put(GenericPosition.c4, 0);cMap.put(GenericPosition.d4, 5);cMap.put(GenericPosition.e4, 5);cMap.put(GenericPosition.f4, 0);cMap.put(GenericPosition.g4, 0);cMap.put(GenericPosition.h4, 0);
		cMap.put(GenericPosition.a5, 0);cMap.put(GenericPosition.b5, 0);cMap.put(GenericPosition.c5, 0);cMap.put(GenericPosition.d5, 5);cMap.put(GenericPosition.e5, 5);cMap.put(GenericPosition.f5, 0);cMap.put(GenericPosition.g5, 0);cMap.put(GenericPosition.h5, 0);
		cMap.put(GenericPosition.a6, 0);cMap.put(GenericPosition.b6, 0);cMap.put(GenericPosition.c6, 5);cMap.put(GenericPosition.d6, 0);cMap.put(GenericPosition.e6, 0);cMap.put(GenericPosition.f6, 5);cMap.put(GenericPosition.g6, 0);cMap.put(GenericPosition.h6, 0);
		cMap.put(GenericPosition.a7, 0);cMap.put(GenericPosition.b7, 5);cMap.put(GenericPosition.c7, 0);cMap.put(GenericPosition.d7, 0);cMap.put(GenericPosition.e7, 0);cMap.put(GenericPosition.f7, 0);cMap.put(GenericPosition.g7, 5);cMap.put(GenericPosition.h7, 0);
		cMap.put(GenericPosition.a8, 5);cMap.put(GenericPosition.b8, 0);cMap.put(GenericPosition.c8, 0);cMap.put(GenericPosition.d8, 0);cMap.put(GenericPosition.e8, 0);cMap.put(GenericPosition.f8, 0);cMap.put(GenericPosition.g8, 0);cMap.put(GenericPosition.h8, 5);
        BISHOP_WEIGHTINGS = Collections.unmodifiableMap(cMap);
    }
    
	public static MaterialEvaluation evaluate(Board theBoard) {
		Iterator<Piece> iter_p = theBoard.iterator();
		MaterialEvaluation materialEvaluation = new MaterialEvaluation();
		while ( iter_p.hasNext() ) {
			Piece currPiece = iter_p.next();
			int currValue = 0;
			if ( currPiece instanceof Pawn ) {
				currValue = Pawn.MATERIAL_VALUE;
				if (currPiece.isWhite()) {
					currValue += PAWN_WHITE_WEIGHTINGS.get(currPiece.getSquare());
				} else {
					currValue += PAWN_BLACK_WEIGHTINGS.get(currPiece.getSquare());
				}
			}
			else if ( currPiece instanceof Rook )
				currValue = Rook.MATERIAL_VALUE;
			else if ( currPiece instanceof Bishop ) {
				currValue = Bishop.MATERIAL_VALUE;
				currValue += BISHOP_WEIGHTINGS.get(currPiece.getSquare());
			}
			else if ( currPiece instanceof Knight ) {
				currValue = Knight.MATERIAL_VALUE;
				currValue += KNIGHT_WEIGHTINGS.get(currPiece.getSquare());
			}
			else if ( currPiece instanceof Queen )
				currValue = Queen.MATERIAL_VALUE;
			else if ( currPiece instanceof King )
				currValue = King.MATERIAL_VALUE;
			if (currPiece.isWhite()) {
				materialEvaluation.addWhite(currValue);
			} else { 
				materialEvaluation.addBlack(currValue);
			}
		}
		return materialEvaluation;
	}
}
