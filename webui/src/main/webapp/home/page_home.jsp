<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="org.archive.crawler.framework.JobStage"%>
<%

    Collection<Crawler> crawlers = (Collection)Text.get(request, "crawlers");
    Map<Crawler,Collection<CrawlJob>> actives = (Map)Text.get(request, "actives");
    boolean jndiWarning = (Boolean)request.getAttribute("jndiWarning");

%>

<html>
<head>
    <%@include file="/include/header.jsp"%>
    <title>Heritrix: Administered Crawl Engines</title>
    <% request.setAttribute("headline","Administered Crawl Engines"); %>
</head>
<body>

<%@include file="/include/nav.jsp"%>

This web console knows of the following crawl engines:

    <% boolean alt = false; %>
    <% for (Crawler crawler: crawlers) { %>

    <div class="multilineItem">
    <span class="label">Engine ID:</span>
        <% if (crawler.getError() == null) { %>
            <a href="<%=request.getContextPath()%>/crawler_area/do_show_crawler.jsp?<%=crawler.getQueryString()%>">
        <% } %>
        <%=Text.html(crawler.getLegend())%>
        <% if (crawler.getError() == null) { %>
            </a>
        <% } %>
        <div class="itemDetails">
        <%
           if (crawler.getError() != null) { 
               out.println(crawler.getError());
           } else {
               Collection<CrawlJob> activeJobs = actives.get(crawler);
               if(activeJobs.size()==0){
                   out.println("No active jobs");
               } else {
                   for(CrawlJob job : activeJobs){
                       out.println(job.getCrawlStatus() + ": ");
                       out.println("<a href=\"" + request.getContextPath());
                       out.println("/console/do_show_job_console.jsp?");
                       out.println(crawler.getQueryString());
                       out.println("&job=" + job.getName() + "\">");
                       out.println(job.getName() + "</a>");
                   }
               }
           }
        %>
        <br/>
        <% if (crawler.getError() != null) { %>
            <a class="rowLink" href="do_show_authenticate_crawler.jsp?<%=crawler.getQueryString()%>">Authenticate</a>
        <% } %>
          <a class="rowLink" href="<%=request.getContextPath()%>/crawler_area/do_show_about_crawler.jsp?<%=crawler.getQueryString()%>">About</a>
        <% if (crawler.getError() == null) { %>
          <a class="rowLink" href="do_show_stop_crawler.jsp?<%=crawler.getQueryString()%>">Stop</a>
        <% } %>
        <% if (crawler.getSource() == Crawler.Source.MANUAL && crawler.getPort() != -1) { %>
            <a class="rowLink" href="do_remove_crawler.jsp?<%=crawler.getQueryString()%>">Remove from UI</a>
        <% } %>
        </div>
    </div>
    <% } %>
    <% if (crawlers.isEmpty()) { %>
       <div class="placeholder">(none)</div>
    <% } %>

<a href="do_show_add_crawler.jsp">Add a remote crawl engine...</a><br/>
<a href="do_show_stop_webui.jsp">Terminate the Web UI...</a>


<% if (jndiWarning) { %>
   <p>Note: No directory server (JNDI) was configured as a source of
   crawl engine information.</p>
<% } %>