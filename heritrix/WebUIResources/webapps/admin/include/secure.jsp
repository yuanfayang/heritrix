<%
		User user = (User)session.getAttribute("user");
		
		if(user == null || user.authenticate() != User.ADMINISTRATOR)
		{
			// Not logged in.  Send to login page.
			response.sendRedirect("/admin/login.jsp?action=redirect");
		}
%>