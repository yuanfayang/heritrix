<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<html>
	<body>
	<pre><%=handler.getThreadsReport().replaceAll(" ","&nbsp;")%></pre>
	</body>
</html>
