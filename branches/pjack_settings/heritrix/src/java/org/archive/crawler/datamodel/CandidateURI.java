/* Copyright (C) 2003 Internet Archive.
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
 * CandidateURI.java
 * Created on Sep 30, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.processors.ProcessorURI;
import org.archive.processors.credential.CredentialAvatar;
import org.archive.processors.extractor.HTMLLinkContext;
import org.archive.processors.extractor.Hop;
import org.archive.processors.extractor.Link;
import org.archive.processors.extractor.LinkContext;
import org.archive.settings.SheetManager;
import org.archive.state.Key;
import org.archive.state.StateProvider;
import org.archive.util.ArchiveUtils;
import org.archive.util.Recorder;
import org.archive.util.Reporter;


/**
 * A URI, discovered or passed-in, that may be scheduled.
 * When scheduled, a CandidateURI becomes a {@link CrawlURI}
 * made with the data contained herein. A CandidateURI
 * contains just the fields necessary to perform quick in-scope analysis.
 * 
 * <p>Has a flexible attribute list that will be promoted into
 * any {@link CrawlURI} created from this CandidateURI.  Use it
 * to add custom data or state needed later doing custom processing.
 * See accessors/setters {@link #putString(String, String)},
 * {@link #getString(String)}, etc. 
 *
 * @author Gordon Mohr
 */
public class CandidateURI
implements Serializable, Reporter, CoreAttributeConstants, StateProvider, ProcessorURI {
    private static final long serialVersionUID = -3L;

    /** Highest scheduling priority.
     * Before any others of its class.
     */
    public static final int HIGHEST = 0;
    
    /** High scheduling priority.
     * After any {@link #HIGHEST}.
     */
    public static final int HIGH = 1;
    
    /** Medium priority.
     * After any {@link #HIGH}.
     */
    public static final int MEDIUM = 2;
    
    /** Normal/low priority.
     * Whenever/end of queue.
     */
    public static final int NORMAL = 3;

    
    private int schedulingDirective = NORMAL;

    
    /** 
     * Usuable URI under consideration. Transient to allow
     * more efficient custom serialization 
     */
    private transient UURI uuri;
    
    /** Seed status */
    private boolean isSeed = false;

    private boolean forceRevisit = false; // even if already visited
    
    /** String of letters indicating how this URI was reached from a seed.
     * <pre>
     * P precondition
     * R redirection
     * E embedded (as frame, src, link, codebase, etc.)
     * X speculative embed (as from javascript, some alternate-format extractors
     * L link</pre>
     * For example LLLE (an embedded image on a page 3 links from seed).
     */
    private String pathFromSeed;
    
    /**
     * Where this URI was (presently) discovered. . Transient to allow
     * more efficient custom serialization
     */
    private transient UURI via;

    /**
     * Context of URI's discovery, as per the 'context' in Link
     */
    private LinkContext viaContext;
    
    /**
     * Flexible dynamic attributes list.
     * <p>
     * The attribute list is a flexible map of key/value pairs for storing
     * status of this URI for use by other processors. By convention the
     * attribute list is keyed by constants found in the
     * {@link CoreAttributeConstants} interface.  Use this list to carry
     * data or state produced by custom processors rather change the
     * classes {@link CrawlURI} or this class, CandidateURI.
     *
     * Transient to allow more efficient custom serialization.
     * 
     * Package-protected so CrawlURI can access it directly.
     */
    transient Map<String,Object> data;


    /**
     * Frontier/Scheduler lifecycle info.
     * This is an identifier set by the Frontier for its
     * purposes. Usually its the name of the Frontier queue
     * this URI gets queued to.  Values can be host + port
     * or IP, etc.
     */
    private String classKey;

    
    private transient SheetManager manager;
    private transient StateProvider provider;
    
    
    /**
     * Constructor.
     * Protected access to block access to default constructor.
     */
    protected CandidateURI () {
        super();
    }
    
    /**
     * @param u uuri instance this CandidateURI wraps.
     */
    public CandidateURI(UURI u) {
        this.uuri = u;
    }
    
    /**
     * @param u uuri instance this CandidateURI wraps.
     * @param pathFromSeed
     * @param via
     * @param viaContext
     */
    public CandidateURI(UURI u, String pathFromSeed, UURI via,
            LinkContext viaContext) {
        this.uuri = u;
        this.pathFromSeed = pathFromSeed;
        this.via = via;
        this.viaContext = viaContext;
    }

    /**
     * Set the <tt>isSeed</tt> attribute of this URI.
     * @param b Is this URI a seed, true or false.
     */
    public void setSeed(boolean b) {
        this.isSeed = b;
        if (this.isSeed) {
            if(pathFromSeed==null) {
                this.pathFromSeed = "";
            }
//          seeds created on redirect must have a via to be recognized; don't clear
//            setVia(null);
        }
    }

    /**
     * @return UURI
     */
    public UURI getUURI() {
        return this.uuri;
    }

    /**
     * @return Whether seeded.
     */
    public boolean isSeed() {
        return this.isSeed;
    }

    /**
     * @return path (hop-types) from seed
     */
    public String getPathFromSeed() {
        return this.pathFromSeed;
    }

    /**
     * @return URI via which this one was discovered
     */
    public UURI getVia() {
        return this.via;
    }

    /**
     * @return CharSequence context in which this one was discovered
     */
    public LinkContext getViaContext() {
        return this.viaContext;
    }
    
    /**
     * @param string
     */
    protected void setPathFromSeed(String string) {
        pathFromSeed = string;
    }
    
    /**
     * Called when making a copy of another CandidateURI.
     * @param data   data map to use
     */
    protected void setData(Map<String,Object> data) {
        this.data = data;
    }

    public void setVia(UURI via) {
        this.via = via;
    }


    /**
     * Method returns string version of this URI's referral URI.
     * @return String version of referral URI
     */
    public String flattenVia() {
        return (via == null)? "": via.toString();
    }


    /**
     * @return The UURI this CandidateURI wraps as a string 
     */
    public String toString() {
    	return getUURI().toString();
    }


    /**
     * Compares the domain of this CandidateURI with that of another
     * CandidateURI
     *
     * @param other The other CandidateURI
     *
     * @return True if both are in the same domain, false otherwise.
     * @throws URIException
     */
    public boolean sameDomainAs(CandidateURI other) throws URIException {
        String domain = getUURI().getHost();
        if (domain == null) {
            return false;
        }
        while(domain.lastIndexOf('.') > domain.indexOf('.')) {
            // While has more than one dot, lop off first segment
            domain = domain.substring(domain.indexOf('.') + 1);
        }
        if(other.getUURI().getHost() == null) {
            return false;
        }
        return other.getUURI().getHost().endsWith(domain);
    }

    /**
     * If this method returns true, this URI should be fetched even though
     * it already has been crawled. This also implies
     * that this URI will be scheduled for crawl before any other waiting
     * URIs for the same host.
     *
     * This value is used to refetch any expired robots.txt or dns-lookups.
     *
     * @return true if crawling of this URI should be forced
     */
    public boolean forceFetch() {
        return forceRevisit;
    }

   /**
     * Method to signal that this URI should be fetched even though
     * it already has been crawled. Setting this to true also implies
     * that this URI will be scheduled for crawl before any other waiting
     * URIs for the same host.
     *
     * This value is used to refetch any expired robots.txt or dns-lookups.
     *
     * @param b set to true to enforce the crawling of this URI
     */
    public void setForceFetch(boolean b) {
        forceRevisit = b;
    }

    /**
     * @return Returns the schedulingDirective.
     */
    public int getSchedulingDirective() {
        return schedulingDirective;
    }
    /** 
     * @param priority The schedulingDirective to set.
     */
    public void setSchedulingDirective(int priority) {
        this.schedulingDirective = priority;
    }


    /**
     * @return True if needs immediate scheduling.
     */
    public boolean needsImmediateScheduling() {
        return schedulingDirective == HIGH;
    }

    /**
     * @return True if needs soon but not top scheduling.
     */
    public boolean needsSoonScheduling() {
        return schedulingDirective == MEDIUM;
    }

    /**
     * Tally up the number of transitive (non-simple-link) hops at
     * the end of this CandidateURI's pathFromSeed.
     * 
     * In some cases, URIs with greater than zero but less than some
     * threshold such hops are treated specially. 
     * 
     * <p>TODO: consider moving link-count in here as well, caching
     * calculation, and refactoring CrawlScope.exceedsMaxHops() to use this. 
     * 
     * @return Transhop count.
     */
    public int getTransHops() {
        String path = getPathFromSeed();
        int transCount = 0;
        for(int i=path.length()-1;i>=0;i--) {
            if(path.charAt(i)==Hop.NAVLINK.getHopChar()) {
                break;
            }
            transCount++;
        }
        return transCount;
    }

    /**
     * Given a string containing a URI, then optional whitespace
     * delimited hops-path and via info, create a CandidateURI 
     * instance.
     * 
     * @param uriHopsViaString String with a URI.
     * @return A CandidateURI made from passed <code>uriHopsViaString</code>.
     * @throws URIException
     */
    public static CandidateURI fromString(String uriHopsViaString)
            throws URIException {
        String args[] = uriHopsViaString.split("\\s+");
        String pathFromSeeds = (args.length > 1 && !args[1].equals("-")) ?
                args[1]: "";
        UURI via = (args.length > 2 && !args[2].equals("-")) ?
                UURIFactory.getInstance(args[2]) : null;
        LinkContext viaContext = (args.length > 3 && !args[3].equals("-")) ?
                new HTMLLinkContext(args[2]): null;
        return new CandidateURI(UURIFactory.getInstance(args[0]),
                pathFromSeeds, via, viaContext);
    }
    
    public static CandidateURI createSeedCandidateURI(UURI uuri) {
        CandidateURI c = new CandidateURI(uuri);
        c.setSeed(true);
        return c;
    }
    
    /**
     * Utility method for creation of CandidateURIs found extracting
     * links from this CrawlURI.
     * @param baseUURI BaseUURI for <code>link</code>.
     * @param link Link to wrap CandidateURI in.
     * @return New candidateURI wrapper around <code>link</code>.
     * @throws URIException
     */
    public CandidateURI createCandidateURI(UURI baseUURI, Link link)
    throws URIException {
        UURI u = (link.getDestination() instanceof UURI)?
            (UURI)link.getDestination():
            UURIFactory.getInstance(baseUURI,
                link.getDestination().toString());
        CandidateURI newCaURI = new CandidateURI(u, getPathFromSeed() + link.getHopType().getHopChar(),
                getUURI(), link.getContext());
        newCaURI.inheritFrom(this);
        newCaURI.setStateProvider(manager);
        return newCaURI;
    }

    /**
     * Utility method for creation of CandidateURIs found extracting
     * links from this CrawlURI.
     * @param baseUURI BaseUURI for <code>link</code>.
     * @param link Link to wrap CandidateURI in.
     * @param scheduling How new CandidateURI should be scheduled.
     * @param seed True if this CandidateURI is a seed.
     * @return New candidateURI wrapper around <code>link</code>.
     * @throws URIException
     */
    public CandidateURI createCandidateURI(UURI baseUURI, Link link,
        int scheduling, boolean seed)
    throws URIException {
        final CandidateURI caURI = createCandidateURI(baseUURI, link);
        caURI.setSchedulingDirective(scheduling);
        caURI.setSeed(seed);
        return caURI;
    }
    
    /**
     * Inherit (copy) the relevant keys-values from the ancestor. 
     * 
     * @param ancestor
     */
    protected void inheritFrom(CandidateURI ancestor) {
    	Map<String,Object> adata = ancestor.getData();
    	@SuppressWarnings("unchecked")
    	List<String> heritableKeys = (List<String>)adata.get(A_HERITABLE_KEYS);
    	Map<String,Object> thisData = getData();
        if (heritableKeys != null) {
            for (String key: heritableKeys) {
    	        thisData.put(key, adata.get(key));
    	    }
        }
    }
    
    /**
     * Get the token (usually the hostname + port) which indicates
     * what "class" this CrawlURI should be grouped with,
     * for the purposes of ensuring only one item of the
     * class is processed at once, all items of the class
     * are held for a politeness period, etc.
     *
     * @return Token (usually the hostname) which indicates
     * what "class" this CrawlURI should be grouped with.
     */
    public String getClassKey() {
        return classKey;
    }

    public void setClassKey(String key) {
        classKey = key;
    }

    
    public Map<String,Object> getData() {
    	if (data == null) {
    	    data = new HashMap<String,Object>();
    	}
    	return data;
    }
    
    
    public boolean containsDataKey(String key) {
    	if (data == null) {
    		return false;
    	}
    	return data.containsKey(key);
    }


    /**
     * @return True if this CandidateURI was result of a redirect:
     * i.e. Its parent URI redirected to here, this URI was what was in 
     * the 'Location:' or 'Content-Location:' HTTP Header.
     */
    public boolean isLocation() {
        return this.pathFromSeed != null && this.pathFromSeed.length() > 0 &&
            this.pathFromSeed.charAt(this.pathFromSeed.length() - 1) ==
                Hop.REFER.getHopChar();
    }

    /**
     * Custom serialization writing 'uuri' and 'via' as Strings, rather
     * than the bloated full serialization of their object classes, and 
     * an empty alist as 'null'. Shrinks serialized form by 50% or more
     * in short tests. 
     * 
     * @param stream
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream stream)
        throws IOException {
        stream.defaultWriteObject();
        stream.writeUTF(uuri.toString());
        stream.writeObject((via == null) ? null : via.getURI());
        stream.writeObject((data==null) ? null : data);
    }

    /**
     * Custom deserialization to reconstruct UURI instances from more
     * compact Strings. 
     * 
     * @param stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        uuri = readUuri(stream.readUTF());
        via = readUuri((String)stream.readObject());
        @SuppressWarnings("unchecked")
        Map<String,Object> temp = (Map<String,Object>)stream.readObject();
        this.data = temp;
    }

    /**
     * Read a UURI from a String, handling a null or URIException
     * 
     * @param u String or null from which to create UURI
     * @return the best UURI instance creatable
     */
    protected UURI readUuri(String u) {
        if (u == null) {
            return null;
        }
        try {
            return UURIFactory.getInstance(u);
        } catch (URIException ux) {
            // simply continue to next try
        }
        try {
            // try adding an junk scheme
            return UURIFactory.getInstance("invalid:" + u);
        } catch (URIException ux) {
            ux.printStackTrace();
            // ignored; method continues
        }
        try {
            // return total junk
            return UURIFactory.getInstance("invalid:");
        } catch (URIException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    //
    // Reporter implementation
    //

    public String singleLineReport() {
        return ArchiveUtils.singleLineReport(this);
    }
    
    public void singleLineReportTo(PrintWriter w) {
        String className = this.getClass().getName();
        className = className.substring(className.lastIndexOf(".")+1);
        w.print(className);
        w.print(" ");
        w.print(getUURI().toString());
        w.print(" ");
        w.print(pathFromSeed);
        w.print(" ");
        w.print(flattenVia());
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineLegend()
     */
    public String singleLineLegend() {
        return "className uri hopsPath viaUri";
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Reporter#getReports()
     */
    public String[] getReports() {
        // none but default: empty options
        return new String[] {};
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#reportTo(java.lang.String, java.io.Writer)
     */
    public void reportTo(String name, PrintWriter writer) {
        singleLineReportTo(writer);
        writer.print("\n");
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#reportTo(java.io.Writer)
     */
    public void reportTo(PrintWriter writer) throws IOException {
        reportTo(null,writer);
    }

    /** Make the given key 'heritable', meaning its value will be 
     * added to descendant CandidateURIs. Only keys with immutable
     * values should be made heritable -- the value instance may 
     * be shared until the AList is serialized/deserialized. 
     * 
     * @param key to make heritable
     */
    public void makeHeritable(String key) {
        @SuppressWarnings("unchecked")
        List<String> heritableKeys = (List<String>)data.get(A_HERITABLE_KEYS);
        if (heritableKeys == null) {
            heritableKeys = new ArrayList<String>();
            data.put(A_HERITABLE_KEYS, heritableKeys);
        }
        heritableKeys.add(key);
    }
    
    /** Make the given key non-'heritable', meaning its value will 
     * not be added to descendant CandidateURIs. Only meaningful if
     * key was previously made heritable.  
     * 
     * @param key to make non-heritable
     */
    public void makeNonHeritable(String key) {
        List heritableKeys = (List)data.get(A_HERITABLE_KEYS);
        if(heritableKeys == null) {
            return;
        }
        heritableKeys.remove(key);
        if(heritableKeys.size()==1) {
            // only remaining heritable key is itself; disable completely
            data.remove(A_HERITABLE_KEYS);
        }
    }


    public <T> T get(Object module, Key<T> key) {
        if (provider == null) {
            throw new AssertionError("ToeThread never set up CrawlURI's sheet.");
        }
        return provider.get(module, key);
    }


    public ProcessorURI asProcessorURI() {
        return this; // FIXME
    }


    public String getSourceTag() {
    	return (String)getData().get(A_SOURCE_TAG);
    }
    
    
    public void setSourceTag(String sourceTag) {
    	getData().put(A_SOURCE_TAG, sourceTag);
    	makeHeritable(A_SOURCE_TAG);
    }


    public void setStateProvider(SheetManager manager) {
        this.manager = manager;
        this.provider = manager.findConfig(toString());
    }

    
    public StateProvider getStateProvider() {
        return provider;
    }

    public Collection<String> getAnnotations() {
        // TODO Auto-generated method stub
        return null;
    }

    public UURI getBaseURI() {
        // TODO Auto-generated method stub
        return null;
    }

    public long getContentLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getContentSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getContentType() {
        // TODO Auto-generated method stub
        return null;
    }

    public Set<CredentialAvatar> getCredentialAvatars() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getDNSServerIPLabel() {
        // TODO Auto-generated method stub
        return null;
    }

    public long getFetchBeginTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getFetchCompletedTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getFetchStatus() {
        // TODO Auto-generated method stub
        return 0;
    }

    public FetchType getFetchType() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getFrom() {
        // TODO Auto-generated method stub
        return null;
    }

    public HttpMethod getHttpMethod() {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<Throwable> getNonFatalFailures() {
        @SuppressWarnings("unchecked")
        List<Throwable> list = (List)getData().get(A_LOCALIZED_ERRORS);
        if (list == null) {
                list = new ArrayList<Throwable>();
                getData().put(A_LOCALIZED_ERRORS, list);
        }
        
        // FIXME: Previous code automatically added annotation when "localized error"
        // was added, override collection to implement that?
        return list;
    }

    public Collection<Link> getOutLinks() {
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub
//        return null;
    }

    public Recorder getRecorder() {
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub
    }

    public String getUserAgent() {
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub
//        return null;
    }

    public boolean hasBeenLinkExtracted() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean hasCredentialAvatars() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isPrerequisite() {
        // TODO Auto-generated method stub
        return false;
    }

    public void linkExtractorFinished() {
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub
        
    }

    public void setBaseURI(UURI base) {
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub
        
    }

    public void setContentDigest(String algorithm, byte[] digest) {
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub
        
    }

    public void setContentSize(long size) {
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub
        
    }

    public void setContentType(String mimeType) {
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub
        
    }

    public void setDNSServerIPLabel(String label) {
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub
        
    }

    public void setError(String msg) {
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub
        
    }

    public void setFetchBeginTime(long time) {
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub
        
    }

    public void setFetchCompletedTime(long time) {
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub
        
    }

    public void setFetchStatus(int status) {
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub
        
    }

    public void setFetchType(FetchType type) {
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub
        
    }

    public void setHttpMethod(HttpMethod method) {
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub
        
    }

    public void setPrerequisite(boolean prereq) {
        throw new UnsupportedOperationException();
        // TODO Auto-generated method stub
        
    }




}
