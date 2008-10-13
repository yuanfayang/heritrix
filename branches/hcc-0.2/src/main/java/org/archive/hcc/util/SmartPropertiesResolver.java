package org.archive.hcc.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SmartPropertiesResolver {
	private static final Logger log = Logger.getLogger(SmartPropertiesResolver.class.getName());

	public static Properties getProperties(String filePath) {
		if(filePath == null){
			throw new NullPointerException("filePath must be non null");
		}
		

		//first try to resolve by system property
		
		InputStream is = null;
		
		if(filePath != null){
			File f = new File(filePath);
			
			if(!f.exists()){
				String fileName = filePath.substring(filePath.lastIndexOf(File.separator) +1);
				f = new File( 
					System.getProperty("user.home") + 
						File.separator + 
						fileName);

				if(!f.exists()){
					f = new File(
							System.getProperty("user.dir") + 
								File.separator + 
								fileName);
					
				}
			}			
			
			try{
				if(!f.exists()){
					is = SmartPropertiesResolver.class.getResourceAsStream(filePath);
					if(is == null){
						throw new RuntimeException(new FileNotFoundException("cannot find resource: " + filePath));
					}
				}else{
					is = new FileInputStream(f);
				}

				Properties p = new Properties();
				p.load(is);
				
				if(log.isLoggable(Level.FINE)){
					log.fine("loaded properties(" + filePath + ").");
				}
				return p;

			}catch(Exception e){

				if(log.isLoggable(Level.SEVERE)){
					log.severe("Failed to load property file: " + (f.exists()?f.getAbsolutePath():filePath) + "; message=" + e.getMessage());
				}
				throw new RuntimeException(e);

			}
		}

		if(log.isLoggable(Level.SEVERE))
			log.severe("Neither File (" + filePath + ")  not found anywhere.");

		throw new RuntimeException(new FileNotFoundException(filePath));
			
		
	}
}
