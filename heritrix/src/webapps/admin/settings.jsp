<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%
	String title = "General settings";
	int tab = 2;
%>

<%@include file="/include/head.jsp"%>


<p>
<b>Default crawl order</b><br>
<ul>
	<li><a href="/admin/settings/defaultcrawlorder.jsp">Modify</a> - <i>Safe, but limited options</i>
	<li><a href="/admin/settings/powereditdefaultcrawlorder.jsp">Power edit</a> - <i>Advanced users only</i>
</ul>
The default crawl order that is used as a template for new crawl jobs.<br>

<p>
<b>Access tool</b><br>
Not yet availible


<%@include file="/include/foot.jsp"%>
