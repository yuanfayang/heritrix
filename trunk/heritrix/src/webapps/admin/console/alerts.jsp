<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%@ page import="java.util.Vector,java.util.HashMap" %>
<%@ page import="java.util.logging.Level"%>
<%@ page import="org.archive.crawler.admin.Alert"%>

<%
    HashMap levelColors = new HashMap(7);
    levelColors.put(Level.SEVERE,"#da9090");
    levelColors.put(Level.WARNING,"#daaaaa");
    levelColors.put(Level.INFO,"#dab0b0");
    levelColors.put(Level.CONFIG,"#dababa");
    levelColors.put(Level.FINE,"#dac0c0");
    levelColors.put(Level.FINER,"#dacaca");
    levelColors.put(Level.FINEST,"#dad0d0");

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
	<table cellspacing="1" cellpadding="0" border="0">
		<tr>
			<th>
			</th>
			<th>
				&nbsp;Time of alert&nbsp;
			</th>
			<th>
                &nbsp;Level&nbsp;
			<th>
				&nbsp;Alert title&nbsp;
			</th>
		</tr>
		<%
			for(int i = alerts.size()-1 ; i >= 0 ; i--)
			{
				Alert alert = (Alert)alerts.get(i);
		%>
				<tr bgcolor="<%=levelColors.get(alert.getLevel())%>" <%=alert.isNew()?"style='font-weight: bold'":""%>>
					<td nowrap>
						&nbsp;<input name="alerts" value="<%=alert.getID()%>" type="checkbox">&nbsp;
					</td>
                    <td nowrap>
                        &nbsp;<a style="color: #003399; text-decoration: none" href="/admin/console/readalert.jsp?alert=<%=alert.getID()%>"><%=sdf.format(alert.getTimeOfAlert())%> GMT</a>&nbsp;
                    </td>
                    <td nowrap>
                        &nbsp;<a style="color: #000000; text-decoration: none" href="/admin/console/readalert.jsp?alert=<%=alert.getID()%>"><%=alert.getLevel().getName()%></a>&nbsp;
                    </td>
					<td nowrap>
						&nbsp;<a style="color: #000000; text-decoration: none" href="/admin/console/readalert.jsp?alert=<%=alert.getID()%>"><%=alert.getTitle()%></a>&nbsp;
					</td>
				</tr>
		<%
			}
		%>
	</table>
	</form>
	<p>
		<input type="button" value="Mark selected as read" onClick="doMarkAllAsRead()">
		<input type="button" value="Delete selected" onClick="doDeleteAll()">
<% } %>
<%@include file="/include/foot.jsp"%>