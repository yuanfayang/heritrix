<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="org.archive.crawler.checkpoint.Checkpoint" %>

<%
    CrawlJob job = handler.getJob(request.getParameter("job"));
    String cpName = request.getParameter("cp");
    Checkpoint cp = job.getCheckpoint(cpName);
    String title = "Resuming from checkpoint";
    int tab = 1;
%>

<%@include file="/include/head.jsp"%>
<p>
    <b style="color:red">
<%
    // This method has been removed.  Comment out for now till
    // the method gets implemented to suit current checkpointing
    // or an alternate is put in its place.
    // handler.resumeJobFromCheckpoint(job,cp);
    out.println("Job "+job.getDisplayName()
                +" scheduled for resumption at checkpoint "
                +cp.getDisplayName());
%>
    </b>
<p>
        
<%@include file="/include/foot.jsp"%>
