<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>

<%
	/**
	 * This page enables changes to the default crawl job settings.
	 * (order.xml is rewritten)
	 */

	if(request.getParameter(handler.XP_CRAWL_ORDER_NAME) != null)
	{
		// Got something in the request.  Let's update!
		handler.updateDefaultCrawlOrder(request);
		response.sendRedirect("/admin/main.jsp");
	}


	CrawlOrder crawlOrder = handler.getDefaultCrawlOrder();
	int iInputSize = 50;

	String title = "Default crawl order";
%>

<%@include file="/include/head.jsp"%>

		<form xmlns:java="java" xmlns:ext="http://org.archive.crawler.admin.TextUtils" name="frmConfig" method="post" action="defaultcrawlorder.jsp">

		<%@include file="/include/jobconfig.jsp"%>
		
		<input type="submit" value="Apply changes">
		
		</form>
		
<%@include file="/include/foot.jsp"%>