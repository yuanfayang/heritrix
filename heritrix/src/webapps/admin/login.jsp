<%@include file="/include/nocache.jsp"%>
<%@ page errorPage="/error.jsp" %>
<%@page import="java.net.URLDecoder" %>

<%
    String sMessage = null;
    String sAction = request.getParameter("action");
    if(sAction != null) {
        // Some action has been specified.
        if(sAction.equalsIgnoreCase("redirect")) {
            sMessage = "Incorrect login/password combination.";
        } else if(sAction.equalsIgnoreCase("login")) {
            // Do nothing.  Its all done by the container. 
        } else if(sAction.equalsIgnoreCase("logout")) {
            // Logging out.
            session = request.getSession();
            if (session != null) {
                session.invalidate();
                sMessage = "Successful logout.";
            }
        }
    }
%>

<html>
    <head>
        <title>Heritrix: Login</title>
        <link rel="stylesheet" 
            href="<%=request.getContextPath()%>/css/heritrix.css">
    </head>

    <body>
        <table border="0" cellspacing="0" cellpadding="0" height="100%">
            <tr>
                <td width="155" height="60" valign="top" nowrap>
                    <table border="0" cellspacing="0" cellpadding="0"
                            width="100%" height="100%">
                        <tr>
                            <td align="center" height="40" valign="bottom">
                                <a border="0" 
                                href="<%=request.getContextPath()%>/"><img border="0" src="<%=request.getContextPath()%>/images/logo.gif" width="145"></a>
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
                    <form method="post" 
                        action='<%= response.encodeURL("j_security_check") %>'>
                        <input type="hidden" name="action" value="login">
                        <input type="hidden" name="redirect" 
                            value="<%=request.getParameter("back")%>">
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
                                    <input name="j_username">
                                </td>
                            </tr>
                            <tr>
                                <td class="dataheader">
                                    Password:
                                </td>
                                <td>
                                    <input type="password" name="j_password">
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
