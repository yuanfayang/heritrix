/* XMLSettingsHandler
 *
 * $Id$
 *
 * Created on Dec 18, 2003
 *
 * Copyright (C) 2004 Internet Archive.
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
package org.archive.crawler.datamodel.settings;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.util.ArchiveUtils;
import org.archive.util.FileUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/** A SettingsHandler which uses XML files as persistent storage.
 *
 * @author John Erik Halse
 */
public class XMLSettingsHandler extends SettingsHandler {
    private static Logger logger =
        Logger.getLogger(
            "org.archive.crawler.datamodel.settings.XMLSettingsHandler");

    // XML element name constants
    protected static final String XML_SCHEMA = "heritrix_settings.xsd";
    protected static final String XML_ROOT_ORDER = "crawl-order";
    protected static final String XML_ROOT_HOST_SETTINGS = "crawl-settings";
    protected static final String XML_ROOT_REFINEMENT = "crawl-refinement";
    protected static final String XML_ELEMENT_CONTROLLER = "controller";
    protected static final String XML_ELEMENT_META = "meta";
    protected static final String XML_ELEMENT_NAME = "name";
    protected static final String XML_ELEMENT_DESCRIPTION = "description";
    protected static final String XML_ELEMENT_DATE = "date";
    protected static final String XML_ELEMENT_REFINEMENTLIST = "refinement-list";
    protected static final String XML_ELEMENT_REFINEMENT = "refinement";
    protected static final String XML_ELEMENT_REFERENCE = "reference";
    protected static final String XML_ELEMENT_LIMITS = "limits";
    protected static final String XML_ELEMENT_TIMESPAN = "timespan";
    protected static final String XML_ELEMENT_PORTNUMBER = "portnumber";
    protected static final String XML_ELEMENT_URIMATCHES = "uri-matches";
    protected static final String XML_ELEMENT_OBJECT = "object";
    protected static final String XML_ELEMENT_NEW_OBJECT = "newObject";
    protected static final String XML_ATTRIBUTE_NAME = "name";
    protected static final String XML_ATTRIBUTE_CLASS = "class";
    protected static final String XML_ATTRIBUTE_FROM = "from";
    protected static final String XML_ATTRIBUTE_TO = "to";

    private File orderFile;
    private final static String settingsFilename = "settings";
    private final static String settingsFilenameSuffix = "xml";
    private final static String REFINEMENT_DIR = "_refinements";

    /** Create a new XMLSettingsHandler object.
     *
     * @param orderFile where the order file is located.
     * @throws InvalidAttributeValueException
     */
    public XMLSettingsHandler(File orderFile)
        throws InvalidAttributeValueException {
        super();
        this.orderFile = orderFile;
    }

    /** Initialize the SettingsHandler.
     *
     * This method builds the settings data structure and initializes it with
     * settings from the order file given to the constructor.
     */
    public void initialize() {
        super.initialize();
    }

    /** Initialize the SettingsHandler from a source.
     *
     * This method builds the settings data structure and initializes it with
     * settings from the order file given as a parameter. The intended use is
     * to create a new order file based on a default (template) order file.
     *
     * @param source the order file to initialize from.
     */
    public void initialize(File source) {
        File tmpOrderFile = orderFile;
        orderFile = source;
        this.initialize();
        orderFile = tmpOrderFile;
    }

    private File getSettingsDirectory() {
        String settingsDirectoryName = null;
        try {
            settingsDirectoryName =
                    (String) getOrder().getAttribute(
                        CrawlOrder.ATTR_SETTINGS_DIRECTORY);
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        } catch (MBeanException e) {
            e.printStackTrace();
        } catch (ReflectionException e) {
            e.printStackTrace();
        }

        return getPathRelativeToWorkingDirectory(settingsDirectoryName);
    }

    /** Resolves the filename for a settings object into a file path.
     *
     * It will also create the directory structure leading to this file
     * if it doesn't exist.
     *
     * @param settings the settings object to get file path for.
     * @return the file path for this settings object.
     */
    protected final File settingsToFilename(CrawlerSettings settings) {
        File settingsDirectory = getSettingsDirectory();
        File file;

        if (settings.getScope() == null || settings.getScope().equals("")) {
            if (settings.isRefinement()) {
                file = new File(getSettingsDirectory(), File.separatorChar
                        + REFINEMENT_DIR + File.separatorChar
                        + settings.getName() + '.' + settingsFilenameSuffix);
            } else {
                file = orderFile;
            }
        } else {
            String elements[] = settings.getScope().split("\\.");
            if (elements.length == 0) {
                return orderFile;
            }

            StringBuffer path = new StringBuffer();
            for (int i = elements.length - 1; i > 0; i--) {
                path.append(elements[i]);
                path.append(File.separatorChar);
            }
            path.append(elements[0]);

            if (settings.isRefinement()) {
                file = new File(getSettingsDirectory(), path.toString()
                        + File.separatorChar + REFINEMENT_DIR
                        + File.separatorChar + settings.getName() + '.'
                        + settingsFilenameSuffix);
            } else {
                file = new File(getSettingsDirectory(), path.toString()
                        + File.separatorChar + settingsFilename + "."
                        + settingsFilenameSuffix);
            }
        }
        return file;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.settings.SettingsHandler#writeSettingsObject(org.archive.crawler.datamodel.settings.CrawlerSettings)
     */
    public final void writeSettingsObject(CrawlerSettings settings) {
        File filename = settingsToFilename(settings);
        writeSettingsObject(settings, filename);
    }

    /** Write a CrawlerSettings object to a specified file.
     *
     * This method is similar to {@link #writeSettingsObject(CrawlerSettings)}
     * except that it uses the submitted File object instead of trying to
     * resolve where the file should be written.
     *
     * @param settings the settings object to be serialized.
     * @param filename the file to which the settings object should be written.
     */
    public final void writeSettingsObject(
            CrawlerSettings settings, File filename) {

        logger.fine("Writing " + filename.getAbsolutePath());
        filename.getParentFile().mkdirs();

        try {
            if (getOrder().getController() != null && filename.exists()) {
                // The crawler is running and file exists - make backup first.
                String name = filename.getName();
                //name = name.substring(0, name.lastIndexOf('.'));
                name = name.substring(0, name.lastIndexOf('.'))
                        + '_'
                        + ArchiveUtils.get14DigitDate(settings
                                .getLastSavedTime().getTime()) + "."
                        + settingsFilenameSuffix;
                File backup = new File(filename.getParentFile(), name);
                FileUtils.copyFiles(filename, backup);
            }

            StreamResult result =
                new StreamResult(
                    new BufferedOutputStream(new FileOutputStream(filename)));
            Transformer transformer =
                TransformerFactory.newInstance().newTransformer();
            Source source = new CrawlSettingsSAXSource(settings);
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Read the CrawlerSettings object from a specific file.
     *
     * @param settings the settings object to be updated with data from the
     *                 persistent storage.
     * @param filename the file to read from.
     * @return the updated settings object or null if there was no data for this
     *         in the persistent storage.
     */
    protected final CrawlerSettings readSettingsObject(
            CrawlerSettings settings, File filename) {
        if (filename.exists()) {
            logger.fine("Reading " + filename.getAbsolutePath());
            try {
                XMLReader parser = SAXParserFactory.newInstance()
                        .newSAXParser().getXMLReader();
                InputStream file = new BufferedInputStream(new FileInputStream(
                        filename));
                parser.setContentHandler(new CrawlSettingsSAXHandler(settings));
                InputSource source = new InputSource(file);
                source.setSystemId(filename.toURL().toExternalForm());
                parser.parse(source);
            } catch (SAXParseException e) {
                logger.warning(e.getMessage() + " in '" + e.getSystemId()
                        + "', line: " + e.getLineNumber() + ", column: "
                        + e.getColumnNumber());
            } catch (SAXException e) {
                logger.warning(e.getMessage() + ": "
                        + e.getException().getMessage());
            } catch (ParserConfigurationException e) {
                logger.warning(e.getMessage() + ": "
                        + e.getCause().getMessage());
            } catch (FactoryConfigurationError e) {
                logger.warning(e.getMessage() + ": "
                        + e.getException().getMessage());
            } catch (IOException e) {
                logger.warning("Could not access file '"
                        + filename.getAbsolutePath() + "': " + e.getMessage());
            }
        } else {
            // File doesn't exist, return null to inidcate this.
            settings = null;
        }
        return settings;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.datamodel.settings.SettingsHandler#readSettingsObject(org.archive.crawler.datamodel.settings.CrawlerSettings,
     *      java.lang.String)
     */
    protected final CrawlerSettings readSettingsObject(CrawlerSettings settings) {
        File filename = settingsToFilename(settings);
        return readSettingsObject(settings, filename);
    }

    /** Get the <code>File</code> object pointing to the order file.
     *
     * @return File object for the order file.
     */
    public File getOrderFile() {
        return orderFile;
    }

    /** Creates a replica of the settings file structure in another directory
     * (fully recursive, includes all per host settings). The SettingsHandler
     * will then refer to the new files.
     *
     * Observe that this method should only be called after the SettingsHandler
     * has been initialized.
     *
     * @param newOrderFileName where the new order file should be saved.
     * @param newSettingsDirectory the top level directory of the per host/domain
     *                          settings files.
     * @throws IOException
     */
    public void copySettings(File newOrderFileName, String newSettingsDirectory)
      throws IOException {
        File oldSettingsDirectory = getSettingsDirectory();

        // Write new orderfile and point the settingshandler to it
        orderFile = newOrderFileName;
        try {
            getOrder().setAttribute(
                new Attribute(
                    CrawlOrder.ATTR_SETTINGS_DIRECTORY, newSettingsDirectory));
        } catch (Exception e) {
            throw new IOException("Could not update settings with new location: "
                + e.getMessage());
        }
        writeSettingsObject(getSettingsObject(null));

        File newDir = getPathRelativeToWorkingDirectory(newSettingsDirectory);

        // Copy the per host files
        FileUtils.copyFiles(oldSettingsDirectory, newDir);
    }

    /**
     * Transforms a relative path so that it is relative to the location of the
     * order file. If an absolute path is given, it will be returned unchanged.<p>
     * The location of it's order file is always considered as the 'working'
     * directory for any given settings.
     * @param path A relative path to a file (or directory)
     * @return The same path modified so that it is relative to the file level
     *         location of the order file for the settings handler.
     */
    public File getPathRelativeToWorkingDirectory(String path){
        File f = new File(path);
        // If path is not absolute, set f's directory
        // relative to the path of the order file
        if (!f.isAbsolute()) {
            f = new File(this.getOrderFile().getParent(), path);
        }
        return f;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.settings.SettingsHandler#getDomainOverrides(java.lang.String)
     */
    public ArrayList getDomainOverrides(String rootDomain) {
        File settingsDir = getSettingsDirectory();

        //Find the right start directory.
        ArrayList domains = new ArrayList();
        //First we deconstruct the rootDomain string
        while(rootDomain != null && rootDomain.length()>0){
            if(rootDomain.indexOf('.')<0){
                // Last level.
                domains.add(rootDomain);
                break; //We're done.
            } else {
                // Got more then one level left.
                domains.add(rootDomain.substring(0,rootDomain.indexOf('.')));
                // Strip down rootDomain.
                rootDomain = rootDomain.substring(rootDomain.indexOf('.')+1);
            }
        }
        //Build up a proper path
        //Since the domains are right to left, we start at the end of the array.
        StringBuffer subDir = new StringBuffer();
        for(int i=(domains.size()-1) ; i>=0 ; i--){
            subDir.append(File.separator+domains.get(i));
        }
        //Then we move to the approprite directory.
        settingsDir = new File(settingsDir.getPath()+subDir);
        ArrayList confirmedSubDomains = new ArrayList();
        if(settingsDir.exists()){
            // Found our place! Search through it's subdirs.
            File[] possibleSubDomains = settingsDir.listFiles();
            for (int i = 0; i < possibleSubDomains.length; i++) {
                if (possibleSubDomains[i].isDirectory()
                    && isOverride(possibleSubDomains[i])) {
                    // Found one!
                    confirmedSubDomains.add(possibleSubDomains[i].getName());
                }
            }
        }
        return confirmedSubDomains;
    }

    /**
     * Checks if a file is a a 'per host' override or if it's a directory if it
     * or it's subdirectories  contains a 'per host' override file.
     * @param f The file or directory to check
     * @return True if the file is an override or it's a directory that contains
     *         such a file.
     */
    private boolean isOverride(File f){
        if(f.isDirectory()){
            // Have a directory, check it's contents.
            File[] subs = f.listFiles();
            for(int i=0 ; i < subs.length ; i++){
                if(isOverride(subs[i])){
                    // Found one. Can stop looking.
                    return true;
                }
            }
        } else if(f.getName().equals(settingsFilename + "." + settingsFilenameSuffix)){
            // This is an override file (or sure looks like one in any case).
            return true;
        }
        // Didn't find an override.
        return false;
    }

    /** Delete a settings object from persistent storage.
     *
     * Deletes the file represented by the submitted settings object. All empty
     * directories that are parents to the files path are also deleted.
     *
     * @param settings the settings object to delete.
     */
    public void deleteSettingsObject(CrawlerSettings settings) {
        super.deleteSettingsObject(settings);
        File settingsDirectory = getSettingsDirectory();
        File settingsFile = settingsToFilename(settings);

        settingsFile.delete();
        settingsFile = settingsFile.getParentFile();
        while (settingsFile.isDirectory() && settingsFile.list().length == 0
                && !settingsFile.equals(settingsDirectory)) {
            settingsFile.delete();
            settingsFile = settingsFile.getParentFile();
        }
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.settings.SettingsHandler#getListOfAllFiles()
     */
    public List getListOfAllFiles() {
        ArrayList list = new ArrayList();
        // Add CrawlOrder.
        list.add(getOrderFile().getAbsolutePath());
        // Iterate through the entire override hierarchy
        recursiveFindFiles(getSettingsDirectory(),list);
        // Get files used by settings modules.
        recursiveFindSecondaryFiles(getOrder(),list);
        return list;
    }
    
    /**
     * Add any files being used by any of the Modules making up the settings to
     * the list.
     * 
     * @param mbean A ModuleType to interrogate for files. Any child modules 
     *           will be recursively interrogated.
     * @param list The list to add found files to.
     */
    private void recursiveFindSecondaryFiles(ComplexType mbean, ArrayList list) {
        MBeanInfo info = mbean.getMBeanInfo();
        MBeanAttributeInfo[] a = info.getAttributes();
        // Interrogate the current module
        if(mbean instanceof ModuleType){
            ((ModuleType)mbean).listUsedFiles(list);
        }
        
        // Recursively interrogate all sub modules that are of ModuleType
        for(int n=0; n<a.length; n++) {
            if(a[n] == null) {
                // Error null attribute.
            } else {
                ModuleAttributeInfo att = (ModuleAttributeInfo)a[n];
                Object currentAttribute;
                try {
                    currentAttribute = mbean.getAttribute(att.getName());
                    if(currentAttribute instanceof ComplexType) {
                        recursiveFindSecondaryFiles((ComplexType)currentAttribute,list);
                    }
                } catch (AttributeNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (MBeanException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ReflectionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }       
    }

    /**
     * Starting at the specific directory this method will iterate through all
     * sub directories and add each file (as absolute name, with path as a 
     * string) to the provided ArrayList. Any file found under the settings
     * directory with the proper suffix will be considered valid and added to 
     * the list.
     * @param dir Starting directory
     * @param list The list to add to
     */
    private void recursiveFindFiles(File dir, ArrayList list){
        File[] subs = dir.listFiles();
        if (subs != null) {
            for(int i=0 ; i < subs.length ; i++){
                if(subs[i].isDirectory()){
                    recursiveFindFiles(subs[i],list);
                } else {
                    if(subs[i].getName().endsWith(settingsFilenameSuffix)){
                        // Add it to list
                        list.add(subs[i].getAbsolutePath());
                    }
                }
            }
        }
    }
}
