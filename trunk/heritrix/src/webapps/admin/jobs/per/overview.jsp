<%
  /**
   * This pages displays availible options for per host overrides of 
   * a given crawl job.
   *
   * @author Kristinn Sigurdsson
   */
%>
<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@page import="java.util.ArrayList"%>

<%@page import="org.archive.crawler.admin.CrawlJob"%>
<%@page import="org.archive.crawler.datamodel.settings.CrawlerSettings"%>
<%@page import="org.archive.crawler.datamodel.settings.XMLSettingsHandler"%>

<%
	// Load the job.
	CrawlJob theJob = handler.getJob(request.getParameter("job"));
	
	if(theJob == null)
	{
		// Didn't find any job with the given UID or no UID given.
		response.sendRedirect("/admin/jobs.jsp?message=No job selected "+request.getParameter("job"));
		return;
	} else if(theJob.isReadOnly()){
		// Can't edit this job.
		response.sendRedirect("/admin/jobs.jsp?message=Can't configure a running job");
		return;
	}

	// Load display level
	String currDomain = request.getParameter("currDomain");
	String parentDomain = null;
	if(currDomain==null){
		currDomain = "";
	} else {
		if(currDomain.indexOf('.')==-1){
			parentDomain = "";
		} else {
			parentDomain = currDomain.substring(currDomain.indexOf('.')+1);
		}
	}

	// Get the settings objects.
	XMLSettingsHandler settingsHandler = theJob.getSettingsHandler();
	CrawlerSettings localSettings = settingsHandler.getSettingsObject(currDomain);
	
	// Check for actions
	String action = request.getParameter("action");
	if(action != null){
		// Need to do something!
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
		}else if(action.equals("modules")){
			// Go to modules.
			response.sendRedirect("/admin/jobs/modules.jsp?job="+theJob.getUID());
			return;
		} else if(action.equals("filters")){
			// Go to filters.
			response.sendRedirect("/admin/jobs/filters.jsp?job="+theJob.getUID());
			return;
		} else if(action.equals("configure")){
			// Go to configure settings.
			response.sendRedirect("/admin/jobs/configure.jsp?job="+theJob.getUID());
			return;
		}
	}
	
	String title = "Per domain settings";
	int tab = theJob.isProfile()?2:1;
	int jobtab = 3;
%>
<%@include file="/include/head.jsp"%>
	<script type="text/javascript">
		function doSubmit(){
			document.frmPer.submit();
		}
		
		function doGotoModules(){
			document.frmPer.action.value="modules";
			doSubmit();
		}
		
		function doGotoFilters(){
			document.frmPer.action.value="filters";
			doSubmit();
		}

		function doGotoConfigure(){
			document.frmPer.action.value="configure";
			doSubmit();
		}
		
		function doGotoDomain(domain){
			document.frmPer.currDomain.value = domain;
			document.frmPer.action.value="noop";
			doSubmit();
		}
	</script>	
	<p>
		<%@include file="/include/jobnav.jsp"%>
	<p>
	<form name="frmPer" method="post" action="overview.jsp">
		<input type="hidden" name="action" value="done">
		<input type="hidden" name="job" value="<%=theJob.getUID()%>">
		<input type="hidden" name="currDomain" value="<%=currDomain%>">
		<b>
			Known overrides for 
			<%=currDomain.length()>0?"'"+currDomain+"' of the ":""%>
			<%=theJob.isProfile()?"profile":"job"%>
			<%=theJob.getJobName()%>:
		</b>
		<p>
		<ul>
		<% 	
			if(currDomain.length()>0) { 
		%>
				<li> <a href="javascript:doGotoDomain('<%=parentDomain%>')">- Up -</a>
		<%
			}
			ArrayList subs = settingsHandler.getDomainOverrides(currDomain);
			for(int i=0 ; i < subs.size() ; i++){
				String printDomain = (String)subs.get(i);
				if(currDomain.length()>0){
					printDomain += "."+currDomain;
				}
		%>
				<li> <a href="javascript:doGotoDomain('<%=printDomain%>')"><%=printDomain%></a>
		<%
			}
		%>
		</ul>
		<% 
			if(currDomain.length()>0){ 
				if(localSettings != null){
		%>
					<p><a href="">Edit override for '<%=currDomain%>'</a><br>
		<%
				} else {
		%>
					<p><a href="">Create override for '<%=currDomain%>'</a>
		<% 
				}
			} 
		%>
		<p>
			<b>New override:</b><br>
			Domain: <input value="<%=currDomain%>"><input type="button" value="Create">
	</form>
<%@include file="/include/foot.jsp"%>