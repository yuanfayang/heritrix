<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder,org.archive.crawler.framework.CrawlJob,java.io.*,org.archive.util.ArchiveUtils" %>

<%
	/**
	 * This page enables changes to the default crawl job settings.
	 * (order.xml is rewritten)
	 */


	if(request.getParameter("order")!=null){
		// Got something in the request.  Let's update!
		String filename = handler.getDefaultOrderFileName();

		// Write the file.		
		try {
		    File file = new File(filename);
		    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		    if (writer != null) {
		        writer.write(request.getParameter("order"));
		        writer.close();
		    }
		} catch (Exception e) {
		    // TODO: handle exception
		    e.printStackTrace();
		}
				
		handler.setDefaultCrawlOrder(handler.getDefaultOrderFileName()); //Causes the handler to reload the default order file from disk.
		response.sendRedirect("/admin/settings.jsp");
		return;
	}
	
	BufferedReader crawlOrderContent = new BufferedReader(new FileReader(new File(handler.getDefaultOrderFileName())));

	String title = "Default crawl order";
	int tab = 2;
%>

<%@include file="/include/head.jsp"%>

	<form xmlns:java="java" xmlns:ext="http://org.archive.crawler.admin.TextUtils" name="frmConfig" method="post" action="powereditdefaultcrawlorder.jsp">

	<table border="0">
		<tr>
			<td nowrap>
				Default crawl order
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
	<input type="submit" value="Save changes">
	
	</form>
	
<%@include file="/include/foot.jsp"%>