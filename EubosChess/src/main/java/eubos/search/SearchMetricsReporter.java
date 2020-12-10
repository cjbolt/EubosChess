package eubos.search;

import com.fluxchess.jcpi.commands.ProtocolInformationCommand;

import eubos.main.EubosEngineMain;

public class SearchMetricsReporter extends Thread {
	
	private boolean sendInfo = false;
	private volatile boolean reporterActive;
	private SearchMetrics sm;
	private EubosEngineMain eubosEngine;
	private static final int UPDATE_RATE_MS = 1000;
	
	public SearchMetricsReporter( EubosEngineMain eubos ) {
		reporterActive = true;
		eubosEngine = eubos;
		sm = null;
		this.setName("SearchMetricsReporter");
	}
	
	public void register(SearchMetrics inputSm) {
		sm = inputSm;
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
		if (sendInfo && sm != null) {
			ProtocolInformationCommand info = new ProtocolInformationCommand();
			sm.setPeriodicInfoCommand(info); 
			eubosEngine.sendInfoCommand(info);
		}
	}
	
	void reportPrincipalVariation() {
		if (sendInfo && sm != null) {
			ProtocolInformationCommand info = new ProtocolInformationCommand();
			sm.setPvInfoCommand(info);
			eubosEngine.sendInfoCommand(info);
		}
	}

	public void setSendInfo(boolean enable) {
		sendInfo = enable;		
	}
}
