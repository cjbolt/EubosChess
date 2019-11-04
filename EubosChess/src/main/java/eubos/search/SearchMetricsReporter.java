package eubos.search;

import com.fluxchess.jcpi.commands.ProtocolInformationCommand;

import eubos.board.pieces.King;
import eubos.main.EubosEngineMain;

class SearchMetricsReporter extends Thread {
	
	private boolean sendInfo = false;
	private volatile boolean reporterActive;
	private SearchMetrics sm;
	private EubosEngineMain eubosEngine;
	private static final int UPDATE_RATE_MS = 500;
	
	SearchMetricsReporter( EubosEngineMain eubos, SearchMetrics inputSm ) {
		sm = inputSm;
		reporterActive = true;
		eubosEngine = eubos;
		this.setName("SearchMetricsReporter");
	}
	
	public void run() {
		long timestampIntoWait = 0;
		long timestampOutOfWait = 0;
		do {
			timestampIntoWait = System.currentTimeMillis();
			try {
				synchronized (this) {
					wait(UPDATE_RATE_MS);
				}
			} catch (InterruptedException e) {
				reporterActive = false;
			}
			timestampOutOfWait = System.currentTimeMillis();
			sm.incrementTime((int)(timestampOutOfWait - timestampIntoWait));
			reportNodeData();
		} while (reporterActive);
	}
	
	public void end() {
		reporterActive = false;
		synchronized (this) {
			this.notify();
		}
	}
	
	void reportNodeData() {
		ProtocolInformationCommand info = new ProtocolInformationCommand();
		info.setNodes(sm.getNodesSearched());
		info.setNps(sm.getNodesPerSecond());
		info.setTime(sm.getTime());
		info.setHash(sm.getHashFull());
		if (info.getTime() > 10) { 
			eubosEngine.sendInfoCommand(info);
		}
	}
	
	void reportPrincipalVariation() {
		if (sendInfo) {
			ProtocolInformationCommand info = new ProtocolInformationCommand();
			info.setMoveList(sm.getPrincipalVariation());
			info.setTime(sm.getTime());
			int score = sm.getCpScore();
			int depth = sm.getDepth();
			if (java.lang.Math.abs(score)<King.MATERIAL_VALUE) {
				info.setCentipawns(score);
			} else {
				int mateMove = 0;
				if (score > 0) {
					mateMove = Short.MAX_VALUE - score;
				} else {
					mateMove = Short.MIN_VALUE - score;
				}
				info.setMate(mateMove);
			}
			info.setDepth(depth);
			eubosEngine.sendInfoCommand(info);
		}
	}
	
	void reportCurrentMove() {
		if (sendInfo) {
			ProtocolInformationCommand info = new ProtocolInformationCommand();
			info.setCurrentMove(sm.getCurrentMove());
			info.setCurrentMoveNumber(sm.getCurrentMoveNumber());
			eubosEngine.sendInfoCommand(info);
		}
	}

	public void setSendInfo(boolean enable) {
		sendInfo = enable;		
	}
}
