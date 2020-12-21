package eubos.search.transposition;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.main.EubosEngineMain;
import eubos.position.Move;

public class PrincipalVariationTransposition extends Transposition {

	private List<Integer> pv;
	
	public static final boolean TRUNCATION_OF_PV_ENABLED = false;

	public PrincipalVariationTransposition(byte depth, short score, byte scoreType, GenericMove bestMove) {
		// Only used by tests
		this(depth, score, scoreType, Move.toMove(bestMove, null, Move.TYPE_REGULAR_NONE), null);
	}
	
	public PrincipalVariationTransposition(byte depth, short score, byte scoreType, int bestMove, List<Integer> pv) {
		super(depth, score, scoreType, bestMove, pv);
		setPv(pv);
	}
	
	@Override
	public List<Integer> getPv() {
		return pv;
	}
	
	protected void truncateOnwardPvToSearchDepth(List<Integer> pv) {
		if (TRUNCATION_OF_PV_ENABLED) {
			if (pv != null && pv.size() > depthSearchedInPly) {
				pv.subList(depthSearchedInPly, pv.size()).clear();
			}
		}
	}

	public void setPv(List<Integer> pv) {
		if (EubosEngineMain.UCI_INFO_ENABLED) {
			truncateOnwardPvToSearchDepth(pv);
			this.pv = pv;
		}
	}
	
	@Override
	public String report() {
		String onward_pv = "";
		if (EubosEngineMain.UCI_INFO_ENABLED) {
			if (pv != null) {
				for (int move : pv) {
					onward_pv += String.format("%s, ", Move.toString(move));
				}
			}
		}
		String output = String.format("trans best=%s, dep=%d, sc=%d, type=%s, pv=%s", 
				Move.toString(bestMove),
				depthSearchedInPly,
				score,
				type,
				onward_pv);
		return output;
	}
	
	@Override
	public synchronized boolean checkUpdate(
			byte new_Depth, 
			short new_score,  
			byte new_bound,
			int new_bestMove, 
			List<Integer> pv) {
		boolean updateTransposition = false;	
		if (updateTransposition = super.checkUpdate(new_Depth, new_score, new_bound, new_bestMove, pv)) {
			// order is important because setPv uses depth
			setPv(pv);
		}
		return updateTransposition;
	}
}
