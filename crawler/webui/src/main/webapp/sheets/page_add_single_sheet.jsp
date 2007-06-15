<%@ page import="java.util.Collection" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
String error = (String)request.getAttribute("error");

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Add New Single Sheet</h3>

Enter a name for the new single sheet below.<br/>

<form class="nospace" method="post" action="do_add_single_sheet.jsp">
<% Text.printJobFormFields(request, out); %>

<input type="text" name="sheet" value=""/><br/>
<input type="submit" value="Submit"></form><form class="nospace"
 method="get" action="do_show_sheets.jsp"><input 
   type="submit" value="Cancel"><% Text.printJobFormFields(request, out); %>
   </form>


</body>
</html>