<%@ page pageEncoding="UTF-8" %> 
<%@ page import="java.util.Collection" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
String error = (String)request.getAttribute("error");
boolean single = (Boolean)request.getAttribute("single");

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Add New Settings Sheet <%= single ? "" : "Bundle" %></h3>

Enter a name for the new Settings Sheet <%= single ? "" : "Bundle" %> below.<br/>

<form class="nospace" method="post" action="<%= single ? "do_add_single_sheet.jsp" : "do_add_sheet_bundle.jsp"%>" accept-charset='UTF-8'>
<% Text.printJobFormFields(request, out); %>
<input type="hidden" name="single" value="<%=single%>">

<input type="text" name="sheet" value=""><br>
<input type="submit" value="Submit"></form>

</body>
</html>
