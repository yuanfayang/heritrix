<%@ page pageEncoding="UTF-8" %> 
<%@ page import="java.util.Collection" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%@ page import="org.archive.crawler.webui.Setting" %>
<%@ page import="org.archive.crawler.webui.Settings" %>
<%@ page import="org.archive.crawler.webui.Settings.Editability" %>

<%

Settings the_settings = (Settings)request.getAttribute("settings");
String the_path = (String)Text.get(request, "path");
Setting the_setting = the_settings.getSetting(the_path);

%>
<p>
<%

// FIXME: Add relevent hyperlinks below.
switch (the_settings.getEditability(the_setting)) {
    case EDITABLE:
        break;
    case BOOTSTRAP:
        %>
        <span class="alert">(!)</span> You cannot edit this setting because it 
        was specifically set for the settings framework. Changing it could 
        destabilize the framework.  This setting is only provided in case you 
        want to reuse it in another module.
        <%
        break;
    case GLOBAL:
        %>
        <span class="alert">(!)</span> You cannot edit this setting because it 
        is a global setting, and you are not editing the global sheet.
        <%
        break;
    case IMMUTABLE:
        %>
        <span class="alert">(!)</span> You cannot edit this setting because 
        changing it in the middle of a running job could destabilize that job.  
        You can only change this setting in a profile, or in a job that 
        hasn't been launched yet.
        <%
        break;
    case NOT_OVERRIDDEN:
        %>
        <span class="alert">(!)</span> You cannot edit this setting because it 
        hasn't been added to the sheet yet.
        <%
        break;
    case PATH_INVALID:
        %>
        <span class="alert">(!)</span> You cannot edit this setting because its
        path (<%=Text.html(the_setting.getPath())%>) is invalid.  This might
        have been a typo from a hand-edited sheet file.
        <%
        break;
    case COMPLETED:
        %>
        <span class="alert">(!)</span> You cannot edit this setting because it
        belongs to a completed job.        
        <%
}

%>
