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
 * Sheets.java
 *
 * Created on May 9, 2007
 *
 * $Id:$
 */

package org.archive.crawler.webui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.crawler.framework.CrawlJobManager;
import org.archive.settings.jmx.JMXSheetManager;
import org.archive.settings.jmx.Types;
import org.archive.settings.path.PathValidator;

import static org.archive.settings.path.PathChanger.LIST_TAG;
import static org.archive.settings.path.PathChanger.MAP_TAG;
import static org.archive.settings.path.PathChanger.OBJECT_TAG;
import static org.archive.settings.path.PathChanger.REFERENCE_TAG;
import static org.archive.crawler.webui.Settings.TYPE_PREFIX;
import static org.archive.crawler.webui.Settings.VALUE_PREFIX;


/**
 * @author pjack
 *
 */
public class Sheets {

    
    private Sheets() {
    }
    

    public static void showSheets(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        try {
            SortedSet<String> sheets = new TreeSet<String>(
                    Arrays.asList(sheetManager.getSheets()));
            Set<String> problems = new HashSet<String>(
                    Arrays.asList(sheetManager.getProblemSingleSheetNames()));
            Set<String> checkedOut = new HashSet<String>(
                    Arrays.asList(sheetManager.getCheckedOutSheets()));
            request.setAttribute("sheets", sheets);
            request.setAttribute("problems", problems);
            request.setAttribute("checkedOut", checkedOut);
            Misc.forward(request, response, "page_sheets.jsp");
        } finally {
            remote.close();
        }
    }

    
    public static void showSheetDetail(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        try {
            CompositeData[] settings = sheetManager.getAll(sheet);
            CompositeData[] problems = sheetManager.getSingleSheetProblems(sheet);
            request.setAttribute("settings", Arrays.asList(settings));
            request.setAttribute("problems", Arrays.asList(problems));
            Misc.forward(request, response, "page_sheet_detail.jsp");
        } finally {
            remote.close();
        }
    }
    
    
    public static void showSheetEditor(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        try {
            if (!Arrays.asList(sheetManager.getCheckedOutSheets()).contains(sheet)) {
                sheetManager.checkout(sheet);
            }
            
            if (sheetManager.isSingleSheet(sheet)) {
                Settings settings = getSettings(sheetManager, sheet);
                request.setAttribute("settings", settings);
                Misc.forward(request, response, "page_sheet_editor.jsp");
            } else {
                String[] bundled = sheetManager.getBundledSheets(sheet);
                request.setAttribute("bundled", Arrays.asList(bundled));
                Misc.forward(request, response, "page_bundle_editor.jsp");
            }
        } finally {
            remote.close();
        }
    }
    
    
    public static void showPathDetail(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        String path = request.getParameter("path");
        request.setAttribute("sheet", sheet);
        request.setAttribute("path", path);
        try {
            if (!Arrays.asList(sheetManager.getCheckedOutSheets()).
                    contains(sheet)) {
                sheetManager.checkout(sheet);
            }
            Settings settings = getSettings(sheetManager, sheet);
            request.setAttribute("settings", settings);
            
            Setting setting = settings.getSetting(path);
            if (setting.getType().equals(LIST_TAG) 
                    || setting.getType().equals(MAP_TAG)) {
                Misc.forward(request, response, "page_list_detail.jsp");
            } else {
                Misc.forward(request, response, "page_path_detail.jsp");
            }
        } finally {
            remote.close();
        }
    }
            

    
    public static void removePath(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        
        String path = request.getParameter("path");
        String type = request.getParameter("type");
        try {
            sheetManager.set(sheet, path, type, null);
        } finally {
            remote.close();
        }
        
        showSheetEditor(sc, request, response);
    }
    
    
    public static void overridePath(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        
        String path = request.getParameter("path");
        String type = request.getParameter("type");
        String value = request.getParameter("value");
        try {
            sheetManager.set(sheet, path, type, value);
        } finally {
            remote.close();
        }
        
        showSheetEditor(sc, request, response);
    }
    
    
    
    public static void showAddSheetBundle(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Home.getCrawler(request);
        getJob(request);
        request.setAttribute("single", false);
        Misc.forward(request, response, "page_add_sheet.jsp");
    }
    
    
    public static void showAddSingleSheet(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Home.getCrawler(request);
        getJob(request);
        request.setAttribute("single", true);
        Misc.forward(request, response, "page_add_sheet.jsp");
    }

    
    public static void addSheetBundle(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        try {
            sheetManager.makeSheetBundle(sheet);
        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());
            Misc.forward(request, response, "page_add_sheet_bundle.jsp");
        } finally {
            remote.close();
        }
        
        showSheetEditor(sc, request, response);
    }

    
    public static void addSingleSheet(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        try {
            sheetManager.makeSingleSheet(sheet);
        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());
            Misc.forward(request, response, "page_add_single_sheet.jsp");
        } finally {
            remote.close();
        }
        
        showSheetEditor(sc, request, response);
    }

    
    public static void saveSingleSheet(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        try {
            List<CompositeData> changes = new ArrayList<CompositeData>();
            for (int i = 1; request.getParameter(Integer.toString(i)) != null; i++) {
                String path = request.getParameter(Integer.toString(i));

                String type = request.getParameter(TYPE_PREFIX + path);
                String value = request.getParameter(VALUE_PREFIX + path);
                if (value != null) {
                    if (type.equals(OBJECT_TAG) || type.equals(REFERENCE_TAG)) {
                        int p = value.indexOf(',');
                        type = value.substring(0, p);
                        value = value.substring(p + 2);
                    }
                    CompositeData cd = new CompositeDataSupport(
                            Types.SET_DATA, 
                            new String[] { "path", "type", "value" },
                            new Object[] { path, type, value }
                    );
                    changes.add(cd);
                }
            }
            CompositeData[] arr = changes.toArray(new CompositeData[0]);
            sheetManager.setMany(sheet, arr);
            showSheetEditor(sc, request, response);
        } finally {
            remote.close();
        }
    }
    
    
    public static void savePath(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        try {
            String path = request.getParameter("path");
            String type = request.getParameter(TYPE_PREFIX + path);
            String value;
            if (type.equals(OBJECT_TAG) || type.equals(REFERENCE_TAG)) {
                String valueKind = request.getParameter("value_kind");
                value = request.getParameter(valueKind);
                type = valueKind.startsWith("obj") ? OBJECT_TAG : REFERENCE_TAG;
            } else {
                value = request.getParameter(VALUE_PREFIX + path);
            }
            sheetManager.set(sheet, path, type, value);
        } finally {
            remote.close();
        }
        
        showSheetEditor(sc, request, response);
    }
    
    
    public static void commitSheet(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        try {
            sheetManager.commit(sheet);
        } finally {
            remote.close();
        }
        showSheets(sc, request, response);
    }

    
    public static void cancelSheet(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        try {
            sheetManager.cancel(sheet);
        } finally {
            remote.close();
        }
        showSheets(sc, request, response);
    }

    
    public static void moveElementUp(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        moveElement(sc, request, response, true);
    }

    
    public static void moveElementDown(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        moveElement(sc, request, response, false);
    }

    
    private static void moveElement(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response,
            boolean up) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        String path = request.getParameter("path");
        String key = request.getParameter("key");
        String elementPath = path + PathValidator.DELIMITER + key;
        try {
            if (up) {
                sheetManager.moveElementUp(sheet, elementPath);
            } else {
                sheetManager.moveElementDown(sheet, elementPath);
            }
        } finally {
            remote.close();
        }
        showPathDetail(sc, request, response);
    }
    
    public static void addElement(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String key = request.getParameter("key");
        if (key.length() == 0) {
            request.setAttribute("error", "You must specify a name for the new element.");
            showPathDetail(sc, request, response);            
        }
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        try {
            String path = request.getParameter("path");
            String elementPath = path + PathValidator.DELIMITER
                   + request.getParameter("key");
            String type = request.getParameter(TYPE_PREFIX + path);
            String value;
            if (type.equals(OBJECT_TAG) || type.equals(REFERENCE_TAG)) {
                String valueKind = request.getParameter("value_kind");
                value = request.getParameter(valueKind);
                type = valueKind.startsWith("obj") ? OBJECT_TAG : REFERENCE_TAG;
            } else {
                value = request.getParameter(VALUE_PREFIX + path);
            }
            sheetManager.set(sheet, elementPath, type, value);
        } finally {
            remote.close();
        }
        
        showPathDetail(sc, request, response);
    }
    
    
    public static void associate(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        boolean add = request.getParameter("add").equals("Y");
        String sheet = request.getParameter("sheet");
        String surts = request.getParameter("surts");
        BufferedReader br = new BufferedReader(new StringReader(surts));
        int count = 0;
        try {
            for (String s = br.readLine(); s != null; s = br.readLine()) {
                if (!s.startsWith("#") && s.trim().length() > 0) {
                    String surt = Misc.toSURT(s);
                    if (add) {
                        sheetManager.associate(sheet, surt);
                    } else {
                        sheetManager.disassociate(sheet, surt);
                    }
                }
            }
            count++;
        } catch (IOException e) { 
            // Impossible
            throw new Error();
        } finally {
            remote.close();
        }

        request.setAttribute("message", count + " SURT prefixes were " +
                        "associated with " + sheet + ".");
        showSheets(sc, request, response);
    }

    
    public static void showAssociate(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        getSheetManager(request).close();
        request.setAttribute("sheet", request.getParameter("sheet"));
        request.setAttribute("add", request.getParameter("add"));
        Misc.forward(request, response, "page_associations.jsp");
    }

    
    public static void showConfig(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String url = request.getParameter("url");
        request.setAttribute("sheet", url);
        try {
            String surt = Misc.toSURT(url);
            if (request.getParameter("button").equals("Sheets")) {
                String[] pairs = sheetManager.findConfigNames(surt);
                Map<String,String> map = new LinkedHashMap<String,String>();
                for (int i = 0; i < pairs.length; i += 2) {
                    map.put(pairs[i], pairs[i + 1]);
                }
                request.setAttribute("surtToSheet", map);
                Misc.forward(request, response, "page_config_names.jsp");
            } else {
                CompositeData[] settings = sheetManager.findConfig(surt);
                request.setAttribute("settings", Arrays.asList(settings));
                request.setAttribute("problems", Collections.emptyList());
                Misc.forward(request, response, "page_sheet_detail.jsp");
            }
        } finally {
            remote.close();
        }
    }

    
    public static void moveBundledSheets(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        String move = request.getParameter("move");
        int index = Integer.parseInt(request.getParameter("index"));
        try {
            sheetManager.moveBundledSheet(sheet, move, index);
        } finally {
            remote.close();
        }
        
        showSheetEditor(sc, request, response);
    }
            

    public static Settings getGlobalSettings(JMXSheetManager mgr) {
        return getSettings(mgr, "default");
    }
    

    private static Settings getSettings(JMXSheetManager mgr,
            String sheet) {
        Map<String,Setting> result = new LinkedHashMap<String,Setting>();
        CompositeData[] settings = mgr.resolveAll(sheet);
        for (CompositeData cd: settings) {
            Setting setting = new Setting();
            setting.setPath((String)cd.get("path"));
            setting.setValue((String)cd.get("value"));
            setting.setType((String)cd.get("type"));
            setting.setSheets((String[])cd.get("sheets"));
            result.put(setting.getPath(), setting);
        }
        
        CompositeData[] problems = mgr.getSingleSheetProblems(sheet);
        for (CompositeData cd: problems) {
            String path = (String)cd.get("path");
            String value = (String)cd.get("value");
            String type = (String)cd.get("type");
            String error = (String)cd.get("error");
            Setting setting = result.get(path);
            if (setting == null) {
                setting = new Setting();
                setting.setPath("!" + path);
                setting.setType(type);
                setting.setSheets(new String[] { sheet } );
                result.put(setting.getPath(), setting);                
            }
            setting.setValue(value);
            setting.setErrorMessage(error);
        }
        
        return new Settings(sheet, mgr.isOnline(), result);
    }
    
    
    private static String getJob(HttpServletRequest request) {
        String job = request.getParameter("job");
        if (job == null) {
            job = request.getParameter("profile");
            if (job == null) {
                throw new IllegalStateException("Must specify job or profile.");
            }
            request.setAttribute("profile", job);
        } else {
            request.setAttribute("job", job);
        }
        return job;
    }
    
    
    private static boolean isProfile(HttpServletRequest request) {
        return request.getParameter("profile") != null;
    }
    
    
    /**
     * Returns the JMXSheetManager specified in the request.
     * 
     * Needs four request parameters.  The first three are host, port and
     * id; these identify the CrawlJobManager like always.  The last is
     * either a parameter named "job", which indicates we're dealing with
     * an active crawl job, or a parameter named "profile", which indicates
     * we're dealing with an offline profile.
     * 
     * <p>If "profile" is provided, then this method will invoke 
     * openProfile to create the remote JMXSheetManager if it does not 
     * already exist.  Otherwise it will find the remote JMXSheetManager
     * for the specified job name.
     * 
     * @param request   the request containing the parameters described above
     * @return   the remote JMXSheetManager specified in the request
     * @throws Exception   if anything goes wrong
     */
    public static Remote<JMXSheetManager> getSheetManager(
            HttpServletRequest request) throws Exception {
        Crawler crawler = Home.getCrawler(request);
        boolean profile = isProfile(request);
        String job = getJob(request);
        
        JMXConnector jmxc = crawler.connect();
        String query = "org.archive.crawler:*," + 
            "name=" + job + 
            ",type=" + JMXSheetManager.class.getName();

        Set<ObjectName> set = Misc.find(jmxc, query);
        if (set.size() == 1) {
            ObjectName name = set.iterator().next();
            return Remote.make(jmxc, name, JMXSheetManager.class);
        }

        if (set.size() != 0) {
            throw new IllegalStateException("Expected unique SheetManager for "
                    + query + " but found " + set);
        }
        
        if (!profile) {
            throw new IllegalStateException("Can't open sheets for completed job.");
        }
        
        Remote<CrawlJobManager> manager = Remote.make(
                jmxc, crawler.getObjectName(), CrawlJobManager.class);
        manager.getObject().openProfile(job);
        
        ObjectName name = Misc.waitFor(jmxc, query, true);
        return Remote.make(jmxc, name, JMXSheetManager.class);
    }

}
