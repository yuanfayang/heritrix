<%--For now just redirect all requests to root to the admin webapp.
    Later we can change it so the root is presentable listing 
    the webapps available such as root, arc viewer, etc.
 --%>
<% response.sendRedirect("/admin/"); %> 
