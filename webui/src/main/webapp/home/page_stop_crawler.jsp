<%@ page pageEncoding="UTF-8" %> 
<%@ page import="java.util.List" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix Copy Job</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Stop the <%=Text.html(crawler.getLegend())%> engine?</h3>

<form method="get" action="do_stop_crawler.jsp" accept-charset='UTF-8'>
<% crawler.printFormFields(out); %>

<input type="radio" name="kind" value="graceful" id="graceful" checked>
<label for="graceful">Gracefully shut down engine.  Any running jobs will be
told to terminate so they can close down databases and write reports.  Once all
running jobs are terminated, the crawl engine itself will shut down.</label>

<p>

<input type="radio" name="kind" value="klutz" id="klutz">
<label for="klutz">Shut down immediately.  Any running jobs will abruptly halt.
Their final reports will not be written, and any databases they use may be 
left in an inconsistent state.
<% if (crawler.getPort() == -1) { %>
<i>This will also shut down the web UI!</i>
<% } %>

</label>

<p>

<span class="alert">(!)</span> Be careful! Once you click the button below,
you will not be able to restore the crawl engine.  You will have to restart it
from the command line.



<p>

<input type="submit" value="Stop Crawl Engine">

</form>
</body>
</html>
