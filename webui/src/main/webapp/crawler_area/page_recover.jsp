<%@ page pageEncoding="UTF-8" %> 
<%@ page import="java.util.Collection" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
CrawlJob job = (CrawlJob)Text.get(request, "job");
Collection<String> checkpoints = (Collection)Text.get(request, "checkpoints");
String defaultName = (String)Text.get(request, "defaultName");

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix Crawler List</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Recover Checkpoint:</h3>

<% if (checkpoints.isEmpty()) { %>

There are no checkpoints to recover for <%=Text.html(job.getName())%>.

<% } else { %>


<form class="nospace" action="do_recover.jsp" method="post" accept-charset='UTF-8'>
<% crawler.printFormFields(out); %>
<% Text.printJobFormFields(request, out); %>

Enter a name for the recovered job:<br>

<input style="width:400px" type="text" name="newName" value="<%=Text.attr(defaultName)%>">

<p>

Select the checkpoint to recover:<br>

<select name="checkpoint">
<% for (String cp: checkpoints) { %>
<option value="<%=Text.attr(cp)%>"><%=Text.html(cp)%></option>
<% } %>
</select>

<p>

<input class="nospace" type="submit" value="Recover">
</form>

<% } %>

</body>
</html>
