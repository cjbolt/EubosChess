package eubos.search;

import java.util.ArrayList;
import java.util.List;

import com.fluxchess.jcpi.commands.ProtocolInformationCommand;

import eubos.main.EubosEngineMain;
import eubos.search.transposition.ITranspositionAccessor;

public class SearchMetricsReporter extends Thread {
	
	private volatile boolean sendInfo = false;
	private volatile boolean reporterActive;
	private List<SearchMetrics> sm_list;
	private EubosEngineMain eubosEngine;
	private ITranspositionAccessor tt;
	private ReferenceScore refScore;
	private static final int UPDATE_RATE_MS = 1000;
	
	private int lastScore = 0;
	private int lastDepth = 0;
	
	public SearchMetricsReporter() {
		reporterActive = false;
		this.setName("DummySearchMetricsReporter");
	}
	
	public SearchMetricsReporter(EubosEngineMain eubos, ITranspositionAccessor tt, ReferenceScore refScore) {
		reporterActive = true;
		eubosEngine = eubos;
		this.tt = tt;
		this.refScore = refScore;
		sm_list = new ArrayList<SearchMetrics>(EubosEngineMain.DEFAULT_NUM_SEARCH_THREADS);
		lastScore = refScore.getReference().score;
		lastDepth = 0; //Math.max(0, refScore.getReference().depth-5);
		this.setName("SearchMetricsReporter");
	}
	
	public void register(SearchMetrics registering_sm) {
		sm_list.add(registering_sm);
	}
	
	public void setSendInfo(boolean enable) {
		sendInfo = enable;		
	}
	
	public void run() {
		do {
			try {
				synchronized (this) {
					wait(UPDATE_RATE_MS);
				}
			} catch (InterruptedException e) {
				reporterActive = false;
				break;
			}
			if (reporterActive) {
				reportNodeData();
			}
		} while (reporterActive);
	}
	
	public void end() {
		reporterActive = false;
		synchronized (this) {
			this.notify();
		}
	}
	
	void reportPrincipalVariation(SearchMetrics sm) {
		if (sendInfo && sm != null) {
			boolean sendPv = false;
			// Doesn't need to be synchronised as only written by PlySearcher thread (i.e this thread)
			int currDepth = sm.getDepth();
			int score = sm.getCpScore();
			synchronized (this) {
				if (currDepth > lastDepth || (currDepth == lastDepth && score > lastScore)) {
					lastDepth = currDepth;
					lastScore = score;
					sendPv = true;
				}
			}
			if (sendPv) {
				ProtocolInformationCommand info = new ProtocolInformationCommand();
				// Doesn't need to be synchronised as only written by PlySearcher thread (i.e this thread)
				generatePvInfoCommand(info, sm);
				eubosEngine.sendInfoCommand(info);
			}
		}
	}
	
	synchronized void resetAfterWindowingFail() {
		lastScore = Score.PROVISIONAL_ALPHA;
	}
	
	private void reportNodeData() {
		if (sendInfo && !sm_list.isEmpty()) {
			ProtocolInformationCommand info = new ProtocolInformationCommand();
			generatePeriodicInfoCommand(info); 
			eubosEngine.sendInfoCommand(info);
		}
	}
	
	private void generatePeriodicInfoCommand(ProtocolInformationCommand info) {
		long time = 0;
		long nodes = 0;
		int nps = 0;
		
		for (SearchMetrics thread : sm_list) {
			thread.incrementTime();
			nodes += thread.getNodesSearched();
			nps += thread.getNodesPerSecond();
			time = thread.getTime();
		}
		
		info.setNodes(nodes);
		info.setNps(nps);
		info.setTime(time);
		info.setHash(tt.getHashUtilisation());
		//if (EubosEngineMain.ENABLE_TT_DIAGNOSTIC_LOGGING) {
		//	info.setString(tt.getDiagnostics());
		//}
	}
	
	private void generatePvInfoCommand(ProtocolInformationCommand info, SearchMetrics pv) {
		pv.incrementTime();

		if (pv.pvValid) {
			info.setMoveList(pv.getPrincipalVariation());
		}
		
		short score = pv.getCpScore();
		if (refScore != null) {
			refScore.update(score, (byte)pv.getDepth());
		}
		if (Score.isMate(score)) {
			int matePly = (score > 0) ? Score.PROVISIONAL_BETA - score + 1 : Score.PROVISIONAL_ALPHA - score;
			int mateMove = matePly / 2;
			info.setMate(mateMove);
		} else {
			info.setCentipawns(score);
		}
		
		info.setDepth(pv.getDepth());
		info.setMaxDepth(pv.getPartialDepth());
		
		int nps = 0;
		long nodes = 0;
		for (SearchMetrics thread : sm_list) {
			// collate NPS from all registered SMs
			nps += thread.getNodesPerSecond();
			nodes += thread.getNodesSearched();
		}
		info.setNodes(nodes);
		info.setNps(nps);
		info.setHash(tt.getHashUtilisation());
		info.setTime(pv.getTime());
	}

	public void reportCurrentMove() {
		if (sendInfo) {
			if (sm_list.size() == 1) {
				// The current move being searched is only meaningful for single threaded search
				SearchMetrics sm = sm_list.get(0);
				if (sm.getTime() > 1000) {
					ProtocolInformationCommand info = new ProtocolInformationCommand();
					info.setCurrentMove(sm.getCurrentMove());
					info.setCurrentMoveNumber(sm.getCurrentMoveNum());
					eubosEngine.sendInfoCommand(info);
				}
			}
		}
	}
}
