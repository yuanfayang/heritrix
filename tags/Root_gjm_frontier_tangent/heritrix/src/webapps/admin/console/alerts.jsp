<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%@ page import="java.util.Vector" %>
<%@ page import="org.archive.crawler.admin.Alert"%>

<%
	String action = request.getParameter("action");
	if(action != null){
		String alertIDs[] = request.getParameterValues("alerts");
		
		for(int i=0 ; i<alertIDs.length ; i++){		
			if(action.equals("markasread")){
				Heritrix.seenAlert(alertIDs[i]);				
			} else if(action.equals("delete")){
				Heritrix.removeAlert(alertIDs[i]);
			}
		}
	}

	Vector alerts = Heritrix.getAlerts();
	
	
	String title = "Alerts";
	int tab = 0;
	
%>

<%@include file="/include/head.jsp"%>

<script type="text/javascript">
	function doDeleteAll(){
		document.frmAlerts.action.value="delete";
		document.frmAlerts.submit();
	}
	
	function doMarkAllAsRead(){
		document.frmAlerts.action.value="markasread";
		document.frmAlerts.submit();
	}
</script>

<p>

<% if(alerts.size() == 0){ %>
	There are no alerts at this time.
<% } else { %>
	<form name="frmAlerts" method="post" action="alerts.jsp">
	<input type="hidden" name="action">
	<table cellspacing="0" cellpadding="0" border="0">
		<tr>
			<th>
			</th>
			<th>
				&nbsp;Time of alert&nbsp;
			</th>
			<th>
				&nbsp;Alert title&nbsp;
			</th>
		</tr>
		<%
			boolean alt = true;
			for(int i = alerts.size()-1 ; i >= 0 ; i--)
			{
				Alert alert = (Alert)alerts.get(i);
		%>
				<tr <%=alt?"bgcolor='#EEEEFF'":""%> <%=alert.isNew()?"style='font-weight: bold'":""%>>
					<td nowrap>
						&nbsp;<input name="alerts" value="<%=alert.getID()%>" type="checkbox">&nbsp;
					</td>
					<td nowrap>
						<a style="color: #000000; text-decoration: none" href="/admin/console/readalert.jsp?alert=<%=alert.getID()%>"><%=sdf.format(alert.getTimeOfAlert())%> GMT</a>&nbsp;&nbsp;
					</td>
					<td nowrap>
						<a style="color: #000000; text-decoration: none" href="/admin/console/readalert.jsp?alert=<%=alert.getID()%>"><%=alert.getTitle()%></a>
					</td>
				</tr>
		<%
				alt = !alt;
			}
		%>
	</table>
	</form>
	<p>
		<input type="button" value="Mark selected as read" onClick="doMarkAllAsRead()">
		<input type="button" value="Delete selected" onClick="doDeleteAll()">
<% } %>
<%@include file="/include/foot.jsp"%>