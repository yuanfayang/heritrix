<%@ page import="java.util.Collection" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
String sheet = (String)Text.get(request, "sheet");
String add = (String)Text.get(request, "add");

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Associate URL prefixes with '<%=sheet%>'</h3>

<form method="post" action="do_associate.jsp">
<% Text.printSheetFormFields(request, out); %>
<input type="hidden" name="add" value="<%=Text.attr(add)%>">

<textarea name="surts" rows="25" cols="75">
</textarea>
<br>
<input type="submit" name="Submit">
</form>

</body>
</html>
