package eubos.position;

import java.util.Iterator;

import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Piece.Colour;

public class PositionEvaluator implements IEvaluate {
	
	MaterialEvaluator me;
	
	public static int HAS_CASTLED_BOOST_CENTIPAWNS = 150;
	public static int DOUBLED_PAWN_HANDICAP = 50;
	
	public PositionEvaluator() {	
		this.me = new MaterialEvaluator();
	}
	
	public int evaluatePosition(IPositionAccessors pos) {
		int score = me.evaluate(pos.getTheBoard());
		score += encourageCastling(pos);
		score += discourageDoubledPawns(pos);
		return score;
	}
	
	private int encourageCastling(IPositionAccessors pos) {
		int castleScoreBoost = 0;
		Colour onMoveWas = Colour.getOpposite(pos.getOnMove());
		if (pos.hasCastled(onMoveWas)) {
			castleScoreBoost = HAS_CASTLED_BOOST_CENTIPAWNS;
		}
		if (onMoveWas == Colour.black) {
			castleScoreBoost = -castleScoreBoost;
		}
		return castleScoreBoost;
	}
	
	int discourageDoubledPawns(IPositionAccessors pos) {
		Colour onMoveWas = Colour.getOpposite(pos.getOnMove());
		Iterator<Piece> iter = pos.getTheBoard().iterateColour(onMoveWas);
		int pawnHandicap = 0;
		int pawnCount[] = {0,0,0,0,0,0,0,0};
		while (iter.hasNext()) {
			Piece currPiece = iter.next();
			if (currPiece instanceof Pawn) {
				switch (currPiece.getSquare().file) {
				case Fa:
					pawnCount[0] += 1;
					break;
				case Fb:
					pawnCount[1] += 1;
					break;
				case Fc:
					pawnCount[2] += 1;
					break;
				case Fd:
					pawnCount[3] += 1;
					break;
				case Fe:
					pawnCount[4] += 1;
					break;
				case Ff:
					pawnCount[5] += 1;
					break;
				case Fg:
					pawnCount[6] += 1;
					break;
				case Fh:
					pawnCount[7] += 1;
					break;
				}
			}
		}
		for (int i=0; i<8; i++) {
			if (pawnCount[i] > 1)
				pawnHandicap -= DOUBLED_PAWN_HANDICAP;
		}
		if (onMoveWas == Colour.black) {
			pawnHandicap = -pawnHandicap;
		}
		return pawnHandicap;
	}
}
