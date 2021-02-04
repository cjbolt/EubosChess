package eubos.score;

import eubos.board.Piece.Colour;
import eubos.position.IPositionAccessors;
import eubos.search.Score;
import eubos.search.transposition.FixedSizeTranspositionTable;
import eubos.search.transposition.ITransposition;

public class ReferenceScore {
	
	boolean lastScoreIsValid = false;
	short lastScore = 0;
	int lastMoveNumber = 0;
	byte lastDepth = 0;
	
	short staticEvalOfRootPosition = 0;
	short referenceScore = 0;
	public String referenceScoreSetFrom = "None";
	byte referenceScoreDepth = 0;
	
	IPositionAccessors rootPosition;
	FixedSizeTranspositionTable hashMap;
	
	public ReferenceScore(FixedSizeTranspositionTable hashMap) {
		this.hashMap = hashMap;
	}
	
	public void updateAtNewRootPosition(IPositionAccessors rootPos) {
		this.rootPosition = rootPos;
		staticEvalOfRootPosition = Score.getScore(rootPos.getPositionEvaluator().evaluatePosition());
		
		ITransposition trans = hashMap.getTransposition(rootPos.getHash());		
		if (trans != null && trans.getType() == Score.exact) {
			// Set reference score from previous Transposition table, if an exact entry exists 
			referenceScoreSetFrom = trans.report();
			referenceScore = trans.getScore();
			referenceScoreDepth = trans.getDepthSearchedInPly();
		} else if (lastScoreIsValid) {
			// Use the last score as an estimate of the initial score
			referenceScoreSetFrom = "set from last score";
			referenceScore = lastScore;
			referenceScoreDepth = (byte)(lastDepth - 2);
		} else {
			// Back off to a static evaluation to work out initial score
			referenceScoreSetFrom = "set from static eval";
			referenceScore = staticEvalOfRootPosition;
			referenceScoreDepth = 1;
		}
		
		// Convert to UCI scores
		if (Colour.isBlack(rootPos.getOnMove())) {
			referenceScore = (short)-referenceScore;
			staticEvalOfRootPosition = (short)-staticEvalOfRootPosition;
		}
	}
	
	public void checkLastScoreValidity(IPositionAccessors pos) {
		if (pos.getMoveNumber() != (lastMoveNumber + 1)) {
			// This check is for when running from Arena in analyse mode, for example
			lastScoreIsValid = false;
			lastScore = 0;
			lastMoveNumber = 0;
			lastDepth = 0;
		}
	}

	public short getReferenceUciScore() {
		lastScoreIsValid = false; // invalidate, will be validated by next UCI PV message
		return referenceScore;
	}
	
	public byte getReferenceDepth() {
		lastScoreIsValid = false; // invalidate, will be validated by next UCI PV message
		return referenceScoreDepth;
	}
	
	public short getStaticEvalOfRootPositionAsUciScore() {
		lastScoreIsValid = false; // invalidate, will be validated by next UCI PV message
		return staticEvalOfRootPosition;
	}
	
	public void updateLastScore(short uciScore, byte depth) {
		if (rootPosition != null) {
			// Convert to a Eubos score from UCI
			short eubosScore = Colour.isWhite(rootPosition.getOnMove()) ? uciScore : (short)-uciScore;
			lastScoreIsValid = true; 
			lastScore = eubosScore;
		    lastMoveNumber = rootPosition.getMoveNumber();
		    lastDepth = depth;
		}
	}
}
