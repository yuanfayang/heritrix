<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.archive.crawler.framework.JobStage" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.CrawlJob" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
CrawlJob job = (CrawlJob)Text.get(request, "job");
String seedsfile = (String)Text.get(request, "seedfile");
String seeds = (String)Text.get(request, "seeds");
int pageNum = (Integer)Text.get(request, "page");

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Seeds</h3>

<p>

<b>File:</b> <%=Text.html(seedsfile)%> <br/>
<b>Page:</b> <%=pageNum%> 

<form action="do_save_seeds.jsp">
<% Text.printJobFormFields(request, out); %>
<input type="hidden" name="page" value="<%=pageNum%>">

<textarea rows="25" cols="75" name="seeds">
<%=Text.html(seeds)%>
</textarea>
<br/>

<% if (job.getJobStage() == JobStage.COMPLETED) { %>
You cannot edit seeds for a completed job.
<% } else { %>
<input type="submit" value="Submit">
<% } %>
</form>

</body>
</html>
