<%@ page errorPage="/error.jsp" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<% 

{ // Start a new local variable scope so we don't clobber other pages.

Crawler the_crawler = (Crawler)request.getAttribute("crawler");
String the_job = (String)request.getAttribute("job");
String the_profile = (String)request.getAttribute("profile");
String the_sheet = (String)request.getAttribute("sheet");

%>

<table>
<tr>
<td>
<a border="0" href="<%=request.getContextPath()%>/index.jsp">
<img border="0" src="<%=request.getContextPath()%>/images/logo.gif" height="37" width="145">
</a>
</td>
<td>

<% if (the_crawler != null) { %>
   <b>Crawler:</b> 
   <a href="<%=request.getContextPath()%>/crawler_area/do_show_crawler.jsp?<%=the_crawler.getQueryString()%>">
    <%=Text.html(the_crawler.getLegend())%>
   </a>
   <br/>
<% } %>

<% if (the_job != null) { %>
   <b>Job:</b> 
   <%=Text.html(the_job)%>
   <br/>
<% } %>

<% if (the_profile != null) { %>
<b>Profile:</b> <%=Text.html(the_profile)%><br/>
<% } %>

<% if (the_sheet != null) { %>
<b>Sheet:</b> <%=Text.html(the_sheet)%><br/>
<% } %>

</td>
</table>

<% } // end of local variable scope %>