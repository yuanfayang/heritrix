<%@ page import="org.archive.util.ArchiveUtils" %>
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
        href="http://webteam.archive.org/confluence/display/Heritrix/2.0.0+Release+Notes">Release Notes</a></b><br>
</p>
<p>
    <b><a href="<%=request.getContextPath()%>/help/do_show_webui-prefs.jsp">Change the Web UI Password</a></b>
</p>
<p>
	<b><a href="http://crawler.archive.org/issue-tracking.html" target="_blank">Issue Tracking</a></b><br />
	If you have found a bug or would like to see new features in 
	Heritrix, you can report, discuss, and vote on outstanding issues
	in our <a href="http://webteam.archive.org/jira/browse/HER">Heritrix 
	JIRA issue tracker</a>.

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
<font size="-1">Heritrix version <%=ArchiveUtils.VERSION %> (Web UI)</font>
</div>
</body>
</html>