<%@page import="org.archive.crawler.admin.auth.User" %>

<%
		User user = (User)session.getAttribute("user");
		
		if(user == null || user.authenticate() != User.ADMINISTRATOR)
		{
			// Not logged in.  Send to login page.
			response.sendRedirect("/admin/login.jsp?action=redirect");
			return; // Without this the rest of the code on the page would be executed before the redirect is done.
		}
%>