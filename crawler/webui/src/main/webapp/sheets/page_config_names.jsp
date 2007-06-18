<%@ page import="java.util.Map" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
String url = (String)Text.get(request, "sheet");
Map<String,String> map = (Map)Text.get(request, "surtToSheet");
int row = 1;

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Sheets for "<%=url%>" Settings:</h3>

<table class="info">

<% for (Map.Entry<String,String> me: map.entrySet()) { %>

<% 

   String surt = me.getKey();
   String sheet = me.getValue();
   
%>

<tr>
<td class="info<%=row%>">
<%=Text.html(surt) %>
</td>
<td class="info<%=row%>">
  <% if (sheet != null) { %>
  <%=Text.html(sheet) %>
  <% } %>
</td>

</tr>


<% } %>


</table>


</body>
</html>
