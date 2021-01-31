package eubos.search;

import java.util.ArrayList;
import java.util.List;

import com.fluxchess.jcpi.commands.ProtocolInformationCommand;

import eubos.main.EubosEngineMain;
import eubos.search.transposition.FixedSizeTranspositionTable;

public class SearchMetricsReporter extends Thread {
	
	private boolean sendInfo = false;
	private volatile boolean reporterActive;
	private List<SearchMetrics> sm;
	private EubosEngineMain eubosEngine;
	private FixedSizeTranspositionTable tt;
	private static final int UPDATE_RATE_MS = 1000;
	
	private int lastScore = 0;
	private int lastDepth = 0;
	
	public SearchMetricsReporter( EubosEngineMain eubos, FixedSizeTranspositionTable tt ) {
		reporterActive = true;
		eubosEngine = eubos;
		this.tt = tt;
		sm = new ArrayList<SearchMetrics>(EubosEngineMain.DEFAULT_NUM_SEARCH_THREADS);
		lastScore = 0;
		lastDepth = 0;
		this.setName("SearchMetricsReporter");
	}
	
	public synchronized void register(SearchMetrics registering_sm) {
		sm.add(registering_sm);
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
	
	synchronized void reportPrincipalVariation(SearchMetrics pv) {
		if (sendInfo && pv != null) {
			int currDepth = pv.getDepth();
			int score = pv.getCpScore();
			if (currDepth > lastDepth || (currDepth == lastDepth && score > lastScore)) {
				ProtocolInformationCommand info = new ProtocolInformationCommand();
				generatePvInfoCommand(info, pv);
				eubosEngine.sendInfoCommand(info);
				lastDepth = currDepth;
				lastScore = score;
			}
		}
	}
	
	private void reportNodeData() {
		if (sendInfo && !sm.isEmpty()) {
			ProtocolInformationCommand info = new ProtocolInformationCommand();
			generatePeriodicInfoCommand(info); 
			eubosEngine.sendInfoCommand(info);
		}
	}
	
	private void generatePeriodicInfoCommand(ProtocolInformationCommand info) {
		long time = 0;
		long nodes = 0;
		int nps = 0;
		
		for (SearchMetrics thread : sm) {
			thread.incrementTime();
			nodes += thread.getNodesSearched();
			nps += thread.getNodesPerSecond();
			time = thread.getTime();
		}
		
		info.setNodes(nodes);
		info.setNps(nps);
		info.setTime(time);
		info.setHash(tt.getHashUtilisation());
	}
	
	private void generatePvInfoCommand(ProtocolInformationCommand info, SearchMetrics pv) {
		pv.incrementTime();

		if (pv.pvValid) {
			info.setMoveList(pv.getPrincipalVariation());
		}
		
		short score = pv.getCpScore();
		eubosEngine.setLastScore(score);
		if (Score.isMate(score)) {
			int matePly = (score > 0) ? Short.MAX_VALUE - score + 1 : Short.MIN_VALUE - score;
			int mateMove = matePly / 2;
			info.setMate(mateMove);
		} else {
			info.setCentipawns(score);
		}
		
		info.setDepth(pv.getDepth());
		info.setMaxDepth(pv.getPartialDepth());
		
		int nps = 0;
		long nodes = 0;
		for (SearchMetrics thread : sm) {
			// collate NPS from all registered SMs
			nps += thread.getNodesPerSecond();
			nodes += thread.getNodesSearched();
		}
		info.setNodes(nodes);
		info.setNps(nps);
		
		info.setTime(pv.getTime());
	}
}
