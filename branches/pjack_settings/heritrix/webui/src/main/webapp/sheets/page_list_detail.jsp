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
String error = (String)request.getAttribute("error");
Setting setting = settings.getSetting(path);
int row = -1;

int count = 0;
for (Setting s: settings.getSettings()) {
    if (s.isDirectChild(path)) {
        count++;
    }
}

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
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
<tr>
<td><b>Element Type:</b></td>
<td><%=Text.html(setting.getValue())%></td>
</tr>
</table>

<% if (error != null) { %>
<font color="red"><h3><%=Text.html(error)%></h3></font>
<% } %>

<hr/>

<h3>Elements:</h3>

<table class="info">

<% for (Setting s: settings.getSettings()) { %>
<%     if (s.isDirectChild(path)) { %>


<% String qs = Text.sheetQueryString(request) + "&path=" + Text.query(path) 
                + "&key=" + Text.query(Text.lastPath(s.getPath())); %>

<% row++; %>
<tr>
<td class="info<%=(row % 2)%>">
 <%=Text.html(Text.lastPath(s.getPath()))%>
</td>
<td class="info<%=(row % 2)%>">
 <%=Text.html(s.getValue())%>
</td>
<td class="info<%=(row % 2)%>">
 <% if (row > 0) { %>
   <a href="do_move_element_up.jsp?<%=qs%>" title="Move this element up.">
 <% } %>
  Move Up
 <% if (row > 0) { %>
   </a>
 <% } %>
 
 <% if (row < count - 1) { %>
 |
    <a href="do_move_element_down.jsp?<%=qs%>" title="Move this element down.">
  Move Down
 </a>
 <% } %>
</td>
</tr>
<%   } // end if %>
<% } // end for %>
</table>

<p>
<hr/>

<h3>Add New Element</h3>


<form action="do_add_element.jsp" method="post">
<% Text.printSheetFormFields(request, out); %>
<input type="hidden" name="path" value="<%=Text.attr(setting.getPath())%>">

<% if (setting.getType().equals("map")) { %>
<p>Enter a name for the new element: 
<input class="textbox" type="text" name="key">
<% } else { %>
<input type="hidden" name="key" value="<%=count%>">
<% } %>

<p>Enter the value for the new element below:<br/>

<% settings.printElementFormField(out, setting); %>

<input type="submit" value="Submit">
</form>


</body>
</html>