package org.archive.spring;
import java.util.HashSet;
import java.util.Map;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;


/**
 * Collection of overrides. 
 *
 */
public class Sheet implements BeanFactoryAware {
    private static final long serialVersionUID = 9129011082185864377L;
    
    BeanFactory beanFactory; 
    Map<String,Object> map; 
    
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public void setMap(Map<String, Object> m) {
        this.map = m;
    }


    /**
     * Ensure any properties targetted by this Sheet know to 
     * check the right property paths for overrides at lookup time.
     * 
     * Should be done before a map of overrides is 'pushed' into
     * the contextual stack. TODO: determine if this should be 
     * automatic on push. TODO: consider if an 'un-priming' 
     * also needs to occur to prevent confusing side-effects. 
     */
    public void prime() {
        HashSet<String> addressed = new HashSet<String>();
        for (String path : map.keySet()) {
            addressed.add(path.substring(0,path.lastIndexOf(".")));
        }
        for (String path : addressed) {
            int i = path.indexOf(".");
            HasKeyedProperties hkp;
            if (i < 0) {
                hkp = (HasKeyedProperties) beanFactory.getBean(path);
            } else {
                String beanName = path.substring(0,i);
                String propPath = path.substring(i+1);
                hkp = (HasKeyedProperties) (new BeanWrapperImpl(beanFactory.getBean(beanName)).getPropertyValue(propPath));
            }
            hkp.getKeyedProperties().addExternalPath(path);
        }
    }
}
