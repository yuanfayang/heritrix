<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.framework.CrawlJob,org.archive.util.LogReader" %>

<%
	int linenumber = 1;
	
	try
	{
		linenumber = Integer.parseInt(request.getParameter("linenumber"));
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
	<body>
		<pre><%= LogReader.get(diskPath + fileName,linenumber,30).replaceAll(" ","&nbsp;") %></pre>
	</body>
</html>
