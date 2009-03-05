<%@ page pageEncoding="UTF-8" %> 
<%@ page import="java.util.List" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
CrawlJob job = (CrawlJob)Text.get(request, "job");

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix Copy Job</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Delete <%=Text.html(job.getName())%>?</h3>

You will not be able to restore it.

<table>
<tr>
<td>
 <form method="get" action="do_show_crawler.jsp" accept-charset='UTF-8'>
 <% crawler.printFormFields(out); %>
 <input type="submit" value="Cancel">
 </form>
</td>
<td>
 <form method="post" action="do_delete.jsp" accept-charset='UTF-8'>
 <% Text.printJobFormFields(request, out); %>
 <input type="submit" value="Delete">
 </form>
</td>
</tr>
</table>

</body>
</html>
