<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="org.apache.commons.httpclient.URI" %>
<%@ page import="org.archive.crawler.checkpoint.Checkpoint" %>

<%
	CrawlJob job = handler.getJob(request.getParameter("job"));
	job.scanCheckpoints();
	String title = "Resume from an existing checkpoint";
	int tab = 1;
%>

<%@include file="/include/head.jsp"%>
	<b style="color:red">Note that the resumed crawl will continue in the
	original directory, possibly destroying preexisting subsequent 
	checkpoints.</b>
<p>
	<b>Select a checkpoint to resume:</b>
<p>
	<ul>
<%
	Iterator iter = job.getCheckpoints().iterator();
	while(iter.hasNext()) {
		Checkpoint cp = (Checkpoint)iter.next();
		String cpName = cp.getName().replaceAll("\\+","%2B");
		String uri = "/admin/jobs/resume.jsp?cp="+cpName+"&job="+job.getUID();
		// String uri = (new URI("/admin/jobs/resume.jsp?cp="+cp.getName()+"&job="+job.getUID())).toString();
		out.println("<li>");
		if(cp.isValid()) {
			out.println(" <a href='"+uri+"'>");
		}
		out.println(cp.getDisplayName()+"</a>");
		if(cp.isValid()) {
			out.println(" </a>");
		}
	}
%>	
	</ul>

		
<%@include file="/include/foot.jsp"%>
