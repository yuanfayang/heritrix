<html>
<head>
    <%@include file="/include/header.jsp"%>
    <title>Heritrix Help</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<div class="margined">
    <h1>Heritrix online help</h1>
<p>
    <b><a href="<%=request.getContextPath()%>/help/do_show_about_ui.jsp">About Heritrix</a></b></br>
    Includes license and current environment information.
</p>
<p>
    <b><a target="_blank" 
        href="<%=request.getContextPath()%>/docs/articles/releasenotes/index.html">Release Notes</a></b><br>
</p>
<p>
    <b><a href="<%=request.getContextPath()%>/help/do_show_webui-prefs.jsp">Change the Web UI Password</a></b>
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
    <b><a href="<%=request.getContextPath()%>/help/do_show_regexpr.jsp">Regular Expressions</a></b><br />
    Information about the regular expressions used in Heritrix and a tool to double check that your regular expressions are valid and that they correctly identify the desired strings.
</p>
<p>
    <b><a href="<%=request.getContextPath()%>/help/do_show_codes.jsp">URI Fetch Status Codes</a></b><br />
    This reference details what each of the fetch status codes assigned to URIs means.
</p>
<hr />
<font size="-1">Heritrix version @VERSION@ (Web UI)</font>
</div>
</body>
</html>