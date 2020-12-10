package eubos.search;

import java.util.ArrayList;
import java.util.List;

import com.fluxchess.jcpi.commands.ProtocolInformationCommand;

import eubos.main.EubosEngineMain;

public class SearchMetricsReporter extends Thread {
	
	private boolean sendInfo = false;
	private volatile boolean reporterActive;
	private List<SearchMetrics> sm;
	private EubosEngineMain eubosEngine;
	private static final int UPDATE_RATE_MS = 1000;
	public static final int MAX_THREADS = 4;
	
	private int lastScore = 0;
	private int lastDepth = 0;
	
	public SearchMetricsReporter( EubosEngineMain eubos ) {
		reporterActive = true;
		eubosEngine = eubos;
		sm = new ArrayList<SearchMetrics>(MAX_THREADS);
		this.setName("SearchMetricsReporter");
	}
	
	public void register(SearchMetrics registering_sm) {
		sm.add(registering_sm);
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
	
	public void reportNodeData() {
		if (sendInfo && !sm.isEmpty()) {
			ProtocolInformationCommand info = new ProtocolInformationCommand();
			generatePeriodicInfoCommand(info); 
			eubosEngine.sendInfoCommand(info);
		}
	}
	
	void reportPrincipalVariation(SearchMetrics sm) {
		if (sendInfo && sm != null) {
			int currDepth = sm.getDepth();
			int score = sm.getCpScore();
			if (currDepth > lastDepth || (currDepth == lastDepth && score > lastScore)) {
				ProtocolInformationCommand info = new ProtocolInformationCommand();
				generatePvInfoCommand(info, sm);
				eubosEngine.sendInfoCommand(info);
				lastDepth = currDepth;
				lastScore = score;
			}
		}
	}

	public void setSendInfo(boolean enable) {
		sendInfo = enable;		
	}
	
	public synchronized void generatePeriodicInfoCommand(ProtocolInformationCommand info) {
		long time = 0;
		long nodes = 0;
		int nps = 0;
		int hashFull = 0;
		
		for (SearchMetrics thread : sm) {
			thread.incrementTime();
			nodes += thread.getNodesSearched();
			nps += thread.getNodesPerSecond();
			time = thread.getTime();
			hashFull = thread.getHashFull();
		}
		
		info.setNodes(nodes);
		info.setNps(nps);
		info.setTime(time);
		info.setHash(hashFull);
	}
	
	public void generatePvInfoCommand(ProtocolInformationCommand info, SearchMetrics sm) {
		sm.incrementTime();
		info.setNodes(sm.getNodesSearched());
		info.setHash(sm.getHashFull());
		if (sm.pvValid) {
			info.setMoveList(sm.getPrincipalVariation());
		}
		short score = sm.getCpScore();
		int depth = sm.getDepth();
		if (Score.isMate(score)) {
			int matePly = (score > 0) ? Short.MAX_VALUE - score + 1 : Short.MIN_VALUE - score;
			int mateMove = matePly / 2;
			info.setMate(mateMove);
		} else {
			info.setCentipawns(score);
		}
		info.setDepth(depth);
		info.setMaxDepth(sm.getPartialDepth());
		info.setNps(sm.getNodesPerSecond());
		info.setTime(sm.getTime());
	}
}
