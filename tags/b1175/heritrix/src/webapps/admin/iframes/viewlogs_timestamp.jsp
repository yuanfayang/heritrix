<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.framework.CrawlJob,org.archive.util.LogReader" %>

<%
	String timestamp = request.getParameter("timestamp");
	String logcontent = null;

	if(timestamp == null || timestamp.length() < 1)
	{
		// No data
		logcontent = "No timestamp!";
	}	
	else
	{

		CrawlOrder crawlOrder = handler.getDefaultCrawlOrder();
		if(handler.getCurrentJob() != null)
		{
			// Use current job settings rather then default
			crawlOrder = handler.getCurrentJob().getCrawlOrder();
		}
		
		String fileName = request.getParameter("log");
		String diskPath = crawlOrder.getStringAt(handler.XP_DISK_PATH)+"/";
		
		int linenumber = LogReader.findFirstLineContaining(diskPath+fileName,timestamp+".*");
		
		logcontent = LogReader.get(diskPath + fileName,linenumber,30).replaceAll(" ","&nbsp;");
	}
%>

<html>
	<body>
		<pre><%=logcontent%></pre>
	</body>
</html>
