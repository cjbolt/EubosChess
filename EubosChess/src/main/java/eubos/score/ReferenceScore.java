package eubos.score;

import eubos.board.Piece.Colour;
import eubos.position.IPositionAccessors;
import eubos.search.Score;
import eubos.search.transposition.FixedSizeTranspositionTable;
import eubos.search.transposition.ITransposition;

public class ReferenceScore {
	public class Reference {
		public short score = 0;
		public String origin = "None";
		public byte depth = 0;
	}
	private Reference reference;
	
	private boolean lastScoreIsValid = false;
	private short lastScore = 0;
	private int lastMoveNumber = 0;
	private byte lastDepth = 0;
	
	private IPositionAccessors rootPosition;
	private FixedSizeTranspositionTable hashMap;
	
	public ReferenceScore(FixedSizeTranspositionTable hashMap) {
		this.hashMap = hashMap;
		this.reference = new Reference();
	}
	
	public void updateReference(IPositionAccessors rootPos) {
		this.rootPosition = rootPos;
		checkLastScoreValidity();
		
		ITransposition trans = hashMap.getTransposition(rootPos.getHash());		
		if (trans != null && trans.getType() == Score.exact) {
			// Set reference score from previous Transposition table, if an exact entry exists 
			reference.origin = trans.report();
			reference.score = trans.getScore();
			reference.depth = trans.getDepthSearchedInPly();
		} else if (lastScoreIsValid) {
			// Use the last reported score (from previous Search) as the reference score
			reference.origin = "set from last score";
			reference.score = lastScore;
			reference.depth = (byte)(lastDepth - MateScoreGenerator.PLIES_PER_MOVE);
		} else {
			// Back off to a static evaluation to work out initial score
			reference.origin = "set from static eval";
			reference.score = Score.getScore(rootPos.getPositionEvaluator().evaluatePosition());
			reference.depth = 0;
		}
		
		// Convert to UCI score
		if (Colour.isBlack(rootPos.getOnMove())) {
			reference.score = (short)-reference.score;
		}
		
		// Invalidate the last score, this will be re-validated when the first UCI PV message of search is received
		lastScoreIsValid = false;
	}
	
	private void checkLastScoreValidity() {
		if (lastScoreIsValid && (rootPosition.getMoveNumber() != (lastMoveNumber + 1))) {
			// This check is for when running from Arena in analyse mode, for example
			lastScoreIsValid = false;
			lastScore = 0;
			lastMoveNumber = 0;
			lastDepth = 0;
		}
	}
	
	public Reference getReference() {
		return reference;
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

	public void updateLastScore(ITransposition trans) {
		lastScoreIsValid = true;
		lastScore = trans.getScore();
	    lastDepth = trans.getDepthSearchedInPly();
	    lastMoveNumber = rootPosition.getMoveNumber();
	}
}
