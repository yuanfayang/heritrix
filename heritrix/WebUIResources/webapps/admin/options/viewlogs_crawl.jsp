<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.admin.CrawlJob" %>

<jsp:useBean id="textutils" class="org.archive.crawler.admin.TextUtils" scope="application">
</jsp:useBean>

<%
	int iTime = 10;
	
	try
	{
		iTime = Integer.parseInt(request.getParameter("time"));
	}
	catch(Exception e){}

	CrawlOrder crawlOrder = handler.getDefaultCrawlOrder();
	if(handler.getCurrentJob() != null)
	{
		// Use current job settings rather then default
		crawlOrder = CrawlOrder.readFromFile(handler.getCurrentJob().getOrderFile());
	}
	
	String fileName = request.getParameter("log");
	String diskPath = crawlOrder.getStringAt(handler.XP_DISK_PATH)+"/";
	
%>

<html>
	<head>
		<meta http-equiv=Refresh content="<%=iTime%> URL=/admin/options/viewlogs_crawl.jsp?time=<%=iTime%>&log=<%=request.getParameter("log")%>">
	</head>
	<body>
		<pre><%= textutils.tail(diskPath + fileName,30) %></pre>
	</body>
</html>
