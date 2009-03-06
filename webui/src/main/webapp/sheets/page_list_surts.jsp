<%@ page pageEncoding="UTF-8" %> 
<%@ page import="java.util.Collection" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
String sheet = (String)Text.get(request, "sheet");
Collection<String> surts = (Collection)Text.get(request, "surts");
int row = (Integer)Text.get(request, "start");

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Surt Prefixes Associated with <%=Text.html(sheet)%>:</h3>

<code>
<% for (String surt: surts) { %>

<%=Text.html(surt)%><br/>

<% row++; %>
<% } %>
</code>

</body>
</html>
