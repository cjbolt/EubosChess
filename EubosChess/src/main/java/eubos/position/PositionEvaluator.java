package eubos.position;

import java.util.Iterator;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericRank;

import eubos.board.Board;
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
	
	public short evaluatePosition(IPositionAccessors pos) {
		short score = me.evaluate(pos.getTheBoard());
		score += encourageCastling(pos);
		score += discourageDoubledPawns(pos);
		score += encouragePassedPawns(pos);
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
	
	int encouragePassedPawns(IPositionAccessors pos) {
		int passedPawnBoost = 0;
		passedPawnBoost += checkPassedPawnsForColour(pos, pos.getOnMove());
		Colour onMoveWas = Colour.getOpposite(pos.getOnMove());
		passedPawnBoost += checkPassedPawnsForColour(pos, onMoveWas);
		return passedPawnBoost;
	}

	private int checkPassedPawnsForColour(IPositionAccessors pos, Colour onMoveWas) {
		Board board = pos.getTheBoard();
		int passedPawnBoost = 0;
		Iterator<Piece> iter = board.iterateColour(onMoveWas);
		while (iter.hasNext()) {
			Piece currPiece = iter.next();
			
			if (currPiece instanceof Pawn) {
				GenericRank rank = currPiece.getSquare().rank;
				switch (currPiece.getSquare().file) {
				case Fa:
					if (!board.checkIfOpposingPawnInFile(GenericFile.Fa, rank, onMoveWas) &&
						!board.checkIfOpposingPawnInFile(GenericFile.Fb, rank, onMoveWas)) {
						passedPawnBoost += 25;
					};
					break;
				case Fb:
					if (!board.checkIfOpposingPawnInFile(GenericFile.Fa, rank, onMoveWas) &&
					    !board.checkIfOpposingPawnInFile(GenericFile.Fb, rank, onMoveWas) &&
					    !board.checkIfOpposingPawnInFile(GenericFile.Fc, rank, onMoveWas)) {
						passedPawnBoost += 50;
					}
					break;
				case Fc:
					if (!board.checkIfOpposingPawnInFile(GenericFile.Fb, rank, onMoveWas) &&
						!board.checkIfOpposingPawnInFile(GenericFile.Fc, rank, onMoveWas) &&
					    !board.checkIfOpposingPawnInFile(GenericFile.Fd, rank, onMoveWas)) {
					    passedPawnBoost += 50;	
					}
					break;
				case Fd:
					if (!board.checkIfOpposingPawnInFile(GenericFile.Fc, rank, onMoveWas) &&
						!board.checkIfOpposingPawnInFile(GenericFile.Fd, rank, onMoveWas) &&
					    !board.checkIfOpposingPawnInFile(GenericFile.Fe, rank, onMoveWas)) {
						passedPawnBoost += 50;
					}
					break;
				case Fe:
					if (!board.checkIfOpposingPawnInFile(GenericFile.Fd, rank, onMoveWas) &&
						!board.checkIfOpposingPawnInFile(GenericFile.Fe, rank, onMoveWas) &&
						!board.checkIfOpposingPawnInFile(GenericFile.Ff, rank, onMoveWas)) {
						passedPawnBoost += 50;
					}
					break;
				case Ff:
					if (!board.checkIfOpposingPawnInFile(GenericFile.Fe, rank, onMoveWas) &&
						!board.checkIfOpposingPawnInFile(GenericFile.Ff, rank, onMoveWas) &&
					    !board.checkIfOpposingPawnInFile(GenericFile.Fg, rank, onMoveWas)) {
						passedPawnBoost += 50;
					}
					break;
				case Fg:
					if (!board.checkIfOpposingPawnInFile(GenericFile.Ff, rank, onMoveWas) &&
						!board.checkIfOpposingPawnInFile(GenericFile.Fg, rank, onMoveWas) &&
					    !board.checkIfOpposingPawnInFile(GenericFile.Fh, rank, onMoveWas)) {
						passedPawnBoost += 50;
					}
					break;
				case Fh:
					if (!board.checkIfOpposingPawnInFile(GenericFile.Fg, currPiece.getSquare().rank, onMoveWas)) {
						passedPawnBoost += 25;
					}
					break;
				}
			}
		}
		if (onMoveWas == Colour.black) {
			passedPawnBoost = -passedPawnBoost;
		}
		return passedPawnBoost;
	}
	
	int discourageDoubledPawns(IPositionAccessors pos) {
		int doubledPawnScoreModifier = 0;
		doubledPawnScoreModifier += discourageDoubledPawnsForColour(pos, pos.getOnMove());
		Colour onMoveWas = Colour.getOpposite(pos.getOnMove());
		doubledPawnScoreModifier += discourageDoubledPawnsForColour(pos, onMoveWas);
		return doubledPawnScoreModifier;
	}

	private int discourageDoubledPawnsForColour(IPositionAccessors pos, Colour onMoveWas) {
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
