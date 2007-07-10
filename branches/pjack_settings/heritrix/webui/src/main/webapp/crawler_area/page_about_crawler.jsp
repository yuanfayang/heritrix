<%@ page import="javax.management.openmbean.TabularData"%>
<%@ page import="javax.management.openmbean.CompositeData"%>
<%@ page import="java.util.Collection"%>
<%@ page import="org.archive.crawler.webui.Crawler"%>
<%@ page import="java.util.SortedMap"%>
<%@ page import="java.util.TreeMap"%>
<%@ page import="org.archive.util.ArchiveUtils"%>
<%@ page import="java.lang.management.OperatingSystemMXBean"%>

<%
    Crawler crawler = (Crawler)request.getAttribute("crawler");
    
    TabularData properties = (TabularData)request.getAttribute("system.properties");
    SortedMap<String, String> systemProperties = new TreeMap<String, String>();
    for(CompositeData property : (Collection<CompositeData>)properties.values()){
        systemProperties.put((String)property.get("key"),(String)property.get("value"));
    }
    
%>

<html>
<head>
    <%@include file="/include/header.jsp"%>
    <title>Heritrix About</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<div class="margined">

<h1>About Heritrix (<%=crawler.getLegend() %>)</h1>

<p>The information on this page refers to an instance of the Heritrix crawler running on <%=crawler.getLegend() %>.</p>
        
	<fieldset><legend>Heritrix</legend>
	Heritrix <%=request.getAttribute("heritrix.version")%><br/>
    </fieldset>
    <br/>

    <fieldset><legend>Java Virtual Machine</legend>
	<%=systemProperties.get("java.runtime.name") %>
	<%=systemProperties.get("java.vm.version") %><br/>
    Vendor: <%=systemProperties.get("java.vm.vendor")%><br/>
    Started: <%=ArchiveUtils.getLog14Date((Long)request.getAttribute("runtime.starttime")) %><br/>
    Uptime: <%=ArchiveUtils.formatMillisecondsToConventional((Long)request.getAttribute("runtime.uptime")) %><br/>
    Input arguments:
    <pre><% 
        String[] args = (String[])request.getAttribute("runtime.inputarguments");
        for(String arg : args){
            out.println("   " + arg); 
        }
    %></pre>
    
    System properties:
    <pre><%
        for(String key : systemProperties.keySet()){
            out.println("   "+key+"="+systemProperties.get(key));
        }
    %></pre>
    
    <br/>
    </fieldset>
    <br/>

    <fieldset><legend>Operating system</legend>
	<%=systemProperties.get("os.name") %>
	<%=systemProperties.get("os.version") %><br/>
    Architecture: <%=systemProperties.get("os.arch")  %><br/>
    Available processors: <%=request.getAttribute("os.availableprocessors") %><br/>
	</fieldset>
	<br/>
    <fieldset><legend>License</legend>
    
    <pre>Copyright (C) 2003-2006 Internet Archive.

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