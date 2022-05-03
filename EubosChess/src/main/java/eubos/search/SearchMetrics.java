package eubos.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.Piece.Colour;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import it.unimi.dsi.fastutil.ints.IntArrays;

public class SearchMetrics {
	
	public static final boolean ENABLE_SINGLE_MOVE_PV = false;
	
	private IPositionAccessors pos;
	private AtomicLong nodesSearched;
	private long time;
	private int[] pv;
	public boolean pvValid = false;
	private short cpScore;
	private int depth;
	private int partialDepth;
	private long initialTimestamp;
	private int moveNum;
	private int move;
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
		move = Move.NULL_MOVE;
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
			pv = IntArrays.trim(pc, length_pc);
		} else {
			pvValid = false;
		}
	}
	
	synchronized List<GenericMove> getPrincipalVariation() {
		List<GenericMove> thePv = null;
		if (pvValid) {
			if (ENABLE_SINGLE_MOVE_PV) {
				thePv = new ArrayList<GenericMove>(1);
				thePv.add(Move.toGenericMove(pv[0]));
			} else {
				// Need to convert from internal move representation to a generic list of moves for the UCI package API
				thePv = new ArrayList<GenericMove>(pv.length);
				for (int move : pv) {
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
	public void setCurrentMove(int move, int moveNumber) {
		moveNum = moveNumber;
		this.move = move;
	}
	public GenericMove getCurrentMove() {
		return Move.toGenericMove(move);
	}

	public boolean isScoreBackedUpFromSearch() {
		return isScoreBackedUpFromSearch;
	}
}
