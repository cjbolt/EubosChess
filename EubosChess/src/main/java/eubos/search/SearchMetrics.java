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
	private AtomicLong time;
	private int[] pv;
	public volatile boolean pvValid = false;
	private volatile short cpScore;
	private volatile int depth;
	private volatile int partialDepth;
	private long initialTimestamp;
	private int moveNum;
	private int move;
	boolean isScoreBackedUpFromSearch = false;
	
	public SearchMetrics(int searchDepth, IPositionAccessors pos) {
		nodesSearched = new AtomicLong(0);
		time = new AtomicLong(0);
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
	
	void incrementNodesSearched() { nodesSearched.incrementAndGet(); }
	long getNodesSearched() { return nodesSearched.get(); }
	
	void incrementTime() {
		long currentTimestamp = System.currentTimeMillis();
		time.set(currentTimestamp - initialTimestamp);
	}
	long getTime() { return time.get(); }
	
	int getNodesPerSecond() {
		int nps = 0;
		long time_copy = time.get();
		if (time_copy != 0) {
			nps = (int)(nodesSearched.get()*1000/time_copy);
		}
		return nps;
	}
	
	public void setPrincipalVariation(int [] pc, int length_pc) {
		if (pc != null && length_pc != 0) {
			pvValid = true;
			pv = IntArrays.trim(pc, length_pc);
		} else {
			pvValid = false;
		}
	}
	
	List<GenericMove> getPrincipalVariation() {
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
	
	void setPrincipalVariationData(int extendedSearchDeepestPly, int[] pc, int pc_length, short positionScore) {
		setPartialDepth(extendedSearchDeepestPly);
		setPrincipalVariation(pc, pc_length);
		setCpScore(positionScore);
		isScoreBackedUpFromSearch = true;
	}
	
	void setPrincipalVariationDataFromHash(int extendedSearchDeepestPly, short positionScore) {
		setCpScore(positionScore);
		this.cpScore = positionScore;
	}
	
	public short getCpScore() { return cpScore; }
	void setCpScore(short positionScore) { 
		if (Colour.isBlack(pos.getOnMove())) {
			if (Score.isMate(positionScore)) {
				positionScore += 1; // out by one error due to negation of mate scores?
			}
		}
		this.cpScore = positionScore;
	}
	
	public int getDepth() { return depth; }
	public void setDepth(int depth) { this.depth = depth; }
	
	int getPartialDepth() { return partialDepth; }
	void setPartialDepth(int depth ) { this.partialDepth = depth; }

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
