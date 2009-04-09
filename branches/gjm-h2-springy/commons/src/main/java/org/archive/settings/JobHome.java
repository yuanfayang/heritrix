/*
 *  This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.archive.settings;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.archive.spring.ConfigPath;
import org.archive.spring.PathSharingContext;
import org.archive.spring.ReadSource;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * JobHome represents certain once-per-job information, such as job
 * name and home directory. As a BeanPostProcessor, it also fixes 
 * any configured beans' ConfigPath instances with empty base paths
 * to use the job's base path. 
 * 
 * TODO: rename or merge with something else? it's become an central 
 * part of any spring-configured crawl, but seems like functionality
 * should live elsewhere.
 */
public class JobHome implements ApplicationContextAware, BeanPostProcessor {
    
    //// BEAN PROPERTIES
    
    String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    ConfigPath path; 
    public ConfigPath getPath() {
        return path;
    }
    public void setPath(ConfigPath p) {
        path = p; 
    }
    
    //// APPLICATIONCONTEXTAWARE IMPLEMENTATION

    ApplicationContext appCtx;
    /**
     * Remember ApplicationContext, and if possible primary 
     * configuration file's home directory. 
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext(ApplicationContext appCtx) throws BeansException {
        this.appCtx = appCtx;
        String basePath;
        if(appCtx instanceof PathSharingContext) {
            File configFile = new File(
                    ((PathSharingContext)appCtx).getPrimaryConfigurationPath());
            basePath = configFile.getParent();
        } else {
            basePath = ".";
        }
        path = new ConfigPath("job base",basePath); 
    }
    
    //// BEANPOSTPROCESSOR IMPLEMENTATION
    
    /**
     * Fix all beans with ConfigPath properties that lack a base path
     * or a name, to use a job-implied base path and name. 
     * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object, java.lang.String)
     */
    public Object postProcessAfterInitialization(Object bean, String beanName)
    throws BeansException {
        fixupPaths(bean, beanName);
        return bean;
        
    }
    
    protected Object fixupPaths(Object bean, String beanName) {
        BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);
        for(PropertyDescriptor d : wrapper.getPropertyDescriptors()) {
            if(ConfigPath.class.isAssignableFrom(d.getPropertyType())
                || ReadSource.class.isAssignableFrom(d.getPropertyType())) {
                Object value = wrapper.getPropertyValue(d.getName());
                if(ConfigPath.class.isInstance(value)) {
                    ConfigPath cp = (ConfigPath) value;
                    if(cp==null) {
                        continue;
                    }
                    if(cp.getBase()==null) {
                        cp.setBase(path);
                    }
                    if(StringUtils.isEmpty(cp.getName())) {
                        cp.setName(beanName+"."+d.getName());
                    }
                    remember(cp);
                }
            }
        }
        return bean;
    }

    // REMEBERED PATHS
    Map<String,ConfigPath> paths = new HashMap<String,ConfigPath>();
    protected void remember(ConfigPath cp) {
        paths.put(cp.getName(), cp);
    }
    public Map<String,ConfigPath> getPaths() {
        return paths; 
    }
    
    // noop
    public Object postProcessBeforeInitialization(Object bean, String beanName) 
    throws BeansException {
        return bean;
    }
}
