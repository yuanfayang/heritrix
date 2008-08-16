package org.archive.modules.fetchcache;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import org.archive.modules.ProcessorURI;

public class FetchCache implements Closeable, Serializable {
    private static final long serialVersionUID = 1L;
    
    private static Logger logger = 
        Logger.getLogger(FetchCache.class.getName());
    
    protected Map<String, Collection<String>> resources = null;
    
    protected Map<String, Collection<String>> dependents = null;
    
    protected Map<String, Object> availables = null;
    
    protected Map<String, Collection<String>> candidates = null;
    
    private Map<String, Collection<String>> waitToSchedule = null;
    
    public FetchCache() {
        this(new Hashtable<String, Collection<String>>(),
                new Hashtable<String, Collection<String>>(),
                new Hashtable<String, Object>(),
                new Hashtable<String, Collection<String>>());
    }
    
    // TODO: need to change class Object to class defined for resource location
    public FetchCache(Map<String, Collection<String>> resources,
            Map<String, Collection<String>> dependents,
            Map<String, Object> availables,
            Map<String, Collection<String>> candidates) {
        this.resources = resources;
        this.dependents = dependents;
        this.availables = availables;
        this.candidates = candidates;
    }
    
    public synchronized void removeResource(String urikey) {
        Collection<String> depList = getResourceEntry(urikey);

        if (depList.size() > 0) {
            for (String dependent : depList) {
                Collection<String> unavailResList = 
                    getDependentEntry(dependent);
                unavailResList.remove(urikey);
                
                if (unavailResList.size() == 0) {
                    Collection<String> availResList = getCandidateEntry(dependent);
                    getWaitToScheduleURIs().put(dependent, availResList);
                    dependents.remove(dependent);
                }
            }
        }
        resources.remove(urikey);
    }
    
    /**
     * Update map resources and dependents, when a new resource is available.
     * @param puri the ProcessorURI of this new resource.
     */
    public synchronized void updateResourceStatus(ProcessorURI puri) {
        String urikey = puri.getUURI().toString();
        
        if (isResourceAvailable(urikey)) {
            return;
        }
        
        //addAvailableEntry(urikey, puri.getRecorder());
        try {
        	addAvailableEntry(urikey, 
        			puri.getRecorder().getReplayCharSequence().toString());
        } catch (IOException e) {
            logger.severe("Failed getting ReplayCharSequence: " + 
            		e.getMessage());
        }
        
        Collection<String> depList = getResourceEntry(urikey);
        
        if (depList.size() > 0) {
            for (String dependent : depList) {
                Collection<String> unavailResList = 
                    getDependentEntry(dependent);
                unavailResList.remove(urikey);
                
                Collection<String> availResList = getCandidateEntry(dependent);
                availResList.add(urikey);
                
                if (unavailResList.size() == 0) {
                    getWaitToScheduleURIs().put(dependent, availResList);
                    dependents.remove(dependent);
                }
            }
        }
        resources.remove(urikey);
        
        return;
    }
    /**
     * Update map dependents, candidates and resources, when a new HTML document 
     * is available.
     * @param puri the ProcessorURI of this new HTML document.
     * @param resources the list of required resources of this HTML document.
     */
    public synchronized void updateDependentStatus(ProcessorURI puri, 
            Collection<String> resources) {
        String urikey = puri.getUURI().toString();
        
        if (dependents.containsKey(urikey)) {
            return;
        }
        
        //addAvailableEntry(urikey, puri.getRecorder());
        try {
	        addAvailableEntry(urikey, 
	        		puri.getRecorder().getReplayCharSequence().toString());
        } catch (IOException e) {
            logger.severe("Failed getting ReplayCharSequence: " + 
            		e.getMessage());
        }
        
        Collection<String> unavailResList = new HashSet<String>();
        Collection<String> availResList = new HashSet<String>();
        
        for (String resUri : resources) {
            if (isResourceAvailable(resUri)) {
                availResList.add(resUri);
            } else {
                unavailResList.add(resUri);
                addResourceEntry(resUri, urikey);
            }
        }
        
        if (unavailResList.size() == 0) {
            getWaitToScheduleURIs().put(urikey, availResList);
        } else {
            addDependentEntry(urikey, unavailResList);
            addCandidateEntry(urikey, availResList);
        }
        
        return;
    }
    
    /**
     * Pass the uris of the HTML documents which are ready to be parsed.
     * to other object and clear the map.
     * @return
     */
    public synchronized Map<String, Collection<String>> getNClearReadyURIs() {
        Map<String, Collection<String>> tmpCollection = waitToSchedule;
         waitToSchedule = null;
         return tmpCollection;
    }
    
    public synchronized Object getContentLocation(String urikey) {
        return availables.get(urikey);
    }
    
    public synchronized boolean setContentLocation(String urikey, Object loc) {
        if (loc != null && availables.containsKey(urikey)) {
            availables.put(urikey, loc);
            return true;
        }
        return false;
    }
    
    /**
     * Get uris of HTML documents which depend on the resource.
     * @param urikey uri of the resource.
     * @return a list of dependent uris, if map resources contains urikey, 
     * otherwise an empty list.
     */
    private Collection<String> getResourceEntry(String urikey) {
        Collection<String> depList = resources.get(urikey);
        
        if (depList != null) {
            return depList;
        }
        
        depList = new HashSet<String>();
        resources.put(urikey, depList);
        return depList;
    }
    
    /**
     * Get uris of unavailable resources on which the HTML document depends on.
     * @param urikey uri of the HTML document.
     * @return a list of resource uris, if map dependents contains urikey,
     * otherwise an empty list.
     */
    private Collection<String> getDependentEntry(String urikey) {
        Collection<String> resList = dependents.get(urikey);
        if (resList != null) {
            return resList;
        }
        
        resList = new HashSet<String>();
        dependents.put(urikey, resList);
        return resList;
    }
    
    /**
     * Get uris of available resources on which the HTML document depends on.
     * @param urikey uri of the HTML document.
     * @return a list of resource uris, if map cadidates contains urikey,
     * otherwise an empty list.
     */
    private Collection<String> getCandidateEntry(String urikey) {
        Collection<String> resList = candidates.get(urikey);
        if (resList != null) {
            return resList;
        }
        
        resList = new HashSet<String>();
        candidates.put(urikey, resList);
        return resList;
    }
    
    /**
     * Return or Create a map to store the uris of the HTML documents.
     * which are ready to be parsed.
     * @return a map.
     */
    private Map<String, Collection<String>> getWaitToScheduleURIs() {
        if (waitToSchedule == null) {
            waitToSchedule = new Hashtable<String, Collection<String>>();
        }
        
        return waitToSchedule;
    }
    
    /**
     * Add a new dependent of a resource in map resources.
     * @param urikey the uri of the resource.
     * @param dependent the uri of the dependent.
     */
    private void addResourceEntry(String urikey, String dependent) {
        getResourceEntry(urikey).add(dependent);
    }
    
    /**
     * Add a new entry in map dependents.
     * @param urikey the uri of the HTML document.
     * @param resources the list of requried resources of the HTML document.
     */
    private void addDependentEntry(String urikey, Collection<String> resources) {
        Collection<String> resList = getDependentEntry(urikey);
        if (resList.size() != 0) {
            for (String resUri : resources) {
                if (! resList.contains(resUri)) {
                    resList.add(resUri);
                }
            }
        } else {
            dependents.put(urikey, resources);
        }
    }
    
    /**
     * Add a new entry in map availables.
     * @param urikey the uri of the available document or resource.
     * @param location the content location.
     */
    private void addAvailableEntry(String urikey, Object location) {
        if (! availables.containsKey(urikey)) {
            availables.put(urikey, location);
        }
    }
    
    /**
     * Add an entry in map candidates when an HTML document is ready.
     * @param urikey the uri of the HTML document.
     * @param resources the list of required resources.
     */
    private void addCandidateEntry(String urikey, Collection<String> resources) {
        Collection<String> resList = getCandidateEntry(urikey);
        if (resList.size() != 0) {
            for (String resUri : resources) {
                if (! resList.contains(resUri)) {
                    resList.add(resUri);
                }
            }
        } else {
            candidates.put(urikey, resources);
        }
    }
    
    /**
     * Test is a document or resource is available.
     * @param urikey the uri of the document or resource.
     * @return true if available, otherwise false.
     */
    private boolean isResourceAvailable(String urikey) {
        return availables.containsKey(urikey);
    }

    public void close() {
        
    }
}
