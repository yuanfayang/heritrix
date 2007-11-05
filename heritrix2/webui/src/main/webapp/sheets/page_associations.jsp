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

<h2>Associate URL prefixes with '<%=sheet%>'</h2>

One entry per line. SURT prefixes entered will be used directly. Other
URIs will be converted to SURT form then truncated to the implied 
prefix. Plain hosts/domains will be changed to the implied HTTP URI 
then converted as above.

<form method="post" action="do_associate.jsp">
<% Text.printSheetFormFields(request, out); %>
<input type="hidden" name="add" value="<%=Text.attr(add)%>">

<textarea name="surts" rows="25" cols="75">
</textarea>
<br>
<input type="submit" name="Submit" value="Submit">
</form>

</body>
</html>
