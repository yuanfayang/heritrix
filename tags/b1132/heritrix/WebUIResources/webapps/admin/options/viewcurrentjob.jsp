<%@include file="/include/handler.jsp"%>


	<html>
		<head>
			<title>Crawl in progress</title>
		</head>
		<body>

		<fieldset style="width: 600px" align="top">
			<legend>Crawler status</legend>
			<iframe name="frmStatus" src="/admin/status.jsp?time=10" width="730" height="170" frameborder="0" ></iframe>
		</fieldset><a href="/admin/main.jsp">Main page</a>
		<fieldset style="width: 600px" align="top">
			<legend>Frontier data</legend>
			<iframe name="frmFrontier" src="/admin/options/viewfrontier.jsp" width="730" height="300" frameborder="0" ></iframe>
		</fieldset><a href="viewfrontier.jsp">View frontier report in fullscreen</a>
		<fieldset style="width: 600px" align="top">
			<legend>Thread data</legend>
			<iframe name="frmThreads" src="/admin/options/viewthreads.jsp" width="730" height="300" frameborder="0" ></iframe>
		</fieldset><a href="viewthreads.jsp">View threads report in fullscreen</a>
		</body>
	</html>
