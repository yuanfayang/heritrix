package org.archive.monkeys.harness;

import java.io.IOException;

public class MonkeyHeartbeatTracker extends Thread {
	private static final int HEART_BEAT_LENGTH = 20000;

	private static final String BROWSER_COMMAND = "/usr/lib/firefox/firefox -P dev";

	private boolean pinged;

	private Process browserProc;

	public void ping() {
		synchronized (this) {
			this.pinged = true;
		}
	}

	@Override
	public void run() {
		try {
			startBrowserProc();
			while (!interrupted()) {
				sleep(HEART_BEAT_LENGTH);
				validateOrRestart();
			}
		} catch (InterruptedException e) {
			// it's ok
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				killBrowser();
			} catch (InterruptedException e) {
				System.err.println("Why am I interrupted again?");
				e.printStackTrace();
			}
		}
	}

	private void validateOrRestart() throws Exception {
		synchronized (this) {
			if (!pinged) {
				killBrowser();
				startBrowserProc();
			} else {
				this.pinged = false;
			}
		}
	}

	private void killBrowser() throws InterruptedException {
		this.browserProc.destroy();
		this.browserProc.waitFor();
	}

	private void startBrowserProc() throws IOException {
		this.browserProc = Runtime.getRuntime().exec(BROWSER_COMMAND);
		this.pinged = false;
	}

}
