<%@include file="/include/handler.jsp"%>

<html>
	<body>
		<pre><%=handler.getFrontierReport().replaceAll(" ","&nbsp;")%></pre>
	</body>
</html>
