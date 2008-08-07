/* 
 * Copyright (C) 2007 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
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
 *
 * KeyMetadataMaker.java
 *
 * Created on Jan 31, 2007
 *
 * $Id:$
 */
package org.archive.state;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.TreeMap;

import org.archive.util.IoUtils;


/**
 * @author pjack
 *
 */
class KeyMetadataMaker {

    
    public static void makeDefaultLocale(File srcDir, File resDir, Class c) {
        String cpath = c.getName().replace('.', '/');
        File dest = new File(resDir, cpath + "_en.utf8");
        if (dest.exists()) {
            return;
        }
        
        File src = new File(srcDir, cpath + ".java");
        if (!src.exists()) {
            System.err.println("Warning: No such file " + src.getAbsolutePath());
            System.err.println("Aborting.");
            return;
        }

        Map<String,String> fields;
        try {
            fields = parseSourceFile(src);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        if (fields.isEmpty()) {
            return;
        }
        
        System.out.println("Writing key field metadata to " + dest.getAbsolutePath());
        OutputStreamWriter osw = null;
        try {
            osw = new OutputStreamWriter(new FileOutputStream(dest), "UTF-8");
            for (Map.Entry<String,String> me: fields.entrySet()) {
                osw.write(me.getKey() + "-description:\n");
                osw.write(me.getValue() + "\n\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } finally {
            IoUtils.close(osw);
        }
    }
    
    

    static Map<String,String> parseSourceFile(File src) 
    throws IOException {
        FileReader fr = new FileReader(src);
        BufferedReader br = new BufferedReader(fr);
        try {
            return parseSourceFile(br);
        } finally {
            IoUtils.close(br);
        }
    }
    
    
    
    static Map<String,String> parseSourceFile(BufferedReader br) 
    throws IOException {
        String mostRecentJavaDoc = null;
        Map<String,String> fields = new TreeMap<String,String>();
        StringBuffer javaDoc = null;
        
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            line = line.trim();
            if (line.startsWith("/**")) {
                mostRecentJavaDoc = null;
                javaDoc = new StringBuffer(line).append('\n');
                if (line.endsWith("*/")) {
                    mostRecentJavaDoc = javaDoc.toString();
                    javaDoc = null;
                }
                continue;
            } else if (line.endsWith("*/")) {
                if (javaDoc != null) {
                    javaDoc.append(line);
                    mostRecentJavaDoc = javaDoc.toString();
                    javaDoc = null;
                }
            }
            
            String keyFieldName = getKeyFieldName(line);
            if (keyFieldName != null) {
                if (mostRecentJavaDoc != null) {
                    String key = keyFieldName.toLowerCase().replace('_', '-');
                    String value = unformatJavaDoc(mostRecentJavaDoc);
                    fields.put(key, value);
                    mostRecentJavaDoc = null;
                }
            } else if (javaDoc != null) {
                javaDoc.append(line).append('\n');
            }
        }

        return fields;
    }


    private static String getKeyFieldName(String line) {
        if (!line.contains("static")) {
            return null;
        }
        
        if (!line.contains("public")) {
            return null;
        }
        
        int start = line.indexOf("Key<");
        if (start < 0) {
            return null;
        }
        
        int end = line.indexOf('>', start);
        if (end < 0) {
            return null;
        }
        
        int equals = line.indexOf('=', end);
        if (equals < 0) {
            return null;
        }
        
        return line.substring(end + 1, equals).trim();
    }

    
    private static String unformatJavaDoc(String javaDoc) {
        // Eliminate tailing blank line.
        if (javaDoc.endsWith("\n")) {
            javaDoc = javaDoc.substring(0, javaDoc.length() - 1);
        }
        
        // Eliminate starting /** and trailing */
        javaDoc = javaDoc.substring(3, javaDoc.length() - 2);
        
        String[] lines = javaDoc.split("\n");
        StringBuffer result = new  StringBuffer();
        for (String line: lines) {
            line = line.trim();
            if (line.startsWith("*")) {
                line = line.substring(1).trim();
            }
            if (line.length() > 0) {
                result.append(line).append(" \n");
            }
        }
        
        return result.toString();
    }

}
