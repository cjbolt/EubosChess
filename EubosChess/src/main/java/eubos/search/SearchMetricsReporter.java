package eubos.search;

import com.fluxchess.jcpi.commands.ProtocolInformationCommand;

import eubos.main.EubosEngineMain;

public class SearchMetricsReporter extends Thread {
	
	private boolean sendInfo = false;
	private volatile boolean reporterActive;
	private SearchMetrics sm;
	private EubosEngineMain eubosEngine;
	private static final int UPDATE_RATE_MS = 500;
	
	public SearchMetricsReporter( EubosEngineMain eubos, SearchMetrics inputSm ) {
		sm = inputSm;
		reporterActive = true;
		eubosEngine = eubos;
		this.setName("SearchMetricsReporter");
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
		if (sendInfo) {
			ProtocolInformationCommand info = new ProtocolInformationCommand();
			sm.setPeriodicInfoCommand(info); 
			eubosEngine.sendInfoCommand(info);
		}
	}
	
	void reportPrincipalVariation() {
		if (sendInfo) {
			ProtocolInformationCommand info = new ProtocolInformationCommand();
			sm.setPvInfoCommand(info);
			eubosEngine.sendInfoCommand(info);
		}
	}

	public void setSendInfo(boolean enable) {
		sendInfo = enable;		
	}
}
