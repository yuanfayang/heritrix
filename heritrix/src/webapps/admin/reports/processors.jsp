<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%
	String title = "Processors report";
	int tab = 4;
%>

<%@include file="/include/head.jsp"%>

	<pre><%=handler.getProcessorsReport().replaceAll(" ","&nbsp;")%></pre>

<%@include file="/include/foot.jsp"%>
