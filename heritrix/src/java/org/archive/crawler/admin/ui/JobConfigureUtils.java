/*
 * Heritrix
 *
 * $Id$
 *
 * Created on Aug 30, 2004
 *
 * Copyright (C) 2003 Internet Archive.
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
 */
package org.archive.crawler.admin.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.admin.CrawlJobHandler;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.framework.ProcessorChain;
import org.archive.crawler.settings.ComplexType;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.ListType;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.ModuleAttributeInfo;
import org.archive.crawler.settings.ModuleType;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.util.IoUtils;
import org.archive.util.TextUtils;

/**
 * Utility methods used configuring jobs in the admin UI.
 * 
 * Methods are mostly called by the admin UI jsp.
 * 
 * @author stack
 * @version $Date$, $Revision$
 */
public class JobConfigureUtils {
    private static Logger logger = Logger.getLogger(JobConfigureUtils.class
            .getName());
    public static final String ACTION = "action";
    public static final String SUBACTION = "subaction";
    public static final String FILTERS = "filters";
    private static final String MAP = "map";
    private static final String FILTER = "filter";
    private static final Object ADD = "add";
    private static final Object MOVEUP = "moveup";
    private static final Object MOVEDOWN = "movedown";
    private static final Object REMOVE = "remove";
    private static final Object GOTO = "goto";
    private static final Object DONE = "done";
    private static final Object CONTINUE = "continue"; // keep editting

    /**
     * Check passed crawljob CrawlJob setting. Call this method at start of
     * page.
     * 
     * @param job
     *            Current CrawlJobHandler.
     * @param request
     *            Http request.
     * @param response
     *            Http response.
     * @return Crawljob.
     */
    protected static CrawlJob getAndCheckJob(CrawlJob job,
            HttpServletRequest request, HttpServletResponse response) {
        return job;
    }

    /**
     * This methods updates a ComplexType with information passed to it by a
     * HttpServletRequest. It assumes that for every 'simple' type there is a
     * corresponding parameter in the request. A recursive call will be made for
     * any nested ComplexTypes. For each attribute it will check if the relevant
     * override is set (name.override parameter equals 'true'). If so the
     * attribute setting on the specified domain level (settings) will be
     * rewritten. If it is not we well ensure that it isn't being overridden.
     * 
     * @param mbean
     *            The ComplexType to update
     * @param settings
     *            CrawlerSettings for the domain to override setting for. null
     *            denotes the global settings.
     * @param request
     *            The HttpServletRequest to use to update the ComplexType
     * @param expert
     *            if true expert settings will be updated, otherwise they will
     *            be ignored.
     */
    public static void writeNewOrderFile(ComplexType mbean,
            CrawlerSettings settings, HttpServletRequest request, boolean expert) {
        // If mbean is transient or a hidden expert setting.
        if (mbean.isTransient() || (mbean.isExpertSetting() && expert == false)) {
            return;
        }

        MBeanAttributeInfo a[] = mbean.getMBeanInfo(settings).getAttributes();
        for (int n = 0; n < a.length; n++) {
            checkAttribute((ModuleAttributeInfo) a[n], mbean, settings,
                    request, expert);
        }
    }

    /**
     * Process passed attribute. Check if needs to be written and if so, write
     * it.
     * 
     * @param att
     *            Attribute to process.
     * @param mbean
     *            The ComplexType to update
     * @param settings
     *            CrawlerSettings for the domain to override setting for. null
     *            denotes the global settings.
     * @param request
     *            The HttpServletRequest to use to update the ComplexType
     * @param expert
     *            if true expert settings will be updated, otherwise they will
     *            be ignored.
     */
    protected static void checkAttribute(ModuleAttributeInfo att,
            ComplexType mbean, CrawlerSettings settings,
            HttpServletRequest request, boolean expert) {
        // The attributes of the current attribute.
        Object currentAttribute = null;
        try {
            currentAttribute = mbean.getAttribute(settings, att.getName());
        } catch (Exception e) {
            logger.severe("Failed getting " + mbean.getAbsoluteName()
                    + " attribute " + att.getName() + ": " + e.getMessage());
            return;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("MBEAN: " + mbean.getAbsoluteName() + " "
                    + att.getName() + " TRANSIENT " + att.isTransient() + " "
                    + att.isExpertSetting() + " " + expert);
        }

        if (att.isTransient() == false
                && (att.isExpertSetting() == false || expert)) {
            if (currentAttribute instanceof ComplexType) {
                writeNewOrderFile((ComplexType) currentAttribute, settings,
                        request, expert);
            } else {
                String attName = att.getName();
                // Have a 'setting'. Let's see if we need to update it (if
                // settings == null update all, otherwise only if override
                // is set.
                String attAbsoluteName = mbean.getAbsoluteName() + "/"
                        + attName;
                boolean override = (request.getParameter(attAbsoluteName
                        + ".override") != null)
                        && (request.getParameter(attAbsoluteName + ".override")
                                .equals("true"));
                if (settings == null || override) {
                    if (currentAttribute instanceof ListType) {
                        ListType list = (ListType) currentAttribute;
                        list.clear();
                        String[] elems = request
                                .getParameterValues(attAbsoluteName);
                        for (int i = 0; elems != null && i < elems.length; i++) {
                            list.add(elems[i]);
                        }
                    } else {
                        writeAttribute(attName, attAbsoluteName, mbean,
                                settings, request.getParameter(attAbsoluteName));
                    }

                } else if (settings != null && override == false) {
                    // Is not being overridden. Need to remove possible
                    // previous overrides.
                    try {
                        mbean.unsetAttribute(settings, attName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.severe("Unsetting attribute on "
                                + attAbsoluteName + ": " + e.getMessage());
                        return;
                    }
                }
            }
        }
    }

    /**
     * Write out attribute.
     * 
     * @param attName
     *            Attribute short name.
     * @param attAbsoluteName
     *            Attribute full name.
     * @param mbean
     *            The ComplexType to update
     * @param settings
     *            CrawlerSettings for the domain to override setting for. null
     *            denotes the global settings.
     * @param value
     *            Value to set into the attribute.
     */
    protected static void writeAttribute(String attName,
            String attAbsoluteName, ComplexType mbean,
            CrawlerSettings settings, String value) {
        try {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("MBEAN SET: " + attAbsoluteName + " " + value);
            }
            mbean.setAttribute(settings, new Attribute(attName, value));
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("Setting attribute value " + value + " on "
                    + attAbsoluteName + ": " + e.getMessage());
            return;
        }
    }

    /**
     * Check passed job is not null and not readonly.
     * @param job Job to check.
     * @param response Http response.
     * @param redirectBasePath Full path for where to go next if an error.
     * @param currDomain May be null.
     * E.g. "/admin/jobs/per/overview.jsp".
     * @return A job else we've redirected if no job or readonly.
     * @throws IOException
     */
    public static CrawlJob checkCrawlJob(CrawlJob job,
        HttpServletResponse response, String redirectBasePath,
        String currDomain)
    throws IOException {
        if (job == null) {
            // Didn't find any job with the given UID or no UID given.
            response.sendRedirect(redirectBasePath +
                "?message=No job selected");
        } else if (job.isReadOnly()) {
            // Can't edit this job.
            response.sendRedirect(redirectBasePath +
                "?job=" + job.getUID() +
                ((currDomain != null && currDomain.length() > 0)?
                    "&currDomain=" + currDomain: "") +
                "&message=Can't edit a read only job");
        }
        return job;
    }

    /**
     * Handle job action.
     * @param handler CrawlJobHandler to operate on.
     * @param request Http request.
     * @param response Http response.
     * @param redirectBasePath Full path for where to go next if an error.
     * E.g. "/admin/jobs/per/overview.jsp".
     * @param currDomain Current domain.  Pass null for global domain.
     * @return The crawljob configured.
     * @throws IOException
     * @throws AttributeNotFoundException
     * @throws InvocationTargetException
     * @throws InvalidAttributeValueException
     */
    public static CrawlJob handleJobAction(CrawlJobHandler handler,
            HttpServletRequest request, HttpServletResponse response,
            String redirectBasePath, String currDomain)
    throws IOException, AttributeNotFoundException, InvocationTargetException,
        InvalidAttributeValueException {

        // Load the job to manipulate
        CrawlJob theJob =
            checkCrawlJob(handler.getJob(request.getParameter("job")),
                response, redirectBasePath, currDomain);

        XMLSettingsHandler settingsHandler = theJob.getSettingsHandler();
        // If currDomain is null, then we're at top-level.
        CrawlerSettings settings = settingsHandler
            .getSettingsObject(currDomain);

        // See if we need to take any action
        if (request.getParameter(ACTION) != null) {
            // Need to take some action.
            String action = request.getParameter(ACTION);
            String subaction = request.getParameter(SUBACTION);
            if (action.equals(FILTERS)) {
                // Doing something with the filters.
                String map = request.getParameter(MAP);
                if (map != null && map.length() > 0) {
                    String filter = request.getParameter(FILTER);
                    MapType filterMap = (MapType) settingsHandler
                        .getComplexTypeByAbsoluteName(settings, map);
                    if (subaction.equals(ADD)) {
                        // Add filter
                        String className = request.getParameter(map + ".class");
                        String typeName = request.getParameter(map + ".name");
                        if (typeName != null && typeName.length() > 0 &&
                                className != null && className.length() > 0) {
                            ModuleType tmp = SettingsHandler
                                .instantiateModuleTypeFromClassName(
                                    typeName, className);
                            filterMap.addElement(settings, tmp);
                        }
                    } else if (subaction.equals(MOVEUP)) {
                        // Move a filter down in a map
                        if (filter != null && filter.length() > 0) {
                            filterMap.moveElementUp(settings, filter);
                        }
                    } else if (subaction.equals(MOVEDOWN)) {
                        // Move a filter up in a map
                        if (filter != null && filter.length() > 0) {
                            filterMap.moveElementDown(settings, filter);
                        }
                    } else if (subaction.equals(REMOVE)) {
                        // Remove a filter from a map
                        if (filter != null && filter.length() > 0) {
                            filterMap.removeElement(settings, filter);
                        }
                    }
                }
                // Finally save the changes to disk
                settingsHandler.writeSettingsObject(settings);
            } else if (action.equals(DONE)) {
                // Ok, done editing.
                if(subaction.equals(CONTINUE)) {
                    // was editting an override, simply continue
                    if (theJob.isRunning()) {
                        handler.kickUpdate(); //Just to make sure.
                    }
                    response.sendRedirect(redirectBasePath +
                        "?job=" + theJob.getUID() +
                        ((currDomain != null && currDomain.length() > 0)?
                                "&currDomain=" + currDomain: "") +
                         "&message=Override changes saved");
                } else {
                    // on main, truly 'done'
                    if (theJob.isNew()) {
                        handler.addJob(theJob);
                        response.sendRedirect(redirectBasePath
                                + "?message=Job created");
                    } else {
                        if (theJob.isRunning()) {
                            handler.kickUpdate();
                        }
                        if (theJob.isProfile()) {
                            response.sendRedirect(redirectBasePath
                                    + "?message=Profile modified");
                        } else {
                            response.sendRedirect(redirectBasePath
                                    + "?message=Job modified");
                        }
                    }
                }
            } else if (action.equals(GOTO)) {
                // Goto another page of the job/profile settings
                response.sendRedirect(request.getParameter(SUBACTION) +
                    ((currDomain != null && currDomain.length() > 0)?
                        "&currDomain=" + currDomain: ""));
            }
        }
        return theJob;
    }
    
    /**
     * Print complete seeds list on passed in PrintWriter.
     * @param hndlr Current handler.
     * @param payload What to write out.
     * @throws AttributeNotFoundException
     * @throws MBeanException
     * @throws ReflectionException
     * @throws IOException
     * @throws IOException
     */
    public static void printOutSeeds(SettingsHandler hndlr, String payload)
    throws AttributeNotFoundException, MBeanException, ReflectionException,
    IOException {
        File seedfile = getSeedFile(hndlr);
        writeReader(new StringReader(payload),
            new BufferedWriter(new FileWriter(seedfile)));
    }
    
    /**
     * Print complete seeds list on passed in PrintWriter.
     * @param hndlr Current handler.
     * @param out Writer to write out all seeds to.
     * @throws ReflectionException
     * @throws MBeanException
     * @throws AttributeNotFoundException
     * @throws IOException
     */
    public static void printOutSeeds(SettingsHandler hndlr, Writer out)
    throws AttributeNotFoundException, MBeanException, ReflectionException,
            IOException {
        // getSeedStream looks for seeds on disk and on classpath.
        InputStream is = getSeedStream(hndlr);
        writeReader(new BufferedReader(new InputStreamReader(is)), out);
    }
    
    /**
     * Test whether seeds file is of a size that's reasonable
     * to edit in an HTML textarea. 
     * @param h current settingsHandler
     * @return true if seeds size is manageable, false otherwise
     * @throws AttributeNotFoundException 
     * @throws MBeanException 
     * @throws ReflectionException 
     * 
     */
    public static boolean seedsEdittableSize(SettingsHandler h)
            throws AttributeNotFoundException, MBeanException,
            ReflectionException {
        return getSeedFile(h).length() <= (32 * 1024); // 32K
    }
    /**
     * @param hndlr Settings handler.
     * @return Seeds file.
     * @throws ReflectionException
     * @throws MBeanException
     * @throws AttributeNotFoundException
     */
    protected static File getSeedFile(SettingsHandler hndlr)
    throws AttributeNotFoundException, MBeanException, ReflectionException {
        String seedsFileStr = (String)((ComplexType)hndlr.getOrder().
            getAttribute("scope")).getAttribute("seedsfile");
        return hndlr.getPathRelativeToWorkingDirectory(seedsFileStr);
    }
    
    /**
     * Return seeds as a stream.
     * This method will work for case where seeds are on disk or on classpath.
     * @param hndlr SettingsHandler.  Used to find seeds.txt file.
     * @return InputStream on current seeds file.
     * @throws IOException
     * @throws ReflectionException
     * @throws MBeanException
     * @throws AttributeNotFoundException
     */
    protected static InputStream getSeedStream(SettingsHandler hndlr)
    throws IOException, AttributeNotFoundException, MBeanException,
            ReflectionException {
        InputStream is = null;
        File seedFile = getSeedFile(hndlr);
        if (!seedFile.exists()) {
            // Is the file on the CLASSPATH?
            is = SettingsHandler.class.
                getResourceAsStream(IoUtils.getClasspathPath(seedFile));
        } else if(seedFile.canRead()) {
            is = new FileInputStream(seedFile);
        }
        if (is == null) {
            throw new IOException(seedFile + " does not" +
            " exist -- neither on disk nor on CLASSPATH -- or is not" +
            " readable.");
        }
        return is;
    }
    
    /**
     * Print complete seeds list on passed in PrintWriter.
     * @param reader File to read seeds from.
     * @param out Writer to write out all seeds to.
     * @throws IOException
     */
    protected static void writeReader(Reader reader, Writer out)
    throws IOException {
        final int bufferSize = 1024 * 4;
        char [] buffer = new char[bufferSize];
        int read = -1;
        while ((read = reader.read(buffer, 0, bufferSize)) != -1) {
            out.write(buffer, 0, read);
        }
        out.flush();
    }

    /**
      *
     * Generates the HTML code to display and allow manipulation of passed
     * <code>type</code>.
     *
     * Will work it's way recursively down the crawlorder.
     *
     * MOVED FROM JSP webapps/admin/include/filters.jsp
     *
     * @param mbean The ComplexType representing the crawl order or one
     * of it's subcomponents.
     * @param indent A string to prefix to the current ComplexType to
     * visually indent it.
     * @param possible If true then the current ComplexType MAY be a
     * configurable <code>type</code> (Generally this means that the current
     * ComplexType belongs to a Map)
     * @param first True if mbean is the first element of a Map.
     * @param last True if mbean is the last element of a Map.
     * @parent The absolute name of the ComplexType that contains the
     * current ComplexType (i.e. parent).
     * @param alt If true and mbean is a filter then an alternate background
     * color is used for displaying it.
     * @param type Class to check for.
     * @param printAtttributeNames True if we're to print out attribute names
     * as we recurse.
     * @return The variable part of the HTML code for selecting filters.
     */
    public static String printOfType(ComplexType mbean, String indent,
            boolean possible, boolean first, boolean last, String parent,
            boolean alt, Class type, boolean printAttributeNames)
    throws Exception {
        if(mbean.isTransient()){
            return "";
        }
        List availableOptions = getOptionsForType(type);
        MBeanInfo info = mbean.getMBeanInfo();
        MBeanAttributeInfo a[] = info.getAttributes();
        StringBuffer p = new StringBuffer();
        if(possible && type.isInstance(mbean)) {
            p.append("<tr");
            if(alt){
                p.append(" bgcolor='#EEEEFF'");
            }
            alt = !alt;
            p.append("><td nowrap>" + indent + mbean.getName() +
                "</td><td nowrap>");
            if(first==false){
                p.append("<a href=\"javascript:doMoveUp('" +
                    mbean.getName() + "','" + parent + "')\">Move up</a>");
            }
            p.append("</td><td nowrap>");
            if(last==false){
                p.append("<a href=\"javascript:doMoveDown('" +
                    mbean.getName() + "','" + parent + "')\">Move down</a>");
            }
            p.append("</td><td><a href=\"javascript:doRemove('" +
                mbean.getName() + "','" + parent + "')\">Remove</a></td>");
            p.append("<td><i>" + mbean.getClass().getName() +
                "</i></td></tr>\n");
        } else if (printAttributeNames) {
            p.append("<tr><td colspan='5'><b>" + indent + mbean.getName() +
                "</b></td></tr>\n");
        }

        possible = mbean instanceof MapType;
        alt=false;
        for(int n = 0; n < a.length; n++) {
            if(a[n] == null) {
                p.append("  ERROR: null attribute");
            } else {
                Object currentAttribute = null;
                //The attributes of the current attribute.
                ModuleAttributeInfo att = (ModuleAttributeInfo)a[n];
                try {
                    currentAttribute = mbean.getAttribute(att.getName());
                } catch (Exception e1) {
                    String error = e1.toString() + " " + e1.getMessage();
                    return error;
                }

                if(currentAttribute instanceof ComplexType) {
                    p.append(printOfType((ComplexType)currentAttribute,
                        indent + "&nbsp;&nbsp;", possible, n == 0,
                        n == a.length - 1, mbean.getAbsoluteName(), alt,
                        type, printAttributeNames));
                    if(currentAttribute instanceof MapType) {
                        MapType thisMap = (MapType)currentAttribute;
                        if(thisMap.getContentType().getName().
                            equals(type.getName())) {
                            p.append("<tr><td colspan='5'>\n" + indent +
                                "&nbsp;&nbsp;&nbsp;&nbsp;");
                            p.append("Name: <input size='8' name='" +
                                mbean.getAbsoluteName() + "/" + att.getName() +
                                ".name' id='" + mbean.getAbsoluteName() + "/" +
                                att.getName() + ".name'>\n");
                            // TODO: replace 'Rule' with relevant 'Type'
                            p.append("Rule: <select name='" +
                                mbean.getAbsoluteName() + "/" + att.getName() +
                                ".class'>\n");
                            for(int i=0 ; i<availableOptions.size() ; i++) {
                                p.append("<option value='" +
                                    availableOptions.get(i) + "'>" +
                                    availableOptions.get(i) + "</option>\n");
                            }
                            p.append("</select>\n");
                            p.append("<input type='button' value='Add'" +
                                " onClick=\"doAdd('" + mbean.getAbsoluteName() +
                                "/" + att.getName() + "')\">\n");
                            p.append("</td></tr>\n");
                        }
                    }
                }
                alt = !alt;
            }
        }
        return p.toString();
    }


    /**
     * Generates the HTML code to display and allow manipulation of all
     * MapTypes which include ModuleTypes with multiple options (except
     * Processors). 
     *
     * Will work it's way recursively down the crawlorder.
     *
     * @param mbean The ComplexType representing the crawl order or one
     * of it's subcomponents.
     * @param parent The absolute name of the ComplexType that contains the
     * current ComplexType (i.e. parent).
     * @return The variable part of the HTML code for selecting filters.
     */
    public static String printAllMaps(ComplexType mbean, boolean inMap, String parent)
    throws Exception {
        if(mbean.isTransient()){
            return "";
        }

        MBeanInfo info = mbean.getMBeanInfo();
        MBeanAttributeInfo a[] = info.getAttributes();
        StringBuffer p = new StringBuffer();
        
        boolean subMap = false;
        boolean processorsMap = false; 
        MapType thisMap = null;
        List availableOptions = Collections.EMPTY_LIST;
        if (mbean instanceof MapType) {
            thisMap = (MapType) mbean;
            if (thisMap.getContentType() != Processor.class) {
                // only if a maptype, with moduletype entries, with
                // multiple options, and not Processor, will this 
                // get map treatment
                subMap = true;
            } else {
                processorsMap = true; 
            }
            if (ModuleType.class.isAssignableFrom(thisMap.getContentType())) {
                availableOptions = getOptionsForType(thisMap.getContentType());
            }
            if(availableOptions.size()==0) {
                subMap = false;
            }
        }
        
        String description = TextUtils.escapeForMarkupAttribute(mbean.getDescription());
        if(inMap) {
            p.append("<tr><td>"+mbean.getName()+"</td>");
            p.append("<td nowrap><a href=\"javascript:doMoveUp('" +
                mbean.getName() + "','" + parent + "')\">Move up</a></td>");
            p.append("<td nowrap><a href=\"javascript:doMoveDown('" +
                mbean.getName() + "','" + parent + "')\">Move down</a></td> ");
            p.append("<td><a href=\"javascript:doRemove('" +
                mbean.getName() + "','" + parent + "')\">Remove</a></td>");
            p.append("<td title='"+description+"'>");
            p.append("<i>" + mbean.getClass().getName()+"</i>");
            p.append("<a href='javascript:alert(\""+description+"\")'> ? </a>");
            p.append("</td></tr>\n");
        } else {
            p.append("<div class='SettingsBlock'>\n");
            p.append("<b title='"+description+"'>" + mbean.getName());
            p.append("</b>\n");
            Class type = mbean.getLegalValueType();
            if(CrawlScope.class.isAssignableFrom(type) 
                    || Frontier.class.isAssignableFrom(type) 
                    || processorsMap) {
                p.append(description + " Change on 'Modules' tab.");
            }
            p.append("<br/>\n");
        }

        
        if(subMap) {
            p.append("<table>\n");
        }
        for(int n = 0; n < a.length; n++) {
            if(a[n] == null) {
                p.append("  ERROR: null attribute");
            } else {
                Object currentAttribute = null;
                //The attributes of the current attribute.
                ModuleAttributeInfo att = (ModuleAttributeInfo)a[n];
                try {
                    currentAttribute = mbean.getAttribute(att.getName());
                } catch (Exception e1) {
                    String error = e1.toString() + " " + e1.getMessage();
                    return error;
                }
                if(currentAttribute instanceof ComplexType) {
                    if(inMap) {
                        p.append("<tr><td colspan='5'>");
                    }
                    p.append(printAllMaps((ComplexType)currentAttribute,
                        subMap,  mbean.getAbsoluteName()));
                    if(inMap) {
                        p.append("</td></tr>\n");
                    }
                }
            }
        }
        if(subMap) {
            p.append("</table>\n");
        }

        if(subMap) {
            // ordered list of options; append add controls
            String name = thisMap.getName();
            if (availableOptions != null) {
                p.append("Name: <input size='8' name='" +
                    mbean.getAbsoluteName() +
                    ".name' id='" + mbean.getAbsoluteName() + ".name'>\n");
                p.append("Type: <select name='" +
                    mbean.getAbsoluteName() + ".class'>\n");
                for(int i=0 ; i<availableOptions.size() ; i++) {
                    p.append("<option value='" +
                        availableOptions.get(i) + "'>" +
                        availableOptions.get(i) + "</option>\n");
                }
                p.append("</select>\n");
                p.append("<input type='button' value='Add'" +
                    " onClick=\"doAdd('" + mbean.getAbsoluteName() +
                    "')\">\n");
                p.append("<br/>");
            }
        }

        if(!inMap) {
            p.append("\n</div>\n");
        }
        return p.toString();
    }

    /**
     * @param type
     * @return
     */
    private static List getOptionsForType(Class type) {
        String typeName = type.getName();
        String simpleName = typeName.substring(typeName.lastIndexOf(".")+1);
        String optionsFilename = simpleName+".options";
        try {
            return CrawlJobHandler.loadOptions(optionsFilename);
        } catch (IOException e) {
            logger.info("No options found for "+optionsFilename);
            return new ArrayList();
        }
    }

    /**
     * Generates the HTML code to display and allow manipulation of which
     * filters are attached to this override. Will work it's way
     * recursively down the crawlorder. Inherited filters are displayed,
     * but no changes allowed. Local filters can be added and manipulated.
     *
     * MOVED FROM webapps/admin/include/jobfilters.jsp
     *
     * @param mbean The ComplexType representing the crawl order or one
     *              of it's subcomponents.
     * @param settings CrawlerSettings for the domain to override setting
     *                 for.
     * @param indent A string to prefix to the current ComplexType to
     *               visually indent it.
     * @param possible If true then the current ComplexType MAY be a
     *                 configurable filter. (Generally this means that
     *                 the current ComplexType belongs to a Map)
     * @param first True if mbean is the first element of a Map.
     * @param last True if mbean is the last element of a Map.
     * @parent The absolute name of the ComplexType that contains the
     *         current ComplexType (i.e. parent).
     * @param alt If true and mbean is a filter then an alternate background
     * color is used for displaying it.
     * @param type Class to check for.
     * @param printAtttributeNames True if we're to print out attribute names
     * as we recurse.
     *
     * @return The variable part of the HTML code for selecting filters.
     */
    public String printFilters(ComplexType mbean,
                               CrawlerSettings settings,
                               String indent,
                               boolean possible,
                               boolean first,
                               boolean last,
                               String parent,
                               boolean alt,
                               Class type,
                               boolean printAttributeNames)
                           throws Exception {
        List availableFilters = getOptionsForType(type);
        if(mbean.isTransient()){
            return "";
        }
        StringBuffer p = new StringBuffer();
        MBeanInfo info = mbean.getMBeanInfo(settings);

        MBeanAttributeInfo a[] = info.getAttributes();

        if(type.isInstance(mbean)){
            if(possible){
                // Have a local filter.
                p.append("<tr");
                if(alt){
                    p.append(" bgcolor='#EEEEFF'");
                }
                alt = !alt;
                p.append("><td nowrap><b>" + indent + "</b>" + mbean.getName() + "</td><td nowrap>");
                if(first==false){
                    p.append("<a href=\"javascript:doMoveUp('"+mbean.getName()+"','"+parent+"')\">Move up</a>");
                }
                p.append("</td><td nowrap>");
                if(last==false){
                    p.append("<a href=\"javascript:doMoveDown('"+mbean.getName()+"','"+parent+"')\">Move down</a>");
                }
                p.append("</td><td><a href=\"javascript:doRemove('"+mbean.getName()+"','"+parent+"')\">Remove</a></td>");
                p.append("<td><i>"+mbean.getClass().getName()+"</i></td></tr>\n");
            }
        } else if (printAttributeNames) {
            // Not a filter, or an inherited filter.
            p.append("<tr><td colspan='5'><b>" + indent + mbean.getName() + "</b></td></tr>\n");
        }

        alt=false;
        boolean haveNotFoundFirstEditable = true;
        int firstEditable = -1;
        for(int n=0; n<a.length; n++) {
            possible = mbean instanceof MapType;
            if(a[n] == null) {
                p.append("  ERROR: null attribute");
            } else {
                Object currentAttribute = null;
                Object localAttribute = null;
                ModuleAttributeInfo att = (ModuleAttributeInfo)a[n]; //The attributes of the current attribute.
                try {
                    currentAttribute = mbean.getAttribute(settings, att.getName());
                    localAttribute = mbean.getLocalAttribute(settings, att.getName());
                } catch (Exception e1) {
                    String error = e1.toString() + " " + e1.getMessage();
                    return error;
                }
                if(localAttribute == null){
                    possible = false; //Not an editable filter.
                } else if(haveNotFoundFirstEditable) {
                    firstEditable=n;
                    haveNotFoundFirstEditable=false;
                    alt = true;
                }

                if(currentAttribute instanceof ComplexType) {
                    p.append(printFilters((ComplexType)currentAttribute,settings,indent+"&nbsp;&nbsp;",possible,n==firstEditable,n==a.length-1,mbean.getAbsoluteName(),alt, type, printAttributeNames));
                    if(currentAttribute instanceof MapType)
                    {
                        MapType thisMap = (MapType)currentAttribute;
                        if(thisMap.getContentType().getName().equals(type.getName())){
                            p.append("<tr><td colspan='5'>\n<b>"+indent+"&nbsp;&nbsp;&nbsp;&nbsp;</b>");
                            p.append("Name: <input size='8' name='" + mbean.getAbsoluteName() + "/" + att.getName() + ".name' id='" + mbean.getAbsoluteName() + "/" + att.getName() + ".name'>\n");
                            p.append("Filter: <select name='" + mbean.getAbsoluteName() + "/" + att.getName() + ".class'>\n");
                            for(int i=0 ; i<availableFilters.size() ; i++){
                                p.append("<option value='"+availableFilters.get(i)+"'>"+availableFilters.get(i)+"</option>\n");
                            }
                            p.append("</select>\n");
                            p.append("<input type='button' value='Add' onClick=\"doAdd('" + mbean.getAbsoluteName() + "/" + att.getName() + "')\">\n");
                            p.append("</td></tr>\n");
                        }
                    }
                }
                alt = !alt;
            }
        }
        return p.toString();
    }

    /**
     * Builds the HTML for selecting an implementation of a specific crawler module
     *
     * MOVED FROM webapps/admin/jobs/modules.jsp
     * @param module The MBeanAttributeInfo on the currently set module
     * @param availibleOptions A list of the availibe implementations (full class names as Strings)
     * @param name The name of the module
     *
     * @return the HTML for selecting an implementation of a specific crawler module
     */
    public static String buildModuleSetter(MBeanAttributeInfo module, Class allowableType, String name, String currentDescription){
        StringBuffer ret = new StringBuffer();

        List availableOptions = getOptionsForType(allowableType);

        ret.append("<table><tr><td>&nbsp;Current selection:</td><td>");
        ret.append(module.getType());
        ret.append("</td><td></td></tr>");
        ret.append("<tr><td></td><td width='100' colspan='2'><i>" + currentDescription + "</i></td>");

        if(availableOptions.size()>0){
            ret.append("<tr><td>&nbsp;Available alternatives:</td><td>");
            ret.append("<select name='cbo" + name + "'>");
            for(int i=0 ; i<availableOptions.size() ; i++){
                ret.append("<option value='"+availableOptions.get(i)+"'>");
                ret.append(availableOptions.get(i)+"</option>");
            }
            ret.append("</select>");
            ret.append("</td><td>");
            ret.append("<input type='button' value='Change' onClick='doSetModule(\"" + name + "\")'>");
            ret.append("</td></tr>");
        }
        ret.append("</table>");
        return ret.toString();
    }

    /**
     *
     * Builds the HTML to edit a map of modules
     *
     * MOVED FROM webapps/admin/jobs/modules.jsp
     * @param map The map to edit
     * @param availibleOptions List of availible modules that can be added to the map
     *                         (full class names as Strings)
     * @param name A short name for the map (only alphanumeric chars.)
     *
     * @return the HTML to edit the specified modules map
     */
    public static String buildModuleMap(ComplexType map, Class allowableType, String name){
        StringBuffer ret = new StringBuffer();

        List availableOptions = getOptionsForType(allowableType);

        ret.append("<table cellspacing='0' cellpadding='2'>");

        ArrayList unusedOptions = new ArrayList();
        MBeanInfo mapInfo = map.getMBeanInfo();
        MBeanAttributeInfo m[] = mapInfo.getAttributes();

        // Printout modules in map.
        boolean alt = false;
        for(int n=0; n<m.length; n++) {
            Object currentAttribute = null;
            ModuleAttributeInfo att = (ModuleAttributeInfo)m[n]; //The attributes of the current attribute.

            ret.append("<tr");
            if(alt){
                ret.append(" bgcolor='#EEEEFF'");
            }
            ret.append("><td>&nbsp;"+att.getType()+"</td>");
            if(n!=0){
                ret.append("<td><a href=\"javascript:doMoveMapItemUp('" + name + "','"+att.getName()+"')\">Move up</a></td>");
            } else {
                ret.append("<td></td>");
            }
            if(n!=m.length-1){
                ret.append("<td><a href=\"javascript:doMoveMapItemDown('" + name + "','"+att.getName()+"')\">Move down</a></td>");
            } else {
                ret.append("<td></td>");
            }
            ret.append("<td><a href=\"javascript:doRemoveMapItem('" + name + "','"+att.getName()+"')\">Remove</a></td>");
            ret.append("<td><a href=\"javascript:alert('");
            ret.append(TextUtils.escapeForMarkupAttribute(att.getDescription()));
            ret.append("')\">Info</a></td>\n");
            ret.append("</tr>");
            alt = !alt;
        }

        // Find out which aren't being used.
        for(int i=0 ; i<availableOptions.size() ; i++){
            boolean isIncluded = false;

            for(int n=0; n<m.length; n++) {
                Object currentAttribute = null;
                ModuleAttributeInfo att = (ModuleAttributeInfo)m[n]; //The attributes of the current attribute.

                try {
                    currentAttribute = map.getAttribute(att.getName());
                } catch (Exception e1) {
                    ret.append(e1.toString() + " " + e1.getMessage());
                }
                String typeAndName = att.getType()+"|"+att.getName();
                if(typeAndName.equals(availableOptions.get(i))){
                    //Found it
                    isIncluded = true;
                    break;
                }
            }
            if(isIncluded == false){
                // Yep the current one is unused.
                unusedOptions.add(availableOptions.get(i));
            }
        }
        if(unusedOptions.size() > 0 ){
            ret.append("<tr><td>");
            ret.append("<select name='cboAdd" + name + "'>");
            for(int i=0 ; i<unusedOptions.size() ; i++){
                String curr = (String)unusedOptions.get(i);
                int index = curr.indexOf("|");
                if (index < 0) {
                    throw new RuntimeException("Failed to find '|' required" +
                        " divider in : " + curr + ". Repair modules file.");

                }
                ret.append("<option value='" + curr + "'>" +
                    curr.substring(0, index) + "</option>");
            }
            ret.append("</select>");
            ret.append("</td><td>");
            ret.append("<input type='button' value='Add' onClick=\"doAddMapItem('" + name + "')\">");
            ret.append("</td></tr>");
        }
        ret.append("</table>");
        return ret.toString();
    }

}
