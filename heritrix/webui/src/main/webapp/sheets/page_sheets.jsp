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

<h3>Settings Sheets:</h3>

<% for (String sheet: sheets) { %>
    <div class="multilineItem">
    <% String qs = Text.jobQueryString(request) + "&sheet=" + sheet; %>
    <span class="label">Sheet:</span> <%=Text.html(sheet)%>
    <div class="itemDetails">

    <% if (problems.contains(sheet)) { %>
        <span class="alert">validation problems</span><br/>
    <% } %>
    
    <a class="rowLink"
       title="View settings without changing them."
       href="do_show_sheet_detail.jsp?<%=qs%>">
    View
    </a>

    <a class="rowLink"
       title="Edit settings."
       href="do_show_sheet_editor.jsp?<%=qs%>">
    Edit
    </a>
    
    <% if (!checkedOut.contains(sheet) && !sheet.equals("global")) { %>
    <a class="rowLink"
        title="Delete this sheet."
        href="do_show_delete_sheet.jsp?<%=qs%>">
    Delete
    </a>
    <% } %>
    <br/>

<% if (checkedOut.contains(sheet)) { %>
    <span class="alert">(!)</span> <span class="label">Unsaved edits:</a>

    <a class="rowLink"
       title="Commit changes to this sheet."
       href="do_commit_sheet.jsp?<%=qs%>">
    Commit
    </a>
       
    <a class="rowLink"
       title="Abandon changes to this sheet."
       href="do_cancel_sheet.jsp?<%=qs%>">
    Rollback
    </a>
    <br/>
<% } %>

    <span class="">SURT prefix associations:</span>
    
    <a class="rowLink"
      title="List SURT prefixes associated with this sheet."
      href="do_show_surts.jsp?<%=qs%>">
    List
    </a>

    <a class="rowLink"
       title="Associate SURT prefixes with this sheet."
       href="do_show_associate.jsp?<%=qs%>&add=Y">
    Add
    </a>

    <a class="rowLink"
       title="Disassociate SURT prefixes with this sheet."
       href="do_show_associate.jsp?<%=qs%>&add=N">
    Remove
    </a>
    </div>
    </div>
<% } %>

<a href="do_show_add_single_sheet.jsp?<%=Text.jobQueryString(request)%>">
Add Single Sheet...
</a>

<h3>Test Settings</h3>

<form method="get" action="do_show_config.jsp">
<% Text.printJobFormFields(request, out); %>
Enter a URL below to see what settings will be applied for that URL:<br>
<input type="text" name="url" size="60" value=""><br/>
<input type="submit" name="button" value="Settings">
<input type="submit" name="button" value="Sheets">
</form>

</body>
</html>