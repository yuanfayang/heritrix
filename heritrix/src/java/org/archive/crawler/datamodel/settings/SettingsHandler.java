/* SettingsHandler
 *
 * $Id$
 *
 * Created on Dec 16, 2003
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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.util.ArchiveUtils;

/** An instance of this class holds a hierarchy of settings.
 *
 * More than one instance in memory is allowed so that a new CrawlJob could
 * be configured while another job is running.
 *
 * This class should be subclassed to adapt to a persistent storage.
 *
 * @author John Erik Halse
 */
public abstract class SettingsHandler {
    /** Settings object referencing order file */
    private final CrawlerSettings globalSettings =
        new CrawlerSettings(this, null);

    /** Cached CrawlerSettings objects */
    private final SettingsCache settingsCache = new SettingsCache();

    /** Reference to the order module */
    private final CrawlOrder order;

    /** Datatypes supported by the settings framwork */
    final static String INTEGER = "integer";
    final static String LONG = "long";
    final static String FLOAT = "float";
    final static String DOUBLE = "double";
    final static String BOOLEAN = "boolean";
    final static String STRING = "string";
    final static String TEXT = "text";
    final static String OBJECT = "object";
    final static String TIMESTAMP = "timestamp";
    final static String MAP = "map";
    final static String INTEGER_LIST = "integerList";
    final static String LONG_LIST = "longList";
    final static String FLOAT_LIST = "floatList";
    final static String DOUBLE_LIST = "doubleList";
    final static String STRING_LIST = "stringList";
    private final static String names[][] = new String[][] {
            { INTEGER, "java.lang.Integer"},
            { LONG, "java.lang.Long"},
            { FLOAT, "java.lang.Float"},
            { DOUBLE, "java.lang.Double"},
            { BOOLEAN, "java.lang.Boolean"},
            { STRING, "java.lang.String"},
            { TEXT, "org.archive.crawler.datamodel.settings.TextField"},
            { OBJECT, "org.archive.crawler.datamodel.settings.ModuleType"},
            { TIMESTAMP, "java.util.Date"},
            { MAP, "org.archive.crawler.datamodel.settings.MapType"},
            { INTEGER_LIST,
                    "org.archive.crawler.datamodel.settings.IntegerList"},
            { LONG_LIST, "org.archive.crawler.datamodel.settings.LongList"},
            { FLOAT_LIST, "org.archive.crawler.datamodel.settings.FloatList"},
            { DOUBLE_LIST, "org.archive.crawler.datamodel.settings.DoubleList"},
            { STRING_LIST, "org.archive.crawler.datamodel.settings.StringList"}};
    private final static Map name2class = new HashMap();
    private final static Map class2name = new HashMap();
    static {
        for (int i = 0; i < names.length; i++) {
            name2class.put(names[i][0], names[i][1]);
            class2name.put(names[i][1], names[i][0]);
        }
    }

    /** Create a new SettingsHandler object.
     *
     * @throws InvalidAttributeValueException
     */
    public SettingsHandler() throws InvalidAttributeValueException {
        order = new CrawlOrder();
        order.setAsOrder(this);
    }

    /** Initialize the SettingsHandler.
     *
     * This method reads the default settings from the persistent storage.
     */
    public void initialize() {
        readSettingsObject(globalSettings);
    }

    /** Strip off the leftmost part of a domain name.
     *
     * @param scope the domain name.
     * @return scope with everything before the first dot ripped off.
     */
    protected String getParentScope(String scope) {
        int split = scope.indexOf('.');
        if (split == -1) {
            return null;
        } else {
            return scope.substring(split + 1);
        }
    }

    /** Get a module by name.
     *
     * All modules in the order should have unique names. This method makes it
     * possible to get the modules of the order by its name.
     *
     * @param name the modules name.
     * @return the module the name references.
     */
    public ModuleType getModule(String name) {
        return (ModuleType) globalSettings.getModule(name);
    }

    /** Get a complex type by its absolute name.
     *
     * The absolute name is the complex types name and the path leading to
     * it.
     *
     * @param settings the settings object to query.
     * @param absoluteName the absolute name of the complex type to get.
     * @return the complex type referenced by the absolute name or null if
     *         the complex type could not be found in this settings object.
     * @throws AttributeNotFoundException is thrown if no ComplexType by this
     *         name exist.
     */
    public ComplexType getComplexTypeByAbsoluteName(
            CrawlerSettings settings, String absoluteName)
            throws AttributeNotFoundException {

        settings = settings == null ? globalSettings : settings;

        DataContainer data = settings.getData(absoluteName);
        if (data == null) {
            CrawlerSettings parentSettings = settings.getParent();
            if (parentSettings == null) {
                throw new AttributeNotFoundException(absoluteName);
            } else {
                return getComplexTypeByAbsoluteName(parentSettings, absoluteName);
            }
        } else {
            return data.getComplexType();
        }
    }

    protected static String getTypeName(String className) {
        return (String) class2name.get(className);
    }

    protected static String getClassName(String typeName) {
        return (String) name2class.get(typeName);
    }

    /** Convert a String object to an object of <code>typeName</code>.
     *
     * @param stringValue string to convert.
     * @param typeName type to convert to. typeName should be one of the
     *        supported types represented by constants in this class.
     * @return the new value object.
     * @throws ClassCastException is thrown if string could not be converted.
     */
    protected static Object StringToType(String stringValue, String typeName) {
        Object value;
        if (typeName == SettingsHandler.STRING) {
            value = stringValue;
        } else if (typeName == SettingsHandler.TEXT) {
            value = new TextField(stringValue);
        } else if (typeName == SettingsHandler.INTEGER) {
            value = Integer.decode(stringValue);
        } else if (typeName == SettingsHandler.LONG) {
            value = Long.decode(stringValue);
        } else if (typeName == SettingsHandler.BOOLEAN) {
            value = Boolean.valueOf(stringValue);
        } else if (typeName == SettingsHandler.DOUBLE) {
            value = Double.valueOf(stringValue);
        } else if (typeName == SettingsHandler.FLOAT) {
            value = Float.valueOf(stringValue);
        } else if (typeName == SettingsHandler.TIMESTAMP) {
            try {
                value = ArchiveUtils.parse14DigitDate(stringValue);
            } catch (ParseException e) {
                throw new ClassCastException(
                    "Cannot convert '"
                        + stringValue
                        + "' to type '"
                        + typeName
                        + "'");
            }
        } else {
            throw new ClassCastException(
                "Cannot convert '"
                    + stringValue
                    + "' to type '"
                    + typeName
                    + "'");
        }
        return value;
    }

    /** Get CrawlerSettings object in effect for a host or domain.
     *
     * If there is no specific settings for the host/domain, it will recursively
     * go up the hierarchy to find the settings object that should be used for
     * this host/domain.<p>
     *
     * This method will also check if there are different settings for servers
     * on different port numbers on the same host. (Not implemented yet)
     *
     * @param host the host or domain to get the settings for.
     * @param port the port of the server to get settings for.
     * @return settings object in effect for the host/domain.
     * @see #getSettings(String)
     * @see #getSettingsObject(String)
     * @see #getOrCreateSettingsObject(String)
     */
    public CrawlerSettings getSettings(String host, int port) {
        // TODO: Doesn't honor port numbers yet.
        return getSettings(host);
    }

    /** Get CrawlerSettings object in effect for a host or domain.
     *
     * If there is no specific settings for the host/domain, it will recursively
     * go up the hierarchy to find the settings object that should be used for
     * this host/domain.
     *
     * @param host the host or domain to get the settings for.
     * @return settings object in effect for the host/domain.
     * @see #getSettingsObject(String)
     * @see #getOrCreateSettingsObject(String)
     */
    public CrawlerSettings getSettings(String host) {
        if (host == null || host.equals("")) {
            // No scopestring, return global settings
            return globalSettings;
        } else {
            host = host.intern();
        }
        
        CrawlerSettings settings = settingsCache.getSettings(host);

        if (settings == null) {
            String tmpHost = host;
            settings = getSettingsObject(tmpHost);
            while (settings == null && tmpHost != null) {
                tmpHost = getParentScope(tmpHost);
                settings = getSettingsObject(tmpHost);
            }
            
            settingsCache.putSettings(host, settings);
        }

        return settings;
    }

    /** Get CrawlerSettings object for a host or domain.
     *
     * The difference between this method and the
     * <code>getSettings(String host)</code> is that this method will return
     * null if there is no settings for particular host or domain.
     *
     * @param scope the host or domain to get the settings for.
     * @return settings object for the host/domain or null if no
     *         settings exist for the host/domain.
     * @see #getSettings(String)
     * @see #getOrCreateSettingsObject(String)
     */
    public CrawlerSettings getSettingsObject(String scope) {
        CrawlerSettings settings = null;
        if (scope == null || scope.equals("")) {
            // No scopestring, return global settings
            settings = globalSettings;
        } else {
            scope = scope.intern();
            settings = settingsCache.getSettingsObject(scope);
        }
        
        if (settings == null) {
            // Reference not found
            settings = new CrawlerSettings(this, scope);
            // Try to read settings from persisten storage. If its not there
            // it will be set to null.
            settings = readSettingsObject(settings);
            if (settings != null) {
                settingsCache.putSettings(scope, settings);
            }
        }
        return settings;
    }

    /** Get or create CrawlerSettings object for a host or domain.
     *
     * This method is similar to {@link #getSettingsObject(String)} except that
     * if there is no settings for this particular host or domain a new settings
     * object will be returned.
     *
     * @param scope the host or domain to get or create the settings for.
     * @return settings object for the host/domain.
     * @see #getSettings(String)
     * @see #getSettingsObject(String)
     */
    public CrawlerSettings getOrCreateSettingsObject(String scope) {
        CrawlerSettings settings;
        settings = getSettingsObject(scope);
        if (settings == null) {
            scope = scope.intern();
            
            // No existing settings object found, create one
            settings = new CrawlerSettings(this, scope);
            settingsCache.refreshHostToSettings();
            settingsCache.putSettings(scope, settings);
        }
        return settings;
    }

    /** Write the CrawlerSettings object to persistent storage.
     *
     * @param settings the settings object to write.
     */
    public abstract void writeSettingsObject(CrawlerSettings settings);

    /** Read the CrawlerSettings object from persistent storage.
     *
     * @param settings the settings object to be updated with data from the
     *                 persistent storage.
     * @return the updated settings object or null if there was no data for this
     *         in the persistent storage.
     */
    protected abstract CrawlerSettings readSettingsObject(CrawlerSettings settings);

    /** Delete a settings object from persistent storage.
     *
     * @param settings the settings object to delete.
     */
    public void deleteSettingsObject(CrawlerSettings settings) {
        settingsCache.deleteSettingsObject(settings);
    }

    /** Get the CrawlOrder.
     *
     * @return the CrawlOrder
     */
    public CrawlOrder getOrder() {
        return order;
    }

    /** Instatiate a new ModuleType given its name and className.
     *
     * @param name the name for the new ComplexType.
     * @param className the class name of the new ComplexType.
     * @return an instance of the class identified by className.
     *
     * @throws InvocationTargetException
     */
    public static ModuleType instantiateModuleTypeFromClassName(
            String name, String className)
            throws InvocationTargetException {

        Class cl;
        try {
            cl = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new InvocationTargetException(e);
        }

        ModuleType module;
        try {
            Constructor co =
                cl.getConstructor(new Class[] { String.class });
            module = (ModuleType) co.newInstance(new Object[] { name });
        } catch (IllegalArgumentException e) {
            throw new InvocationTargetException(e);
        } catch (InstantiationException e) {
            throw new InvocationTargetException(e);
        } catch (IllegalAccessException e) {
            throw new InvocationTargetException(e);
        } catch (SecurityException e) {
            throw new InvocationTargetException(e);
        } catch (NoSuchMethodException e) {
            throw new InvocationTargetException(e);
        }
        return module;
    }

    /**
     * Transforms a relative path so that it is relative to a location that is
     * regarded as a working dir for these settings. If an absolute path is given,
     * it will be returned unchanged.
     * @param path A relative path to a file (or directory)
     * @return The same path modified so that it is relative to the file level
     *         location that is considered the working directory for these settings.
     */
    public abstract File getPathRelativeToWorkingDirectory(String path);

    /**
     * Will return an array of strings with domains that contain 'per' domain
     * overrides (or their subdomains contain them). The domains considered are
     * limited to those that are subdomains of the supplied domain. If null or
     * empty string is supplied the TLDs will be considered.
     * @param rootDomain The domain to get domain overrides for. Examples:
     *                   'org', 'archive.org', 'crawler.archive.org' etc.
     * @return An array of domains that contain overrides. If rootDomain does not
     *         exist an empty array will be returned.
     */
    public abstract ArrayList getDomainOverrides(String rootDomain);
}
