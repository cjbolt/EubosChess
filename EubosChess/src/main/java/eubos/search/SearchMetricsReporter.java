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
		while (reporterActive) {
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
			int deltaTime = (int)(timestampOutOfWait - timestampIntoWait);
			ProtocolInformationCommand info = new ProtocolInformationCommand();
			sm.incrementTime(deltaTime);
			info.setCurrentMove(sm.getCurrentMove());
			info.setCurrentMoveNumber(sm.getCurrentMoveNumber());
			info.setNodes(sm.getNodesSearched());
			info.setNps(sm.getNodesPerSecond());
			info.setMoveList(sm.getPrincipalVariation());
			info.setTime(sm.getTime());
			info.setCentipawns(sm.getCpScore());
			info.setDepth(sm.getDepth());
			eubosEngine.sendInfoCommand(info);
		}
	}
	
	public void end() {
		reporterActive = false;
		synchronized (this) {
			this.notify();
		}
	}
}
