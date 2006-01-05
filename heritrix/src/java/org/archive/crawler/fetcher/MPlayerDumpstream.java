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
	
	int exitVal = -1;
	Process proc = null;
	
	public MPlayerDumpstream() {
		super();
	}
	
	public int dumpstream(CrawlURI curi, String streamFilePath) {
		try {
			Runtime rt = Runtime.getRuntime();
			
			System.out.println ("Fetching " + curi);
				
			proc = rt.exec("\"C:\\Documents and Settings\\Nico\\Desktop\\mplayer\\mplayer.exe\" -really-quiet -dumpstream -dumpfile " + streamFilePath + " " + curi);
				
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
				// if capture not finished after media length + 5 minutes (300s)
				if (time > curi.getInt("TIME") + 300) {
					proc.destroy();
					exitVal = 1; // TIMEOUT
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
