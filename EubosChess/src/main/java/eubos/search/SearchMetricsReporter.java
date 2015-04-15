package eubos.search;

import com.fluxchess.jcpi.commands.ProtocolInformationCommand;

import eubos.main.EubosEngineMain;

public class SearchMetricsReporter extends Thread {
	private boolean reporterActive;
	private SearchMetrics sm;
	private EubosEngineMain eubosEngine;
	private static final int UPDATE_RATE_MS = 500;
	
	public SearchMetricsReporter( EubosEngineMain eubos, SearchMetrics inputSm ) {
		sm = inputSm;
		reporterActive = true;
		eubosEngine = eubos;
	}
	
	public void run() {
		long timestampIntoWait = 0;
		long timestampOutOfWait = 0;
		do {
			int deltaTime = (int)(timestampOutOfWait - timestampIntoWait);
			sm.incrementTime(deltaTime);
			reportNodeData();
			timestampIntoWait = System.currentTimeMillis();
			try {
				synchronized (this) {
					this.wait(UPDATE_RATE_MS);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			timestampOutOfWait = System.currentTimeMillis();
		} while (reporterActive);
	}
	
	public void end() {
		reporterActive = false;
		synchronized (this) {
			this.notify();
		}
	}
	
	public void reportNodeData() {
		ProtocolInformationCommand info = new ProtocolInformationCommand();
		info.setNodes(sm.getNodesSearched());
		info.setNps(sm.getNodesPerSecond());
		info.setTime(sm.getTime());
		eubosEngine.sendInfoCommand(info);
	}
	
	public void reportPrincipalVariation() {
		ProtocolInformationCommand info = new ProtocolInformationCommand();
		info.setMoveList(sm.getPrincipalVariation());
		info.setTime(sm.getTime());
		int score = sm.getCpScore();
		int depth = sm.getDepth();
		if (java.lang.Math.abs(score)<300000) {
			info.setCentipawns(score);
		} else {
			int movesSearched = depth/2;
			int mateOnMoveXFromEndOfSearch = (java.lang.Math.abs(score)/300000)-1;
			int mateInX = movesSearched - mateOnMoveXFromEndOfSearch;
			if (score < 0)
				mateInX = -mateInX;
			info.setMate(mateInX);
		}
		info.setDepth(depth);
		eubosEngine.sendInfoCommand(info);
	}
	
	public void reportCurrentMove() {
		ProtocolInformationCommand info = new ProtocolInformationCommand();
		info.setCurrentMove(sm.getCurrentMove());
		info.setCurrentMoveNumber(sm.getCurrentMoveNumber());
		eubosEngine.sendInfoCommand(info);
	}
}