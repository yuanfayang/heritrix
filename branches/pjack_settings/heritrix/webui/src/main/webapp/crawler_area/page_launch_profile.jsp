<%@ page import="java.util.List" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
String profile = (String)Text.get(request, "profile");

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix Crawler List</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>


<h3>Launch New Job Based on <%=profile%>:</h3>

<form class="nospace" action="do_launch_profile.jsp" method="post">
<% crawler.printFormFields(out); %>
<input type="hidden" name="profile" value="<%=Text.attr(profile)%>"/>

Enter a name for the new job: <input type="text" name="job" value=""/>

<input class="nospace" type="submit" value="Launch"></form><form class="nospace" action="index.jsp"></form>

</body>
</html>