<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Map" %>
<%@ page import="javax.management.openmbean.CompositeData" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Setting" %>
<%@ page import="org.archive.crawler.webui.Settings" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%

Crawler crawler = (Crawler)Text.get(request, "crawler");
String editedSheet = (String)Text.get(request, "sheet");
Settings settings = (Settings)Text.get(request, "settings");

int row = 1;
int count = 0;

%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
<style>
input { width: 100%; }
select { width: 100%; }
</style>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Settings for sheet "<%=editedSheet%>":</h3>

<form action="do_save_single_sheet.jsp" method="post">
<% Text.printSheetFormFields(request, out); %>

<table class="info">
<% 

for (Setting setting: settings.getSettings()) {
    String path = setting.getPath();
    String type = setting.getType();
    String value = setting.getValue();
    String[] sheets = setting.getSheets();
    if (sheets == null) {
        sheets = new String[] { "foo" };
    }
    String error = setting.getErrorMessage();
    String disabled = sheets[0].equals(editedSheet) ? "" : "disabled=\"disabled\"";
    String qs = Text.sheetQueryString(request) + "&path=" + path + "&type=" + type + "&value=" + value;
    row = -row + 1;
    count++;
%>
<tr>
<td class="info<%=row%>">
  <a 
     href="do_show_path_detail.jsp?<%=qs%>"
     title="View details for this setting.">     
   Details
  </a>
  <% if (settings.canOverride(setting)) { %>
  |
  <% if (sheets[0].equals(editedSheet)) { %>
    <a href="do_remove_path.jsp?<%=qs%>">
      Remove
    </a>
  <% } else { %>
    <a href="do_override_path.jsp?<%=qs%>">
      Add
    </a>  
  <% } %>
  <% } %>
</td>
<td class="info<%=row%>">
 <% for (String sheet: sheets) { %>
   <%=Text.html(sheet)%>,
 <% } %>
</td>
<td class="info<%=row%>">
 <%=Text.html(path)%>
 <% if (error != null) { %>
   <br/>
   <font color="red"><%=Text.html(error)%></font>
 <% } %>
 
</td>
<td class="info<%=row%>">
 <input type="hidden" name="<%=count%>" value="<%=Text.attr(path)%>">
 <% settings.printFormField(out, setting); %>
</td>
</tr>

<% } %> 
</table>
<input type="Submit" value="Submit"/>
</form>

</body>
</html>