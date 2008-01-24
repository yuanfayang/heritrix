package org.archive.ws.jmxadaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.management.MBeanServer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import mx4j.tools.adaptor.http.HttpCommandProcessorAdaptor;

/**
 * Subclass of HttpCommandProcessorAdaptor that adds some convenience methods for
 * querying object names and serializing objects.
 */
public abstract class JmxCommandProcessor extends HttpCommandProcessorAdaptor {
    protected MBeanNameQuerier nameQuerier;
    private XStream xstream;

    @Override
    public void setMBeanServer(MBeanServer server) {
        super.setMBeanServer(server);
        nameQuerier = new MBeanNameQuerier(server);
    }

    protected XStream getXStream() {
        if (xstream == null) {
            xstream = new XStream(new DomDriver());
            xstream.registerConverter(new CompositeDataConverter());
            xstream.registerConverter(new TabularDataConverter());
            xstream.alias("CompositeData",
                    javax.management.openmbean.CompositeData.class);
            xstream.alias("CompositeData",
                    javax.management.openmbean.CompositeDataSupport.class);
            xstream.alias("TabularData",
                    javax.management.openmbean.TabularDataSupport.class);

        }
        return xstream;
    }

    public Node objectToNode(Document document, Object object) {
        if (object == null) {
            return document.createElement("null");
        }
        try {
            Document valueDoc = builder.parse(new ByteArrayInputStream(getXStream()
                    .toXML(object).getBytes()));
            return document.importNode(valueDoc.getFirstChild(), true);
        } catch (SAXException e) {
            // something went wrong parsing xstream's output.
            e.printStackTrace();
            throw new RuntimeException("Exception in XStream/JDOM", e);
        } catch (IOException e) {
            // something went wrong handling the text
            throw new RuntimeException("Possible character encoding problem", e);
        }
    }

}
