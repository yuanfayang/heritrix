<% 
	int begin = Integer.valueOf(request.getParameter("begin")).intValue();
	int end = Integer.valueOf(request.getParameter("end")).intValue();
	String description = request.getParameter("description");
	int ratio = (int) (100 * begin / end);
%>

<table border="0" cellpadding="0" cellspacing= "0" bgcolor= "white"> 
<tr>
<td width="20%"></td>
<td bgcolor="darkorange" width="<%= (int) (ratio/2) %>%">
	<strong><%= ratio %></strong>%
</td>
<td bgcolor="lightblue" align="right" width="<%= (int) ((100-ratio)/2) %>%">
	<strong>100%</strong>
</td>
<td nowrap>&nbsp;&nbsp;(<%= begin %> of <%= end %>)</td>
</tr>
</table>
