<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.framework.CrawlJob,org.archive.crawler.admin.StatisticsTracker,java.util.*,java.io.*" %>
<%
	String job = request.getParameter("job");
	CrawlJob cjob = null;

	if(job != null)
	{
		cjob = handler.getJob(job);
	}
	
	String title = "View seeds";
	int tab = 1;
%>

<%@include file="/include/head.jsp"%>
	
	<body>
		<%
			if(cjob == null)
			{
				// NO JOB SELECTED - ERROR
		%>
				<b>Invalid job selected</b>
		<%
			}
			else
			{
		%>
				<fieldset style="width: 600px">
					<legend>Seed file for '<%=cjob.getJobName()%>'</legend>
		<%
				BufferedReader seeds = new BufferedReader(new FileReader(new File(cjob.getCrawlOrder().getStringAt(SimpleHandler.XP_SEEDS_FILE))));
				String sout = seeds.readLine();
				while(sout!=null){
					out.println(sout);
					sout = seeds.readLine();
				}
		%>
				</fieldset>
		<%
			} // End if(cjob==null)else clause
		%>
		<a href="javascript:history.back()">Back</a>

<%@include file="/include/foot.jsp"%>
