<%@include file="/include/secure_limited.jsp"%>
<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.admin.SimpleCrawlJob" %>

<%
	/**
	 * This page enables simple jobs to be scheduled (submitted to simplerequesthandler.jsp)
	 * (order.xml is used for defaults)
	 */
%>

<html>
	<head>
	</head>
	<body>
		<form name="frmProfiles" action="simplerequesthandler.jsp" method="post">

		<table border="0" cellspacing="0" cellpadding="0">
			<tr>
				<td colspan="2">
					<strong>SIMPLE CRAWL REQUEST</strong>
				</td>
			</tr>
			<tr>
				<td>
					<strong>Profile:</strong>
				</td>
				<td>
					<%
						String sProfile = request.getParameter("cboProfile");
						if(sProfile == null)
						{
							sProfile = "1";
						}
					%>
					<select name="cboProfile" onChange="document.frmProfiles.action='simplerequest.jsp';document.frmProfiles.submit()">
						<option value="1" <%=(sProfile.equals("1")?"selected":"")%>>Page</option>
						<option value="2" <%=(sProfile.equals("2")?"selected":"")%>>Page+1</option>
						<option value="3" <%=(sProfile.equals("3")?"selected":"")%>>Path</option>
						<option value="4" <%=(sProfile.equals("4")?"selected":"")%>>Host</option>
						<option value="5" <%=(sProfile.equals("5")?"selected":"")%>>Domain</option>
						<option value="-1" <%=(sProfile.equals("-1")?"selected":"")%>>Manually configured</option>
					</select>
				</td>
			</tr>
			<%
				if(sProfile.equals("-1"))
				{
			%>
					<tr>
						<td valign="top">
							<strong>Crawl mode:</strong>
						</td>
						<td>
							<select name="cboFilterMode">
								<option value="domain">Path</option>
								<option value="domain">Host</option>
								<option value="domain">Domain</option>
								<option value="domain">Broad</option>
							</select>
							<input type="hidden" name="<%=handler.XP_CRAWL_ORDER_NAME%>" value="Simple crawl - manual">
						</td>
					</tr>
					<tr>
						<td valign="top">
							<strong>Max embed depth:</strong>
						</td>
						<td>
							<input name="<%=handler.XP_MAX_TRANS_HOPS%>"> (max value: 8)
						</td>
					</tr>
					<tr>
						<td valign="top">
							<strong>Max link depth:</strong>
						</td>
						<td>
							<input name="<%=handler.XP_MAX_LINK_HOPS%>"> (max value: 8)
						</td>
					</tr>
					<tr>
						<td valign="top">
							<strong>Max run time (sec):</strong>
						</td>
						<td>
							<input name="<%=handler.XP_MAX_TIME%>"> (max value: 36,000 (10 hours))
						</td>
					</tr>
					<tr>
						<td valign="top">
							<strong>Max number of documents:</strong>
						</td>
						<td>
							<input name="<%=handler.XP_MAX_DOCUMENT_DOWNLOAD%>"> (max value: 500,000)
						</td>
					</tr>
					<tr>
						<td valign="top">
							<strong>Max amount of data (bytes):</strong>
						</td>
						<td>
							<input name="<%=handler.XP_MAX_BYTES_DOWNLOAD%>"> (max value: 10,737,418,240 (10 Gb))
						</td>
					</tr>
			<%
				}
				else
				{
					out.println("<tr><td></td><td>");
					switch(Integer.parseInt(sProfile))
					{
						case 1 : out.println("Description of a Page crawl");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_ORDER_NAME+"\" value=\"Simple crawl - page\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_LINK_HOPS+"\" value=\"0\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_TRANS_HOPS+"\" value=\"5\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_MODE+"\" value=\"broad\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_TIME+"\" value=\"600\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_DOCUMENT_DOWNLOAD+"\" value=\"2000\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_BYTES_DOWNLOAD+"\" value=\"10485760\">");
								 break;
						case 2 : out.println("Description of a Page+1 crawl");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_ORDER_NAME+"\" value=\"Simple crawl - page+1\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_LINK_HOPS+"\" value=\"1\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_TRANS_HOPS+"\" value=\"5\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_MODE+"\" value=\"broad\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_TIME+"\" value=\"1000\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_DOCUMENT_DOWNLOAD+"\" value=\"3000\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_BYTES_DOWNLOAD+"\" value=\"40485760\">");
								 break;
						case 3 : out.println("Description of a Path crawl");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_ORDER_NAME+"\" value=\"Simple crawl - path\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_LINK_HOPS+"\" value=\"3\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_TRANS_HOPS+"\" value=\"5\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_MODE+"\" value=\"path\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_TIME+"\" value=\"6000\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_DOCUMENT_DOWNLOAD+"\" value=\"20000\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_BYTES_DOWNLOAD+"\" value=\"104857600\">");
								 break;
						case 4 : out.println("Description of a Host crawl");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_ORDER_NAME+"\" value=\"Simple crawl - host\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_LINK_HOPS+"\" value=\"5\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_TRANS_HOPS+"\" value=\"5\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_MODE+"\" value=\"host\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_TIME+"\" value=\"10000\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_DOCUMENT_DOWNLOAD+"\" value=\"100000\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_BYTES_DOWNLOAD+"\" value=\"504857600\">");
						         break;
						case 5 : out.println("Description of a Domain crawl");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_ORDER_NAME+"\" value=\"Simple crawl - domain\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_LINK_HOPS+"\" value=\"6\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_TRANS_HOPS+"\" value=\"5\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_MODE+"\" value=\"domain\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_TIME+"\" value=\"36000\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_DOCUMENT_DOWNLOAD+"\" value=\"500000\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_BYTES_DOWNLOAD+"\" value=\"10737418240\">");
							     break;
					}
					out.println("</td></tr>");
				}
			%>
			<tr>
				<td valign="top">
					<strong>Seeds :</strong>
				</td>
				<td>
					<textarea name="<%=handler.XP_SEEDS%>" wrap="off" cols="48" rows="8"><%=(request.getParameter(handler.XP_SEEDS)==null)?"http://archive.org":request.getParameter(handler.XP_SEEDS)%></textarea>
				</td>
			</tr>
			<tr>
				<td colspan="2" align="right">
					<input type="submit" value="Submit job">
				</td>
			</tr>
		</table>
		</form>
	</body>
</html>