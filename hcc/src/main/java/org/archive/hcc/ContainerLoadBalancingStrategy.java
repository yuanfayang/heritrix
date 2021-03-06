package org.archive.hcc;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ContainerLoadBalancingStrategy {
    /**
     * Returns a list of partially loaded (ie available) containers sorted least loaded host, least loaded container. If no
     * containers are available, returns empty list.
     */

	public List<Container> prioritize(List<Container> containers){
        List<Container>leastLoaded = new LinkedList<Container>();
        List<Container> currentContainers = new LinkedList<Container>(containers);

        //count instances per host
        final Map<String,Integer> instanceCountByHostMap = new HashMap<String,Integer>();
        for (Container n : currentContainers) {
        	String hostName = n.getAddress().getHostName();
        	Integer count = instanceCountByHostMap.get(hostName);
        	if(count == null){
        		count = 0;
        	}

        	count = count + n.getCrawlers().size();
        	instanceCountByHostMap.put(hostName, count);
        }
        
        //filter all containers that already context the max number of instances.
        for (Container n : currentContainers) {
            if (n.getCrawlers().size() >= n.getMaxInstances()) {
                continue;
            }
            
            leastLoaded.add(n);
        }
        
        
        //sort the leastLoaded collection by least loaded host and then least loaded container
        Collections.sort(leastLoaded, new Comparator<Container>(){
        	public int compare(Container c1, Container c2) {
        		Integer hostCount1 = instanceCountByHostMap.get(c1.getAddress().getHostName());
        		Integer hostCount2 = instanceCountByHostMap.get(c2.getAddress().getHostName());
        		if(hostCount1.equals(hostCount2)){
            		return new Integer(c1.getCrawlers().size()).compareTo(new Integer(c2.getCrawlers().size()));
        		}else{
        			return hostCount1.compareTo(hostCount2);
        		}
        	}
        });
        
        
        
        return leastLoaded;

	}
}
