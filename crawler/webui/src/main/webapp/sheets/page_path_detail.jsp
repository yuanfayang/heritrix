<%@ page import="java.util.Collection" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%@ page import="org.archive.crawler.webui.Setting" %>
<%@ page import="org.archive.crawler.webui.Settings" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
Settings settings = (Settings)Text.get(request, "settings");
String path = (String)Text.get(request, "path");
String sheet = (String)Text.get(request, "sheet");
Setting setting = settings.getSetting(path);
String desc = settings.getDescription(setting);
int row = 1;

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
<style>
input.textbox { width: 400px; }
</style>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<table>
<tr>
<td><b>Sheet:</b></td>
<td><%=Text.html(sheet)%>
</tr>
<tr>
<tr>
<td><b>Path:</b></td>
<td><%=Text.html(path)%>
</tr>
<tr>
<td><b>Type:</b></td>
<td><%=Text.html(setting.getType())%>
</tr>
</table>

<p>Enter a value for the setting below.

<form action="do_save_path.jsp" method="post">
<% Text.printSheetFormFields(request, out); %>
<input type="hidden" name="path" value="<%=Text.attr(path)%>">

<% settings.printDetailFormField(out, setting); %>

<input type="submit" value="Submit">
</form>

<% if (desc != null) { %>
<h3>Description for <%=Text.html(path)%></h3>
<%=Text.html(desc)%>
<% } %>

</body>
</html>