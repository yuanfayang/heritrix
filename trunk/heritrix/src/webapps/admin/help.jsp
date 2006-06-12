<%@include file="/include/handler.jsp"%>

<%
    String title = "Help";
    int tab = 6;
    String favicon = request.getParameter("favicon");
    if(favicon!=null) {
        handler.setFavicon(favicon);
    }
%>

<%@include file="/include/head.jsp"%>

<p>
    <b>Heritrix online help</b>
</p>
<p>
    <b><a target="_blank" 
    href="<%=request.getContextPath()%>/docs/articles/user_manual.html">User
        Manual</a></b><br> Covers creating, configuring, launching,
        monitoring and analysing crawl jobs. For all users.
</p>
<p>
    <b><a target="_blank" 
        href="<%=request.getContextPath()%>/docs/articles/developer_manual.html">Developer Manual</a></b><br> Covers how to write add on modules for Heritrix
        and provides in depth coverage of Heritrix's architecture. For
        advanced users.
</p>
<p>
    <b><a target="_blank" 
        href="<%=request.getContextPath()%>/docs/articles/releasenotes.html">Release Notes</a></b><br>
</p>
<p>
	<b><a href="http://crawler.archive.org/issue-tracking.html" target="_blank">Issue Tracking</a></b><br />
	If you have found a bug or would like to see new features in Heritrix, check the following links:
	<ul>
		<li><a href="http://sourceforge.net/tracker/?atid=539099&amp;group_id=73833&amp;func=browse" target="_blank">Bugs</a></li>
		<li><a href="http://sourceforge.net/tracker/?atid=539102&amp;group_id=73833&amp;func=browse" target="_blank">Feature Requests</a></li>
	</ul>
</p>
<p>
    <b><a href="http://crawler.archive.org/mail-lists.html" target="_blank">Mailing Lists</a></b><br />
    For general discussion on Heritrix, use our <a href="http://groups.yahoo.com/group/archive-crawler/" target="_blank">Crawler Discussion List</a>.
</p>
<p>
    <b><a href="<%=request.getContextPath()%>/help/regexpr.jsp">Regular Expressions</a></b><br />
    Information about the regular expressions used in Heritrix and a tool to double check that your regular expressions are valid and that they correctly identify the desired strings.
</p>
<p>
    <b><a href="<%=request.getContextPath()%>/help/codes.jsp">URI Fetch Status Codes</a></b><br />
    This reference details what each of the fetch status codes assigned to URIs means.
</p>
<p>
    <b>Reset Favicon</b><br />
    To help distinguish multiple crawler web interfaces, you may choose this 
    web interface's 'favicon' by clicking any of the following: 
    <a href="<%=request.getContextPath()%>?favicon=h.ico"><img src="<%=request.getContextPath()%>/images/h.ico"/></a>
    <a href="<%=request.getContextPath()%>?favicon=h-blue.ico"><img src="<%=request.getContextPath()%>/images/h-blue.ico"/></a>
    <a href="<%=request.getContextPath()%>?favicon=h-purple.ico"><img src="<%=request.getContextPath()%>/images/h-purple.ico"/></a>
    <a href="<%=request.getContextPath()%>?favicon=h-red.ico"><img src="<%=request.getContextPath()%>/images/h-red.ico"/></a>
    <a href="<%=request.getContextPath()%>?favicon=h-orange.ico"><img src="<%=request.getContextPath()%>/images/h-orange.ico"/></a>
    <a href="<%=request.getContextPath()%>?favicon=h-yellow.ico"><img src="<%=request.getContextPath()%>/images/h-yellow.ico"/></a>
    <a href="<%=request.getContextPath()%>?favicon=h-green.ico"><img src="<%=request.getContextPath()%>/images/h-green.ico"/></a>
    <a href="<%=request.getContextPath()%>?favicon=h-teal.ico"><img src="<%=request.getContextPath()%>/images/h-teal.ico"/></a>
</p>
<hr />
<font size="-1">Heritrix version @VERSION@</font>
<%@include file="/include/foot.jsp"%>
