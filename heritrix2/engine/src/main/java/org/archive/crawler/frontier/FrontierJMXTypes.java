/**
 * 
 */
package org.archive.crawler.frontier;

import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.archive.settings.jmx.Types;

/**
 * @author pjack
 *
 */
public class FrontierJMXTypes {

    
    final public static CompositeType URI_LIST_DATA;

    
    static {
        try {
            URI_LIST_DATA = new CompositeType(
                    "uri_list_data", 
                    "A list of URIs produced by a frontier.  Includes marker so " +
                    "the next segment of the list can be queried.",
                    new String[] { "list", "marker" },
                    new String[] { 
                        "The list of URIs.",
                        "The marker that can be used to fetch the next " +
                              "URIs in the list."
                    },
                    new OpenType[] {
                            Types.STRING_ARRAY,
                            SimpleType.STRING
                    });
        } catch (OpenDataException e) {
            throw new IllegalStateException(e);
        }
    }
    
    
    static String toString(byte[] marker) {
        if (marker == null) {
            return "null";
        }
        if (marker.length == 0) {
            return "";
        }
        StringBuilder r = new StringBuilder();
        r.append(marker[0]);
        for (int i = 1; i < marker.length; i++) {
            r.append(',').append(marker[i]);
        }
        return r.toString();
    }

    
    static byte[] fromString(String marker) {
        if (marker == null || marker.equals("null")) {
            return null;
        }
        String[] tokens = marker.split(",");
        byte[] r = new byte[tokens.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = Byte.parseByte(tokens[i]);
        }
        return r;
    }


}
