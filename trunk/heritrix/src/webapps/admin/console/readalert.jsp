<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.admin.Alert" %>

<%
	Alert alert = Heritrix.getAlert(request.getParameter("alert"));
	alert.setAlertSeen();
	String title = "Read alert";
	int tab = 0;
%>

<%@include file="/include/head.jsp"%>
<p>
<% if(alert == null){ %>
	<b> No matching alert found </b>
<% } else { %>
	<table>
		<tr>
			<td>
				<b>Title:</b>&nbsp;
			</td>
			<td>
				<%=alert.getTitle()%>
			</td>
		</tr>
		<tr>
			<td>
				<b>Time:</b>&nbsp;
			</td>
			<td>
				<%=sdf.format(alert.getTimeOfAlert())%>
			</td>
		</tr>
		<tr>
			<td valign="top">
				<b>Message:</b>&nbsp;
			</td>
			<td>
				<pre><%=alert.getBody()%></pre>
			</td>
		</tr>
	</table>
<% } %>
	<p>
		<a href="/admin/console/alerts.jsp">Back to alerts</a>
		&nbsp;&nbsp;&nbsp;-&nbsp;&nbsp;&nbsp;
		<a href="/admin/console/alerts.jsp?alertIDs=<%=alert.getID()%>&action=delete">Delete this alert</a>
<%@include file="/include/foot.jsp"%>
