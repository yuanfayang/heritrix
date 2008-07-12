package org.archive.spring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class PathSharingContext extends FileSystemXmlApplicationContext {

    public PathSharingContext(String configLocation) throws BeansException {
        super(configLocation);
        // TODO Auto-generated constructor stub
    }

    public PathSharingContext(String[] configLocations, ApplicationContext parent) throws BeansException {
        super(configLocations, parent);
        // TODO Auto-generated constructor stub
    }

    public PathSharingContext(String[] configLocations, boolean refresh, ApplicationContext parent) throws BeansException {
        super(configLocations, refresh, parent);
        // TODO Auto-generated constructor stub
    }

    public PathSharingContext(String[] configLocations, boolean refresh) throws BeansException {
        super(configLocations, refresh);
        // TODO Auto-generated constructor stub
    }

    public PathSharingContext(String[] configLocations) throws BeansException {
        super(configLocations);
        // TODO Auto-generated constructor stub
    }

    public String getPrimaryConfigurationPath() {
        // TODO Auto-generated method stub
        return getConfigLocations()[0];
    }

    @Override
    public void start() {
        Map lifecycleBeans = getLifecycleBeans();
        for (Iterator it = new HashSet(lifecycleBeans.keySet()).iterator(); it.hasNext();) {
            String beanName = (String) it.next();
            doStart(lifecycleBeans, beanName);
        }
        publishEvent(new ContextStartedEvent(this));
    }
    
    protected void doStart(Map lifecycleBeans, String beanName) {
        Lifecycle bean = (Lifecycle) lifecycleBeans.remove(beanName);
        if (bean != null) {
            String[] dependenciesForBean = getBeanFactory().getDependenciesForBean(beanName);
            for (int i = 0; i < dependenciesForBean.length; i++) {
                doStart(lifecycleBeans, dependenciesForBean[i]);
            }
            if (!bean.isRunning()) {
                bean.start();
            }
            //lifecycleBeans.remove(beanName);
        }
    }
    
    public void stop() {
        Map lifecycleBeans = getLifecycleBeans();
        for (Iterator it = new HashSet(lifecycleBeans.keySet()).iterator(); it.hasNext();) {
            String beanName = (String) it.next();
            doStop(lifecycleBeans, beanName);
        }
        publishEvent(new ContextStoppedEvent(this));
    }
    
    protected void doStop(Map lifecycleBeans, String beanName) {
        Lifecycle bean = (Lifecycle) lifecycleBeans.remove(beanName);
        if (bean != null) {
            String[] dependentBeans = getBeanFactory().getDependentBeans(beanName);
            for (int i = 0; i < dependentBeans.length; i++) {
                doStop(lifecycleBeans, dependentBeans[i]);
            }
            if (bean.isRunning()) {
                bean.stop();
            }
            //lifecycleBeans.remove(beanName);
        }
    }

    
    
    protected Map getLifecycleBeans() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        String[] beanNames = beanFactory.getBeanNamesForType(Lifecycle.class, false, false);
        Map beans = new HashMap(beanNames.length);
        for (int i = 0; i < beanNames.length; i++) {
            Object bean = beanFactory.getSingleton(beanNames[i]);
            if (bean != null) {
                beans.put(beanNames[i], bean);
            }
        }
        return beans;
    }

    
}
