<%@page import="org.archive.crawler.admin.auth.*,java.net.URLDecoder" %>

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
			else 
			{
				//Successful login.
				if(request.getParameter("remember") != null && request.getParameter("remember").equalsIgnoreCase("true"))
				{
					// Need to write cookies to remember login.
					Cookie userCookie = new Cookie("username", request.getParameter("username"));
					Cookie passCookie = new Cookie("password", request.getParameter("password"));
					userCookie.setMaxAge(60*60*24*365);//One year
					passCookie.setMaxAge(60*60*24*365);//One year
					response.addCookie(userCookie);
					response.addCookie(passCookie);
				}
				else
				{
					// Delete old cookies to be sure that no old logins are remembered.
					Cookie userCookie = new Cookie("username", "");
					Cookie passCookie = new Cookie("password", "");
					userCookie.setMaxAge(0);//One year
					passCookie.setMaxAge(0);//One year
					response.addCookie(userCookie);
					response.addCookie(passCookie);
				}
				
				session.setAttribute("user",user);
				String back = request.getParameter("redirect");
				String redirect = "";
				//Redirect to user group entry page.
				if(user.authenticate()==User.USER)
				{
					// Redirect to 'User' start page
					redirect = "/admin/simplerequest.jsp";
				}
				else if(user.authenticate()==User.ADMINISTRATOR)
				{
					// Redirect to 'Administrator' start page.
					redirect = "/admin/main.jsp";
				}
				
				if(back!=null && back.length()>0){
					redirect = URLDecoder.decode(back);
				}
				response.sendRedirect(redirect);
			}
		}
		else if(sAction.equalsIgnoreCase("logout"))
		{
			// Logging out
			session.removeAttribute("user");
		}
	}
	

%>

<html>
	<head>
		<title>Heritrix: Login</title>
		<link rel="stylesheet" href="/admin/css/heritrix.css">
	</head>

	<body>
		<table border="0" cellspacing="0" cellpadding="0" height="100%">
			<tr>
				<td width="155" height="60" valign="top" nowrap>
					<table border="0" cellspacing="0" cellpadding="0" width="100%" height="100%">
						<tr>
							<td align="center" height="40" valign="bottom">
								<a border="0" href="/admin/main.jsp"><img border="0" src="/admin/images/logo.gif" width="145"></a>
							</td>
						</tr>
						<tr>
							<td class="subheading">
								Login
							</td>
						</tr>
					</table>
				</td>
				<td width="100%">&nbsp;</td>
			</tr>
			<tr>
				<td bgcolor="#0000FF" height="1" colspan="2">
				</td>
			</tr>
			<tr>
				<td colspan="2" height="100%" valign="top" class="main">
					<form method="post" action="login.jsp">
						<input type="hidden" name="action" value="login">
						<input type="hidden" name="redirect" value="<%=request.getParameter("back")%>">
						<table border="0">
							<% if(sMessage != null ){ %>
								<tr>
									<td colspan="2" align="center">
										<b><font color=red><%=sMessage%></font></b>
									</td>
								</tr>
							<%}%>
							<tr>
								<td class="dataheader">
									Username:
								</td>
								<td> 
									<input name="username">
								</td>
							</tr>
							<tr>
								<td class="dataheader">
									Password:
								</td>
								<td>
									<input type="password" name="password">
								</td>
							</tr>
							<tr>
								<td colspan="2">
									<input type="checkbox" name="remember" value="true"> Remember my login on this computer
								</td>
							</tr>
							<tr>
								<td colspan="2" align="center">
									<input type="submit" value="Login">
								</td>
							</tr>
					</form>
				</td>
			</tr>
		</table>
	</body>
</HTML>
	
