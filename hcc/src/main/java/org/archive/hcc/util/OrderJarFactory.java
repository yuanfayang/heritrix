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
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.hcc.Config;

public class OrderJarFactory {
	public static String SETTINGS_DIRECTORY_PROPERTY= OrderJarFactory.class.getName() + ".settingsDefaultsDir";
	private static Log log = LogFactory.getLog(OrderJarFactory.class);
    private String name;
    private String description;
    private String userAgent;
    private String fromEmail;
    private String organization;
    private Long durationLimit = 60*60*24*3l;
    private Long documentLimit = 0l;
    private List<String> seeds;
    private List<HostConstraint> hostConstraints = null;
    private String diskPath = null;
    private boolean test = false;
    private boolean oneHopOff = false;
    private String operator = null;
    private String persistSource = null;
	private long dataLimit = 0l;
    
    public OrderJarFactory(String name, String userAgent, String fromEmail, String description, String organization, List<String> seeds) {
    	assert name != null;
    	assert userAgent != null;
    	assert fromEmail != null;
    	assert description != null;
    	assert organization != null;
    	assert seeds != null;
    	assert seeds.size() > 0;
    	
    	this.name = name;
    	this.description = description;
    	this.userAgent = userAgent;
    	this.fromEmail = fromEmail;
    	this.organization = organization;
    	this.seeds = seeds;
    }

	public void setPersistSource(String persistSource){
		this.persistSource = persistSource;
	}
	
    private String readOrderFileContentsIntoString() throws IOException{
    	//read in order xml prototype to string buffer
        InputStream orderPrototype = OrderJarFactory.class
                .getResourceAsStream("/order.xml");
        InputStreamReader reader = new InputStreamReader(orderPrototype);
        char[] cbuf = new char[1024];
        int read = -1;
        StringBuffer b = new StringBuffer();
        while ((read = reader.read(cbuf)) > -1) {
            b.append(cbuf, 0, read);
        }
        return b.toString();
        
    }
    
    private String buildOrderFileContents() throws IOException{
    	String order = readOrderFileContentsIntoString();
        
    	order = replaceKey(order, "$name", name);
    	order = replaceKey(order, "$arcPrefix", name);
    	order = replaceKey(order, "$date", new SimpleDateFormat("yyyyMMddhhmmss").format(new Date()));
    	order = replaceKey(order, "$operator", operator, "No Operator Specified");
        order = replaceKey(order, "$writeEnabled", !isTest());
        order = replaceKey(order, "$oneHopOff", isOneHopOff());
        order = replaceKey(order, "$duration", new Long(this.durationLimit)); //order file is in seconds.
        order = replaceKey(order, "$documentLimit", documentLimit);
        order = replaceKey(order, "$dataLimit", dataLimit);
        order = replaceKey(order, "$userAgent", userAgent);
        order = replaceKey(order, "$fromEmail", fromEmail);
        order = replaceKey(order, "$diskPath", diskPath, "");
        order = replaceKey(order, "$organization", organization);
        order = replaceKey(order, "$description", description);
        order = replaceKey(order, "$persistSource", persistSource);

        return order;
    }

    private String replaceKey(String string, String key, Object value){
    	return replaceKey(string, key, value, "");
    }
    
    private String replaceKey(String string, String key, Object value, Object defaultValue){
    	return string.replace(key, value != null?value.toString():defaultValue.toString());
    }
    
    private InputStream createInputStreamFromString(String s) throws IOException{
        ByteArrayOutputStream orderFileOs = new ByteArrayOutputStream();
        orderFileOs.write(s.getBytes());
        InputStream is = new ByteArrayInputStream(orderFileOs.toByteArray());
        orderFileOs.close();
        return is;
    }
    
    private InputStream createSeedsInputStream(Collection<String> seeds) throws IOException{
        // write seeds
        StringBuilder b = new StringBuilder();
        for (String seed : seeds) {
            b.append(seed);
        	b.append("\n");
        }
        return createInputStreamFromString(b.toString());
    }
    
    private Map<String, InputStream> prepareInputStreamMap() throws IOException {
    	String order = buildOrderFileContents();
        Map<String, InputStream> map = new HashMap<String, InputStream>();
        map.put("order.xml", createInputStreamFromString(order));
        map.put("seeds.txt", createSeedsInputStream(seeds));

        //if a settings directory defaults has been specified,
        //add the contents of the settings directory defaults
        addFilesFromSettingsDirectory(map, Config.instance().getDefaultSettingsDirectory());
        
        //create a unique temp work directory for crawlSettings
        File tempCrawlSettingsDirectoryRoot = 
        	new File(System.getProperty("java.io.tmpdir") + File.separator + new Date().getTime());
        
        tempCrawlSettingsDirectoryRoot.deleteOnExit();
        //create hostConstraints hierarchy
        Map<String,InputStream> files = writeHostConstraints(hostConstraints,tempCrawlSettingsDirectoryRoot);
        
        //add files to map - any name clashes with the defaults will be overwritten by
        //the user specified constrants.
        map.putAll(files);
        return map;
    }
    
    public byte[] createOrderJar() {
        try {
        	Map<String,InputStream> map = prepareInputStreamMap();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            JarOutputStream jos = new JarOutputStream(bos);
            byte[] buf = new byte[1024];            
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

            return bos.toByteArray();
        } catch (Exception e) {
        	log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
    
	public File createOrderJarFile() throws IOException {
		File jarFile = File.createTempFile("order", ".jar");
		FileOutputStream fos = new FileOutputStream(jarFile);
		fos.write(createOrderJar());
		return jarFile;
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
            	log.info("Adding files from defaultSettingsDirectory: " + settingsDirectoryRoot);
                recursivelyAddChildren(files, settingsDirectory, null);
            }else{
            	log.warn("defaultSettingsDirectory points to a non-existent directory: " + settingsDirectoryRoot);
            }
        }else{
        	log.info("defaultSettingsDirectory not specified in hcc-config.xml");
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
				log.debug("adding file to settings map: " + file.getCanonicalPath() + " keyed as " + key);
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

	public Long getDurationLimit() {
		return durationLimit;
	}

	public void setDurationLimit(Long durationLimit) {
		this.durationLimit = durationLimit;
	}

	public Long getDocumentLimit() {
		return documentLimit;
	}

	public void setDocumentLimit(Long documentLimit) {
		this.documentLimit = documentLimit;
	}


	public List<HostConstraint> getHostConstraints() {
		return hostConstraints;
	}

	public void setHostConstraints(List<HostConstraint> hostConstraints) {
		this.hostConstraints = hostConstraints;
	}

	public String getDiskPath() {
		return diskPath;
	}

	public void setDiskPath(String diskPath) {
		this.diskPath = diskPath;
	}

	public boolean isTest() {
		return test;
	}

	public void setTest(boolean test) {
		this.test = test;
	}

	public boolean isOneHopOff() {
		return oneHopOff;
	}

	public void setOneHopOff(boolean oneHopOff) {
		this.oneHopOff = oneHopOff;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public void setDataLimit(Long byteLimit) {
		if (byteLimit != null) {
			this.dataLimit = byteLimit;
		} else {
			this.dataLimit = 0l;
		}
	}
}