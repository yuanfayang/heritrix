<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.framework.CrawlJob,java.io.*,org.archive.util.ArchiveUtils" %>

<%
	CrawlJob job = handler.getCurrentJob(); // This page is only intended for updating the current job.  Warning: This may be unsafe if the current job terminates while edits are being made.

	if(request.getParameter(SimpleHandler.XP_CRAWL_ORDER_NAME) != null)
	{
		// Got something in the request.  Let's update!
		String filename = ArchiveUtils.getFilePath(job.getCrawlOrderFile())+"job-"+request.getParameter(SimpleHandler.XP_CRAWL_ORDER_NAME)+"-"+(job.getOrderVersion()+1)+".xml";
		handler.createCrawlOrderFile(request,filename,"seeds-"+request.getParameter(SimpleHandler.XP_CRAWL_ORDER_NAME)+".txt",true);
		job.setCrawlOrder(filename);
		handler.updateCrawlOrder();
		response.sendRedirect("/admin/main.jsp");
		return;
	}
	
	CrawlOrder crawlOrder = null;
	BufferedReader seeds = null;
	int iInputSize = 50;

	if(job != null)
	{	
		crawlOrder = job.getCrawlOrder();
		seeds = new BufferedReader(new FileReader(new File(crawlOrder.getStringAt(SimpleHandler.XP_SEEDS_FILE))));
	}

	String title = "Update current job";
%>

<%@include file="/include/head.jsp"%>
	
	<% if(job == null) { %>
		No current job
	<% } else { %>

		<form xmlns:java="java" xmlns:ext="http://org.archive.crawler.admin.TextUtils" name="frmConfig" method="post" action="updatecurrent.jsp">

		<table border="0">
			<tr>
				<td>
					Crawl name:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_CRAWL_ORDER_NAME%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_CRAWL_ORDER_NAME)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Comment:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_CRAWL_COMMENT%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_CRAWL_COMMENT)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					<b>Crawl scope</b>
				</td>
			</tr>
			<tr>
				<td>
					Maximum link hops:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_MAX_LINK_HOPS%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_MAX_LINK_HOPS)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Maximum trans hops:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_MAX_TRANS_HOPS%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_MAX_TRANS_HOPS)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Mode:
				</td>
				<td>
					<select name="<%=SimpleHandler.XP_CRAWL_MODE%>">
						<option value="broad" <%=crawlOrder.getStringAt(SimpleHandler.XP_CRAWL_MODE).equals("broad")?"selected":""%>>Broad</option>
						<option value="domain" <%=crawlOrder.getStringAt(SimpleHandler.XP_CRAWL_MODE).equals("domain")?"selected":""%>>Domain</option>
						<option value="host" <%=crawlOrder.getStringAt(SimpleHandler.XP_CRAWL_MODE).equals("host")?"selected":""%>>Host</option>
						<option value="path" <%=crawlOrder.getStringAt(SimpleHandler.XP_CRAWL_MODE).equals("path")?"selected":""%>>Path</option>
					</select>
				</td>
			</tr>
			<tr>
				<td>
					<b>Stop crawl after</b>
				</td>
			</tr>
			<tr>
				<td>
					Maximum time (sec):
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_MAX_TIME%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_MAX_TIME)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Maximum bytes to download:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_MAX_BYTES_DOWNLOAD%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_MAX_BYTES_DOWNLOAD)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Maximum documents to download:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_MAX_DOCUMENT_DOWNLOAD%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_MAX_DOCUMENT_DOWNLOAD)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					<b>Politeness</b>
				</td>
			</tr>
			<tr>
				<td>
					Delay factor:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_POLITENESS_DELAY_FACTOR%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_POLITENESS_DELAY_FACTOR)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Min delay (ms):
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_POLITENESS_MIN_DELAY%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_POLITENESS_MIN_DELAY)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Max delay (ms):
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_POLITENESS_MAX_DELAY%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_POLITENESS_MAX_DELAY)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Min interval (ms):
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_POLITENESS_MIN_INTERVAL%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_POLITENESS_MIN_INTERVAL)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					<b>HTTP Fetch</b>
				</td>
			</tr>
			<tr>
				<td>
					Max fetch attempts:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_HTTPFETCH_MAX_FETCH_ATTEMPTS%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_HTTPFETCH_MAX_FETCH_ATTEMPTS)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Socket timeout (ms):
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_HTTPFETCH_SOTIMEOUT%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_HTTPFETCH_SOTIMEOUT)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Timout (sec):
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_HTTPFETCH_TIMEOUT%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_HTTPFETCH_TIMEOUT)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Max length (bytes):
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_HTTPFETCH_MAX_LENGTH_BYTES%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_HTTPFETCH_MAX_LENGTH_BYTES)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					<b>ARC file configurations</b>
				</td>
			</tr>
			<tr>
				<td>
					ARC Prefix:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_ARC_PREFIX%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_ARC_PREFIX)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					ARC Use compression:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_ARC_COMPRESSION_IN_USE%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_ARC_COMPRESSION_IN_USE)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Max ARC size:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_MAX_ARC_SIZE%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_MAX_ARC_SIZE)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					ARC dump path:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_ARC_DUMP_PATH%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_ARC_DUMP_PATH)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					<b>Request headers</b>
				</td>
			</tr>
			<tr>
				<td>
					User agent:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_HTTP_USER_AGENT%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_HTTP_USER_AGENT)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					From:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_HTTP_FROM%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_HTTP_FROM)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					<b>Crawler behavior</b>
				</td>
			</tr>
			<tr>
				<td>
					Disk path:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_DISK_PATH%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_DISK_PATH)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Max worker threads:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_MAX_TOE_THREADS%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_MAX_TOE_THREADS)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					<b>Logging</b>
				</td>
			</tr>
			<tr>
				<td>
					Crawl statistics interval (sec):
				</td>
				<td>
					<input name="<%="//loggers/crawl-statistics/@interval-seconds"%>" value="<%=crawlOrder.getStringAt("//loggers/crawl-statistics/@interval-seconds")%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					<b>Seeds</b>
				</td>
			</tr>
			<!--tr>
				<td>
					Seeds file:
				</td>
				<td>
					<input name="<%=SimpleHandler.XP_SEEDS_FILE%>" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_SEEDS_FILE)%>" size="<%=iInputSize%>">
				</td>
			</tr-->
			<tr>
				<td valign="top">
					Seeds:
				</td>
				<td>
					<textarea name="<%=SimpleHandler.XP_SEEDS%>" rows="8" cols="<%=iInputSize%>"><%
							String sout = seeds.readLine();
							while(sout!=null){
								out.println(sout);
								sout = seeds.readLine();
							}
						%></textarea>
				</td>
			</tr>
		</table>
		
		<input type="submit" value="Update job">
		
		</form>
		
	<% } %>
	
<%@include file="/include/foot.jsp"%>
