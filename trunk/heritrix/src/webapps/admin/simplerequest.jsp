<%@include file="/include/secure_limited.jsp"%>
<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.admin.CrawlJob" %>

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
					<!--tr>
						<td valign="top">
							<strong>Max run time (min):</strong>
						</td>
						<td>
							<input> (max value: 1,800 min)
						</td>
					</tr>
					<tr>
						<td valign="top">
							<strong>Max number of documents:</strong>
						</td>
						<td>
							<input> (max value: 100,000)
						</td>
					</tr>
					<tr>
						<td valign="top">
							<strong>Max amount of data (Mb):</strong>
						</td>
						<td>
							<input> (max value: 10,000 Mb)
						</td>
					</tr-->
			<%
				}
				else
				{
					out.println("<tr><td></td><td>");
					switch(Integer.parseInt(sProfile))
					{
						case 1 : out.println("'Page' crawl: Get seed pages and their embedded resources (frames, online images), but follow no links.");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_ORDER_NAME+"\" value=\"Simple crawl - page\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_LINK_HOPS+"\" value=\"0\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_TRANS_HOPS+"\" value=\"5\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_MODE+"\" value=\"broad\">");
								 break;
						case 2 : out.println("'Page+1' crawl: Get seed pages and their embedded resources, and follow all links one hop out, getting those pages and their embedded resources, also.");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_ORDER_NAME+"\" value=\"Simple crawl - page+1\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_LINK_HOPS+"\" value=\"1\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_TRANS_HOPS+"\" value=\"5\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_MODE+"\" value=\"broad\">");
								 break;
						case 3 : out.println("'Path' crawl: Get seed pages and their embedded resources, and follow links to all URIs which are proper path-extensions of the seeds, up to 3 hops out.");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_ORDER_NAME+"\" value=\"Simple crawl - path\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_LINK_HOPS+"\" value=\"3\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_TRANS_HOPS+"\" value=\"5\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_MODE+"\" value=\"path\">");
								 break;
						case 4 : out.println("'Host' crawl: Get seed pages and their embedded resources, and follow links to all URIs on the same hostnames as the seeds, up to 5 hops out.");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_ORDER_NAME+"\" value=\"Simple crawl - host\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_LINK_HOPS+"\" value=\"5\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_TRANS_HOPS+"\" value=\"5\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_MODE+"\" value=\"host\">");
						         break;
						case 5 : out.println("'Domain' crawl: Get seed pages and their embedded resources, and follow links to all URIs in the same general domain as the seeds, up to 6 hops out. ");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_ORDER_NAME+"\" value=\"Simple crawl - domain\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_LINK_HOPS+"\" value=\"6\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_MAX_TRANS_HOPS+"\" value=\"5\">");
								 out.println("<input type=\"hidden\" name=\""+handler.XP_CRAWL_MODE+"\" value=\"domain\">");
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