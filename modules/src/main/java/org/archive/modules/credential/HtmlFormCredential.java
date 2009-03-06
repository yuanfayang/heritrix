/* HtmlFormCredential
 *
 * Created on Apr 7, 2004
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
package org.archive.modules.credential;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;


import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.archive.state.Expert;
import org.archive.state.Global;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.modules.ProcessorURI;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;



/**
 * Credential that holds all needed to do a GET/POST to a HTML form.
 *
 * @author stack
 * @version $Revision$, $Date$
 */
public class HtmlFormCredential extends Credential {

    private static final long serialVersionUID = -3L;

    private static final Logger logger =
        Logger.getLogger(HtmlFormCredential.class.getName());

    /**
     * Full URI of page that contains the HTML login form we're to apply these
     * credentials too: E.g. http://www.archive.org
     */
    @Global @Expert
    final public static Key<String> LOGIN_URI = Key.make("");


    /**
     * Form items.
     */
    @Global @Expert
    final public static Key<Map<String,String>> FORM_ITEMS = 
        Key.makeMap(String.class);

    
    /**
     * GET or POST.
     */
    @Global @Expert
    final public static Key<String> FORM_METHOD = Key.make("POST");
    

    /**
     * Constructor.
     */
    public HtmlFormCredential() {
    }

    /**
     * @param context ProcessorURI context to use.
     * @return login-uri.
     * @throws AttributeNotFoundException
     */
    public String getLoginUri(final ProcessorURI context) {
        return context.get(this, LOGIN_URI);
    }

    /**
     * @param context ProcessorURI context to use.
     * @return login-uri.
     * @throws AttributeNotFoundException
     */
    public String getHttpMethod(final ProcessorURI context) {
        return context.get(this, FORM_METHOD);
    }

    /**
     * @param context ProcessorURI context to use.
     * @return Form inputs as convenient map.  Returns null if no form items.
     * @throws AttributeNotFoundException
     */
    public Map<String,String> getFormItems(final ProcessorURI context) {
        return context.get(this, FORM_ITEMS);
    }

    public boolean isPrerequisite(final ProcessorURI curi) {
        boolean result = false;
        String curiStr = curi.getUURI().toString();
        String loginUri = getPrerequisite(curi);
        if (loginUri != null) {
            try {
                UURI uuri = UURIFactory.getInstance(curi.getUURI(), loginUri);
                if (uuri != null && curiStr != null &&
                    uuri.toString().equals(curiStr)) {
                    result = true;
                    if (!curi.isPrerequisite()) {
                        curi.setPrerequisite(true);
                        logger.fine(curi + " is prereq.");
                    }
                }
            } catch (URIException e) {
                logger.severe("Failed to uuri: " + curi + ", " +
                    e.getMessage());
            }
        }
        return result;
    }

    public boolean hasPrerequisite(ProcessorURI curi) {
        return getPrerequisite(curi) != null;
    }

    public String getPrerequisite(ProcessorURI curi) {
        return getLoginUri(curi);
    }

    public String getKey(ProcessorURI curi) {
        return getLoginUri(curi);
    }

    public boolean isEveryTime() {
        // This authentication is one time only.
        return false;
    }

    public boolean populate(ProcessorURI curi, HttpClient http, HttpMethod method,
            String payload) {
        // http is not used.
        // payload is not used.
        boolean result = false;
        Map<String,String> formItems = getFormItems(curi);
        if (formItems == null || formItems.size() <= 0) {
            try {
                logger.severe("No form items for " + method.getURI());
            }
            catch (URIException e) {
                logger.severe("No form items and exception getting uri: " +
                    e.getMessage());
            }
            return result;
        }

        NameValuePair[] data = new NameValuePair[formItems.size()];
        int index = 0;
        String key = null;
        for (Iterator i = formItems.keySet().iterator(); i.hasNext();) {
            key = (String)i.next();
            data[index++] = new NameValuePair(key, (String)formItems.get(key));
        }
        if (method instanceof PostMethod) {
            ((PostMethod)method).setRequestBody(data);
            result = true;
        } else if (method instanceof GetMethod) {
            // Append these values to the query string.
            // Get current query string, then add data, then get it again
            // only this time its our data only... then append.
            HttpMethodBase hmb = (HttpMethodBase)method;
            String currentQuery = hmb.getQueryString();
            hmb.setQueryString(data);
            String newQuery = hmb.getQueryString();
            hmb.setQueryString(
                    ((StringUtils.isNotEmpty(currentQuery))
                            ? currentQuery + "&"
                            : "")
                    + newQuery);
            result = true;
        } else {
            logger.severe("Unknown method type: " + method);
        }
        return result;
    }

    public boolean isPost(ProcessorURI curi) {
        String method = getHttpMethod(curi);
        return method != null && method.equalsIgnoreCase("POST");
    }
    
    // good to keep at end of source: must run after all per-Key 
    // initialization values are set.
    static {
        KeyManager.addKeys(HtmlFormCredential.class);
    }

}
