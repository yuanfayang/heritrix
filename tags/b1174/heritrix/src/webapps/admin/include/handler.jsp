<%@ page import="org.archive.crawler.admin.SimpleHandler, org.archive.crawler.Heritrix" %>

<%
	/**
	 * This include page ensures that the handler exists and
	 * is ready to be accessed.
	 */
	SimpleHandler handler = (SimpleHandler)application.getAttribute("handler");
	
	// If handler is empty then this is the first time this bit of code is being run
	// since the server came online.  In that case create the handler.
	if(handler == null)
	{
		if(Heritrix.getHandler() != null)
		{
			// TODO: Once the selection of a CrawlJobHandler is configurable a typecast will be needed here.
			handler = Heritrix.getHandler();
		}
		else
		{
			handler = new SimpleHandler();
		}
		application.setAttribute("handler",handler);
	}
%>