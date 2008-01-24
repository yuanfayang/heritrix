/*
 * Copyright (C) The MX4J Contributors.
 * All rights reserved.
 *
 * This software is distributed under the terms of the MX4J License version 1.0.
 * See the terms of the MX4J License in the documentation provided with this software.
 */

package org.archive.ws.jmxadaptor;

import mx4j.tools.adaptor.http.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * GetAttributeCommandProcessor, processes a request for getting one attribute
 * of a specific MBean. It also support some formats for types like Arrays
 * 
 * @version $Revision: 1.4 $
 */
public class GetAttributeCommandProcessor extends JmxCommandProcessor {
    public GetAttributeCommandProcessor() {
    }

    public Document executeRequest(HttpInputStream in) throws IOException,
            JMException {
        Document document = builder.newDocument();
        Element root = document.createElement("Server");
        document.appendChild(root);
        root.setAttribute("pattern", nameQuerier.patternByHttpVars(in).toString());

        String attributeVariable = in.getVariable("attribute");
        String formatVariable = in.getVariable("format");

        assert attributeVariable != null;

        Set<ObjectName> names = nameQuerier.namesByHttpVars(in);

        for (ObjectName name : names) {
            readAttribute(document, root, name, attributeVariable, formatVariable);
        }
        return document;
    }

    protected Document readAttribute(Document document, Node root, ObjectName objectName,
            String attributeVariable, String formatVariable)
            throws InstanceNotFoundException, IntrospectionException,
            ReflectionException, MBeanException, AttributeNotFoundException {
        MBeanAttributeInfo targetAttribute = findAttribute(objectName,
                attributeVariable);

        if (targetAttribute != null) {
            Element node = document.createElement("MBean");
            root.appendChild(node);

            node.setAttribute("objectname", objectName.toString());
            MBeanInfo info = server.getMBeanInfo(objectName);
            node.setAttribute("classname", info.getClassName());
            node.setAttribute("description", info.getDescription());

            Element attribute = document.createElement("Attribute");
            attribute.setAttribute("name", attributeVariable);
            attribute.setAttribute("classname", targetAttribute.getType());
            Object attributeValue = server.getAttribute(objectName,
                    attributeVariable);
            attribute.setAttribute("isnull", (attributeValue == null) ? "true"
                    : "false");
            node.appendChild(attribute);

            attribute.appendChild(objectToNode(document, attributeValue));       
        }
        return document;
    }

    protected MBeanAttributeInfo findAttribute(ObjectName objectName,
            String attributeVariable) throws InstanceNotFoundException,
            IntrospectionException, ReflectionException {
        MBeanAttributeInfo targetAttribute = null;

        MBeanInfo info = server.getMBeanInfo(objectName);
        MBeanAttributeInfo[] attributes = info.getAttributes();

        if (attributes != null) {
            for (int i = 0; i < attributes.length; i++) {
                if (attributes[i].getName().equals(attributeVariable)) {
                    targetAttribute = attributes[i];
                    break;
                }
            }
        }
        return targetAttribute;
    }
}
