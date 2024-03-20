package eubos.score;

import eubos.position.IPositionAccessors;
import eubos.search.Score;
import eubos.search.transposition.FixedSizeTranspositionTable;
import eubos.search.transposition.Transposition;

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
	
	private void invalidateInAnalysisMode() {
		if (lastScoreIsValid && (rootPosition.getMoveNumber() != (lastMoveNumber + 1))) {
			lastScoreIsValid = false;
			lastScore = 0;
			lastMoveNumber = 0;
			lastDepth = 0;
		}
	}
	
	public void setAtStartOfSearch(IPositionAccessors rootPos) {
		this.rootPosition = rootPos;
		invalidateInAnalysisMode();
		
		long trans = hashMap.getTransposition(rootPos.getHash());		
		if (trans != 0L /*&& Transposition.getType(trans) == Score.upperBound*/) {
			// Set reference score from previous Transposition table, if it exists 
			reference.origin = Transposition.report(trans, rootPosition.getTheBoard());
			reference.score = Transposition.getScore(trans);
			reference.depth = Transposition.getDepthSearchedInPly(trans);
		} else if (lastScoreIsValid) {
			// Use the last reported score (from previous Search) as the reference score
			reference.origin = String.format("set from last score because %s", 
					(trans != 0L) ? Transposition.report(trans, rootPosition.getTheBoard()) : "trans is null");
			reference.score = lastScore;
			reference.depth = (byte)(lastDepth - Score.PLIES_PER_MOVE);
		} else {
			// Back off to a static evaluation to work out initial score
			reference.origin = "set from static eval";
			reference.score = (short) rootPos.getPositionEvaluator().getFullEvaluation();
			reference.depth = 0;
		}
		
		// Invalidate the last score, this will be re-validated when the first UCI PV message of search is received
		lastScoreIsValid = false;
	}
	
	public Reference getReference() {
		return reference;
	}
	
	public void update(short uciScore, byte depth) {
		lastScoreIsValid = true; 
		lastScore = uciScore;
	    lastMoveNumber = (rootPosition == null) ? 0 : rootPosition.getMoveNumber();
	    lastDepth = depth;
	}
}
