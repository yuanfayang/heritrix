<%
	int iTime = 10;
	String log = "crawl.log";
	
	if(request.getParameter("log") != null)
	{
		log = request.getParameter("log");
	}
	
	try
	{
		iTime = Integer.parseInt(request.getParameter("time"));
	}
	catch(Exception e){}
%>

<html>
	<head>
		<title>View logs</title>
	</head>
	<body>
		<table border="0">
			<tr>
				<td>
					<fieldset style="width: 600px">
						<legend>Status</legend>
						<iframe name="frmStatus" src="/admin/status.jsp?time=<%=iTime%>" width="730" height="200" frameborder="0" ></iframe>
					</fieldset>
				</td>
				<td valign="top">
					<form method="post" action="viewlogs.jsp" name="frmLogs">
					Refresh time:<br>
					<select name="time" onChange="document.frmLogs.submit()">
						<option value="-1" <%=iTime==-1?"selected":""%>>No refresh</option>
						<option value="2" <%=iTime==2?"selected":""%>>2 sec.</option>
						<option value="5" <%=iTime==5?"selected":""%>>5 sec.</option>
						<option value="10" <%=iTime==10?"selected":""%>>10 sec.</option>
						<option value="20" <%=iTime==20?"selected":""%>>20 sec.</option>
						<option value="30" <%=iTime==30?"selected":""%>>30 sec.</option>
					</select>
					<input type="hidden" name="log" value="<%=log%>">
					</form>
				</td>
			</tr>
			<tr>
				<td>
					<fieldset style="width: 600px">
						<legend><%=log%></legend>
						<iframe name="frmCrawlLog" src="viewlogs_crawl.jsp?time=<%=iTime%>&log=<%=log%>" width="730" height="530" frameborder="0" ></iframe>
					</fieldset>
				</td>
				<td valign="top">
					<a href="viewlogs.jsp?time=<%=iTime%>&log=crawl.log">crawl.log</a><br>
					<a href="viewlogs.jsp?time=<%=iTime%>&log=local-errors.log">local-errors.log</a><br>
					<a href="viewlogs.jsp?time=<%=iTime%>&log=progress-statistics.log">progress-statistics.log</a><br>
					<a href="viewlogs.jsp?time=<%=iTime%>&log=runtime-errors.log">runtime-errors.log</a><br>
					<a href="viewlogs.jsp?time=<%=iTime%>&log=uri-errors.log">uri-errors.log</a><br>
				</td>
			</tr>
		</table>
		
		<a href="/admin/main.jsp">Main page</a>
	</body>
</html>