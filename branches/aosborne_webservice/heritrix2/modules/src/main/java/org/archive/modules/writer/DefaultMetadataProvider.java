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
 * DefaultMetadataProvider.java
 *
 * Created on Aug 17, 2007
 *
 * $Id:$
 */

package org.archive.modules.writer;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.modules.fetcher.UserAgentProvider;
import org.archive.modules.net.RobotsHonoringPolicy;
import org.archive.settings.SheetManager;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyMaker;
import org.archive.state.KeyManager;
import org.archive.state.Module;
import org.archive.state.PatternConstraint;
import org.archive.state.StateProvider;
import org.archive.util.ArchiveUtils;

/**
 * @author pjack
 *
 */
public class DefaultMetadataProvider implements 
    Initializable, 
    MetadataProvider, 
    UserAgentProvider, 
    Serializable, 
    Module {

    private static final long serialVersionUID = 1L;

    @Immutable
    final public static Key<SheetManager> SHEET_MANAGER = 
        Key.makeAuto(SheetManager.class);
    
    @Immutable
    final public static Key<RobotsHonoringPolicy> ROBOTS_HONORING_POLICY =
        Key.makeAuto(RobotsHonoringPolicy.class);

    @Immutable
    final public static Key<String> OPERATOR_NAME = Key.make("");
    
    @Immutable
    final public static Key<String> DESCRIPTION = Key.make("");

    final public static Key<String> HTTP_USER_AGENT =
        KeyMaker
            .make("Mozilla/5.0 (compatible; heritrix/@VERSION@ +@OPERATOR_CONTACT_URL@)")
            .addConstraint(new PatternConstraint(
                    Pattern.compile("^.*\\+@OPERATOR_CONTACT_URL@.*$")))
            .toKey();
    
    final public static Key<String> OPERATOR_FROM =
        KeyMaker
            .make("ENTER-A-CONTACT-EMAIL-FOR-CRAWL-OPERATOR")
            .addConstraint(new PatternConstraint(
                    Pattern.compile("^\\S+@[-\\w]+\\.[-\\w\\.]+$")))
            .toKey();
    
    final public static Key<String> OPERATOR_CONTACT_URL =
        KeyMaker
            .make("ENTER-A-CONTACT-HTTP-URL-FOR-CRAWL-OPERATOR")
            .addConstraint(new PatternConstraint(
                    Pattern.compile("^https?://.*$")))
            .toKey();
    
    @Immutable
    final public static Key<String> AUDIENCE = Key.make("");
    
    @Immutable
    final public static Key<String> ORGANIZATION = Key.make("");


    static {
        KeyManager.addKeys(DefaultMetadataProvider.class);
    }

    
    private SheetManager manager;
    private String operatorName;
    private String description;
    private String robotsPolicy;
    private String userAgent;
    private String from;
    private String audience;
    private String organization;
    
    
    public void initialTasks(StateProvider global) {
        this.manager = global.get(this, SHEET_MANAGER);
        this.robotsPolicy = global.get(this, ROBOTS_HONORING_POLICY).getType(global).toString();
        this.operatorName = global.get(this, OPERATOR_NAME);
        this.description = global.get(this, DESCRIPTION);
        this.from = global.get(this, OPERATOR_FROM);
        this.userAgent = getUserAgent(global);
        this.audience = global.get(this, AUDIENCE);
        this.organization = global.get(this, ORGANIZATION);
    }

    public String getFrom() {
        return from;
    }
    
    
    public String getFrom(StateProvider context) {
        return from;
    }

    
    public String getUserAgent(StateProvider context) {
        String userAgent = context.get(this, HTTP_USER_AGENT);
        String contactURL = context.get(this, OPERATOR_CONTACT_URL);
        userAgent = userAgent.replaceFirst("@OPERATOR_CONTACT_URL@", contactURL);
        userAgent = userAgent.replaceFirst("@VERSION@",
                Matcher.quoteReplacement(ArchiveUtils.VERSION));
        return userAgent;
    }
    
    public String getUserAgent() {
        return userAgent;
    }


    public String getJobDescription() {
        return description;
    }


    public String getJobName() {
        return manager.getCrawlName();
    }


    public String getJobOperator() {
        return operatorName;
    }


    public String getRobotsPolicy() {
        return robotsPolicy;
    }

    
    public String getAudience() {
        return audience;
    }
    
    
    public String getOrganization() {
        return organization;
    }

}
