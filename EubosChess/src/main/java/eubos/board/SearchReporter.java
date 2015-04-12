package eubos.board;

import com.fluxchess.jcpi.commands.ProtocolInformationCommand;

import eubos.main.EubosEngineMain;

public class SearchReporter extends Thread {
	private boolean reporterActive;
	private SearchMetrics sm;
	private EubosEngineMain eubosEngine;
	
	public SearchReporter( EubosEngineMain eubos, SearchMetrics inputSm ) {
		sm = inputSm;
		reporterActive = true;
		eubosEngine = eubos;
	}
	
	public void run() {
		while (reporterActive) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ProtocolInformationCommand info = new ProtocolInformationCommand();
			sm.incrementTime(500);
			info.setNodes(sm.getNodesSearched());
			info.setNps(sm.getNodesPerSecond());
			info.setMoveList(sm.getPrincipalVariation());
			info.setTime(sm.getTime());
			info.setCentipawns(sm.getCpScore());
			info.setDepth(sm.getDepth());
			eubosEngine.dispatchInfoMessage(info);
		}
	}
	
	public void end() {
		reporterActive = false;
	}
}
