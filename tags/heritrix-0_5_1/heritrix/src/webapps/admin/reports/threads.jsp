<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%
	String title = "Threads report";
	int tab = 4;
%>

<%@include file="/include/head.jsp"%>

	<pre><%=handler.getThreadsReport().replaceAll(" ","&nbsp;")%></pre>

<%@include file="/include/foot.jsp"%>
