package eubos.position;

import java.util.Iterator;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericRank;

import eubos.board.Board;
import eubos.board.SquareAttackEvaluator;
import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Piece.Colour;

public class PositionEvaluator implements IEvaluate {
	
	MaterialEvaluator me;
	PositionManager pm;
	
	public static final int HAS_CASTLED_BOOST_CENTIPAWNS = 150;
	public static final int DOUBLED_PAWN_HANDICAP = 50;
	public static final int PASSED_PAWN_BOOST = 50;
	public static final int ROOK_FILE_PASSED_PAWN_BOOST = 25;
	
	public PositionEvaluator(PositionManager pm) {	
		this.me = new MaterialEvaluator();
		this.pm = pm;
	}
	
	public boolean isQuiescent() {
		if (pm.isKingInCheck(pm.getOnMove())) {
			// In order to check for mates
			return false;
		} else if (pm.lastMoveWasCapture()) {
			// we could keep a capture list, so we know where we are in the exchange series?
			// we can get access to the captured piece in the current codebase, but we need to know the whole capture sequence to do swap off?
			Piece captured = pm.getCapturedPiece();
			if (captured != null)
			{
				if (SquareAttackEvaluator.isAttacked(
						pm.getTheBoard(),
						captured.getSquare(),
						Colour.getOpposite(pm.getOnMove())))
					return false;
			}
		}
		return true;
	}
	
	public short evaluatePosition() {
		short score = me.evaluate(pm.getTheBoard());
		score += encourageCastling();
		score += discourageDoubledPawns();
		score += encouragePassedPawns();
		return score;
	}
	
	int encourageCastling() {
		int castleScoreBoost = 0;
		Colour onMoveWas = Colour.getOpposite(pm.getOnMove());
		if (pm.hasCastled(onMoveWas)) {
			castleScoreBoost = HAS_CASTLED_BOOST_CENTIPAWNS;
		}
		if (onMoveWas == Colour.black) {
			castleScoreBoost = -castleScoreBoost;
		}
		return castleScoreBoost;
	}
	
	int encouragePassedPawns() {
		int passedPawnBoost = checkPassedPawnsForColour(pm.getOnMove());
		passedPawnBoost += checkPassedPawnsForColour(Colour.getOpposite(pm.getOnMove()));
		return passedPawnBoost;
	}

	private int checkPassedPawnsForColour(Colour onMoveWas) {
		Board board = pm.getTheBoard();
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
						passedPawnBoost += ROOK_FILE_PASSED_PAWN_BOOST;
					};
					break;
				case Fb:
					if (!board.checkIfOpposingPawnInFile(GenericFile.Fa, rank, onMoveWas) &&
					    !board.checkIfOpposingPawnInFile(GenericFile.Fb, rank, onMoveWas) &&
					    !board.checkIfOpposingPawnInFile(GenericFile.Fc, rank, onMoveWas)) {
						passedPawnBoost += PASSED_PAWN_BOOST;
					}
					break;
				case Fc:
					if (!board.checkIfOpposingPawnInFile(GenericFile.Fb, rank, onMoveWas) &&
						!board.checkIfOpposingPawnInFile(GenericFile.Fc, rank, onMoveWas) &&
					    !board.checkIfOpposingPawnInFile(GenericFile.Fd, rank, onMoveWas)) {
					    passedPawnBoost += PASSED_PAWN_BOOST;	
					}
					break;
				case Fd:
					if (!board.checkIfOpposingPawnInFile(GenericFile.Fc, rank, onMoveWas) &&
						!board.checkIfOpposingPawnInFile(GenericFile.Fd, rank, onMoveWas) &&
					    !board.checkIfOpposingPawnInFile(GenericFile.Fe, rank, onMoveWas)) {
						passedPawnBoost += PASSED_PAWN_BOOST;
					}
					break;
				case Fe:
					if (!board.checkIfOpposingPawnInFile(GenericFile.Fd, rank, onMoveWas) &&
						!board.checkIfOpposingPawnInFile(GenericFile.Fe, rank, onMoveWas) &&
						!board.checkIfOpposingPawnInFile(GenericFile.Ff, rank, onMoveWas)) {
						passedPawnBoost += PASSED_PAWN_BOOST;
					}
					break;
				case Ff:
					if (!board.checkIfOpposingPawnInFile(GenericFile.Fe, rank, onMoveWas) &&
						!board.checkIfOpposingPawnInFile(GenericFile.Ff, rank, onMoveWas) &&
					    !board.checkIfOpposingPawnInFile(GenericFile.Fg, rank, onMoveWas)) {
						passedPawnBoost += PASSED_PAWN_BOOST;
					}
					break;
				case Fg:
					if (!board.checkIfOpposingPawnInFile(GenericFile.Ff, rank, onMoveWas) &&
						!board.checkIfOpposingPawnInFile(GenericFile.Fg, rank, onMoveWas) &&
					    !board.checkIfOpposingPawnInFile(GenericFile.Fh, rank, onMoveWas)) {
						passedPawnBoost += PASSED_PAWN_BOOST;
					}
					break;
				case Fh:
					if (!board.checkIfOpposingPawnInFile(GenericFile.Fg, currPiece.getSquare().rank, onMoveWas)) {
						passedPawnBoost += ROOK_FILE_PASSED_PAWN_BOOST;
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
	
	int discourageDoubledPawns() {
		int doubledPawnScoreModifier = discourageDoubledPawnsForColour(pm.getOnMove());
		doubledPawnScoreModifier += discourageDoubledPawnsForColour(Colour.getOpposite(pm.getOnMove()));
		return doubledPawnScoreModifier;
	}

	private int discourageDoubledPawnsForColour(Colour onMoveWas) {
		Iterator<Piece> iter = pm.getTheBoard().iterateColour(onMoveWas);
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
			while (pawnCount[i] > 1) {
				pawnHandicap -= DOUBLED_PAWN_HANDICAP;
				pawnCount[i] -= 1;
			}
		}
		if (onMoveWas == Colour.black) {
			pawnHandicap = -pawnHandicap;
		}
		return pawnHandicap;
	}
}
