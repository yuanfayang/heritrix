<%@ page import="java.util.Collection" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="org.archive.crawler.webui.CrawlJob.State"%>
<%

    Collection<Crawler> crawlers = (Collection)request.getAttribute("crawlers");
    boolean jndiWarning = (Boolean)request.getAttribute("jndiWarning");

%>

<html>
<head>
    <%@include file="/include/header.jsp"%>
    <title>Heritrix Crawler List</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h3>Known Crawlers:</h3>

<table class="info">
    <tr>
        <th class="info">Crawler</th>
        <th class="info">Status</th>
        <th class="info">Actions</th>
    </tr>

    <% boolean alt = false; %>
    <% for (Crawler crawler: crawlers) { %>

    <tr <%=alt?"class=\"infoalt\"":""%>>
        <td class="info">
        <% if (crawler.getError() == null) { %>
            <a href="<%=request.getContextPath()%>/crawler_area/do_show_crawler.jsp?<%=crawler.getQueryString()%>">
        <% } %>
        <%=Text.html(crawler.getLegend())%>
        <% if (crawler.getError() == null) { %>
            </a>
        <% } %>
        </td>
        <td class="info">
        <%
                        if (crawler.getError() != null) { 
                        out.println(crawler.getError());
                    } else {
                        Collection<CrawlJob> activeJobs = crawler.getJobs(State.ACTIVE);
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
        </td>

        <td class="info">
            <a href="do_show_authenticate_crawler.jsp?<%=crawler.getQueryString()%>">Authenticate</a>
        <% if (crawler.getError() == null) { %>
            | <a href="do_stop_crawler.jsp?<%=crawler.getQueryString()%>">Stop</a>
        <% } %>
        <% if (crawler.getSource() == Crawler.Source.MANUAL) { %>
            | <a href="do_remove_crawler.jsp?<%=crawler.getQueryString()%>">Remove</a>
        <% } %>
        | <a href="<%=request.getContextPath()%>/crawler_area/do_show_about_crawler.jsp?<%=crawler.getQueryString()%>">About</a>
        </td>
    </tr>
    <% alt = !alt; %>
    <% } %>

</table>

<a href="do_show_add_crawler.jsp">Add</a>
|
<a href="do_crawler_refresh.jsp">Refresh</a>
|
<a href="<%=request.getContextPath()%>/help/do_show_help.jsp">Help</a>

<% if (jndiWarning) { %>
   <p>Note: No JNDI server was configured for the servlet container JVM.
<% } %>