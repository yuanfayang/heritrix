<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.framework.CrawlJob,java.io.*,org.archive.util.ArchiveUtils" %>

<%
	CrawlJob job;
	String jobUID = request.getParameter("job");

	if(jobUID != null && jobUID.length() > 0){
		job = handler.getJob(jobUID);
	}else{
		// If no job UID is supplied we will assume the currently running job
		job = handler.getCurrentJob();
	}	

	if(job != null && job.isReadOnly()){
		// Can't edit a read only job!
		response.sendRedirect("/admin/jobs/noedit.jsp?job="+job.getUID());
		return;
	}
	
	if(request.getParameter("order") != null && job != null){
		// Got something in the request.  Let's update!
		String filename = ArchiveUtils.getFilePath(job.getCrawlOrderFile())+"job-"+request.getParameter("name")+"-"+(job.getOrderVersion()+1)+".xml";

		// Write the file.		
		try {
		    File file = new File(filename);
		    file.createNewFile();
		    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		    if (writer != null) {
		        writer.write(request.getParameter("order"));
		        writer.close();
		    }
		} catch (Exception e) {
		    // TODO: handle exception
		    e.printStackTrace();
		}
		
		job.setCrawlOrder(filename);
		if(handler.getCurrentJob()!=null && job.getUID().equals(handler.getCurrentJob().getUID()))
		{
			// Just updated a running job, must notify handler.
			handler.updateCrawlOrder();
		}
		response.sendRedirect("/admin/jobs.jsp?message=Crawl job modified");
		return;
	}
	
	CrawlOrder crawlOrder = null;
	BufferedReader crawlOrderContent = null;

	String title = "Modify crawl job";
	int tab = 1;
%>

<%@include file="/include/head.jsp"%>
	
	<% if(job == null) { %>
		No job selected
	<% } else { 
			crawlOrder = job.getCrawlOrder();
			crawlOrderContent = new BufferedReader(new FileReader(new File(job.getCrawlOrderFile())));
	%>

		<form xmlns:java="java" xmlns:ext="http://org.archive.crawler.admin.TextUtils" name="frmConfig" method="post" action="poweredit.jsp">

		<table border="0">
			<tr>
				<td nowrap>
					Crawl UID:&nbsp;
				</td>
				<td width="100%">
					<%=job.getUID()%><input type="hidden" name="job" value="<%=job.getUID()%>">
				</td>
			</tr>
			<tr>
				<td nowrap>
					Crawl name:&nbsp;
				</td>
				<td>
					<input name="name" value="<%=crawlOrder.getStringAt(SimpleHandler.XP_CRAWL_ORDER_NAME)%>" size="50">
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<b>Crawl order xml:</b>
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<textarea name="order" rows="12" cols="95"><%
						String sout = crawlOrderContent.readLine();
						while(sout!=null){
							out.println(sout);
							sout = crawlOrderContent.readLine();
						}
					%></textarea>
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<b>WARNING:</b> For advanced users only. Changes made will not be verified for correctness.
				</td>
			</tr>
		</table>
		<input type="submit" value="Update crawl order">
		
		</form>
		
	<% } %>
	
<%@include file="/include/foot.jsp"%>