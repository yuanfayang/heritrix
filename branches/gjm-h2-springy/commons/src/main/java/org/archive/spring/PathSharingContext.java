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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Spring ApplicationContext extended for Heritrix use. 
 * 
 * Notable extensions:
 * 
 * Remembers its primary XML configuration file, and can report its
 * filesystem path. 
 * 
 * Propagates lifecycle events (start, stop) without triggering 
 * loops in the case of circular dependencies.
 * 
 * Reports a summary of Errors collected from self-Validating Beans.
 * 
 * @contributor gojomo
 */
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

    @SuppressWarnings("unchecked")
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
    
    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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

    //
    // Cascading self-validation
    //
    
    Errors errors = null;
    public void validate() {
        errors = new BeanPropertyBindingResult(this,"");
        for(Object entry : getBeansOfType(HasValidator.class).entrySet()) {
            // String name = (String) ((Map.Entry)entry).getKey();
            HasValidator hv = (HasValidator) ((Map.Entry)entry).getValue();
            Validator v = hv.getValidator();
            Errors tempErrors = new BeanPropertyBindingResult(hv,"");
            v.validate(hv, tempErrors);
            errors.addAllErrors(tempErrors);
        }
        System.err.println("===errors===");
        for(Object err : errors.getAllErrors()) {
            System.err.println(err);
        }
        System.err.println("============");
    }

    public Errors getErrors() {
        return errors;
    }
    
}
