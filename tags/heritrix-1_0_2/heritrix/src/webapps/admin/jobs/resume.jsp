<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

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
	handler.resumeJobFromCheckpoint(job,cp);
	out.println("Job "+job.getDisplayName()
	            +" scheduled for resumption at checkpoint "
	            +cp.getDisplayName());
%>
	</b>
<p>
		
<%@include file="/include/foot.jsp"%>
