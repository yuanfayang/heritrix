<%@ page import="org.archive.crawler.admin.CrawlJobHandler, org.archive.crawler.Heritrix" %>

<%
	/**
	 * This include page ensures that the handler exists and
	 * is ready to be accessed.
	 */
	CrawlJobHandler handler = (CrawlJobHandler)application.getAttribute("handler");
	
	// If handler is empty then this is the first time this bit of code is being run
	// since the server came online.  In that case get or create the handler.
	if(handler == null)
	{
		if(Heritrix.getJobHandler() != null)
		{
			handler = Heritrix.getJobHandler();
		}
		else
		{
			handler = new CrawlJobHandler();
		}
		application.setAttribute("handler",handler);
	}
%>