<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.datamodel.settings.*" %>
<%@ page import="org.archive.crawler.framework.CrawlController" %>

<%@ page import="java.io.*,java.lang.Boolean,java.util.Vector" %>
<%@ page import="javax.management.MBeanInfo, javax.management.Attribute, javax.management.MBeanAttributeInfo,javax.management.AttributeNotFoundException, javax.management.MBeanException,javax.management.ReflectionException"%>

<%
	// Get the default settings.
	
	CrawlJob theJob = handler.getJob(request.getParameter("job"));
	
	
	if(theJob == null)
	{
		// Didn't find any job with the given UID or no UID given.
		response.sendRedirect("/admin/jobs.jsp?message=No job selected");
		return;
	} else if(theJob.isReadOnly() || theJob.isRunning()){
		// Can't edit this job.
		response.sendRedirect("/admin/jobs.jsp?message=Can't edit modules on a running or read only job");
		return;
	}

	XMLSettingsHandler settingsHandler = (XMLSettingsHandler)theJob.getSettingsHandler();


	if(request.getParameter("action") != null){
		// Need to take some action.
		String action = request.getParameter("action");
		if(action.equals("done")){
			// Ok, done editing.
			if(theJob.isNew()){			
				handler.addJob(theJob);
				response.sendRedirect("/admin/jobs.jsp?message=Job created");
			}else{
				if(theJob.isRunning()){
					handler.kickUpdate();
				}
				if(theJob.isProfile()){
					response.sendRedirect("/admin/profiles.jsp?message=Profile modified");
				}else{
					response.sendRedirect("/admin/jobs.jsp?message=Job modified");
				}
			}
			return;
		}else if(action.equals("configure")){
			// Go to configure settings.
			response.sendRedirect("/admin/jobs/configure.jsp?job="+theJob.getUID());
			return;
		}else if(action.equals("modules")){
			// Go to modules
			response.sendRedirect("/admin/jobs/modules.jsp?job="+theJob.getUID());
			return;
		}
	}

	String title = "Adjust modules";
	int tab = theJob.isProfile()?2:1;
%>

<%@include file="/include/head.jsp"%>
<script type="text/javascript">
	function doSubmit(){
		document.frmModules.submit();
	}
	
	function doGotoConfigure(){
		document.frmModules.action.value="configure";
		doSubmit();
	}
	
	function doGotoModules(){
		document.frmModules.action.value="modules";
		doSubmit();
	}
</script>
<p>
<form name="frmModules" method="post" action="modules.jsp">
	<input type="hidden" name="job" value="<%=theJob.getUID()%>">
	<input type="hidden" name="action" value="done">
	<input type="button" value="Adjust modules" onClick="doGotoModules()">
	<input type="button" value="Configure settings" onClick="doGotoConfigure()">
	<input type="button" value="Done" onClick="doSubmit()">

<p>
	<b>Select module to apply filter to</b>

<p>	
	<input type="button" value="Adjust modules" onClick="doGotoModules()">
	<input type="button" value="Configure settings" onClick="doGotoConfigure()">
	<input type="button" value="Done" onClick="doSubmit()">
</form>
<%@include file="/include/foot.jsp"%>


