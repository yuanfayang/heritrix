<%@ page pageEncoding="UTF-8" %> 
<%@ page import="java.util.Collection" %>
<%@ page import="javax.management.openmbean.CompositeData" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
Collection<CompositeData> settings = (Collection)Text.get(request, "settings");
int row = 1;
String previousPath = ":";
boolean showSheets = true;
%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Settings:</h3>


<table class="info">
<% 

for (CompositeData setting: settings) {
    String path = (String)setting.get("path");
    String value = (String)setting.get("value");
    String type = (String)setting.get("type");
    String[] sheets = (String[])setting.get("sheets");
    row = -row + 1;
%>

<% if (showSheets) { %>
<tr>
<td class="info<%=row%>">
<%=Text.html(sheets[0])%>
<% for (int i = 1; i < sheets.length; i++) { %>
, <%=Text.html(sheets[i])%>

<% } %>

</td>


<% } %>


<td class="info<%=row%>">
<%=Text.html(path)%>
</td>

<td class="info<%=row%>">
<%=Text.html(type)%>
</td>

<td class="info<%=row%>">
<%=Text.html(value)%>
</td>

</tr>
<% } // for %> 

</table>


</body>
</html>
