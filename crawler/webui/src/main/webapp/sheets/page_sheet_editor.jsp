<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Map" %>
<%@ page import="javax.management.openmbean.CompositeData" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
Collection<CompositeData> settings = (Collection)Text.get(request, "settings");
Map<String,String> problems = (Map)Text.get(request, "problems");
int row = 1;
String previousPath = ":";

String error = null;

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
    String[] sheets = (String[])setting.get("sheets");
    row = -row + 1;
%>
<tr>
<td class="info<%=row%>">
  <a href=""></a>

 Details | Remove
</td>
<td class="info<%=row%>">
 <% for (String sheet: sheets) { %>
   <%=Text.html(sheet)%>,
 <% } %>
</td>
<td class="info<%=row%>">
 <%=Text.html(path)%>
 <% error = problems.get(path); if (error != null) { %>
   <br/>
   <font color="red"><%=Text.html(error)%></font>
 <% } %>
 
</td>
<td class="info<%=row%>">
 <input type="text" name="<%=Text.attr(path)%>" value="<%=Text.attr(value)%>"
</td>
</tr>
<% } %> 

</body>
</html>