<%@ page import="java.util.Collection" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
Collection<String> sheets = (Collection)Text.get(request, "sheets");
int row = 1;

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
</head>
<body>

<table>
<tr>
<td>
<a border="0" href="<%=request.getContextPath()%>/index.jsp">
<img border="0" src="<%=request.getContextPath()%>/images/logo.gif" height="37" width="145">
</a>
</td>
<td>
<%=Text.html(crawler.getLegend())%>
</td>
</table>

<h3>Sheets:</h3>

<table class="info">
<% for (String sheet: sheets) { %>
<% row = -row + 1; %>
<% String qs = Text.jobQueryString(request) + "&sheet=" + sheet; %>
<tr>
<td class="info<%=row%>">
<%=Text.html(sheet)%>
</td>
<td class="info<%=row%>">
<a
   title="View settings without changing them."
   href="do_show_sheet_detail.jsp?<%=qs%>">
View
</a>

Edit | Save
</td>
</tr>
<% } %>
</table>


</body>
</html>