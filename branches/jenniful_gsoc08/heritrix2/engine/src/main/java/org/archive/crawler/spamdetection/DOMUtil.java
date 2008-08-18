package org.archive.crawler.spamdetection;

import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

/**
 * This class provides some methods to transform a DOM to other forms.
 * @author Ping Wang
 *
 */
public class DOMUtil {
    private static Logger logger =
        Logger.getLogger(DOMUtil.class.getName());
    
    private DOMUtil() {
    }

    /**
     * Transform a DOM to an HTML document and save it.
     * @param document DOM
     * @param name the name of the file you want to save the HTML document as.
     */
    public static void saveHtml(Document document, String name) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            DOMSource dSource = new DOMSource(document);
            FileOutputStream fos = new FileOutputStream(name);
            Result out = new StreamResult(fos);
            transformer.transform(dSource, out);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Transform a DOM to a string.
     * @param document DOM
     * @return the resulting string
     */
    public static String transfomToString(Document document) {
        StringWriter output = new StringWriter();
        try {
            output = new StringWriter();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(document), new StreamResult(output));
            
        } catch(Exception e) {
            e.printStackTrace();
        }

        return output.toString();
    }
}