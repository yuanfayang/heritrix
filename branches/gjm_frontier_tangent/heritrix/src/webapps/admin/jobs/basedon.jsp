<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="java.util.Vector" %>

<%!
	public String printJobList(Vector jobs, boolean isJobs){
		if(jobs==null){
			return null;
		}
		StringBuffer ret = new StringBuffer();
		for(int i=0 ; i<jobs.size() ; i++){
			CrawlJob tmp = (CrawlJob)jobs.get(i);
			ret.append("<li><a href='/admin/jobs/new.jsp?job="+tmp.getUID()+"'>"+tmp.getJobName());
			if(isJobs){
				ret.append(" ["+tmp.getUID()+"]");
			}
			ret.append("</a>");
		}
		return ret.toString();
	}
%>
<%
	boolean isJobs = request.getParameter("type")!=null&&request.getParameter("type").equals("jobs");
	String title = "New via "+(isJobs?"an existing job":"a profile");
	int tab = 1;
%>

<%@include file="/include/head.jsp"%>
<p>
	<b>Select <%=isJobs?"job":"profile"%> to base new job on:</b>
<p>
	<ul>
<%
	if(isJobs){
		out.println(printJobList(handler.getPendingJobs(),true));
		if(handler.getCurrentJob()!=null){
			out.println("<li><a href='/admin/jobs/new.jsp?job="+handler.getCurrentJob().getUID()+"'>"+handler.getCurrentJob().getJobName()+" ["+handler.getCurrentJob().getUID()+"]</a>");
		}
		out.println(printJobList(handler.getCompletedJobs(),true));
	} else {
		out.println(printJobList(handler.getProfiles(),false));
	}
%>	
	</ul>

		
<%@include file="/include/foot.jsp"%>
