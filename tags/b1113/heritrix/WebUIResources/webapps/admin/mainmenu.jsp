<HTML>
<%@include file="header.html" %>
<BODY>
<%@ include file="logo.html" %>
<div id=topnavlinks>
<%@ include file="navlinks.html" %>
</div>
<div id=statusmsg>
<jsp:include page="statusmsg.jsp" >
	<jsp:param name="message" value="<%= request.getAttribute("message") %>" />
</jsp:include>
</div>
<div id=mainmenu>
Main Menu:
<center>
<div id=innerlinks>
<a href="/admin/servlet/AdminController?CrawlerAction=0">Start New Crawl</a><br>
<a href="/admin/servlet/AdminController?CrawlerAction=0");">Configuration</a><br>
<a href="/admin/servlet/AdminController?CrawlerAction=4">Monitoring and Statistics</a><br>
</div>
</div>
</center>
<div id=bottomnavlinks>
<%@ include file="navlinks.html" %>
</div>
</BODY>
</HTML>
