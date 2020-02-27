package eubos.search;

import java.util.ArrayList;
import java.util.List;

import com.fluxchess.jcpi.commands.ProtocolInformationCommand;
import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.Move;

public class SearchMetrics {
	private long nodesSearched;
	private long time;
	private short hashFull;
	private List<Integer> pv;
	private boolean pvValid = false;
	private short cpScore;
	private int depth;
	private int partialDepth;
	private int currMove;
	private int currMoveNum;
	
	public SearchMetrics(int searchDepth) {
		nodesSearched = 0;
		time = 0;
		cpScore = 0;
		pvValid = false;
		depth = searchDepth;
		partialDepth = 0;
		currMoveNum = 0;
		hashFull = 0;
	}

	public SearchMetrics() {
		this(1);
	}
	
	public synchronized void setPeriodicInfoCommand(ProtocolInformationCommand info, int deltaTime) {
		incrementTime(deltaTime);
		info.setNodes(getNodesSearched());
		info.setNps(getNodesPerSecond());
		info.setTime(getTime());
		info.setHash(getHashFull());
	}
	
	synchronized void incrementNodesSearched() { nodesSearched++; }
	long getNodesSearched() { return nodesSearched; }
	
	void incrementTime(int delta) { time += delta; }
	long getTime() { return time; }
	
	int getNodesPerSecond() {
		int nps = 0;
		if (time != 0) {
			nps = (int)(nodesSearched*1000/time);
		}
		return nps;
	}
	
	public synchronized void setPrincipalVariation(List<Integer> pc) {
		if (!pc.isEmpty()) {
			pvValid = true;
			pv = pc;
		}
	}
	synchronized List<GenericMove> getPrincipalVariation() {
		List<GenericMove> thePv = null;
		if (pvValid) {
			thePv = new ArrayList<GenericMove>();
			for (int move : this.pv) {
				thePv.add(Move.toGenericMove(move));
			}
		}
		return thePv;
	}
	
	public synchronized short getCpScore() { return cpScore; }
	synchronized void setCpScore(short cpScore) { this.cpScore = cpScore; }
	
	synchronized int getDepth() { return depth; }
	public synchronized void setDepth(int depth) { this.depth = depth; }
	
	synchronized int getPartialDepth() { return partialDepth; }
	synchronized void setPartialDepth(int depth ) { this.partialDepth = depth; }
	
	synchronized void setCurrentMove(int mov) { currMove = mov;}
	synchronized GenericMove getCurrentMove() { return Move.toGenericMove(currMove); }
	
	synchronized int getCurrentMoveNumber() { return currMoveNum; }
	public synchronized void clearCurrentMoveNumber() { currMoveNum = 0; }
	synchronized void incrementCurrentMoveNumber() { currMoveNum+=1; }
	
	short getHashFull() { return hashFull;	}
	public synchronized void setHashFull(short hashFull) { this.hashFull = hashFull; }
}
