<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.framework.CrawlJob" %>

<jsp:useBean id="textutils" class="org.archive.util.TextUtils" scope="application">
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
		crawlOrder = handler.getCurrentJob().getCrawlOrder();
	}
	
	String fileName = request.getParameter("log");
	String diskPath = crawlOrder.getStringAt(handler.XP_DISK_PATH)+"/";
	
%>

<html>
	<head>
		<meta http-equiv=Refresh content="<%=iTime%> URL=/admin/options/viewlogs_crawl.jsp?time=<%=iTime%>&log=<%=request.getParameter("log")%>">
	</head>
	<body>
		<pre><%= textutils.tail(diskPath + fileName,30).replaceAll(" ","&nbsp;") %></pre>
	</body>
</html>
