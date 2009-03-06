<%@ page pageEncoding="UTF-8" %> 
<%@ page import="java.util.Collection" %>
<%@ page import="javax.management.openmbean.CompositeData" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
String sheet = (String)Text.get(request, "sheet");

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Delete <%=Text.html(sheet)%>?</h3>

You will not be able to restore it.

<table>
<tr>
<td>
 <form method="get" action="do_show_sheets.jsp" accept-charset='UTF-8'>
 <% Text.printJobFormFields(request, out); %>
 <input type="submit" value="Cancel">
 </form>
</td>
<td>
 <form method="post" action="do_delete_sheet.jsp" accept-charset='UTF-8'>
 <% Text.printSheetFormFields(request, out); %>
 <input type="submit" value="Delete">
 </form>
</td>
</tr>
</table>

</body>
</html>
