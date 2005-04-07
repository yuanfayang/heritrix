<%@include file="/include/handler.jsp"%>

<%
    String title = "Help";
    int tab = 6;
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
<%@include file="/include/foot.jsp"%>
