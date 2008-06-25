package org.archive.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
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

}
