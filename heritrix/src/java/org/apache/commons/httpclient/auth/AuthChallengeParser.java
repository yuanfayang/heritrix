/*
 * $Header$
 * $Revision$
 * $Date$
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * [Additional notices, if required by prior licensing conditions]
 *
 */

package org.apache.commons.httpclient.auth;

import java.util.Map;
import java.util.HashMap;

/**
 * This class provides utility methods for parsing HTTP www and proxy authentication 
 * challenges.
 * 
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * 
 * @since 2.0beta1
 */
public final class AuthChallengeParser {
    /** 
     * Extracts authentication scheme from the given authentication 
     * challenge.
     *
     * @param challengeStr the authentication challenge string
     * @return authentication scheme
     * 
     * @throws MalformedChallengeException when the authentication challenge string
     *  is malformed
     * 
     * @since 2.0beta1
     */
    public static String extractScheme(final String challengeStr) 
      throws MalformedChallengeException {
        if (challengeStr == null) {
            throw new IllegalArgumentException("Challenge may not be null"); 
        }
        int i = challengeStr.indexOf(' ');
        String s = null; 
        if (i == -1) {
            s = challengeStr;
        } else {
            s = challengeStr.substring(0, i);
        }
        if (s.equals("")) {
            throw new MalformedChallengeException("Invalid challenge: " + challengeStr);
        }
        return s.toLowerCase();
    }

    /** 
     * Extracts a map of challenge parameters from an authentication challenge.
     * Keys in the map are lower-cased
     *
     * @param challengeStr the authentication challenge string
     * @return a map of authentication challenge parameters
     * @throws MalformedChallengeException when the authentication challenge string
     *  is malformed
     * 
     * @since 2.0beta1
     */
    public static Map extractParams(final String challengeStr)
      throws MalformedChallengeException {
        if (challengeStr == null) {
            throw new IllegalArgumentException("Challenge may not be null"); 
        }
        int i = challengeStr.indexOf(' ');
        if (i == -1) {
            throw new MalformedChallengeException("Invalid challenge: " + challengeStr);
        }

        Map elements = new HashMap();

        i++;
        int len = challengeStr.length();

        String name = null;
        String value = null;

        StringBuffer buffer = new StringBuffer();

        boolean parsingName = true;
        boolean inQuote = false;
        boolean gotIt = false;

        while (i < len) {
            // Parse one char at a time 
            char ch = challengeStr.charAt(i);
            i++;
            // Process the char
            if (parsingName) {
                // parsing name
                if (ch == '=') {
                    name = buffer.toString().trim();
                    parsingName = false;
                    buffer.setLength(0);
                } else if (ch == ',') {
                    name = buffer.toString().trim();
                    value = null;
                    gotIt = true;
                    buffer.setLength(0);
                } else {
                    buffer.append(ch);
                }
                // Have I reached the end of the challenge string?
                if (i == len) {
                    name = buffer.toString().trim();
                    value = null;
                    gotIt = true;
                }
            } else {
                //parsing value
                if (!inQuote) {
                    // Value is not quoted or not found yet
                    if (ch == ',' || ch == ' ') {
                        value = buffer.toString().trim();
                        gotIt = true;
                        buffer.setLength(0);
                    } else {
                        // no value yet
                        if (buffer.length() == 0) {
                            if (ch == ' ') {
                                //discard
                            } else if (ch == '\t') {
                                //discard
                            } else if (ch == '\n') {
                                //discard
                            } else if (ch == '\r') {
                                //discard
                            } else {
                                // otherwise add to the buffer
                                buffer.append(ch);
                                if (ch == '"') {
                                    inQuote = true;
                                }
                            }
                        } else {
                            // already got something
                            // just keep on adding to the buffer
                            buffer.append(ch);
                        }
                    }
                } else {
                    // Value is quoted
                    // Keep on adding until closing quote is encountered
                    buffer.append(ch);
                    if (ch == '"') {
                        inQuote = false;
                    }
                }
                // Have I reached the end of the challenge string?
                if (i == len) {
                    value = buffer.toString().trim();
                    gotIt = true;
                }
            }
            if (gotIt) {
                // Got something
                if ((name == null) || (name.equals(""))) {
                    throw new MalformedChallengeException("Invalid challenge: " + challengeStr);
                }
                // Strip quotes when present
                if ((value != null) && (value.length() > 1)) {
                    if ((value.charAt(0) == '"') 
                     && (value.charAt(value.length() - 1) == '"')) {
                        value = value.substring(1, value.length() - 1);  
                     }
                }
                
                elements.put(name.toLowerCase(), value);
                parsingName = true;
                gotIt = false;
            }
        }
        return elements;
    }
}
