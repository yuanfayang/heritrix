<%@ page import="java.util.Collection" %>
<%@ page import="javax.management.openmbean.CompositeData" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
Collection<CompositeData> settings = (Collection)Text.get(request, "settings");
int row = 1;
String previousPath = ":";

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Settings:</h3>

<ul>
<% 

for (CompositeData setting: settings) {
    String path = (String)setting.get("path");
    String value = (String)setting.get("value");
    int p = path.lastIndexOf('.');
    String base = (p < 0) ? path : path.substring(p + 1);
        
    String parent = Text.parentPath(path);
    String previousParent = Text.parentPath(previousPath);
    int count = Text.countSegments(path);
    int previousCount = Text.countSegments(previousPath);
    if (parent.equals(previousParent)) {
        if (count > previousCount) {
           for (int i = 0; i < count - previousCount; i++) {
               out.println("<ul>\n");
           }
        }
        if (count < previousCount) {
           for (int i = 0; i < previousCount - count; i++) {
               out.println("</ul>\n");
           }
        }
    } else {
        out.println("</ul>\n");    
    }
%>
<li><b title="<%=path%>"><%=Text.html(base)%></b></br>
<%=Text.html(value)%></li>
<% 
    previousPath = path;
} 
%>
</ul>

</body>
</html>