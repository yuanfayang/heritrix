<%@ page pageEncoding="UTF-8" %> 
<%@ page import="org.archive.crawler.webui.Text" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="java.util.Collection"%>

<% 

Crawler crawler = (Crawler)Text.get(request, "crawler");
CrawlJob job = (CrawlJob)Text.get(request, "job");

String regex = (String)Text.get(request, "regex");

%>

<%@page import="org.archive.crawler.framework.CrawlController"%>
<html>
<head>
    <%@include file="/include/header.jsp"%>
    <title>Heritrix Delete URIs</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h1>Delete Frontier URIs</h1>

<form method="post" action="do_delete_uris.jsp" accept-charset='UTF-8'>
<% Text.printJobFormFields(request, out); %>

<p>
Regular Expression for Queues:<br>
<input type="text" name="queueRegex" value="^.*$">

<p>
Regular Expression for URIs:<br>
<input type="text" name="uriRegex" value="<%=Text.attr(regex)%>">

<p>
Please be careful; you will not be able to restore deleted URIs.

<p>
<input type="submit" name="action" value="Delete">
</form>

</body>
</html>
