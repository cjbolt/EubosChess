package eubos.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.Piece.Colour;
import eubos.position.IPositionAccessors;
import eubos.position.Move;

public class SearchMetrics {
	
	public static final boolean SINGLE_MOVE_PV = true;
	private IPositionAccessors pos;
	private AtomicLong nodesSearched;
	private long time;
	private List<Integer> pv;
	public boolean pvValid = false;
	private short cpScore;
	private int depth;
	private int partialDepth;
	private long initialTimestamp;
	private int moveNum;
	private GenericMove move;
	boolean isScoreBackedUpFromSearch = false;
	
	public SearchMetrics(int searchDepth, IPositionAccessors pos) {
		nodesSearched = new AtomicLong(0);
		time = 0;
		cpScore = 0;
		isScoreBackedUpFromSearch = false;
		pvValid = false;
		depth = searchDepth;
		partialDepth = 0;
		initialTimestamp = System.currentTimeMillis();
		this.pos = pos;
		moveNum = 0;
		move = null;
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
	
	public synchronized void setPrincipalVariation(int [] pc, int length_pc) {
		if (pc != null && length_pc != 0) {
			pvValid = true;
			pv = Arrays.stream(pc).boxed().collect(Collectors.toList());
			pv = pv.subList(0, length_pc);
		} else {
			pvValid = false;
		}
	}
	
	synchronized List<GenericMove> getPrincipalVariation() {
		List<GenericMove> thePv = null;
		if (pvValid) {
			if (SINGLE_MOVE_PV) {
				thePv = new ArrayList<GenericMove>(1);
				thePv.add(Move.toGenericMove(pv.get(0)));
			} else {
				thePv = new ArrayList<GenericMove>(pv.size());
				for (int move : this.pv) {
					if (move != Move.NULL_MOVE) {
						thePv.add(Move.toGenericMove(move));
					}
				}
			}
		}
		return thePv;
	}
	
	synchronized void setPrincipalVariationData(int extendedSearchDeepestPly, int[] pc, int pc_length, short positionScore) {
		setPartialDepth(extendedSearchDeepestPly);
		setPrincipalVariation(pc, pc_length);
		setCpScore(positionScore);
		isScoreBackedUpFromSearch = true;
	}
	
	synchronized void setPrincipalVariationDataFromHash(int extendedSearchDeepestPly, short positionScore) {
		setCpScore(positionScore);
		this.cpScore = positionScore;
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

	public int getCurrentMoveNum() {
		return moveNum;
	}
	public void setCurrentMove(GenericMove move, int moveNumber) {
		moveNum = moveNumber;
		this.move = move;
	}
	public GenericMove getCurrentMove() {
		return move;
	}

	public boolean isScoreBackedUpFromSearch() {
		return isScoreBackedUpFromSearch;
	}
}
