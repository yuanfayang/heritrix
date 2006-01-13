package org.archive.crawler.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.crawler.datamodel.CrawlURI;

public class MPlayerIdentify {
	
	private static final Logger LOGGER =
		Logger.getLogger(MPlayerIdentify.class.getName());

	// MEDIA METADATA
	static final String MIME_TYPE = "(?:Stream\\smimetype:\\s*(.+))";
    // (G1) STREAM MEDIA MIMETYPE
	static final String NAME = "(?:\\sname:\\s(.+))";
    // (G1) STREAM MEDIA NAME
	static final String AUTHOR = "(?:\\sauthor:\\s(.+))";
    // (G1) STREAM MEDIA AUTHOR
	static final String COPYRIGHT = "(?:\\scopyright:\\s(.+))";
    // (G1) STREAM MEDIA COPYRIGHT
	static final String COMMENTS = "(?:\\scomments:\\s(.+))";
    // (G1) STREAM MEDIA COMMENTS
	static final String DESCRIPTION = "(?:Stream\\sdescription:\\s(.+))";
    // (G1) STREAM MEDIA DESCRIPTION
	static final String LENGTH = "(?:ID_LENGTH=(.+))";
	// (G1) STREAM MEDIA LENGTH
	
	// VIDEO PROPERTIES
	static final String VIDEO_FORMAT = "(?:ID_VIDEO_FORMAT=(.+))";
	// (G1) STREAM VIDEO FORMAT
	static final String VIDEO_WIDTH = "(?:ID_VIDEO_WIDTH=(.+))";
    // (G1) STREAM VIDEO WEIGTH
	static final String VIDEO_HEIGHT = "(?:ID_VIDEO_HEIGHT=(.+))";
    // (G1) STREAM VIDEO HEIGHT
	static final String VIDEO_FPS = "(?:ID_VIDEO_FPS=(.+))";
    // (G1) STREAM VIDEO FRAMES PER SECOND
	
	// AUDIO PROPERTIES
	static final String AUDIO_CODEC = "(?:ID_AUDIO_CODEC=(.+))";
    // (G1) STREAM AUDIO CODEC
	static final String AUDIO_FORMAT = "(?:ID_AUDIO_FORMAT=(.+))";
	// (G1) STREAM AUDIO FORMAT
	static final String AUDIO_BITRATE = "(?:ID_AUDIO_BITRATE=(.+))";
    // (G1) STREAM AUDIO BITRATE
	static final String AUDIO_RATE = "(?:ID_AUDIO_RATE=(.+))";
    // (G1) STREAM AUDIO SAMPLE RATE
	static final String AUDIO_NCH = "(?:ID_AUDIO_NCH=(.+))";
    // (G1) STREAM NUMBER OF CHANNEL
	
	// MPLAYER OUTPUT
	static final String START = "Starting\\splayback\\.\\.\\.";
	static final String EXIT = "Exiting\\.\\.\\.\\s\\(End\\sof\\sfile\\)";
	
	static final int LIVE_TIME = 120;
	String os = System.getProperty("org.archive.crawler.fetcher.MPlayerIdentify.os", "LINUX");
		
	Process proc = null;
	volatile int exit = -1;
	volatile int status = 2;
	
	String mime_type;
	String name;
	String author;
	String copyright;
	String comments;
	String description;
	String audio_codec;
	String audio_format;
	String video_format;
	int audio_bitrate;
	int audio_rate;
	int audio_nch;
	int video_width = 0;
	int video_height = 0;
	float video_fps = 0;	
	int length;
	
	Matcher matLen;
	Matcher matMim;
	Matcher matCod;
	Matcher matBit;
	Matcher matWid;
	Matcher matHei;
	Matcher matFPS;
	Matcher matNCH;
	Matcher matRat;
	Matcher matNam;
	Matcher matAut;
	Matcher matCop;
	Matcher matCom;
	Matcher matDes;
	Matcher matAF;
	Matcher matVF;
	Pattern patLen = Pattern.compile(LENGTH);
	Pattern patMim = Pattern.compile(MIME_TYPE);
	Pattern patCod = Pattern.compile(AUDIO_CODEC);
	Pattern patBit = Pattern.compile(AUDIO_BITRATE);
	Pattern patWid = Pattern.compile(VIDEO_WIDTH);
	Pattern patHei = Pattern.compile(VIDEO_HEIGHT);
	Pattern patFPS = Pattern.compile(VIDEO_FPS);
	Pattern patNCH = Pattern.compile(AUDIO_NCH);
	Pattern patRat = Pattern.compile(AUDIO_RATE);
	Pattern patNam = Pattern.compile(NAME);
	Pattern patAut = Pattern.compile(AUTHOR);
	Pattern patCop = Pattern.compile(COPYRIGHT);
	Pattern patCom = Pattern.compile(COMMENTS);
	Pattern patDes = Pattern.compile(DESCRIPTION);
	Pattern patAF  = Pattern.compile(AUDIO_FORMAT);
	Pattern patVF  = Pattern.compile(VIDEO_FORMAT);
	
	public MPlayerIdentify() {
		super();
	}
	
	public int identify(CrawlURI curi) {
		try {
			Runtime rt = Runtime.getRuntime();
			
			System.out.println ("Identifying " + curi);
			
			if ( os.equals("LINUX") ) {
				proc = rt.exec("mplayer -vo null -ao null -identify -cache-min 0 -frames 0 \'" + curi + "\'");
			}
			else {
				proc = rt.exec("\"C:\\Documents and Settings\\Nico\\Desktop\\mplayer\\mplayer.exe\"" +
						"-vo null -ao null -identify -cache-min 0 -frames 0 \"" + curi + "\"");
			}
			StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR");
			StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT");

			errorGobbler.start();
			outputGobbler.start();
		
			int time = 0;
			// Wait for stream length TIME
			while (true) {
				// wait delay: 1 sec
				Thread.sleep(1000);
				// if no response after 2 min
				if (time > 120) {
					proc.destroy();
					status = 1; // TIMEOUT
					break;
				}
				if (exit == 0) {
					break;
				} 
				else {
					time++;
				}
			}
			//exitVal = proc.waitFor();
			
			proc = null;
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		finally {
			if (proc != null)
				proc.destroy();
		}
		
		if (status == 0) {
			// in case of LIVE stream (ID_LENGTH=0)
			// set length to 2min (120sec)
			if (length == 0) {
				length = LIVE_TIME;
				curi.putString("TYPE", "live");
			}
			else {
				curi.putString("TYPE", "on-demand");
			}
			curi.putInt("TIME", length);
			if (mime_type == null && audio_codec == "ffwmav2") {
				mime_type = "audio/x-ms-wma";
			}
			curi.setContentType(mime_type);
		}
		System.out.println("status: " + status);
		System.out.println("mime-type: " + mime_type);
		System.out.println("length: " + length);
		System.out.println("audio codec: " + audio_codec);
		System.out.println("audio format: " + audio_format);
		System.out.println("audio bitrate: " + audio_bitrate);
		System.out.println("sample rate: " + audio_rate);
		System.out.println("number of channel: " + audio_nch);
		System.out.println("video format: " + video_format);
		System.out.println("video size: " + video_width + " x " + video_height);
		System.out.println("frames per second: " + video_fps);
		System.out.println("name: " + name);
		System.out.println("author: " + author);
		System.out.println("copyright: " + copyright);
		System.out.println("comments: " + comments);
		System.out.println("description: " + description);
		
		return status;
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
				
				while ((line = br.readLine()) != null) {
					
					matLen = patLen.matcher(line);
					matMim = patMim.matcher(line);
					matCod = patCod.matcher(line);
					matBit = patBit.matcher(line);
					matWid = patWid.matcher(line);
					matHei = patHei.matcher(line);
					matFPS = patFPS.matcher(line);
					matNCH = patNCH.matcher(line);
					matRat = patRat.matcher(line);
					matNam = patNam.matcher(line);
					matAut = patAut.matcher(line);
					matCop = patCop.matcher(line);
					matCom = patCom.matcher(line);
					matDes = patDes.matcher(line);
					matAF  = patAF.matcher(line);
					matVF  = patVF.matcher(line);
					
					if (type == "OUTPUT") {
						if (matLen.matches()) {
							length = Integer.parseInt(matLen.group(1));
						}
						if (matMim.matches()) {
							mime_type = matMim.group(1);
						}
						if (matCod.matches()) {
							audio_codec = matCod.group(1);
						}
						if (matBit.matches()) {
							audio_bitrate = Integer.parseInt(matBit.group(1));
						}
						if (matWid.matches()) {
							video_width = Integer.parseInt(matWid.group(1));
						}
						if (matHei.matches()) {
							video_height = Integer.parseInt(matHei.group(1));
						}
						if (matFPS.matches()) {
							video_fps = Float.parseFloat(matFPS.group(1));
						}
						if (matNCH.matches()) {
							audio_nch = Integer.parseInt(matNCH.group(1));
						}
						if (matRat.matches()) {
							audio_rate = Integer.parseInt(matRat.group(1));
						}
						if (matNam.matches()) {
							name = matNam.group(1);
						}
						if (matAut.matches()) {
							author = matAut.group(1);
						}
						if (matCop.matches()) {
							copyright = matCop.group(1);
						}
						if (matCom.matches()) {
							comments = matCom.group(1);
						}
						if (matDes.matches()) {
							description = matDes.group(1);
						}
						if (matAF.matches()) {
							audio_format = matAF.group(1);
						}
						if (matVF.matches()) {
							video_format = matVF.group(1);
						}
						if (Pattern.matches(START, line)) {
							status = 0;
						}
						if (Pattern.matches(EXIT, line)) {
							exit = 0;
						}
					}
					matLen.reset();
					matMim.reset();
					matCod.reset();
					matBit.reset();
					matWid.reset();
					matHei.reset();
					matFPS.reset();
					matNCH.reset();
					matRat.reset();
					matNam.reset();
					matAut.reset();
					matCop.reset();
					matCom.reset();
					matDes.reset();
					matAF.reset();
					matVF.reset();
					
					LOGGER.info(type + ">" + line); 
				}
			}
			catch (IOException ioe) {
				ioe.printStackTrace();  
			}
        }
    }
}
