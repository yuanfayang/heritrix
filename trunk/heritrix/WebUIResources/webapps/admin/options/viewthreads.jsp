<%@include file="/include/handler.jsp"%>

<html>
	<body>
	<pre><%=handler.getThreadsReport().replaceAll(" ","&nbsp;")%></pre>
	</body>
</html>
