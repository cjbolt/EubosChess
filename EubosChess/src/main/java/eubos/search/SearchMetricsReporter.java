package eubos.search;

import com.fluxchess.jcpi.commands.ProtocolInformationCommand;

import eubos.board.pieces.King;
import eubos.main.EubosEngineMain;

class SearchMetricsReporter extends Thread {
	
	private boolean sendInfo = false;
	private boolean reporterActive;
	private SearchMetrics sm;
	private EubosEngineMain eubosEngine;
	private static final int UPDATE_RATE_MS = 500;
	
	SearchMetricsReporter( EubosEngineMain eubos, SearchMetrics inputSm ) {
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
	
	void reportNodeData() {
		ProtocolInformationCommand info = new ProtocolInformationCommand();
		info.setNodes(sm.getNodesSearched());
		info.setNps(sm.getNodesPerSecond());
		info.setTime(sm.getTime());
		eubosEngine.sendInfoCommand(info);
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
				int movesSearched = depth/2;
				int mateOnMoveXFromEndOfSearch = (java.lang.Math.abs(score)/King.MATERIAL_VALUE)-1;
				int mateInX = movesSearched - mateOnMoveXFromEndOfSearch;
				if (score < 0)
					mateInX = -mateInX;
				info.setMate(mateInX);
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
