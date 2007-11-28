<%@page import="org.archive.crawler.webui.AuthFilter"%>
<%
  /* 
   * Very basic single-password authentication support, in concert
   * with the AuthFilter configured to redirect all unauthorized 
   * sessions to here. 
   */
  String password = request.getParameter("enteredPassword");
  if(password!=null && password.equals(AuthFilter.getUIPassword(application))) {
    // auth successful
    
    request.getSession(true).setAttribute(AuthFilter.IS_AUTHORIZED,true);
    // send to continueUrl or servlet root
    String continueUrl = (String)request.getSession(true).getAttribute(AuthFilter.CONTINUE_URL);
    if(continueUrl == null || continueUrl.matches("(?i).*\\.(gif|jpe?g|ico|png)$")) {
        continueUrl = request.getContextPath();
    }
    response.sendRedirect(continueUrl);
  }
%>
<html>
    <head>
        <title>Auth</title>
    </head>
    <body onLoad="document.passwordForm.enteredPassword.focus()">

    <form name="passwordForm">
    Auth? <input method="POST" type="password" name="enteredPassword"/>
    </form>

    </body>
</html> 