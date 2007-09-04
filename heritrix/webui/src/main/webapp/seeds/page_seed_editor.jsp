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
boolean overflow = (Boolean)Text.get(request, "overflow");

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

<% if (overflow) { %>
<h3>Seed File Too Big</h3>

Only seed files smaller than 32 kilobytes can be changed by this web UI.  You
will have to directly edit the seed file using a text editor on the crawler.

<p>

The first 32K of the seeds file appears below.

  <% if (job.getJobStage() == JobStage.ACTIVE) { %>
  <form action="do_refresh_seeds.jsp" method="post">
  <% Text.printJobFormFields(request, out); %>
  After you edit the seeds file, you can tell the running job to reload the 
  seeds file by clicking this button.<p>

  <input type="submit" value="Refresh Seeds"/>

  </form>
  <% } %>
<% } %>

<form action="do_save_seeds.jsp">
<% Text.printJobFormFields(request, out); %>

<textarea rows="25" cols="75" name="seeds">
<%=Text.html(seeds)%>
</textarea>
<br/>

<% if (job.getJobStage() == JobStage.COMPLETED) { %>
You cannot edit seeds for a completed job.
<% } else if (overflow) { %>
You cannot edit a seeds file over 32K.
<% } else { %>
<input type="submit" value="Submit">
<% } %>
</form>

</body>
</html>
