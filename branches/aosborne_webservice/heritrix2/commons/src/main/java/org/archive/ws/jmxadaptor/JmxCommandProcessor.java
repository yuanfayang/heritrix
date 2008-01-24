package org.archive.ws.jmxadaptor;

import javax.management.MBeanServer;

import mx4j.tools.adaptor.http.HttpCommandProcessorAdaptor;

public abstract class JmxCommandProcessor extends HttpCommandProcessorAdaptor {
    protected MBeanNameQuerier nameQuerier;

    @Override
    public void setMBeanServer(MBeanServer server) {
        super.setMBeanServer(server);
        nameQuerier = new MBeanNameQuerier(server);
    }

}
