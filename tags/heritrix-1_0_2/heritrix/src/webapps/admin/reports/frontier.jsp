<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%
	String title = "Frontier report";
	int tab = 4;
%>

<%@include file="/include/head.jsp"%>
		<pre><%=handler.getFrontierReport().replaceAll(" ","&nbsp;")%></pre>
<%@include file="/include/foot.jsp"%>