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

import org.archive.modules.fetcher.UserAgentProvider;
import org.archive.modules.net.RobotsHonoringPolicy;
import org.archive.settings.JobHome;
import org.archive.spring.HasKeyedProperties;
import org.archive.spring.KeyedProperties;
import org.springframework.beans.factory.InitializingBean;
import org.archive.state.Key;
import org.archive.state.Module;
import org.archive.state.StateProvider;
import org.archive.util.ArchiveUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author pjack
 */
public class DefaultMetadataProvider implements 
    InitializingBean, 
    MetadataProvider, 
    UserAgentProvider, 
    Serializable, 
    Module,
    HasKeyedProperties {

    private static final long serialVersionUID = 1L;

    KeyedProperties kp = new KeyedProperties();
    public KeyedProperties getKeyedProperties() {
        return kp;
    }
    
    public RobotsHonoringPolicy getRobotsHonoringPolicy() {
        return (RobotsHonoringPolicy) kp.get("robotsHonoringPolicy");
    }
    @Autowired
    public void setRobotsHonoringPolicy(RobotsHonoringPolicy policy) {
        kp.put("robotsHonoringPolicy",policy);
    }

    String operator = "";
    public String getOperator() {
        return operator;
    }
    public void setOperator(String operatorName) {
        this.operator = operatorName;
    }

    String description = ""; 
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    
    {
        setUserAgentTemplate("Mozilla/5.0 (compatible; heritrix/@VERSION@ +@OPERATOR_CONTACT_URL@)");
    }
    public String getUserAgentTemplate() {
        return (String) kp.get("userAgentTemplate");
    }
    public void setUserAgentTemplate(String template) {
        // TODO compile pattern outside method
        if(!template.matches("^.*\\+@OPERATOR_CONTACT_URL@.*$")) {
            throw new IllegalArgumentException("bad user-agent: "+template);
        }
        kp.put("userAgentTemplate",template);
    }
    
    {
        setOperatorFrom("");
    }
    public String getOperatorFrom() {
        return (String) kp.get("operatorFrom");
    }
    public void setOperatorFrom(String operatorFrom) {
        // TODO compile pattern outside method
        if(!operatorFrom.matches("^(\\s*|\\S+@[-\\w]+\\.[-\\w\\.]+)$")) {
            throw new IllegalArgumentException("bad operatorFrom: "+operatorFrom);
        }
        kp.put("operatorFrom",operatorFrom);
    }
    
    {
        // set default to illegal value
        kp.put("operatorContactUrl","ENTER-A-CONTACT-HTTP-URL-FOR-CRAWL-OPERATOR");
    }
    public String getOperatorContactUrl() {
        return (String) kp.get("operatorContactUrl");
    }
    public void setOperatorContactUrl(String operatorContactUrl) {
        // TODO compile pattern outside method
        if(!operatorContactUrl.matches("^https?://.*$")) {
            throw new IllegalArgumentException("bad operatorContactUrl: "+operatorContactUrl);
        }
        kp.put("operatorContactUrl",operatorContactUrl);
    }


    String audience = ""; 
    public String getAudience() {
        return audience;
    }
    public void setAudience(String audience) {
        this.audience = audience;
    }
   
    String organization = ""; 
    public String getOrganization() {
        return organization;
    }
    public void setOrganization(String organization) {
        this.organization = organization;
    }
    final public static Key<String> ORGANIZATION = Key.make("");


    public void afterPropertiesSet() {

    }
    
    public String getUserAgent() {
        String userAgent = getUserAgentTemplate();
        String contactURL = getOperatorContactUrl();
        userAgent = userAgent.replaceFirst("@OPERATOR_CONTACT_URL@", contactURL);
        userAgent = userAgent.replaceFirst("@VERSION@",
                Matcher.quoteReplacement(ArchiveUtils.VERSION));
        return userAgent;
    }

    protected JobHome jobHome;
    public JobHome getJobHome() {
        return jobHome;
    }
    @Autowired
    public void setJobHome(JobHome home) {
        this.jobHome = home;
    }
    
    public String getJobName() {
        return jobHome.getName();
    }

    public String getRobotsPolicyName() {
        return getRobotsHonoringPolicy().getType().toString();
    }

    public String getFrom() {
        return getOperatorFrom();
    } 
}
