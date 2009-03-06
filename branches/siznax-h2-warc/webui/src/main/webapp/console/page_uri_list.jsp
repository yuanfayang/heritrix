<%@ page pageEncoding="UTF-8" %> 
<%@ page import="org.archive.crawler.webui.Text" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="java.util.Collection"%>

<% 

Crawler crawler = (Crawler)Text.get(request, "crawler");
CrawlJob job = (CrawlJob)Text.get(request, "job");

String regex = (String)Text.get(request, "regex");
int num = (Integer)Text.get(request, "num");
boolean verbose = (Boolean)Text.get(request, "verbose");
String marker = (String)request.getAttribute("marker");

Collection<String> uriList = (Collection)Text.get(request, "uriList");

String checked = (verbose) ? " checked" : "";

%>

<%@page import="org.archive.crawler.framework.CrawlController"%>
<html>
<head>
    <%@include file="/include/header.jsp"%>
    <title>Heritrix List URIs</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h1>List Frontier URIs</h1>

<form method="get" action="do_update_uri_list.jsp" accept-charset='UTF-8'>
<% Text.printJobFormFields(request, out); %>
<input type="hidden" name="marker" value="<%=marker%>">

Maximum URIs to Display:<br>
<input type="text" name="num" value="<%=num%>">

<p>
Regular Expression for URIs:<br>
<input type="text" name="regex" value="<%=Text.attr(regex)%>">

<p>
<input id="verbose" type="checkbox" name="verbose" <%=checked%>>
<label for="verbose">Include verbose information.</label>

<p>

<input type="submit" name="action" value="Get Next Matches">
<input type="submit" name="action" value="Start Over">
</form>

<pre>
<% for (String s: uriList) { %><%=Text.html(s)%>
<% } %>

</pre>

</body>
</html>
