<%@ page pageEncoding="UTF-8" %> 
<%@ page import="java.util.List" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
CrawlJob job = (CrawlJob)Text.get(request, "job");
String defaultName = (String)Text.get(request, "defaultName");
String error = (String)request.getAttribute("error");

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix Copy Job</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Copy <%=job.getName()%>:</h3>

<% if (error != null) { %>
<font color="red"><%=Text.html(error)%></font>
<% } %>

<form class="nospace" action="do_copy.jsp" method="post" accept-charset='UTF-8'>
<% crawler.printFormFields(out); %>
<% Text.printJobFormFields(request, out); %>

<input id="asReady" type="radio" name="newStage" value="READY" checked>
<label for="asReady">Copy to a new, ready-to-run job</label><br>

<input id="asProfile" type="radio" name="newStage" value="PROFILE">
<label for="asProfile">Copy to a new profile</label><br>

<p>

Enter a name for the new job/profile:<br>

<input style="width:400px" type="text" name="newName" value="<%=Text.attr(defaultName)%>">

<p>

<input class="nospace" type="submit" value="Copy"></form>

</body>
</html>
