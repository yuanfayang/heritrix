<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.framework.CrawlJob" %>

<%
	String title = "Administrator Console";
%>

<%@include file="/include/head.jsp"%>

					<p>
						<fieldset style="width: 600px">
							<legend>Crawler status</legend>
							<iframe name="frmStatus" src="/admin/iframes/status.jsp?time=5" width="730" height="185" frameborder="0" ></iframe>
						</fieldset><br>

<%@include file="/include/foot.jsp"%>
