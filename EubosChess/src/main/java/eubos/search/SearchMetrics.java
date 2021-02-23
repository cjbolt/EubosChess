package eubos.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.Piece.Colour;
import eubos.position.IPositionAccessors;
import eubos.position.Move;

public class SearchMetrics {
	private IPositionAccessors pos;
	private AtomicLong nodesSearched;
	private long time;
	private List<Integer> pv;
	public boolean pvValid = false;
	private short cpScore;
	private int depth;
	private int partialDepth;
	private long initialTimestamp;
	
	public SearchMetrics(int searchDepth, IPositionAccessors pos) {
		nodesSearched = new AtomicLong(0);
		time = 0;
		cpScore = 0;
		pvValid = false;
		depth = searchDepth;
		partialDepth = 0;
		initialTimestamp = System.currentTimeMillis();
		this.pos = pos;
	}

	public SearchMetrics(IPositionAccessors pos) {
		this(1, pos);
	}
	
	synchronized void incrementNodesSearched() { nodesSearched.incrementAndGet(); }
	synchronized long getNodesSearched() { return nodesSearched.get(); }
	
	synchronized void incrementTime() {
		long currentTimestamp = System.currentTimeMillis();
		time = currentTimestamp - initialTimestamp;
	}
	synchronized long getTime() { return time; }
	
	synchronized int getNodesPerSecond() {
		int nps = 0;
		if (time != 0) {
			nps = (int)(nodesSearched.get()*1000/time);
		}
		return nps;
	}
	
	public synchronized void setPrincipalVariation(List<Integer> pc) {
		if (!pc.isEmpty()) {
			pvValid = true;
			pv = new ArrayList<Integer>(pc);
		} else {
			pvValid = false;
		}
	}
	
	synchronized List<GenericMove> getPrincipalVariation() {
		List<GenericMove> thePv = null;
		if (pvValid) {
			thePv = new ArrayList<GenericMove>(pv.size());
			for (int move : this.pv) {
				if (move != Move.NULL_MOVE) {
					thePv.add(Move.toGenericMove(move));
				}
			}
		}
		return thePv;
	}
	
	synchronized void setPrincipalVariationData(int extendedSearchDeepestPly, List<Integer> pc, short positionScore) {
		setPartialDepth(extendedSearchDeepestPly);
		setPrincipalVariation(pc);
		setCpScore(positionScore);
	}
	
	public synchronized short getCpScore() { return cpScore; }
	synchronized void setCpScore(short positionScore) { 
		if (Colour.isBlack(pos.getOnMove())) {
			if (Score.isMate(positionScore)) {
				positionScore += 1; // out by one error due to negation of mate scores?
			}
		}
		this.cpScore = positionScore;
	}
	
	public synchronized int getDepth() { return depth; }
	public synchronized void setDepth(int depth) { this.depth = depth; }
	
	synchronized int getPartialDepth() { return partialDepth; }
	synchronized void setPartialDepth(int depth ) { this.partialDepth = depth; }
}
