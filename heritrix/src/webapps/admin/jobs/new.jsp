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
    	CrawlJob newJob = handler.newJob(settingsHandler,request.getParameter("meta/name"));
    	if(request.getParameter("action").equals("configure")){
    		response.sendRedirect("/admin/jobs/configure.jsp?job="+newJob.getUID());
    	} else {
    		response.sendRedirect("/admin/jobs/modules.jsp?job="+newJob.getUID());
    	}
    	return;
    }
	
	String title = "New crawl job";
	int tab = 1;
%>

<%@include file="/include/head.jsp"%>

		<form name="frmNew" method="post" action="new.jsp">
			<input type="hidden" name="action" value="configure">		
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
			</table>
			<input type="button" value="Adjust modules" onClick="document.frmNew.action.value='modules';document.frmNew.submit()"> <input type="submit" value="Configure settings">
		
		</form>
		
<%@include file="/include/foot.jsp"%>
