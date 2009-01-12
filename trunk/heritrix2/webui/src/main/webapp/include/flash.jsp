<%@ page pageEncoding="UTF-8" %> 
<%@ page import="org.archive.crawler.webui.Flash" %>
<% 
// The default flash format

{ // Start a new local variable scope so we don't clobber other pages.
    Flash flash = (Flash) request.getAttribute("flash");
%>

<div class="flash <%=flash.getKind()%>"><div>
<%=flash.getMessage()%>
</div></div>

<%
}
%>
