/* DecideRuleSequenceTest
 * 
 * Created on Apr 4, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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
package org.archive.crawler.deciderules;

import java.io.File;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURIFactory;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.util.TmpDirTestCase;

/**
 * @author stack
 * @version $Date$, $Revision$
 */
public class DecideRuleSequenceTest extends TmpDirTestCase {
    /**
     * Gets setup by {@link #setUp()}.
     */
    private DecideRuleSequence rule = null;
    
    protected void setUp() throws Exception {
        super.setUp();
        final String name = this.getClass().getName();
        SettingsHandler settingsHandler = new XMLSettingsHandler(
            new File(getTmpDir(), name + ".order.xml"));
        settingsHandler.initialize();
        // Create a new ConfigureDecideRule instance and add it to a MapType
        // (I can change MapTypes after instantiation).  The chosen MapType
        // is the rules canonicalization rules list.
        this.rule = (DecideRuleSequence)((MapType)settingsHandler.getOrder().
            getAttribute(CrawlOrder.ATTR_RULES)).addElement(settingsHandler.
                getSettingsObject(null), new DecideRuleSequence(name));
    }
    
    public void testEmptySequence() {
        Object decision = this.rule.decisionFor("test");
        assertTrue("Expect PASS but got " + decision,
            decision == DecideRule.PASS);
    }
    
    public void testSingleACCEPT() throws InvalidAttributeValueException {
        Object decision = addDecideRule(new AcceptDecideRule("ACCEPT")).
            decisionFor("test");
        assertTrue("Expect ACCEPT but got " + decision,
            decision == DecideRule.ACCEPT);
    }
    
    public void testSingleREJECT() throws InvalidAttributeValueException {
        Object decision = addDecideRule(new RejectDecideRule("REJECT")).
            decisionFor("test");
        assertTrue("Expect REJECT but got " + decision,
                decision == DecideRule.REJECT);
    }
    
    public void testSinglePASS() throws InvalidAttributeValueException {
        Object decision = addDecideRule(new DecideRule("PASS")).
            decisionFor("test");
        assertTrue("Expect PASS but got " + decision,
                decision == DecideRule.PASS);
    }
    
    
    public void testACCEPTWins() throws InvalidAttributeValueException {
        addDecideRule(new DecideRule("PASS1"));
        addDecideRule(new RejectDecideRule("REJECT1"));
        addDecideRule(new DecideRule("PASS2"));
        addDecideRule(new AcceptDecideRule("ACCEPT1"));
        addDecideRule(new RejectDecideRule("REJECT2"));
        addDecideRule(new DecideRule("PASS3"));
        addDecideRule(new AcceptDecideRule("ACCEPT2"));
        addDecideRule(new DecideRule("PASS4"));
        Object decision = this.rule.decisionFor("test");
        assertTrue("Expect ACCEPT but got " + decision,
            decision == DecideRule.ACCEPT);
    }
    
    public void testREJECTWins() throws InvalidAttributeValueException {
        addDecideRule(new DecideRule("PASS1"));
        addDecideRule(new RejectDecideRule("REJECT1"));
        addDecideRule(new DecideRule("PASS2"));
        addDecideRule(new AcceptDecideRule("ACCEPT1"));
        addDecideRule(new RejectDecideRule("REJECT2"));
        addDecideRule(new DecideRule("PASS3"));
        addDecideRule(new AcceptDecideRule("ACCEPT2"));
        addDecideRule(new DecideRule("PASS4"));
        addDecideRule(new RejectDecideRule("REJECT3"));
        Object decision = this.rule.decisionFor("test");
        assertTrue("Expect REJECT but got " + decision,
            decision == DecideRule.REJECT);
    }
    
    public void testRegex()
    throws InvalidAttributeValueException, AttributeNotFoundException,
    MBeanException, ReflectionException {
        final String regexName = "REGEX";
        DecideRule r = addDecideRule(new MatchesRegExpDecideRule(regexName));
        // Set regex to be match anything that ends in archive.org.
        r.setAttribute(new Attribute(MatchesRegExpDecideRule.ATTR_REGEXP,
            "^.*\\.archive\\.org"));
        Object decision = this.rule.decisionFor("http://google.com");
        assertTrue("Expect PASS but got " + decision,
            decision == DecideRule.PASS);
        decision = this.rule.decisionFor("http://archive.org");
        assertTrue("Expect PASS but got " + decision,
            decision == DecideRule.PASS);
        decision = this.rule.decisionFor("http://www.archive.org");
        assertTrue("Expect ACCEPT but got " + decision,
            decision == DecideRule.ACCEPT);
    }
    
    public void testNotRegex()
    throws InvalidAttributeValueException, AttributeNotFoundException,
    MBeanException, ReflectionException {
        final String regexName = "NOT_REGEX";
        DecideRule r = addDecideRule(new NotMatchesRegExpDecideRule(regexName));
        // Set regex to be match anything that ends in archive.org.
        r.setAttribute(new Attribute(MatchesRegExpDecideRule.ATTR_REGEXP,
            "^.*\\.archive\\.org"));
        Object decision = this.rule.decisionFor("http://google.com");
        assertTrue("Expect ACCEPT but got " + decision,
            decision == DecideRule.ACCEPT);
        decision = this.rule.decisionFor("http://www.archive.org");
        assertTrue("Expect PASS but got " + decision,
            decision == DecideRule.PASS);
    }
    
    
    public void testPrerequisite()
    throws InvalidAttributeValueException, URIException {
        addDecideRule(new PrerequisiteAcceptDecideRule("PREREQUISITE"));
        UURI uuri = UURIFactory.getInstance("http://archive.org");
        CandidateURI candidate = new CandidateURI(uuri);
        Object decision = this.rule.decisionFor(candidate);
        assertTrue("Expect PASS but got " + decision,
            decision == DecideRule.PASS);
        candidate = new CandidateURI(uuri, "LLP", null, null);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect ACCEPT but got " + decision,
            decision == DecideRule.ACCEPT);
    }
    
    public void testHops()
    throws InvalidAttributeValueException, URIException {
        addDecideRule(new TooManyHopsDecideRule("HOPS"));
        testHopLimit(TooManyHopsDecideRule.DEFAULT_MAX_HOPS.intValue(), 'L',
            DecideRule.PASS, DecideRule.REJECT);
    }
    
    public void testTransclusion()
    throws InvalidAttributeValueException, URIException {
        addDecideRule(new TransclusionDecideRule("TRANSCLUSION"));
        final int max =
            TransclusionDecideRule.DEFAULT_MAX_TRANS_HOPS.intValue();
        final char pathExpansion = 'X';
        UURI uuri = UURIFactory.getInstance("http://archive.org");
        CandidateURI candidate = new CandidateURI(uuri);
        Object decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.PASS + " but got " + decision,
            decision == DecideRule.PASS);
        StringBuffer path = new StringBuffer(max);
        for (int i = 0; i < (max - 1); i++) {
            path.append(pathExpansion);
        }
        candidate = new CandidateURI(uuri, path.toString(), null, null);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.ACCEPT + " but got " + decision,
            decision == DecideRule.ACCEPT);
        String pathCopy = path.toString();
        path.append(pathExpansion);
        candidate = new CandidateURI(uuri, path.toString(), null, null);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.ACCEPT + " but got " + decision,
            decision == DecideRule.ACCEPT);
        path.append(pathExpansion);
        candidate = new CandidateURI(uuri, path.toString(), null, null);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.PASS + " but got " + decision,
            decision == DecideRule.PASS);
        candidate = new CandidateURI(uuri, pathCopy + 'L', null, null);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.PASS + " but got " + decision,
            decision == DecideRule.PASS);
    }
    
    public void testPathologicalPath()
    throws InvalidAttributeValueException, URIException {
        addDecideRule(new PathologicalPathDecideRule("PATHOLOGICAL"));
        final int max =
            PathologicalPathDecideRule.DEFAULT_REPETITIONS.intValue();
        final String baseUri = "http://archive.org/abc/";
        UURI uuri = UURIFactory.getInstance(baseUri);
        CandidateURI candidate = new CandidateURI(uuri);
        Object decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.PASS + " but got " + decision,
            decision == DecideRule.PASS);
        uuri = UURIFactory.getInstance(baseUri + "abc/");
        candidate = new CandidateURI(uuri);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.PASS + " but got " + decision,
            decision == DecideRule.PASS);
        uuri = UURIFactory.getInstance(baseUri + "abc/abc/");
        candidate = new CandidateURI(uuri);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.REJECT + " but got " + decision,
            decision == DecideRule.REJECT);
    }
    
    public void testTooManyPathSegments()
    throws InvalidAttributeValueException, URIException {
        addDecideRule(new TooManyPathSegmentsDecideRule("SEGMENTS"));
        final int max =
            TooManyPathSegmentsDecideRule.DEFAULT_MAX_PATH_DEPTH.intValue();
        StringBuffer baseUri = new StringBuffer("http://archive.org");
        for (int i = 0; i < max; i++) {
            baseUri.append('/');
            baseUri.append(Integer.toString(i + 1));
        }
        UURI uuri = UURIFactory.getInstance(baseUri.toString());
        CandidateURI candidate = new CandidateURI(uuri);
        Object decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.PASS + " but got " + decision,
            decision == DecideRule.PASS);
        baseUri.append("/x");
        uuri = UURIFactory.getInstance(baseUri.toString());
        candidate = new CandidateURI(uuri);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + DecideRule.REJECT + " but got " + decision,
            decision == DecideRule.REJECT);
    }
    
    protected void testHopLimit(final int max, final char pathExpansion,
        final String defaultDecision, final String overLimitDecision)
    throws URIException {
        UURI uuri = UURIFactory.getInstance("http://archive.org");
        CandidateURI candidate = new CandidateURI(uuri);
        Object decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + defaultDecision + " but got " + decision,
            decision == defaultDecision);
        StringBuffer path = new StringBuffer(max);
        for (int i = 0; i < (max - 1); i++) {
            path.append(pathExpansion);
        }
        candidate = new CandidateURI(uuri, path.toString(), null, null);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + defaultDecision + " but got " + decision,
            decision == defaultDecision);
        path.append(pathExpansion);
        candidate = new CandidateURI(uuri, path.toString(), null, null);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + defaultDecision + " but got " + decision,
            decision == defaultDecision);
        path.append(pathExpansion);
        candidate = new CandidateURI(uuri, path.toString(), null, null);
        decision = this.rule.decisionFor(candidate);
        assertTrue("Expect " + overLimitDecision + " but got " + decision,
            decision == overLimitDecision);       
    }
    
    protected DecideRule addDecideRule(DecideRule dr)
    throws InvalidAttributeValueException {
        MapType rules = this.rule.getRules(null);
        rules.addElement(null, dr);
        return dr;
    }
}
