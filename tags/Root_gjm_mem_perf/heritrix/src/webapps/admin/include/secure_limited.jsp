<%@page import="org.archive.crawler.admin.auth.User,java.net.URLEncoder" %><%!

	public static String getCookieValue(Cookie[] cookies,
                                        String cookieName,
                                        String defaultValue) {
	    if(cookies != null){
		    for(int i=0; i<cookies.length; i++) {
		    	Cookie cookie = cookies[i];
		      	if(cookieName.equals(cookie.getName())){
		        	return(cookie.getValue());
		    	}
		    }
		}
	    return(defaultValue);
  	}

%><%  
	
		/**
		 * Access control include
		 *
		 * One page (/iframes/xml.jsp) breaks in certain browsers (Mozilla) if
		 * there are any empty lines before it's content.  Since it includes
		 * this file it is important that it create no empty lines.
		 */

		User user = (User)session.getAttribute("user");
		
		if(user == null)
		{
			//Try cookies.
			user = new User(getCookieValue(request.getCookies(),"username",null),
			                getCookieValue(request.getCookies(),"password",null));
			                
		}

		if(user == null || (user.authenticate() != User.ADMINISTRATOR && user.authenticate() != User.USER))
		{
			// Not logged in.  Send to login page.
			response.sendRedirect("/admin/login.jsp?action=redirect&back=" + URLEncoder.encode(request.getRequestURL().toString()) );
			return; // Without this the rest of the code on the page would be executed before the redirect is done.
		}
%>