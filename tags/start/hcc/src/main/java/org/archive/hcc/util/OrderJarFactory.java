/* $Id$
 *
 * (Created on Dec 12, 2005
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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderJarFactory {
    private static Logger log = Logger.getLogger(OrderJarFactory.class.getName());


    public OrderJarFactory() {
        super();
        // TODO Auto-generated constructor stub
    }

    public static File createOrderJar(Map parameters) {
        try {

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

            String order = b.toString();
            String date = new SimpleDateFormat("yyyyMMddhhmmss")
                    .format(new Date());

            order = order.replace("$name", parameters.get("name").toString());
            order = order.replace("$arcPrefix", parameters
                    .get("name")
                    .toString());
            order = order.replace("$date", date);
            ByteArrayOutputStream orderFileOs = new ByteArrayOutputStream();
            orderFileOs.write(order.getBytes());
            map.put("order.xml", new ByteArrayInputStream(orderFileOs
                    .toByteArray()));
            orderFileOs.close();

            // write seeds
            ByteArrayOutputStream seedsOs = new ByteArrayOutputStream();
            int count = 0;
            Collection<String> seeds = (Collection<String>) parameters
                    .get("seeds");
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

}