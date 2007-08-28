
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="org.archive.crawler.webui.Flash" %>
<%@ page import="org.archive.crawler.framework.JobStage"%>
<%@ page import="java.util.Collection"%>

<% 

{ // Start a new local variable scope so we don't clobber other pages.

String the_headline = (String)request.getAttribute("headline"); 
Crawler the_crawler = (Crawler)request.getAttribute("crawler");
CrawlJob the_job = (CrawlJob)request.getAttribute("job");
String the_jqs = "";
if(the_job != null){
    the_jqs = Text.jobQueryString(request);
}
String the_sheet = (String)request.getAttribute("sheet");


%>

<div style="float:right">
<a href="<%=request.getContextPath()%>/help/do_show_about_ui.jsp">About</a><br/>
<a href="<%=request.getContextPath()%>/help/do_show_help.jsp">Help</a>
</div>

<table>
<tr>
<td>
<a border="0" href="<%=request.getContextPath()%>/index.jsp">
<img border="0" src="<%=request.getContextPath()%>/images/heritrix-v1-logo.png" height="47" width="155" hspace="10">
</a>
</td>
<td>

<% if (the_headline != null) { %>
    <h1><%=the_headline %></h1>
<% } %>

<% if (the_crawler != null) { %>
    <b>Crawl Engine:</b> 
    <a href="<%=request.getContextPath()%>/crawler_area/do_show_crawler.jsp?<%=the_crawler.getQueryString()%>">
        <%=Text.html(the_crawler.getLegend())%>
    </a>
    <br/>
<% } %>

<% if (the_job != null) { %>
    <b><%=Text.html(the_job.getJobStage().getLabel())%>:</b>
    <%=Text.html(the_job.getName())%>
    <% if (the_job.getCrawlStatus() != null) { %>
    <span class='status <%=the_job.getCrawlStatus()%>'><%=the_job.getCrawlStatus()%></span>
    <% } %>
	<% if (the_job.hasReports()) { %>
        <a title="View and control the current status for this job."
           href="<%=request.getContextPath()%>/console/do_show_job_console.jsp?<%=the_jqs%>">Console</a>
        |
        <a title="View reports for this job."
           href="<%=request.getContextPath()%>/reports/do_show_reports.jsp?<%=the_jqs%>">Reports</a>
        | 
        <a title="View logs for this job."
           href="<%=request.getContextPath()%>/logs/do_show_log.jsp?<%=the_jqs%>">Logs</a>
   <% } else if (the_job.getJobStage() == JobStage.COMPLETED) { %>
        <a title="View logs for this job."
           href="<%=request.getContextPath()%>/logs/do_show_log.jsp?<%=the_jqs%>">Logs</a>
   <% } else { %>
    <a class="rowLink" href="<%=request.getContextPath()%>/sheets/do_show_sheets.jsp?<%=the_jqs%>">sheets</a>
    <a class="rowLink" href="<%=request.getContextPath()%>/seeds/do_show_seeds.jsp?<%=the_jqs%>">seeds</a>
   <% } %>
   <br/>
<% } %>


<% if (the_sheet != null) { %>
<b>Sheet:</b> <%=Text.html(the_sheet)%>
<%
    // Check for uncommitted changes to the sheet and display commit/rollback options if so
    Collection<String> navCheckedOut = (Collection)request.getAttribute("checkedOut");
    if(navCheckedOut != null && navCheckedOut.contains(the_sheet)){
        String the_qs = Text.jobQueryString(request) + "&sheet=" + the_sheet;
%>
    <span class="alert">(!)</span> Unsaved edits:

    <a class="rowLink" 
       title="Commit changes to this sheet."
       href="do_commit_sheet.jsp?<%=the_qs%>">commit</a>
       
    <a class="rowLink" 
       title="Abandon changes to this sheet."
       href="do_cancel_sheet.jsp?<%=the_qs%>">rollback</a>
<%
    }
%>
<br/>
<% } %>

</td>
</table>

<hr/>

<% 
out.flush();
Flash.fromSession(request,response);
} // end of local variable scope 

%>