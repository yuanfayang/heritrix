<HTML>
<%@ include file="header.html" %>
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

<div id=orderform>
Crawl Configuration (Basic | Advanced )
<%@ include file="orderform.html" %>
</div>

<div id=bottomnavlinks>
<%@ include file="navlinks.html" %>
</div>

</BODY>
</HTML>
