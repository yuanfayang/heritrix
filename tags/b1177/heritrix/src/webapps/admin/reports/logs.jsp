<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%@page import="java.net.URLEncoder"%>

<%
	int iTime = 10;
	String log = "crawl.log";
	String mode = request.getParameter("mode");
	String iframesrc = null;
	
	if(request.getParameter("log") != null)
	{
		log = request.getParameter("log");
	}
	
	try
	{
		iTime = Integer.parseInt(request.getParameter("time"));
	}
	catch(Exception e){}

	if(mode != null && mode.equalsIgnoreCase("number"))
	{
		iframesrc = "/admin/iframes/viewlogs_linenumber.jsp?linenumber="+request.getParameter("linenumber")+"&log="+log;
	}
	else if(mode != null && mode.equalsIgnoreCase("time"))
	{
		iframesrc = "/admin/iframes/viewlogs_timestamp.jsp?timestamp="+request.getParameter("timestamp")+"&log="+log;
	}
	else if(mode != null && mode.equalsIgnoreCase("regexpr"))
	{
		iframesrc = "/admin/iframes/viewlogs_regexpr.jsp?regexpr="+URLEncoder.encode(request.getParameter("regexpr"),"UTF-8")+"&log="+log+"&ln="+request.getParameter("ln")+"&indent="+request.getParameter("indent");
	}
	else
	{
		// Assume tail
		mode = "tail";
		iframesrc = "/admin/iframes/viewlogs_tail.jsp?time="+iTime+"&log="+log;
	}

	String title = "View logs";
	
%>

<%@include file="/include/head.jsp"%>
	<script type="text/javascript">
		function doChangeMode()
		{
			if(document.frmLogs.mode.value == "number")
			{
				getStyleObject('tail').visibility = 'hidden';
				getStyleObject('times').visibility = 'hidden';
				getStyleObject('regexpr').visibility = 'hidden';
				getStyleObject('numbers').visibility = 'visible';
			}
			else if(document.frmLogs.mode.value == "time")
			{
				getStyleObject('tail').visibility = 'hidden';
				getStyleObject('numbers').visibility = 'hidden';
				getStyleObject('regexpr').visibility = 'hidden';
				getStyleObject('times').visibility = 'visible';
			}
			else if(document.frmLogs.mode.value == "regexpr")
			{
				getStyleObject('tail').visibility = 'hidden';
				getStyleObject('numbers').visibility = 'hidden';
				getStyleObject('times').visibility = 'hidden';
				getStyleObject('regexpr').visibility = 'visible';
			}
			else
			{
				// Assume tail.
				getStyleObject('numbers').visibility = 'hidden';
				getStyleObject('times').visibility = 'hidden';
				getStyleObject('regexpr').visibility = 'hidden';
				getStyleObject('tail').visibility = 'visible';
				//document.frmLogs.submit();
			}
		}
		
		function getTail()
		{
			window.frameCrawlLog.location = '/admin/iframes/viewlogs_tail.jsp?time='+document.frmLogs.time.value+'&log='+document.frmLogs.log.value;
		}
		
		function getLinenumber()
		{
			window.frameCrawlLog.location = '/admin/iframes/viewlogs_linenumber.jsp?linenumber='+document.frmLogs.linenumber.value+'&log='+document.frmLogs.log.value;
		}
		
		function getTimestamp()
		{
			window.frameCrawlLog.location = '/admin/iframes/viewlogs_timestamp.jsp?timestamp='+document.frmLogs.timestamp.value+'&log='+document.frmLogs.log.value;
		}
		
	</script>

	<form method="post" action="logs.jsp" name="frmLogs">
		<table border="0" cellspacing="0" cellpadding="0">
			<tr>
				<td height="3"></td>
			</tr>
			<tr>
				<td valign="top" width="230">
					<table border="0" cellspacing="0" cellpadding="0">
						<tr>
							<td width="50" align="right">
								&nbsp;<b>View:</b>&nbsp;
							</td>
							<td align="left" valign="top" width="180">
								<select name="mode" onChange="doChangeMode()">
									<option value="tail" <%=mode.equalsIgnoreCase("tail")?"selected":""%>>Tail</option>
									<option value="number" <%=mode.equalsIgnoreCase("number")?"selected":""%>>By line number</option>
									<option value="time" <%=mode.equalsIgnoreCase("time")?"selected":""%>>By time stamp</option>
									<option value="regexpr" <%=mode.equalsIgnoreCase("regexpr")?"selected":""%>>By regular expression</option>
								</select>
								<input type="hidden" name="log" value="<%=log%>">
							</td>
						</tr>
					</table>
				</td>
				<td>
					&nbsp;&nbsp;&nbsp;
				</td>
			</tr>
		</table>
		<!-- View specific configurations -->
		
		<!-- TAIL -->
		<div id="tail" style="position:absolute; top: 82px; left: 220px; visibility:<%=mode.equalsIgnoreCase("tail")?"visible":"hidden"%>">
			<table border="0" cellspacing="0" cellpadding="0">
				<tr>
					<td>
						<b>Refresh time:</b>&nbsp;
					</td>
					<td>
						<select name="time">
							<option value="-1" <%=iTime==-1?"selected":""%>>No refresh</option>
							<option value="2" <%=iTime==2?"selected":""%>>2 sec.</option>
							<option value="5" <%=iTime==5?"selected":""%>>5 sec.</option>
							<option value="10" <%=iTime==10?"selected":""%>>10 sec.</option>
							<option value="20" <%=iTime==20?"selected":""%>>20 sec.</option>
							<option value="30" <%=iTime==30?"selected":""%>>30 sec.</option>
						</select>
						<input type="button" onClick="getTail()" value="Get">
					</td>
				</tr>
			</table>
		</div>
		<!-- END TAIL -->
		<!-- LINE NUMBER -->
		<div id="numbers" style="position:absolute; top: 82px; left: 220px; visibility:<%=mode.equalsIgnoreCase("number")?"visible":"hidden"%>">
			<table border="0" cellspacing="0" cellpadding="0">
				<tr>
					<td nowrap align="right">
						<b>Line number:</b>&nbsp;
					</td>
					<td width="100%">
						<input value="<%=request.getParameter("linenumber")==null?"":request.getParameter("linenumber")%>" name="linenumber">&nbsp;<input type="button" onClick="getLinenumber()" value="Get">
					</td>
				</tr>
			</table>
		</div>	
		<!-- END LINE NUMBER -->
		<!-- TIME STAMP -->
		<div id="times" style="position:absolute; top: 82px; left: 220px; visibility:<%=mode.equalsIgnoreCase("time")?"visible":"hidden"%>">
			<table border="0" cellspacing="0" cellpadding="0">
				<tr>
					<td nowrap align="right">
						<b>Timestamp:</b>&nbsp;
					</td>
					<td width="100%">
						<input value="<%=request.getParameter("timestamp")==null?"":request.getParameter("timestamp")%>" name="timestamp" align="absmiddle">&nbsp;(YYYYMMDDHHMMSS)&nbsp;<input type="button" onClick="getTimestamp()" value="Get">
					</td>
				</tr>
			</table>
		</div>	
		<!-- END TIME STAMP -->
		<!-- REGULAR EXPRESSION -->
		<div id="regexpr" style="position:absolute; top: 82px; left: 220px; visibility:<%=mode.equalsIgnoreCase("regexpr")?"visible":"hidden"%>">
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
					<td>
					</td>
					<td nowrap>
						<input name="ln" value="true" type="checkbox" <%=request.getParameter("ln")!=null&&request.getParameter("ln").equalsIgnoreCase("true")?"checked":""%>>
					</td>
					<td nowrap>
						&nbsp;Line numbers&nbsp;&nbsp;
					</td>
					<td nowrap>
						<input name="indent" value="true" type="checkbox" <%=request.getParameter("indent")!=null&&request.getParameter("indent").equalsIgnoreCase("true")?"checked":""%>>
					</td>
					<td width="100%">
						&nbsp;Include following indented lines&nbsp;&nbsp;
					</td>
				</tr>
			</table>
		</div>	
		<!-- END REGULAR EXPRESSION -->
	</form>
	
	<table border="0" width="100%" cellspacing="0" cellpadding="0">
		<tr>
			<td colspan="2"></td>
			<td width="100%" height="5" colspan="3">
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
				&nbsp;<%=log%>
			</td>
		</tr>
		<tr>
			<td height="1" colspan="3" bgcolor="#0c0c0c">
			</td>
		</tr>
		<tr>
			<td colspan="3">
					<iframe name="frameCrawlLog" src="<%=iframesrc%>" width="100%" height="530" frameborder="0" ></iframe>
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
