<%@ page pageEncoding="UTF-8" %> 
<%@ page import="org.archive.util.ArchiveUtils"%>
<%@ page import="java.util.Properties"%>
<%@ page import="java.lang.management.ManagementFactory"%>
<%@ page import="java.lang.management.RuntimeMXBean"%>
<%@ page import="java.lang.management.OperatingSystemMXBean"%>


<html>
<head>
    <%@include file="/include/header.jsp"%>
    <title>Heritrix About</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<%
RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
%>

<div class="margined">

<h1>About Heritrix (Web UI)</h1>

<p>The information on this page refers to this web UI part of Heritrix running on <%=request.getRemoteHost() %>.</p>
        
    <fieldset><legend>Heritrix</legend>
	Heritrix <%=ArchiveUtils.VERSION%><br/>
    </fieldset>
    <br/>

    <fieldset><legend>Java Virtual Machine</legend>
	<%=System.getProperties().getProperty("java.runtime.name") %>
	<%=System.getProperties().getProperty("java.vm.version") %><br/>
    Vendor: <%=System.getProperties().getProperty("java.vm.vendor")%><br/>
    Started: <%=ArchiveUtils.getLog14Date(runtime.getStartTime()) %><br/>
    Uptime: <%=ArchiveUtils.formatMillisecondsToConventional(runtime.getUptime()) %><br/>
    
    Input arguments:
    <pre><% 
        String[] args = runtime.getInputArguments().toArray(new String[0]);
        for(String arg : args){
            out.println("   " + arg); 
        }
    %></pre>

    System properties:
    <pre><%
        Properties systemProperties = System.getProperties();
        for(Object key : systemProperties.keySet()){
            out.println("   "+key+"="+systemProperties.get(key));
        }
    %></pre>
    
    <br/>
    </fieldset>
    <br/>

    <fieldset><legend>Operating system</legend>
	<%=System.getProperties().getProperty("os.name") %>
	<%=System.getProperties().getProperty("os.version") %><br/>
    Architecture: <%=systemProperties.get("os.arch")  %><br/>
    Available processors: <%=os.getAvailableProcessors() %><br/>
    </fieldset>
    <br/>
    <fieldset><legend>License</legend>
    
    <pre> Copyright (C) 2003-2006 Internet Archive.

Heritrix is free software; you can redistribute it and/or modify
it under the terms of the GNU Lesser Public License as published by
the Free Software Foundation; either version 2.1 of the License, or
any later version.

Heritrix is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser Public License for more details.

You should have received a copy of the GNU Lesser Public License
along with Heritrix; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

Heritrix contains many other free and open source libraries; they are
governed by their respective licenses.</pre>
    </fieldset>
    <br/>

</div>

For more information, see 
<a href="http://crawler.archive.org">crawler.archive.org</a><br/>
<br/>

</body>
</html>

