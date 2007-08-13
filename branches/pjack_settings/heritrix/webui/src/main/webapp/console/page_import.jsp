<%@ page import="org.archive.crawler.webui.Text" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="java.util.Collection"%>

<% 

Crawler crawler = (Crawler)Text.get(request, "crawler");
CrawlJob job = (CrawlJob)Text.get(request, "job");
Collection<CrawlJob> completed = (Collection)Text.get(request, "completed"); 

String qs = crawler.getQueryString() + "&job=" + job.getName();


%>

<html>
<head>
    <%@include file="/include/header.jsp"%>
    <title>Heritrix Import Frontier Log</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<form method="post" action="do_import.jsp">
<% Text.printJobFormFields(request, out); %>

Select a completed job below to import that job's frontier log:<br>

<select name="recoverJob">
<option value="" selected></option>
<% for (CrawlJob c: completed) { %>
<option value="<%=Text.attr(c.encode())%>">
<%=Text.html(c.getName())%>
</option>
<% } %>
</select>

<p>Or, enter a full filesystem path to the frontier recovery log you'd like to
import.  (This is a path on the <i>crawler<i>, not the web UI server.) 

<br>

<input type="text" name="path" value="">

<p>
<input type="submit" value="Import">
</form>

</body>
</html>