
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%@ page import="org.archive.crawler.webui.CrawlJob"%>

<% 

{ // Start a new local variable scope so we don't clobber other pages.

String the_headline = (String)request.getAttribute("headline"); 
Crawler the_crawler = (Crawler)request.getAttribute("crawler");
String the_job = (String)request.getAttribute("job");
CrawlJob crawljob = null;
String the_jqs = "";
if(the_crawler != null && the_job != null){
    crawljob = new CrawlJob(the_job,the_crawler);
    the_jqs = the_crawler.getQueryString() + "&job=" + the_job;
}
String the_profile = (String)request.getAttribute("profile");
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
<img border="0" src="<%=request.getContextPath()%>/images/logo.gif" height="37" width="145" hspace="10">
</a>
</td>
<td>

<% if (the_headline != null) { %>
    <h1><%=the_headline %></h1>
<% } %>

<% if (the_crawler != null) { %>
    <b>Crawler:</b> 
    <a href="<%=request.getContextPath()%>/crawler_area/do_show_crawler.jsp?<%=the_crawler.getQueryString()%>">
        <%=Text.html(the_crawler.getLegend())%>
    </a>
    <br/>
<% } %>

<% if (the_job != null) { %>
    <b>Job:</b> 
    <%=Text.html(the_job)%>
    <span class='status <%=crawljob.getCrawlStatus()%>'><%=crawljob.getCrawlStatus()%></span>
	<% if(crawljob.getState()==CrawlJob.State.ACTIVE){ %>
        <a title="View and control the current status for this job."
           href="<%=request.getContextPath()%>/console/do_show_job_console.jsp?<%=the_jqs%>">Console</a>
        |
        <a title="View logs for this job."
           href="<%=request.getContextPath()%>/reports/do_show_reports.jsp?<%=the_jqs%>">Reports</a>
        | 
        <a title="View logs for this job."
           href="<%=request.getContextPath()%>/logs/do_show_log.jsp?<%=the_jqs%>">Logs</a>
   <% } else if(crawljob.getState()==CrawlJob.State.COMPLETED) { %>
        <a title="View logs for this job."
           href="<%=request.getContextPath()%>/logs/do_show_log.jsp?<%=the_jqs%>">Logs</a>
   <% }  %>
   <br/>
<% } %>

<% if (the_profile != null) { %>
<b>Profile:</b> <%=Text.html(the_profile)%>
<%
    Integer tabObject = (Integer)request.getAttribute("tab");
    int tab = (tabObject == null) ? -1 : tabObject.intValue();
    String the_qs = Text.jobQueryString(request);
%>
    <a class="rowLink" href="<%=request.getContextPath()%>/sheets/do_show_sheets.jsp?<%=the_qs%>">sheets</a>
    <a class="rowLink" href="<%=request.getContextPath()%>/seeds/do_show_seeds.jsp?<%=the_qs%>">seeds</a>
    <br/>
<% } %>

<% if (the_sheet != null) { %>
<b>Sheet:</b> <%=Text.html(the_sheet)%><br/>
<% } %>

</td>
</table>

<hr/>
<% } // end of local variable scope %>