<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.framework.CrawlJob,java.io.*,org.archive.util.ArchiveUtils" %>
<%@ page import="javax.xml.parsers.SAXParserFactory,javax.xml.parsers.SAXParser,org.xml.sax.XMLReader,org.xml.sax.SAXException,org.xml.sax.InputSource" %>
<%
	CrawlJob job;
	String jobUID = request.getParameter("job");
	boolean wellformed = true;

	if(jobUID != null && jobUID.length() > 0){
		job = handler.getJob(jobUID);
	}else{
		// If no job UID is supplied we will assume the currently running job
		job = handler.getCurrentJob();
	}	

	if(job != null && job.isReadOnly()){
		// Can't edit a read only job!
		response.sendRedirect("/admin/jobs.jsp?message=Can not edit a read-only job");
		return;
	}
	
	if(request.getParameter("order") != null && job != null){
		// Got something in the request.  Check for well formedness

		try {
			InputSource inSource = new InputSource(new StringReader(request.getParameter("order")));
			SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
			SAXParser saxParser = saxParserFactory.newSAXParser();
			
			XMLReader xmlReader = saxParser.getXMLReader();
			xmlReader.parse(inSource);
		}
		catch (SAXException e) {
			wellformed = false;
		}
		
		if(wellformed)
		{
			// Ok everything checks out. Let's update!
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
	
			StringBuffer bufferorder = new StringBuffer();
			String sout = crawlOrderContent.readLine();
			while(sout!=null){
				bufferorder.append(sout+"\n");
				sout = crawlOrderContent.readLine();
			}
			
			String order = bufferorder.toString();
			String name = crawlOrder.getStringAt(SimpleHandler.XP_CRAWL_ORDER_NAME);
			
			if(wellformed == false){
				//Overwrite with input values (that are contain errors).
				order = request.getParameter("order");
				name = request.getParameter("name");
			}
	%>

		<form xmlns:java="java" xmlns:ext="http://org.archive.crawler.admin.TextUtils" name="frmConfig" method="post" action="poweredit.jsp">
		
		<% if(wellformed == false){ %>
			<font color="red"><b>ERROR: XML not well formed!</b></font>
		<% } %>
		
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
					<input name="name" value="<%=name%>" size="50">
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<b>Crawl order xml:</b>
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<textarea name="order" rows="12" cols="95"><%=order%></textarea>
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<b>WARNING:</b> For advanced users only. Changes made will not be verified for correctness, only for well formedness.
				</td>
			</tr>
		</table>
		<input type="submit" value="Update crawl order">
		
		</form>
		
	<% } %>
	
<%@include file="/include/foot.jsp"%>