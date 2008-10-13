/* $Id$
 *
 * Created on Dec 12, 2005
 *
 * Copyright (C) 2005 Internet Archive.
 *  
 * This file is part of the Heritrix Cluster Controller (crawler.archive.org).
 *  
 * HCC is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 * 
 * Heritrix is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.hcc.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderJarFactory {
	public static String SETTINGS_DIRECTORY_PROPERTY= OrderJarFactory.class.getName() + ".settingsDefaultsDir";
    private static Logger log =
        Logger.getLogger(OrderJarFactory.class.getName());
    public static final String NAME_KEY = "name";
    public static final String OPERATOR_KEY = "operator";
    
    public static final String DURATION_KEY = "duration";
    public static final String TEST_CRAWL_KEY = "isTest";
    public static final String ONE_HOP_OFF_KEY = "oneHopOff";

    public static final String DOCUMENT_LIMIT_KEY = "documentLimitKey";
    public static final String USER_AGENT_KEY = "userAgent";
    public static final String FROM_EMAIL_KEY = "fromEmail";
    public static final String DISK_PATH_KEY = "diskPath";
    public static final String DESCRIPTION = "description";
    public static final String ORGANIZATION = "organization";
    
    public static final String SEEDS_KEY = "seeds";
    public static final String HOST_CONSTRAINTS_KEY = "hostConstraints";
    

    public OrderJarFactory() {
        super();
        // TODO Auto-generated constructor stub
    }

    public static File createOrderJar(Map parameters) {
        try {
        	
        	//read in order xml prototype to string buffer
            Map<String, InputStream> map = new HashMap<String, InputStream>();
            InputStream orderPrototype = OrderJarFactory.class
                    .getResourceAsStream("/order.xml");
            InputStreamReader reader = new InputStreamReader(orderPrototype);
            char[] cbuf = new char[1024];
            int read = -1;
            StringBuffer b = new StringBuffer();
            while ((read = reader.read(cbuf)) > -1) {
                b.append(cbuf, 0, read);
            }

            //replace values
            String order = b.toString();
            String date = new SimpleDateFormat("yyyyMMddhhmmss")
                    .format(new Date());

            order = order.replace("$name", parameters.get(NAME_KEY).toString());
            Object operator = parameters.get(OPERATOR_KEY);
            if(operator == null){
            	operator = "No Operator Specified";
            }
            order = order.replace("$operator", operator.toString());

            order = order.replace("$arcPrefix", parameters
                    .get(NAME_KEY)
                    .toString());
            order = order.replace("$date", date);
            Object isTest = parameters.get(TEST_CRAWL_KEY);
    
            boolean isTestFlag = isTest != null && new Boolean(isTest.toString()).booleanValue();
            order = order.replace("$writeEnabled", String.valueOf(!isTestFlag));
	        
            Object oneHopOff = parameters.get(ONE_HOP_OFF_KEY);
            
            order = order.replace(
            			"$oneHopOff", 
            			String.valueOf(
            					oneHopOff != null && new Boolean(oneHopOff.toString())));
	       
            
            order = order.replace("$date", date);

            int duration = 60*60*24*3;
            Object durationStr = parameters.get(DURATION_KEY);
            if(durationStr != null){
                duration = Integer.parseInt(durationStr.toString())/1000;
            }
            
            order = order.replace("$duration", duration +"");
            
            int documentLimit = 0;
            
            Object documentLimitStr = parameters.get(DOCUMENT_LIMIT_KEY);
            if(documentLimitStr != null){
                documentLimit = Integer.parseInt(documentLimitStr.toString());
            }
            order = order.replace("$documentLimit", documentLimit+"");

            order = order.replace("$userAgent", parameters.get(USER_AGENT_KEY).toString());
            order = order.replace("$fromEmail", parameters.get(FROM_EMAIL_KEY).toString());

            String diskPath = (String)parameters.get(DISK_PATH_KEY);
            if(diskPath == null){
            	diskPath = "";
            }
            
            order = order.replace("$diskPath", diskPath);

            String organization = (String)parameters.get(ORGANIZATION);
            
            if(organization == null){
            	organization = "";
            }
            
            order = order.replace("$organization", organization);

            String description = (String)parameters.get(DESCRIPTION);
            
            if(description == null){
            	description = "";
            }
            
            order = order.replace("$description", description);
            
            ByteArrayOutputStream orderFileOs = new ByteArrayOutputStream();
            orderFileOs.write(order.getBytes());
            map.put("order.xml", new ByteArrayInputStream(orderFileOs
                    .toByteArray()));
            orderFileOs.close();

            // write seeds
            ByteArrayOutputStream seedsOs = new ByteArrayOutputStream();
            int count = 0;
            Collection<String> seeds = (Collection<String>) parameters
                    .get(SEEDS_KEY);
            for (String seed : seeds) {
                if (count++ > 0) {
                    seedsOs.write("\n".getBytes());
                }
                seedsOs.write(seed.getBytes());

            }

            seedsOs.flush();
            map.put(
                    "seeds.txt",
                    new ByteArrayInputStream(seedsOs.toByteArray()));
            seedsOs.close();

            // write jar file.
            File jarFile = File.createTempFile("order", ".jar");
            JarOutputStream jos = new JarOutputStream(new FileOutputStream(
                    jarFile));
            byte[] buf = new byte[1024];
            

            //if a settings directory defaults has been specified,
            //add the contents of the settings directory defaults
            Properties p =null;
            try{
            	p = SmartPropertiesResolver.getProperties("hcc.properties");

            }catch(RuntimeException ex){
            	log.info("hcc.properties not found");
            }
            
            if(p != null){
                String defaultSettingsDirectoryRoot = p.getProperty(SETTINGS_DIRECTORY_PROPERTY);
                addFilesFromSettingsDirectory(map, defaultSettingsDirectoryRoot);
            }            
            
            
            //create a unique temp work directory for crawlSettings
            File tempCrawlSettingsDirectoryRoot = 
            	new File(System.getProperty("java.io.tmpdir") + File.separator + new Date().getTime());
            
            tempCrawlSettingsDirectoryRoot.deleteOnExit();
            //create hostConstraints hierarchy
            Map<String,InputStream> files = 
            	writeHostConstraints(
            			(List<HostConstraint>)parameters.get(HOST_CONSTRAINTS_KEY),
            			tempCrawlSettingsDirectoryRoot);
            
            //add files to map - any name clashes with the defaults will be overwritten by
            //the user specified constrants.
            map.putAll(files);


            
            // for each map entry
            for (String filename : map.keySet()) {
                // Add ZIP entry to output stream.
                jos.putNextEntry(new JarEntry(filename));

                // Transfer bytes from the file to the jar file
                InputStream in = map.get(filename);

                int len;
                while ((len = in.read(buf)) > 0) {
                    jos.write(buf, 0, len);
                }

                // Complete the entry
                jos.closeEntry();
                in.close();
            }

            jos.close();
            
            if (log.isLoggable(Level.FINE)) {
                log.fine("created jar file" + jarFile.getAbsolutePath());
            }

            return jarFile;

        } catch (Exception e) {
            if (log.isLoggable(Level.SEVERE)) {
                log.severe(e.getMessage());
            }

            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    
    protected static Map<String, InputStream> writeHostConstraints(List<HostConstraint> hostConstraints, File crawlSettingsDirectoryRoot) throws IOException{
    		
    	Map<String, InputStream> files = new HashMap<String, InputStream>();
    	if(hostConstraints != null){
    		//for each constraint
    		for(HostConstraint hc : hostConstraints){
    			//put filename path into list.
    			File file = writeSettingsFile(hc, crawlSettingsDirectoryRoot);
    			files.put(hc.getSettingsFilePath(), new FileInputStream(file));
    			
    			
    		}
    	}
    	
    	return files;
    
    }
    
    protected static File writeSettingsFile(HostConstraint hc, File crawlSettingsDirectoryRoot) throws IOException{
		//create directory hierarchy if doesn't exist
		File directory = new File(crawlSettingsDirectoryRoot, hc.getSettingsFileDirectory());
		directory.mkdirs();
		File file = File.createTempFile("order", "xml", directory);
		file.deleteOnExit();
		//write order file
		FileWriter w = new FileWriter(file);
		
		w.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		w.append("<crawl-settings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"heritrix_settings.xsd\">");
		/*
		w.append("<meta>");
		w.append("<name></name>");
		w.append("<description></description>");
		w.append("<operator>Admin</operator>");
		w.append("<audience></audience>");
		w.append("<organization></organization>");
		w.append("<date></date>");
		w.append("</meta>");
		*/
		if(hc.getIgnoreRobots() != null && hc.getIgnoreRobots()){
			w.append("<object name=\"robots-honoring-policy\"><string name=\"type\">ignore</string></object>");
		}
		
		if(hc.getBlock()  != null && hc.getBlock()){
			/*w.append("<object name=\"scope\"><boolean name=\"enabled\">false</boolean></object>");*/
            w.append("<object name=\"Preselector\"><boolean name=\"block-all\">true</boolean></object>");
		}
		
		if(hc.getRegex() != null){
			w.append("<object name=\"rejectIfRegexMatch\">" +
                    "<string name=\"regexp\">"+ hc.getRegex() +"</string>" +
                     "</object>");
		}
		
		Long docLimit = hc.getDocumentLimit();
		if(docLimit != null && docLimit > 0){	
			int errorPenalty = docLimit.intValue()/100;
			if(errorPenalty == 0){
				errorPenalty = 1;
			}
		   w.append("<object name=\"frontier\">");
		   w.append("<integer name=\"error-penalty-amount\">"+errorPenalty+"</integer>"); 
		   w.append("<long name=\"queue-total-budget\">"+docLimit+"</long>"); 
		   w.append("</object>");
		}  
			  
		w.append("</crawl-settings>");
			
		w.close();

		return file;
    }
    
    protected static void addFilesFromSettingsDirectory(Map<String,InputStream> files, String settingsDirectoryRoot) throws IOException{
        if(settingsDirectoryRoot != null){
            File settingsDirectory = new File(settingsDirectoryRoot);
            if(settingsDirectory.exists()){
            	log.info("Settings directory parameter specified: " + settingsDirectoryRoot);
                recursivelyAddChildren(files, settingsDirectory, null);
            }else{
            	log.warning("Settings directory parameter points to a non-existent directory: " + settingsDirectoryRoot);
            }
        }else{
        	log.info("Settings directory property is null: no settings directory specified.");
        }
    }
    
    protected static void recursivelyAddChildren(
    						Map<String,InputStream> files, 
    						File settingDirectoryRoot, 
    						String path) throws IOException{
		File file;
		if(path == null){
			file = settingDirectoryRoot;
		}else{
			file = new File(path);
		}
		if(file.exists()){
			if(file.isFile()){
				FileInputStream fis = new FileInputStream(file);
				String filePath = settingDirectoryRoot.getCanonicalPath();
				
				String key = file.getCanonicalPath().replace(filePath + File.separator, "");
				log.fine("adding file to settings map: " + file.getCanonicalPath() + " keyed as " + key);
				files.put(key, fis);
			}else{
				String[] children = file.list();
				if(children != null){
					for(String child : children){
						recursivelyAddChildren(files, settingDirectoryRoot, file.getCanonicalPath() + File.separator + child);
					}
				}
			}
		}
	}
}