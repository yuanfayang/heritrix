<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>
<%@page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.framework.CrawlJob,org.archive.util.LogReader" %>
<%@page import="java.net.URLDecoder"%>

<%
	String regexpr = request.getParameter("regexpr");

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
		<pre><%
			boolean ln = request.getParameter("ln")!=null&&request.getParameter("ln").equalsIgnoreCase("true");
			
			if(request.getParameter("indent")!=null&&request.getParameter("indent").equalsIgnoreCase("true"))
			{
				out.println(LogReader.getByRegExpr(diskPath + fileName, regexpr, " ", ln));
			}
			else
			{
				out.println(LogReader.getByRegExpr(diskPath + fileName, regexpr, 0, ln));
			}
		%></pre>
	</body>
</html>
