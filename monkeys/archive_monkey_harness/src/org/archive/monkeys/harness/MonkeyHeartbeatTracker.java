package org.archive.monkeys.harness;

import java.io.IOException;

/**
 * The heartbeat tracker is responsible for making sure that the browser process
 * is not hung or crashed. The tracker will start a browser instance and then check every
 * specified amount of time that the monkey plugin has pinged the harness servlet. If not,
 * the browser will be restarted.
 * @author Eugene Vahlis
 */
public class MonkeyHeartbeatTracker extends Thread {
	private static final int HEART_BEAT_LENGTH = 20000;

	private static final String BROWSER_COMMAND = "/usr/lib/firefox/firefox -P dev";

	private boolean pinged;

	private Process browserProc;

	/**
	 * Notifies the tracker that the browser is still alive.
	 */
	public void ping() {
		synchronized (this) {
			this.pinged = true;
		}
	}

	@Override
	/**
	 * The main method of the thread. Check every HEART_BEAT_LENGTH milliseconds
	 * that the tracker was pinged. Otherwise, restarts the browser.
	 */
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

	/**
	 * Makes sure that a browser is running and alive by checking
	 * the ping status and restarting if necessary.
	 * @throws Exception If the restart fails.
	 */
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

	/**
	 * Kills the browser process.
	 * @throws InterruptedException If the tracker thread is interrupted while killing
	 * the browser process.
	 */
	private void killBrowser() throws InterruptedException {
		this.browserProc.destroy();
		this.browserProc.waitFor();
	}

	/**
	 * Starts a browser process.
	 */
	private void startBrowserProc() throws IOException {
		this.browserProc = Runtime.getRuntime().exec(BROWSER_COMMAND);
		this.pinged = false;
	}

}
