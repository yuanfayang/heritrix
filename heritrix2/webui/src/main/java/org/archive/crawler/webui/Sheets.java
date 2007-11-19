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
import org.archive.crawler.framework.JobStage;
import org.archive.settings.Association;
import org.archive.settings.jmx.JMXSheetManager;
import org.archive.settings.jmx.Types;
import org.archive.settings.path.PathChanger;
import org.archive.settings.path.PathValidator;

import static org.archive.settings.path.PathChanger.LIST_TAG;
import static org.archive.settings.path.PathChanger.MAP_TAG;
import static org.archive.settings.path.PathChanger.OBJECT_TAG;
import static org.archive.settings.path.PathChanger.REFERENCE_TAG;
import static org.archive.settings.path.PathChanger.PRIMARY_TAG;
import static org.archive.settings.path.PathChanger.AUTO_TAG;
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
        Set<String> checkedOut = new HashSet<String>(
                Arrays.asList(sheetManager.getCheckedOutSheets()));
        request.setAttribute("checkedOut", checkedOut);
        
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
        Set<String> checkedOut = new HashSet<String>(
                Arrays.asList(sheetManager.getCheckedOutSheets()));
        request.setAttribute("checkedOut", checkedOut);

        try {
            if (!Arrays.asList(sheetManager.getCheckedOutSheets()).contains(sheet)) {
                sheetManager.checkout(sheet);
            }
            
            if (sheetManager.isSingleSheet(sheet)) {
                Settings settings = getSettings(request, sheetManager, sheet);
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
            Settings settings = getSettings(request, sheetManager, sheet);
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

    
    public static void showAddSheetBundle(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Crawler c = Home.getCrawler(request);
        JMXConnector jmxc = c.connect();
        try {
            CrawlJob.fromRequest(request, jmxc);
        } finally {
            Misc.close(jmxc);
        }
        request.setAttribute("single", false);
        Misc.forward(request, response, "page_add_sheet.jsp");
    }
    
    
    public static void showAddSingleSheet(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Crawler c = Home.getCrawler(request);
        JMXConnector jmxc = c.connect();
        try {
            CrawlJob.fromRequest(request, jmxc);
        } finally {
            Misc.close(jmxc);
        }
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

    
    private static String getValue(HttpServletRequest request, String path) {
        String type = request.getParameter(TYPE_PREFIX + path);
        String value = request.getParameter(VALUE_PREFIX + path);
        if (value == null) {
            return null;
        }
        if (PathChanger.isObjectTag(type)) {
            int p = value.indexOf(',');
            value = value.substring(p + 2);
        }
        return value;
    }
    
    
    private static String getType(HttpServletRequest request, String path) {
        String type = request.getParameter(TYPE_PREFIX + path);
        String value = request.getParameter(VALUE_PREFIX + path);
        if (value == null) {
            return type;
        }
        if (PathChanger.isObjectTag(type)) {
            int p = value.indexOf(',');
            type = value.substring(0, p);
        }
        return type;
    }
    
    public static void saveSingleSheet(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        String anchor = "";
        try {
            List<CompositeData> changes = new ArrayList<CompositeData>();
            for (int i = 1; request.getParameter(Integer.toString(i)) != null; i++) {
                String path = request.getParameter(Integer.toString(i));

                String type = getType(request, path);
                String value = getValue(request, path);
                if (value != null) {
                    CompositeData cd = new CompositeDataSupport(
                            Types.SET_DATA, 
                            new String[] { "path", "type", "value" },
                            new Object[] { path, type, value }
                    );
                    changes.add(cd);
                }
            }
            CompositeData[] arr = changes.toArray(new CompositeData[0]);
            sheetManager.setMany(sheet, true, arr);
            if (request.getParameter("remove") != null) {
                String path = request.getParameter("remove");
                anchor = "#" + path;
                sheetManager.remove(sheet, path);
            }
            if (request.getParameter("add") != null) {
                String input = request.getParameter("add");
                int p = input.indexOf('`');
                String path = input.substring(0, p);
                input = input.substring(p + 1);
                p = input.indexOf('`');
                String type = input.substring(0, p);
                String value = input.substring(p + 1);
                sheetManager.set(sheet, path, type, value);
                anchor = "#" + path;
            }
        } finally {
            remote.close();
        }
        String url = request.getContextPath() + 
            "/sheets/do_show_sheet_editor.jsp?" +
            Text.sheetQueryString(request) + anchor;
        response.sendRedirect(url);
//        showSheetEditor(sc, request, response);
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
            String value = request.getParameter(VALUE_PREFIX + path);
            sheetManager.set(sheet, path, type, value);
        } finally {
            remote.close();
        }
        
        showSheetEditor(sc, request, response);
    }
    

    private static void saveObjectPath(String path, 
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        String primary = request.getParameter("primary");
        request.setAttribute("sheet", sheet);
        try {
            String type;
            String value;
            String kind = request.getParameter("kind");
            if (kind.equals("auto")) {
                type = AUTO_TAG;
                value = "";
            } else if (kind.endsWith("_known")) {
                String s = request.getParameter(kind);
                int p = s.indexOf(',');
                type = s.substring(0, p);
                value = s.substring(p + 2);
            } else if (kind.startsWith("create")) {
                type = OBJECT_TAG;
                value = request.getParameter(kind);
            } else {
                type = REFERENCE_TAG;
                value = request.getParameter(kind);
            }
            if (type.equals(OBJECT_TAG) && "primary".equals(primary)) {
                type = PRIMARY_TAG;
            }
            sheetManager.set(sheet, path, type, value);
        } finally {
            remote.close();
        }

        showSheetEditor(sc, request, response);
    }
    
    
    public static void saveObjectPath(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        saveObjectPath(request.getParameter("path"), sc, request, response);
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
            String value = request.getParameter(VALUE_PREFIX + path);
            sheetManager.set(sheet, elementPath, type, value);
        } finally {
            remote.close();
        }
        
        showPathDetail(sc, request, response);
    }

    
    public static void addObjectElement(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String path = getElementPath(sc, request, response);
        if (path == null) {
            showPathDetail(sc, request, response);
            return;            
        }
        saveObjectPath(path, sc, request, response);
    }

    
    private static String getElementPath(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String key = request.getParameter("key");
        if (key.length() == 0) {
            request.setAttribute("error", "You must specify a name for the new element.");
            return null;
        }
        String path = request.getParameter("path");
        String elementPath = path + PathValidator.DELIMITER
               + request.getParameter("key");
        return elementPath;
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
        boolean convertImplied = "on".equals(
                request.getParameter("convertImplied"));
        BufferedReader br = new BufferedReader(new StringReader(surts));
        int count = 0;
        try {
            for (String s = br.readLine(); s != null; s = br.readLine()) {
                if (!s.startsWith("#") && s.trim().length() > 0) {
                    String prefix = convertImplied ? Misc.toSURTPrefix(s) : s;
                    if (add) {
                        sheetManager.associate(sheet, prefix);
                    } else {
                        sheetManager.disassociate(sheet, prefix);
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

    
    public static void showSurts(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        String startString = request.getParameter("start");
        int start = (startString == null) ? 0 : Integer.parseInt(startString);
        try {
            String[] surts = sheetManager.listContexts(sheet, start);
            request.setAttribute("sheet", sheet);
            request.setAttribute("start", start);
            request.setAttribute("surts", Arrays.asList(surts));
            Misc.forward(request, response, "page_list_surts.jsp");
        } finally {
            remote.close();
        }
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
                List<Association> list = new ArrayList<Association>();
                for (int i = 0; i < pairs.length; i += 2) {
                    list.add(new Association(pairs[i], pairs[i + 1]));
                }
                request.setAttribute("surtToSheet", list);
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
    
    

    
    public static void deleteSheet(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        try {
            sheetManager.removeSheet(sheet);
        } finally {
            remote.close();
        }
        showSheets(sc, request, response);
    }
    
    
    public static void showDeleteSheet(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        remote.close();
        Misc.forward(request, response, "page_delete_sheet.jsp");
    }





    private static Settings getSettings(HttpServletRequest request, 
            JMXSheetManager mgr,
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
                setting.setPathInvalid(true);
                result.put(setting.getPath(), setting);                
            }
            setting.setValue(value);
            setting.setErrorMessage(error);
        }
        
        CrawlJob job = (CrawlJob)request.getAttribute("job");
        boolean completed = job.getJobStage() == JobStage.COMPLETED;
        
        return new Settings(sheet, mgr.isOnline(), completed, result);
    }


    /**
     * Returns the JMXSheetManager specified in the request.
     * 
     * Needs five request parameters.  The first three are host, port and
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
        Remote<CrawlJobManager> manager = CrawlerArea.open(request);
        try {
            CrawlJob job = CrawlJob.fromRequest(request, 
                    manager.getJMXConnector());

            JMXConnector jmxc = manager.getJMXConnector();

            ObjectName name;
            if (job.getJobStage() == JobStage.ACTIVE) {
                String query = "org.archive.crawler:*,type=" 
                    + JMXSheetManager.class.getName() + ",name=" + job.getName();
                name = Misc.findUnique(jmxc, query);
            } else {
                name = manager.getObject().getSheetManagerStub(job.encode());
            }
            return Remote.make(jmxc, name, JMXSheetManager.class);
        } catch (RuntimeException e) {
            manager.close();
            throw new IllegalStateException(e);
        }
    }


}
