package org.archive.hcc.util;

import java.net.InetSocketAddress;

import javax.management.ObjectName;

public class JmxUtils {
    public static InetSocketAddress extractRemoteAddress(final ObjectName name) {
        return new InetSocketAddress(name.getKeyProperty("remoteHost"),
            Integer.parseInt(name.getKeyProperty("remoteJmxPort")));
    }
    
}
