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
<%@page import="org.apache.commons.lang.StringUtils"%>
<html>
<head>
<%@include file="/include/header.jsp"%>
<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<form id="the_form" action="do_save_single_sheet.jsp" method="post">


<table width="100%">
<tr>
<td>
<h3>Settings for sheet "<%=editedSheet%>":</h3>
</td>
<td>
<input type="submit" value="Submit Changes">
</td>
</tr>
</table>

<div class="indent">

<% Text.printSheetFormFields(request, out); %>

<input type="hidden" name="otherAction" value="">
<input type="hidden" id="otherActionPath" name="otherActionPath" value="">
<input type="hidden" id="otherActionType" name="otherActionType" value="">
<input type="hidden" id="otherActionValue" name="otherActionValue" value="">

<% 
String previousPath = "";
int previousIndent = 0; 
for (Setting setting: settings.getSettings()) {
    String path = setting.getPath();
    int lastColon = path.lastIndexOf(':');
    String pathPrefix = path.substring(0,(lastColon>=0)?lastColon+1:0);
    String pathLast = path.substring((lastColon>=0)?lastColon+1:0,path.length());
    String type = setting.getType();
    String value = setting.getValue();
    String[] sheets = setting.getSheets();
    if (sheets == null) {
        sheets = new String[] { "foo" };
    }
    String error = setting.getErrorMessage();
    String qs = Text.sheetQueryString(request) + "&path=" + path + "&type=" + type + "&value=" + Text.query(value);
    row = -row + 1;
    count++;
    if(path.startsWith(previousPath) && path.charAt(previousPath.length())==':') {
%>
        <div class="subsettings">
<%
    }
    int indent = StringUtils.split(path,':').length - 1;
    while(previousIndent>indent) {
%>
        </div>
<%
        previousIndent--;
    }
    previousIndent = indent; 
    previousPath = path; 
%>

<div class="editSetting">

 <span class="settingPath" title="<%=Text.html(path)%>">
 <a name="<%=Text.attr(path)%>"></a><%=Text.html(pathLast)%>
 </span>
 
 &nbsp;&nbsp;

<% for (String sheet: sheets) { %>
   <span title="Sheet(s) where this setting is defined." class="settingSource"><%=Text.html(sheet)%></span>
<% } %>
    
 <% if (error != null) { %>
   <br/>
   <span class="alert">Problem: <%=Text.html(error)%>
   <% if (setting.isPathInvalid()) { %>
      <br>This invalid setting will be automatically removed when you modify this
      sheet.
   <% } %>
   
   </span>
 <% } %><br/>
 <input type="hidden" name="<%=count%>" value="<%=Text.attr(path)%>">
 <% settings.printFormField(out, setting); %>
  <% if (settings.canOverride(setting)) { %>
    <% if (sheets[0].equals(editedSheet)) { %>
      <button 
         class="rowLink" 
         type="submit" 
         name="remove" 
         value="<%=Text.attr(path)%>">remove</button>
    <% } else { %>
      <button 
         class="rowLink" 
         type="submit" 
         name="add" 
         value="<%=Text.attr(path +"`" + type + "`" + value)%>">add</button>
    <% } %>
  <% } %>
  <button
     class="rowLink"
     type="submit"
     name="details"
     value="<%=Text.attr(path)%>">details</button>
</div>

<% } // end for %> 
<%
while(previousIndent>0) {
    %>
    </div>
    <%
      previousIndent--;
}
%>

<br/>

<input type="Submit" value="Submit Changes"/> <br/>

Note: you must also 'commit' any changes in the job's 
<a href="<%=request.getContextPath()%>/sheets/do_show_sheets.jsp?<%=Text.jobQueryString(request)%>">
sheets overview page</a> to make changes permanent on disk. 
</form>
</div>

</body>
</html>