<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.datamodel.settings.*" %>
<%@ page import="org.archive.crawler.framework.CrawlController" %>

<%@ page import="java.io.*,java.lang.Boolean" %>
<%@ page import="javax.management.MBeanInfo, javax.management.Attribute, javax.management.MBeanAttributeInfo,javax.management.AttributeNotFoundException, javax.management.MBeanException,javax.management.ReflectionException"%>

<%
	/**
	 * Create a new job
	 */
	XMLSettingsHandler settingsHandler = new XMLSettingsHandler(new File(handler.getDefaultSettingsFilename()));
	settingsHandler.initialize();
	CrawlOrder crawlOrder = settingsHandler.getOrder();
    CrawlerSettings orderfile = settingsHandler.getSettingsObject(null);
    
    if(request.getParameter("action") != null){
    	//Make new job.
    	CrawlJob newJob = handler.newJob(settingsHandler,request.getParameter("meta/name"),request.getParameter("meta/description"),request.getParameter("seeds"));
    	if(request.getParameter("action").equals("configure")){
    		response.sendRedirect("/admin/jobs/configure.jsp?job="+newJob.getUID());
    	} else if(request.getParameter("action").equals("modules")){
    		response.sendRedirect("/admin/jobs/modules.jsp?job="+newJob.getUID());
    	} else {
    		handler.addJob(newJob);
    		response.sendRedirect("/admin/jobs.jsp?message=Job created");
    	}
    	return;
    }
	
	String title = "New crawl job";
	int tab = 1;
%>

<%@include file="/include/head.jsp"%>

		<form name="frmNew" method="post" action="new.jsp">
			<input type="hidden" name="action" value="new">		
			<b>Create new crawl job based on default profile</b>
			<p>			
			<table>
				<tr>
					<td>
						Name of new job:
					</td>
					<td>
						<input name="meta/name" value="<%=orderfile.getName()%>" style="width: 320px">
					</td>
				</tr>
				<tr>
					<td>
						Description:
					</td>
					<td>
						<input name="meta/description" value="<%=orderfile.getDescription()%>" style="width: 320px">
					</td>
				</tr>
				<tr>
					<td valign="top">
						Seeds:
					</td>
					<td>
						<textarea name="seeds" style="width: 320px" rows="8"><%
							BufferedReader seeds = new BufferedReader(new FileReader(settingsHandler.getPathRelativeToWorkingDirectory((String)((ComplexType)settingsHandler.getOrder().getAttribute("scope")).getAttribute("seedsfile"))));
							String sout = seeds.readLine();
							while(sout!=null){
								out.println(sout);
								sout = seeds.readLine();
							}
						%></textarea>
					</td>
				</tr>
			</table>
			<input type="button" value="Adjust modules" onClick="document.frmNew.action.value='modules';document.frmNew.submit()">
			<input type="button" value="Configure settings" onClick="document.frmNew.action.value='configure';document.frmNew.submit()">
			<input type="submit" value="Submit job">
		
		</form>
		
<%@include file="/include/foot.jsp"%>
