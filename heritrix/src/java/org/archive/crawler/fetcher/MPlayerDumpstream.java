package org.archive.crawler.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;

public class MPlayerDumpstream {
	
	private static final Logger LOGGER =
		Logger.getLogger(MPlayerIdentify.class.getName());
	
	static int TOLERANCE = 300; // time tolerance: 5 minutes (300s)
	String os = System.getProperty("org.archive.crawler.fetcher.MPlayerDumpstream.os", "LINUX");
	
	int exitVal = -1;
	int margin; // time margin added before timeout
	Process proc = null;
	
	public MPlayerDumpstream() {
		super();
	}
	
	public int dumpstream(CrawlURI curi, String streamFilePath) {
		try {
			// in case of LIVE stream
			// cannot wait for exitVal
			// hence no need for tolerance
			if ( curi.getString("TYPE").equals("live") ) {
				margin = 0;
			}
			else {
				margin = TOLERANCE;
			}
			Runtime rt = Runtime.getRuntime();
			
			System.out.println ("Fetching " + curi);
				
			if ( os.equals("LINUX") ) {
				proc = rt.exec("mplayer -vo null -ao null -identify -cache-min 0 -frames 0 \'" + curi + "\'");
			}
			else {
				proc = rt.exec("\"C:\\Documents and Settings\\Nico\\Desktop\\mplayer\\mplayer.exe\"" +
						"-vo null -ao null -identify -cache-min 0 -frames 0 \"" + curi + "\"");
			}
			StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR");
			StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT");
			// kick them off
			errorGobbler.start();
			outputGobbler.start();
		
			int time = 0;
			// Wait for stream length TIME
			while (true) {
				// wait delay
				Thread.sleep(30000);
				// if capture not finished after media length + tolerance
				if (time > curi.getInt("TIME") + margin) {
					proc.destroy();
					if (margin == 0){
						exitVal = 0; // OK
					}
					else {
						exitVal = 1; // TIMEOUT	
					}
					break;
				}
				try {
					exitVal = proc.exitValue();
				} catch (IllegalThreadStateException e) {
					time += 30;
					continue;
				}
				break;
			}
			//exitVal = proc.waitFor();
			
			proc = null;
			System.out.println("Process exitValue: " + exitVal);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		finally {
			if (proc != null)
				proc.destroy();
		}
		return exitVal;
	}

	class StreamGobbler extends Thread {
    
		InputStream is;
		String type;
		  
		StreamGobbler(InputStream is, String type) 
		{
			this.is = is;
			this.type = type;
		}
    
		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null)
					LOGGER.info(type + ">" + line); 
			}
			catch (IOException ioe) {
				ioe.printStackTrace();  
			}
        }
    }
}
