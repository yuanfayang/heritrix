<%@ page import="org.archive.crawler.admin.SimpleHandler" %>

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
		handler = new SimpleHandler();
		application.setAttribute("handler",handler);
	}
%>