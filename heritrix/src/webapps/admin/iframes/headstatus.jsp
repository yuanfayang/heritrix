<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.admin.StatisticsTracker" %>
<%@ page import="org.archive.crawler.framework.CrawlJob" %>
<%@ page import="org.archive.util.JavaLiterals" %>

<%
	String sAction = request.getParameter("action");

	if(sAction != null)
	{
		// Need to handle an action	
		if(sAction.equalsIgnoreCase("start"))
		{
			// Tell handler to start crawl job
			handler.startCrawler();
		}
		else if(sAction.equalsIgnoreCase("stop"))
		{
			// Tell handler to stop crawl job
			handler.stopCrawler();
		}
		else if(sAction.equalsIgnoreCase("terminate"))
		{
			// Tell handler to stop crawl job
			handler.terminateJob();
		}
		else if(sAction.equalsIgnoreCase("pause"))
		{
			// Tell handler to pause crawl job
			handler.pauseJob();
		}
		else if(sAction.equalsIgnoreCase("resume"))
		{
			// Tell handler to resume crawl job
			handler.resumeJob();
		}
		else if(sAction.equalsIgnoreCase("gc"))
		{
			// Tell handler to stop crawl job
			System.gc();
		}
		
	}	

	int iTime = 5;
	
	try
	{
		iTime = Integer.parseInt(request.getParameter("time"));
	}
	catch(Exception e){}
	
	StatisticsTracker stats = null;

	if(handler.isCrawling())
	{
		stats = (StatisticsTracker)handler.getStatistics(); //Assume that StatisticsTracker is being used.
	}

%>


<html>

	<head>
		<meta http-equiv=Refresh content="<%=iTime%> URL=headstatus.jsp"> 	
		<link rel="stylesheet" href="/admin/css/heritrix.css">
	</head>
	<body>
		<%
			String alert = handler.consumeAlertMessage();
			if (alert!=null) {
				out.println("<script language=\"Javascript\">\n"
				           +"alert(\""+JavaLiterals.escape(alert)+"\")\n"
				           +"</script>");
			}
		%>
		<table border="0" cellspacing="0" cellpadding="0" height="100%">
			<tr>
				<td class="dataheader" nowrap>
					Crawler running:
				</td>
				<td width="25" nowrap>
					<%=handler.shouldcrawl()?"Yes":"No"%>
				</td>
				<td>
					&nbsp;&nbsp;&nbsp;
				</td>
				<td class="dataheader" nowrap>
					Current job:
				</td>
				<td width="100%">
					<%=handler.isCrawling()?handler.getCurrentJob().getJobName():"None"%>
				</td>
			</tr>
			<tr>
				<td class="dataheader" nowrap>
					Jobs pending:
				</td>
				<td>
					<%=handler.getPendingJobs().size()%>
				</td>
				<td>
					&nbsp;&nbsp;&nbsp;
				</td>
				<% if(handler.isCrawling()){ %>
					<td class="dataheader" nowrap>
						Progress:
					</td>
					<td>
						<%=stats.successfulFetchAttempts()%>/<%=stats.urisEncounteredCount()%> @ 
						<%
							long time = (stats.getCrawlerTotalElapsedTime())/1000;

							if(time>3600)
							{
								//got hours.
								out.println(time/3600 + " h., ");
								time = time % 3600;
							}

							if(time > 60)
							{
								out.println(time/60 + " min. and ");
								time = time % 60;
							}

							out.println(time + " sec.");
						%>
					</td>
				<% } else { %>
					<td colspan="2"></td>
				<% } %>
			</tr>
			<tr>
				<td class="dataheader" nowrap>
					Jobs completed:
				</td>
				<td>
					<%=handler.getCompletedJobs().size()%>
				</td>
				<td>
					&nbsp;&nbsp;&nbsp;
				</td>
				<td colspan="2">
					<%
						if(handler.shouldcrawl())
						{
							out.println("<a href='headstatus.jsp?action=stop'>Stop crawling</a>");
						}
						else
						{
							out.println("<a href='headstatus.jsp?action=start'>Start crawling</a>");
						}

						if(handler.isCrawling())
						{
							out.println(" | <a href='headstatus.jsp?action=terminate'>Terminate current job</a> | ");
							if(handler.getCurrentJob().getStatus().equals(CrawlJob.STATUS_PAUSED) || handler.getCurrentJob().getStatus().equals(CrawlJob.STATUS_WAITING_FOR_PAUSE))
							{
								out.println("<a href='headstatus.jsp?action=resume'>Resume current job</a> ");
							}
							else
							{
								out.println("<a href='headstatus.jsp?action=pause'>Pause current job</a> ");
							}
						}
					%>
				</td>
			</tr>
		</table>
	</body>
</html>
