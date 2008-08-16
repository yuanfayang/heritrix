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

public class DOMUtil {
    private static Logger logger =
        Logger.getLogger(DOMUtil.class.getName());
    
    private DOMUtil() {
    }

    public static void saveHtml(Document document, String name) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            //transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//W3C//DTD HTML 4.0 Transitional//EN");
            DOMSource dSource = new DOMSource(document);
            FileOutputStream fos = new FileOutputStream(name);
            Result out = new StreamResult(fos);
            transformer.transform(dSource, out);
            //System.out.println(fos.getFD().toString());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
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