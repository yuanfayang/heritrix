<%@ page import="java.util.Collection" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
Collection<String> sheets = (Collection)Text.get(request, "sheets");
Collection<String> problems = (Collection)Text.get(request, "problems");
Collection<String> checkedOut = (Collection)Text.get(request, "checkedOut");
int row = 1;

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Sheets:</h3>

<a href="do_show_add_single_sheet.jsp?<%=Text.jobQueryString(request)%>">
Add Single Sheet
</a>
|
<a href="do_show_add_sheet_bundle.jsp?<%=Text.jobQueryString(request)%>">
Add Sheet Bundle
</a>



<table class="info">
<% for (String sheet: sheets) { %>
<% row = -row + 1; %>
<% String qs = Text.jobQueryString(request) + "&sheet=" + sheet; %>
<tr>
<td class="info<%=row%>">
<%=Text.html(sheet)%>
<% if (problems.contains(sheet)) { %>
*
<% } %>
</td>
<td class="info<%=row%>">
<a
   title="View settings without changing them."
   href="do_show_sheet_detail.jsp?<%=qs%>">
View
</a>

|

<a
   title="Edit settings."
   href="do_show_sheet_editor.jsp?<%=qs%>">
Edit
</a>

<% if (checkedOut.contains(sheet)) { %>
|

<a
   title="Commit changes to this sheet."
   href="do_commit_sheet.jsp?<%=qs%>">
Commit
</a>

|

<a
   title="Abandon changes to this sheet."
   href="do_cancel_sheet.jsp?<%=qs%>">
Rollback
</a>

<% } %>

</td>
</tr>
<% } %>
</table>


</body>
</html>