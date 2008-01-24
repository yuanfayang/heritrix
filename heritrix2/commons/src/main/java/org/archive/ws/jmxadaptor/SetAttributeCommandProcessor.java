/*
 * Copyright (C) The MX4J Contributors.
 * All rights reserved.
 *
 * This software is distributed under the terms of the MX4J License version 1.0.
 * See the terms of the MX4J License in the documentation provided with this software.
 */
package org.archive.ws.jmxadaptor;

import java.io.IOException;
import java.util.Set;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import mx4j.tools.adaptor.http.HttpInputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * SetAttributeCommandProcessor, processes a request for setting one attribute
 * in one MBean
 * 
 * @version $Revision: 1.3 $
 */
public class SetAttributeCommandProcessor extends JmxCommandProcessor {

    public SetAttributeCommandProcessor() {
    }

    public Document executeRequest(HttpInputStream in) throws IOException,
            JMException {
        Document document = builder.newDocument();

        Element root = document.createElement("MBeanSetAttribute");
        document.appendChild(root);
        
        String attributeVariable = in.getVariable("attribute");
        String valueVariable = in.getVariable("value");
        
        if (attributeVariable == null || attributeVariable.equals("")
                || valueVariable == null) {
            root.setAttribute("result", "error");
            root.setAttribute("errorMsg",
                    "Incorrect parameters in the request");
            return document;
        }

        root.setAttribute("pattern", nameQuerier.patternByHttpVars(
                in).toString());

        Set<ObjectName> names = this.nameQuerier.namesByHttpVars(in);

        for (ObjectName objectName : names) {
            setAttribute(document, root, objectName, attributeVariable, valueVariable);
        }

        return document;
    }

    protected void setAttribute(Document document,
            Element operationElement, ObjectName objectName,
            String attributeVariable, String valueVariable) throws InstanceNotFoundException,
            IntrospectionException, ReflectionException {
        
        Element node = document.createElement("MBean");
        operationElement.appendChild(node);
        
        node.setAttribute("objectname", objectName.toString());
        node.setAttribute("attribute", attributeVariable);
        
        if (server.isRegistered(objectName)) {    
            MBeanInfo info = server.getMBeanInfo(objectName);
            node.setAttribute("classname", info.getClassName());
            node.setAttribute("description", info.getDescription());
            
            MBeanAttributeInfo[] attributes = info.getAttributes();
            MBeanAttributeInfo targetAttribute = null;
            if (attributes != null) {
                for (int i = 0; i < attributes.length; i++) {
                    if (attributes[i].getName().equals(attributeVariable)) {
                        targetAttribute = attributes[i];
                        break;
                    }
                }
            }
            if (targetAttribute != null) {
                String type = targetAttribute.getType();
                Object value = null;
                if (valueVariable != null) {
                    try {
                        value = CommandProcessorUtil.createParameterValue(type,
                                valueVariable);
                    } catch (Exception e) {
                        node.setAttribute("result", "error");
                        node.setAttribute("errorMsg", "Value: "
                                + valueVariable + " could not be converted to "
                                + type);
                    }
                    if (value != null) {
                        try {
                            server.setAttribute(objectName, new Attribute(
                                    attributeVariable, value));
                            node.setAttribute("result", "success");
                        } catch (Exception e) {
                            node.setAttribute("result", "error");
                            node.setAttribute("errorMsg", e
                                    .getMessage());
                        }
                    }
                }
            } else {
                node.setAttribute("result", "error");
                node.setAttribute("errorMsg", "Attribute "
                        + attributeVariable + " not found");
            }
        } else {
            if (objectName != null) {
                node.setAttribute("result", "error");
                node.setAttribute("errorMsg", "MBean " + objectName
                        + " not registered");
            }
        }
    }

}
