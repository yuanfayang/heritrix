<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%
	String title = "Help";
	int tab = 6;
%>

<%@include file="/include/head.jsp"%>

<p>
	<b>Heritrix online help</b>

<p>
	<b><a target="_blank" href="/admin/docs/articles/user_manual.html">User Manual</a></b><br>
	Covers creating, configuring, launching, monitoring and analysing crawl jobs. For all users.

<p>
	<b><a target="_blank" href="/admin/docs/articles/developer_manual.html">Developer Manual</a></b><br>
	Covers how to write add on modules for Heritrix and provides in depth coverage of Heritrix's architecture. For advanced users.


<%@include file="/include/foot.jsp"%>