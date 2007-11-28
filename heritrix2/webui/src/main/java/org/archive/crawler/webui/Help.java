package org.archive.crawler.webui;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Help {

    public static void showHelp(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response){
        Misc.forward(request, response, "page_help.jsp");
    }
    
    public static void showAbout(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response){
        Misc.forward(request, response, "page_about_ui.jsp");
    }
    
    public static void showCodes(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response){
        Misc.forward(request, response, "page_codes.jsp");
    }
    
    public static void showRegExpr(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response){
        Misc.forward(request, response, "page_regexpr.jsp");
    }
    
    public static void showWebUIPrefs(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response){
        Misc.forward(request, response, "page_webui-prefs.jsp");
    }
    
    public static void changePassword(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        sc = request.getSession(true).getServletContext();
        System.out.println(System.identityHashCode(sc));
        String old = request.getParameter("old");
        String new1 = request.getParameter("new1");
        String new2 = request.getParameter("new2");
        if (!old.equals(AuthFilter.getUIPassword(sc))) {
            new Flash(Flash.Kind.NACK, "Wrong old password.").addToSession(request);
        } else if (!new1.equals(new2)) {
            new Flash(Flash.Kind.NACK, "New passwords didn't match.").addToSession(request);            
        } else {
            AuthFilter.setUIPassword(new1);
            new Flash("Password changed!").addToSession(request);            
        }
        
        showWebUIPrefs(sc, request, response);
    }
    
}
