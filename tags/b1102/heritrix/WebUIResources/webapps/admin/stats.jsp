<HTML>
<%@ include file="header.html" %>

<% 
String refreshRate = request.getParameter("RefreshRate");
if (refreshRate == null){
	refreshRate = "0";
}
%>

<SCRIPT language="javascript">
<!--
function refresh() {
	var timeout = <%= refreshRate %>;
	if (timeout != 0) {
		setTimeout('self.location.reload();', timeout*1000);
	}
}
//-->
</SCRIPT>
<BODY onLoad="refresh();">


<%@ include file="logo.html" %>

<fieldset>
<%@ include file="navlinks.html" %>
</fieldset>

<fieldset>
<jsp:include page="statusmsg.jsp" >
	<jsp:param name="message" value="<%= request.getAttribute("message") %>" />
</jsp:include>
</fieldset>

<fieldset>
<legend>
Statistics and Monitoring (<a href="/admin/servlet/AdminController?CrawlerAction=4">Refresh<a>)
</legend>
<form name="input" action="/admin/servlet/AdminController" method="get">
<input type="hidden" name="CrawlerAction" value="4"></input>
Auto-Refresh in <select name="RefreshRate">
<option><%= refreshRate %></option>
<option>0</option>
<option>3</option>
<option>5</option>
<option>7</option>
<option>10</option>
<option>20</option>
<option>60</option>
<option>600</option>
</SELECT>
seconds. <input type="submit" value="Submit"> (use 0 to turn autorefresh OFF)
</form>
Total Threads: <%= request.getAttribute("ThreadCount") %> <br>
Total Active Threads: <font color="red"><%= request.getAttribute("ActiveThreadCount") %></font><br>
Total Bytes : <%= request.getAttribute("TotalBytesWritten") %> <br>
<center>
<table border=1>
<tr>
<td><center><b><u>DOWNLOADED/DISCOVERED DOCUMENT RATIO</u></b><br>
<jsp:include page="progressbar.jsp" >
	<jsp:param name="begin" value="<%= request.getAttribute("UriFetchSuccessCount") %>" />
	<jsp:param name="end" value="<%= request.getAttribute("UrisEncounteredCount") %>" />
	<jsp:param name="description" value="DOWNLOADED/DISCOVERED DOCUMENT RATIO" />
</jsp:include>
</td>
</tr>
</table>
<br>
<%
String url = "/admin/servlet/AdminController?CrawlerAction=4";
%>
<a href="<%= url + "&UriProcessing=1" %>">Tail URI Processing</a> | 
<a href="<%= url + "&UriErrors=1" %>">Tail URI Errors</a> | 
<a href="<%= url + "&CrawlErrors=1" %>">Tail Crawl Errors</a>
<br>
<br>
</center>
</fieldset>

<jsp:useBean id="textutils" class="org.archive.crawler.admin.TextUtils" scope="application">
</jsp:useBean>

<%
String fileName = "/uri-processing.log";
String description = "URI Porcessing";
String diskPath = (String)request.getAttribute("DiskPath");

if (request.getParameter("UriErrors") != null){
	fileName = "/uri-errors.log";
	description = "URI Errors";
}else{
	if(request.getParameter("CrawlErrors") != null){
		fileName = "/crawl-errors.log";
		description = "Crawl Errors";
	}
}
%>
<fieldset>
	<legend><%= description %></legend>
<pre>
<%= textutils.tail(diskPath + fileName) %>
</pre>
</fieldset>

<fieldset>
<%@ include file="navlinks.html" %>
</fieldset>

</BODY>
</HTML>
