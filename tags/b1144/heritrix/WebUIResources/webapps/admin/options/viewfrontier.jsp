<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<html>
	<body>
		<pre><%=handler.getFrontierReport().replaceAll(" ","&nbsp;")%></pre>
	</body>
</html>
