<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>
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
	String title = "View logs";
	
%>

<%@include file="/include/head.jsp"%>
		<script type="text/javascript">
			function doLog(log)
			{
				document.frmLogs.log.value = log;
				document.frmLogs.submit();
			}
		</script>

		<table border="0" width="980">
			<tr>
				<td colspan="2">
					<fieldset style="width: 980px">
						<legend><%=log%></legend>
						<iframe name="frmCrawlLog" src="viewlogs_crawl.jsp?time=<%=iTime%>&log=<%=log%>" width="980" height="530" frameborder="0" ></iframe>
					</fieldset>
				</td>
			</tr>
			<tr>
				<td valign="top" width="270">
					<form method="post" action="viewlogs.jsp" name="frmLogs">
						Refresh time:
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
		</table>
	
	</body>
</html>