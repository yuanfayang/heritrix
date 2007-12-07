<%@ page import="java.util.Collection" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.management.openmbean.CompositeData" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
String editedSheet = (String)Text.get(request, "sheet");
List<String> sheets = (List)Text.get(request, "bundled");

int row = -1;

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<% if (!sheets.isEmpty()) { %>

<h3>Sheets Contained Inside <%=editedSheet%>:</h3>

<table class="info">
<% 

for (String sheetName: sheets) {
    String qs = Text.sheetQueryString(request) + "&move=" + Text.query(sheetName);
    row++;
%>
<tr>
<td class="info<%=(row % 2)%>">
  <%=Text.html(sheetName)%>
</td>
<td class="info<%=(row % 2)%>">
  <a href="do_move_bundled_sheets.jsp?<%=qs%>&index=<%=row - 1%>">
    Move Up
  </a>
  |
  <a href="do_move_bundled_sheets.jsp?<%=qs%>&index=<%=row + 1%>">
    Move Down
  </a>
</td>
</tr>
<% } %>
</table>

<% } %>

<h3>Add a New Sheet:</h3>

<form method="post" action="do_move_bundled_sheets.jsp">
<% Text.printSheetFormFields(request, out); %>
<input type="hidden" name="index" value="<%=row + 1%>">

Enter the name of a new sheet to add:
<input type="text" name="move" value="">
<input type="submit" value="Submit">
</form>

</body>
</html>