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
	CrawlJob theJob = handler.getJob(request.getParameter("job"));
	boolean isProfile = false;
	if(request.getParameter("profile") != null && request.getParameter("profile").equals("true")){
		isProfile = true;
	}
	
	if(theJob == null)
	{
		//Ok, use default profile then.
		theJob = handler.getDefaultProfile();
		if(theJob == null){
			// ERROR - This should never happen. There must always be at least one (default) profile.
			out.println("ERROR: NO PROFILE FOUND");
			return;
		}
	} 

	XMLSettingsHandler settingsHandler = theJob.getSettingsHandler();
	CrawlOrder crawlOrder = settingsHandler.getOrder();
    CrawlerSettings orderfile = settingsHandler.getSettingsObject(null);
    
    if(request.getParameter("action") != null){
    	//Make new job.
    	CrawlJob newJob;
    	if(isProfile){
    		newJob = handler.newProfile(theJob,request.getParameter("meta/name"),request.getParameter("meta/description"),request.getParameter("seeds"));
    	}else{
    		newJob = handler.newJob(theJob,request.getParameter("meta/name"),request.getParameter("meta/description"),request.getParameter("seeds"));
    	}
    	
    	if(request.getParameter("action").equals("configure")){
    		response.sendRedirect("/admin/jobs/configure.jsp?job="+newJob.getUID());
    	} else if(request.getParameter("action").equals("modules")){
    		response.sendRedirect("/admin/jobs/modules.jsp?job="+newJob.getUID());
    	} else if(request.getParameter("action").equals("filters")){
    		response.sendRedirect("/admin/jobs/filters.jsp?job="+newJob.getUID());
    	} else {
    		handler.addJob(newJob);
    		response.sendRedirect("/admin/jobs.jsp?message=Job created");
    	}
    	return;
    }
	
	String title = isProfile?"New profile":"New crawl job";
	int tab = isProfile?2:1;
%>

<%@include file="/include/head.jsp"%>

		<form name="frmNew" method="post" action="new.jsp">
			<input type="hidden" name="action" value="new">
			<input type="hidden" name="profile" value="<%=isProfile%>">
			<b>
				Create new 
			<% 	if(isProfile){ %>
				profile
			<%	} else { %>
				crawl job 
			<%	}	%>
				based on
			<% 	if(request.getParameter("job")==null){%>
				default profile
			<% 
				}else{ 
					if(theJob.isProfile()){
						out.println("profile ");					
					} else {
						out.println("job ");
					}
					out.println("'"+theJob.getJobName()+"'"); 
				}
			%>	
			</b>
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
			<input type="button" value="Select filters" onClick="document.frmNew.action.value='filters';document.frmNew.submit()">
			<input type="button" value="Configure settings" onClick="document.frmNew.action.value='configure';document.frmNew.submit()">
			<% if(isProfile == false){ %>
				<input type="submit" value="Submit job">
			<% } %>
		</form>
		
<%@include file="/include/foot.jsp"%>
