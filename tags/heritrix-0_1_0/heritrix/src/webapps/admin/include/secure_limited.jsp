<%@page import="org.archive.crawler.admin.auth.User" %><%  
	
		/**
		 * Lesser access control include.  Allows both User and Administrator 
		 * user classes access.
		 *
		 * One page (/iframes/xml.jsp) breaks in certain browsers (Mozilla) if
		 * there are any empty lines before it's content.  Since it includes
		 * this file it is important that it create no empty lines.
		 */

		User user = (User)session.getAttribute("user");
		
		if(user == null || (user.authenticate() != User.ADMINISTRATOR && user.authenticate() != User.USER))
		{
			// Not logged in.  Send to login page.
			response.sendRedirect("/admin/login.jsp?action=redirect");
			return; // Without this the rest of the code on the page would be executed before the redirect is done.
		}
%>