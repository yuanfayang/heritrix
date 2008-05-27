<%@ page import="java.util.List" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%@ page import="org.archive.settings.Association" %>

<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
String url = (String)Text.get(request, "sheet");
List<Association> list = (List)Text.get(request, "surtToSheet");
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

<% for (Association a: list) { %>

<% 

   String surt = a.getContext();
   String sheet = a.getSheetName();
   
%>

<tr>
<td class="info<%=row%>">
<code><%=Text.html(surt) %></code>
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
