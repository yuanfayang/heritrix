package org.archive.modules.fetchcache;

import java.io.Closeable;
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
    
    /*public void updateResourceStatus(ProcessorURI puri) {
        
        // TODO: the second param should be resource location object
        String uriStr = puri.getUURI().toString();
        addAvailableEntry(uriStr, puri);
        updateResourceStatus(uriStr);
    }*/
    
    //public synchronized void updateResourceStatus(String urikey) {
    public synchronized void updateResourceStatus(ProcessorURI puri) {
        String urikey = puri.getUURI().toString();
        
        if (isResourceAvailable(urikey)) {
            return;
        }
        
        addAvailableEntry(urikey, puri.getRecorder());
        
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
    
    /*public void updateDependentStatus(ProcessorURI puri,
            Collection<String> resources) {
        String uriStr = puri.getUURI().toString();
        addAvailableEntry(uriStr, puri);
        updateDependentStatus(uriStr, resources);
    }*/
    
    //public synchronized void updateDependentStatus(String urikey, 
            //Collection<String> resources) {
    public synchronized void updateDependentStatus(ProcessorURI puri, 
            Collection<String> resources) {
        String urikey = puri.getUURI().toString();
        
        if (dependents.containsKey(urikey)) {
            return;
        }
        
        addAvailableEntry(urikey, puri.getRecorder());
        
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
    
    public Collection<String> getResourceEntry(String urikey) {
        Collection<String> depList = resources.get(urikey);
        
        if (depList != null) {
            return depList;
        }
        
        depList = new HashSet<String>();
        resources.put(urikey, depList);
        return depList;
    }
    
    public Collection<String> getDependentEntry(String urikey) {
        Collection<String> resList = dependents.get(urikey);
        if (resList != null) {
            return resList;
        }
        
        resList = new HashSet<String>();
        dependents.put(urikey, resList);
        return resList;
    }
    
    public Collection<String> getCandidateEntry(String urikey) {
        Collection<String> resList = candidates.get(urikey);
        if (resList != null) {
            return resList;
        }
        
        resList = new HashSet<String>();
        candidates.put(urikey, resList);
        return resList;
    }
    
    public Map<String, Collection<String>> getWaitToScheduleURIs() {
        if (waitToSchedule == null) {
            waitToSchedule = new Hashtable<String, Collection<String>>();
        }
        
        return waitToSchedule;
    }
    
    public synchronized Map<String, Collection<String>> getNClearReadyURIs() {
        Map<String, Collection<String>> tmpCollection = waitToSchedule;
         waitToSchedule = null;
         return tmpCollection;
    }
    
    public Object getContentLocation(String urikey) {
        return availables.get(urikey);
    }
    
    protected void addResourceEntry(String urikey, String dependent) {
        getResourceEntry(urikey).add(dependent);
    }
    
    protected void addDependentEntry(String urikey, Collection<String> resources) {
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
    
    protected void addAvailableEntry(String urikey, Object location) {
        if (! availables.containsKey(urikey)) {
            availables.put(urikey, location);
        }
    }
    
    protected void addCandidateEntry(String urikey, Collection<String> resources) {
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
    
    public boolean isResourceAvailable(String urikey) {
        return availables.containsKey(urikey);
    }

    private String magicValue = "This is a magic value for testing!";
    public void test() {
        System.out.println(magicValue);
    }
    
    public void close() {
        
    }
}
