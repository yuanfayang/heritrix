<%@page import="org.archive.crawler.admin.auth.*" %>

<%
	String sMessage = null;
	String sAction = request.getParameter("action");
	if(sAction!=null)
	{
		// Some action has been specified.
		if(sAction.equalsIgnoreCase("redirect"))
		{
			sMessage = "You must log in";
		}
		else if(sAction.equalsIgnoreCase("login"))
		{
			// Attempt login
			User user = new User(request.getParameter("username"),request.getParameter("password"));
			if(user==null || user.authenticate()==User.INVALID_USER)
			{
				// Not logged in
				sMessage = "Login failed";
			}
			else if(user.authenticate()==User.USER)
			{
				// Redirect to 'User' start page
				session.setAttribute("user",user);
				response.sendRedirect("/admin/simplerequesthandler.jsp");
			}
			else if(user.authenticate()==User.ADMINISTRATOR)
			{
				// Redirect to 'Administrator' start page.
				session.setAttribute("user",user);
				response.sendRedirect("/admin/main.jsp");
			}
		}
		else if(sAction.equalsIgnoreCase("logout"))
		{
			// Logging out
			session.removeAttribute("user");
		}
	}
	

%>

<HTML>
	<head>
	</head>
	<body>
		<p><strong>Heritrix login</strong>
		<% if(sMessage != null ){ %>
		<p><font color=red><%=sMessage%></font>
		<%}%>
		<form method="post" action="login.jsp">
			<input type="hidden" name="action" value="login">
			<p>
				Username: <input name="username"><br>
				Password: <input type="password" name="password">
			<p>
				<input type="submit" value="Login">
		</form>
	</body>
</HTML>
	
