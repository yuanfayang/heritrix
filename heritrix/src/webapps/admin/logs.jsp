<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%@page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.datamodel.settings.SettingsHandler,org.archive.crawler.datamodel.settings.XMLSettingsHandler,org.archive.crawler.admin.CrawlJob,org.archive.util.LogReader" %>

<%
	/* Various settings with default values (where applicable) */
	String mode = request.getParameter("mode");
	String logText = "";
	int defaultNumberOfLines = 50;
	int iTime = -1;
	int linenumber = 1;
	String timestamp = null;
	String regexpr = null;
	boolean ln = false;
	boolean grep = false;
	boolean indent = false;
	
	/* Which log to display */
	String fileName = request.getParameter("log");
	if(fileName == null || fileName.length() <= 0)
	{
		fileName = "crawl.log";
	}

	/* Location of logs */
	SettingsHandler settingsHandler = null;
	CrawlJob theJob = null;
	if(request.getParameter("job") != null && request.getParameter("job").length() > 0){
		//Get logs for specific job. This assumes that the logs for each job are stored in a unique location.
		theJob = handler.getJob(request.getParameter("job"));
	}else{
		if(handler.getCurrentJob() != null){
			// If no specific job then assume current one
			theJob = handler.getCurrentJob();
		} else if(handler.getCompletedJobs().size() > 0){
			// If no current job, use the latest completed job.
			theJob = (CrawlJob)handler.getCompletedJobs().get(handler.getCompletedJobs().size()-1);
		}
	}
	
	if(theJob != null)
	{
		settingsHandler = theJob.getSettingsHandler();
		String diskPath = (String)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_DISK_PATH);
		diskPath = settingsHandler.getPathRelativeToWorkingDirectory(diskPath)+"/";

		// Got a valid crawl order, find it's logs
		if(mode != null && mode.equalsIgnoreCase("number"))
		{
			/* Get log by line number */
		
			try
			{
				linenumber = Integer.parseInt(request.getParameter("linenumber"));
			}
			catch(Exception e){/*Ignore*/}
		
			logText = LogReader.get(diskPath + fileName,linenumber,defaultNumberOfLines).replaceAll(" ","&nbsp;");
		}
		else if(mode != null && mode.equalsIgnoreCase("time"))
		{
			/* View by timestamp */
			timestamp = request.getParameter("timestamp");
		
			if(timestamp == null || timestamp.length() < 1)
			{
				// No data
				logText = "No timestamp!";
			}	
			else
			{
				int timestampLinenumber = LogReader.findFirstLineContaining(diskPath+fileName,timestamp+".*");
				logText = LogReader.get(diskPath + fileName,timestampLinenumber,defaultNumberOfLines).replaceAll(" ","&nbsp;");
			}
		}
		else if(mode != null && mode.equalsIgnoreCase("regexpr"))
		{
			/* View by regexpr */
			regexpr = request.getParameter("regexpr");
			
			if(regexpr == null)
			{
				logText = "No regular expression";
			}
			else
			{
				ln = request.getParameter("ln")!=null&&request.getParameter("ln").equalsIgnoreCase("true");
				grep = request.getParameter("grep")!=null&&request.getParameter("grep").equalsIgnoreCase("true");
				indent = request.getParameter("indent")!=null&&request.getParameter("indent").equalsIgnoreCase("true");
				
				if(grep){
					regexpr = ".*" + regexpr + ".*";
				}
				
				if(indent)
				{
					logText = LogReader.getByRegExpr(diskPath + fileName, regexpr, " ", ln);
				}
				else
				{
					logText = LogReader.getByRegExpr(diskPath + fileName, regexpr, 0, ln);
				}
			}
		}
		else
		{
			/* View by tail (default) */
			mode = "tail";
	
			try
			{
				iTime = Integer.parseInt(request.getParameter("time"));
			}
			catch(Exception e){/* Ignore - default value will do */}
			
			logText = LogReader.tail(diskPath + fileName,defaultNumberOfLines).replaceAll(" ","&nbsp;");
		}
	} 
	else 
	{
		mode = "tail";
		logText = "Invalid or missing crawl order";
	}
	
	String title = "View logs";
	int tab = 3;
	
%>

<%@include file="/include/head.jsp"%>
	<% if(iTime>0){ %>
		<meta http-equiv=Refresh content="<%=iTime%> URL=logs.jsp?time=<%=iTime%>&log=<%=fileName%>">
	<% } %>
	
	<% 
		if(theJob == null){
			out.println("<b>No job selected/availible</b>");
			return;
		} 
	%>
	<script type="text/javascript">
		function viewLog(log)
		{
			document.frmLogs.log.value = log;
			document.frmLogs.submit();
		}
		
		function changeMode(mode)
		{
			document.frmLogs.mode.value = mode;
			document.frmLogs.submit();
		}
	</script>

	<form method="get" action="logs.jsp" name="frmLogs">
		<input type="hidden" name="job" value="<%=theJob.getUID()%>">
		<table border="0" cellspacing="0" cellpadding="0">
			<tr>
				<td height="3"></td>
			</tr>
			<tr>
				<td valign="top" width="210">
					<table border="0" cellspacing="0" cellpadding="0">
						<tr>
							<td width="50" align="right" valign="top">
								&nbsp;<b>View:</b>&nbsp;
							</td>
							<td align="left" valign="top" width="160">
								<a href="javascript:viewLog('crawl.log')" <%=fileName.equalsIgnoreCase("crawl.log")?"style='text-decoration: none; color: #000000'":""%>>crawl.log</a><br>
								<a href="javascript:viewLog('local-errors.log')" <%=fileName.equalsIgnoreCase("local-errors.log")?"style='text-decoration: none; color: #000000'":""%>>local-errors.log</a><br>
								<a href="javascript:viewLog('progress-statistics.log')" <%=fileName.equalsIgnoreCase("progress-statistics.log")?"style='text-decoration: none; color: #000000'":""%>>progress-statistics.log</a><br>
								<a href="javascript:viewLog('reports.log')" <%=fileName.equalsIgnoreCase("reports.log")?"style='text-decoration: none; color: #000000'":""%>>reports.log</a><br>
								<a href="javascript:viewLog('runtime-errors.log')" <%=fileName.equalsIgnoreCase("runtime-errors.log")?"style='text-decoration: none; color: #000000'":""%>>runtime-errors.log</a><br>
								<a href="javascript:viewLog('uri-errors.log')" <%=fileName.equalsIgnoreCase("uri-errors.log")?"style='text-decoration: none; color: #000000'":""%>>uri-errors.log</a><br>
								<input type="hidden" name="log" value="<%=fileName%>">
							</td>
						</tr>
					</table>
				</td>
				<td valign="top" width="170">
					<table border="0" cellspacing="0" cellpadding="0">
						<tr>
							<td width="20" align="right" valign="top">
								&nbsp;<b>By:</b>&nbsp;
							</td>
							<td align="left" valign="top" width="150">
								<a href="javascript:changeMode('number')" <%=mode.equalsIgnoreCase("number")?"style='text-decoration: none; color: #000000'":""%>>Line number</a><br>
								<a href="javascript:changeMode('time')" <%=mode.equalsIgnoreCase("time")?"style='text-decoration: none; color: #000000'":""%>>Time stamp</a><br>
								<a href="javascript:changeMode('regexpr')" <%=mode.equalsIgnoreCase("regexpr")?"style='text-decoration: none; color: #000000'":""%>>Regular expression</a><br>
								<a href="javascript:changeMode('tail')" <%=mode.equalsIgnoreCase("tail")?"style='text-decoration: none; color: #000000'":""%>>Tail</a><br>
								<input type="hidden" name="mode" value="<%=mode%>">
							</td>
						</tr>
					</table>
				</td>
				<td valign="top">
				<% if(mode.equalsIgnoreCase("tail")){ %>
					<table border="0" cellspacing="0" cellpadding="0">
						<tr>
							<td>
								<b>Refresh time:</b>&nbsp;
							</td>
							<td>
								<select name="time" onChange="document.frmLogs.submit()" >
									<option value="-1" <%=iTime==-1?"selected":""%>>No refresh</option>
									<option value="2" <%=iTime==2?"selected":""%>>2 sec.</option>
									<option value="5" <%=iTime==5?"selected":""%>>5 sec.</option>
									<option value="10" <%=iTime==10?"selected":""%>>10 sec.</option>
									<option value="20" <%=iTime==20?"selected":""%>>20 sec.</option>
									<option value="30" <%=iTime==30?"selected":""%>>30 sec.</option>
								</select>
							</td>
						</tr>
					</table>
				<% } else if(mode.equalsIgnoreCase("number")){ %>
					<table border="0" cellspacing="0" cellpadding="0">
						<tr>
							<td nowrap align="right">
								<b>Line number:</b>&nbsp;
							</td>
							<td width="100%">
								<input value="<%=linenumber%>" name="linenumber">&nbsp;<input type="submit" value="Get">
							</td>
						</tr>
					</table>
				<% } else if(mode.equalsIgnoreCase("time")){ %>
					<table border="0" cellspacing="0" cellpadding="0">
						<tr>
							<td nowrap align="right" valign="top">
								<b>Timestamp:</b>&nbsp;
							</td>
							<td width="100%">
								<input value="<%=request.getParameter("timestamp")==null?"":request.getParameter("timestamp")%>" name="timestamp" align="absmiddle" size="21">&nbsp;<input type="submit" value="Get"><br>
								(YYYYMMDDHHMMSS)
							</td>
						</tr>
					</table>
				<% } else if(mode.equalsIgnoreCase("regexpr")){ %>
					<table border="0" cellspacing="0" cellpadding="0">
						<tr>
							<td nowrap align="right">
								<b>Regular expression:</b>&nbsp;
							</td>
							<td width="100%" colspan="4">
								<input size="50" name="regexpr" value="<%=request.getParameter("regexpr")==null?"":request.getParameter("regexpr")%>" align="absmiddle">&nbsp;<input type="submit" value="Get">
							</td>
						</tr>
						<tr>
							<td align="right">
								<font size="-2">(<a href="/admin/help/regexpr.jsp">about java reg.expr.</a>)</font>&nbsp;
							</td>
							<td nowrap>
								<input name="ln" value="true" type="checkbox" <%=request.getParameter("ln")!=null&&request.getParameter("ln").equalsIgnoreCase("true")?"checked":""%>>
							</td>
							<td nowrap>
								&nbsp;Line numbers&nbsp;&nbsp;
							</td>
						</tr>
						<tr>
							<td>
							</td>
							<td nowrap>
								<input name="indent" value="true" type="checkbox" <%=request.getParameter("indent")!=null&&request.getParameter("indent").equalsIgnoreCase("true")?"checked":""%>>
							</td>
							<td width="100%" colspan="3">
								&nbsp;Include following indented lines&nbsp;&nbsp;
							</td>
						</tr>
						<tr>
							<td>
							</td>
							<td nowrap>
								<input name="grep" value="true" type="checkbox" <%=request.getParameter("grep")!=null&&request.getParameter("grep").equalsIgnoreCase("true")?"checked":""%>>
							</td>
							<td nowrap>
								&nbsp;Grep style&nbsp;&nbsp;
							</td>
						</tr>
					</table>
				<% } %>
				</td>
			</tr>
		</table>
	</form>
	<p>
	<table border="0" cellspacing="0" cellpadding="0">
		<tr>
			<td colspan="2"></td>
			<td height="5" colspan="3">
			</td>
			<td colspan="2"></td>
		</tr>
		<tr>
			<td rowspan="5" width="3" nowrap >
			<td rowspan="5" width="1" nowrap bgcolor="#0c0c0c">
			</td>
			<td height="1" colspan="3" bgcolor="#0c0c0c">
			</td>
			<td rowspan="5" width="1" nowrap bgcolor="#0c0c0c">
			<td rowspan="5" width="3" nowrap >
		</tr>
		<tr>
			<td colspan="3">
				&nbsp;<%=fileName%> for <%=theJob.getJobName()%>
			</td>
		</tr>
		<tr>
			<td height="1" colspan="3" bgcolor="#0c0c0c">
			</td>
		</tr>
		<tr>
			<td colspan="3" class="main" width="400" height="100" valign="top">
					<pre><%=logText%></pre>
			</td>
		</tr>
		<tr>
			<td height="1" colspan="3" bgcolor="#0c0c0c">
			</td>
		</tr>
		<tr>
			<td height="5" colspan="3">
			</td>
		</tr>
	</table>
	
<%@include file="/include/foot.jsp"%>
