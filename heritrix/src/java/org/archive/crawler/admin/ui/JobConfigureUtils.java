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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.settings.ComplexType;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.ListType;
import org.archive.crawler.settings.ModuleAttributeInfo;


/**
 * Utility methods used configuring jobs in the admin UI.
 * 
 * Methods are mostly called by the admin UI jsp.
 * 
 * @author stack
 * @version $Date$, $Revision$
 */
public class JobConfigureUtils {
    private static Logger logger =
        Logger.getLogger(JobConfigureUtils.class.getName());

    /**
     * Check passed crawljob CrawlJob setting.
     * Call this method at start of page.
     * @param job Current CrawlJobHandler.
     * @param request Http request.
     * @param response Http response.
     */
    protected static CrawlJob getAndCheckJob(CrawlJob job,
            HttpServletRequest request,
            HttpServletResponse response) {
    	    
        return job;
    }
        

    
	/**
	 * This methods updates a ComplexType with information passed to it
	 * by a HttpServletRequest. It assumes that for every 'simple' type
	 * there is a corresponding parameter in the request. A recursive
	 * call will be made for any nested ComplexTypes. For each attribute
	 * it will check if the relevant override is set (name.override 
	 * parameter equals 'true'). If so the attribute setting on the 
	 * specified domain level (settings) will be rewritten. If it is not
	 * we well ensure that it isn't being overridden.
	 * 
	 * @param mbean The ComplexType to update
	 * @param settings CrawlerSettings for the domain to override setting
	 *           for. null denotes the global settings.
	 * @param request The HttpServletRequest to use to update the 
	 *           ComplexType
	 * @param expert if true expert settings will be updated, otherwise they
	 *           will be ignored.     
	 */
	public static void writeNewOrderFile(ComplexType mbean,
			CrawlerSettings settings, HttpServletRequest request,
			boolean expert) {
        // If mbean is transient or a hidden expert setting.
		if (mbean.isTransient() ||
                (mbean.isExpertSetting() && expert == false)) {
			return;
		}
        
		MBeanAttributeInfo a[] = mbean.getMBeanInfo(settings).getAttributes();
		for (int n = 0; n < a.length; n++) {
            checkAttribute((ModuleAttributeInfo)a[n], mbean, settings, request,
                expert);
        }
    }
            
    /**
     * Process passed attribute.
     * Check if needs to be written and if so, write it.
     * 
     * @param att Attribute to process.
     * @param mbean The ComplexType to update
     * @param settings CrawlerSettings for the domain to override setting
     *           for. null denotes the global settings.
     * @param request The HttpServletRequest to use to update the 
     *           ComplexType
     * @param expert if true expert settings will be updated, otherwise they
     *           will be ignored.
     */
	protected static void checkAttribute(ModuleAttributeInfo att,
			ComplexType mbean, CrawlerSettings settings,
			HttpServletRequest request, boolean expert) {
		// The attributes of the current attribute.
		Object currentAttribute = null;
		try {
			currentAttribute = mbean.getAttribute(settings, att.getName());
		} catch (Exception e) {
			logger.severe("Failed getting " + mbean.getAbsoluteName() +
			    " attribute " + att.getName() + ": " + e.getMessage());
			return;
		}
		
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("MBEAN: " + mbean.getAbsoluteName() + " " +
                att.getName() + " TRANSIENT " + att.isTransient() + " " +
				att.isExpertSetting() + " " + expert);
		}
		
		if (att.isTransient() == false &&
                (att.isExpertSetting() == false || expert)) {
			if (currentAttribute instanceof ComplexType) {
				writeNewOrderFile((ComplexType) currentAttribute, settings,
						request, expert);
			} else {
				String attName = att.getName();
				// Have a 'setting'. Let's see if we need to update it (if
				// settings == null update all, otherwise only if override
				// is set.
				String attAbsoluteName = mbean.getAbsoluteName() + "/" +
				    attName;
				boolean override = (request.getParameter(attAbsoluteName +
				        ".override") != null) &&
				    (request.getParameter(attAbsoluteName + ".override").
						equals("true"));
				if (settings == null || override) {
                    if (currentAttribute instanceof ListType) {
                        ListType list = (ListType)currentAttribute;
                        list.clear();
                        String[] elems = request.getParameterValues(attAbsoluteName);
                        for (int i = 0; elems != null && i < elems.length; i++) {
                            list.add(elems[i]);
                        }
                    } else {
                    	    writeAttribute(attName, attAbsoluteName, mbean, settings,
                            request.getParameter(attAbsoluteName));
                    }
                    
                } else if (settings != null && override == false) {
                    // Is not being overridden. Need to remove possible
                    // previous overrides.
                    try {
                        mbean.unsetAttribute(settings, attName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.severe("Unsetting attribute on " +
                                attAbsoluteName + ": " + e.getMessage());
                        return;
                    }
                }
            }
        }
    }
   
    /**
     * Write out attribute.
     * @param attName Attribute short name.
     * @param attAbsoluteName Attribute full name.
     * @param mbean The ComplexType to update
     * @param settings CrawlerSettings for the domain to override setting
     *     for. null denotes the global settings.
     * @param value Value to set into the attribute.
     */
	protected static void writeAttribute(String attName,
            String attAbsoluteName, ComplexType mbean,
            CrawlerSettings settings, String value) {
		try {
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("MBEAN SET: " +  attAbsoluteName + " " + value);
			}
			mbean.setAttribute(settings, new Attribute(attName, value));
		} catch (Exception e) {
			e.printStackTrace();
			logger.severe("Setting attribute value " + value + " on " +
			    attAbsoluteName + ": " + e.getMessage());
			return;
		}
	}
}
