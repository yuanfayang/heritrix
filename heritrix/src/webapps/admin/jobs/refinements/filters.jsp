<%
  /**
   * This pages allows the user to add filters to overrides.
   *
   * @author Kristinn Sigurdsson
   */
%>
<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%@page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@page import="org.archive.crawler.framework.Filter" %>
<%@page import="org.archive.crawler.settings.refinements.*"%>


<%@include file="/include/jobfilters.jsp"%>

<%
	// Load the job to manipulate	
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

    // Load display level
    String currDomain = request.getParameter("currDomain");
    String reference = request.getParameter("reference");

    // Get the settings objects.
    XMLSettingsHandler settingsHandler = theJob.getSettingsHandler();
    CrawlOrder crawlOrder = settingsHandler.getOrder();
    boolean global = currDomain == null || currDomain.length() == 0;

    CrawlerSettings localSettings;
    
    if(global){
        localSettings = settingsHandler.getSettingsObject(null);
    } else {
        localSettings = settingsHandler.getSettingsObject(currDomain);
    }
    
    Refinement refinement = localSettings.getRefinement(reference);

    CrawlerSettings settings = refinement.getSettings();

	// See if we need to take any action
	if(request.getParameter("action") != null){
		// Need to take some action.
		String action = request.getParameter("action");
		if(action.equals("filters")){
			//Doing something with the filters.
			String subaction = request.getParameter("subaction");
			String map = request.getParameter("map");
			if(map != null && map.length() > 0){
				String filter = request.getParameter("filter");
				MapType filterMap = (MapType)settingsHandler.getComplexTypeByAbsoluteName(settings,map);
				if(subaction.equals("add")){
					//Add filter
					String className = request.getParameter(map+".class");
					String typeName = request.getParameter(map+".name");
					if(typeName != null && typeName.length() > 0 
					   && className != null && className.length() > 0 ){
						ModuleType tmp = SettingsHandler.instantiateModuleTypeFromClassName(typeName,className);
						filterMap.addElement(settings,tmp);
					}
				} else if(subaction.equals("moveup")){
					// Move a filter down in a map
					if(filter != null && filter.length() > 0){
						filterMap.moveElementUp(settings,filter);
					}
				} else if(subaction.equals("movedown")){
					// Move a filter up in a map
					if(filter != null && filter.length() > 0){
						filterMap.moveElementDown(settings,filter);
					}
				} else if(subaction.equals("remove")){
					// Remove a filter from a map
					if(filter != null && filter.length() > 0){
						filterMap.removeElement(settings,filter);
					}
				}
			}
			// Finally save the changes to disk
			settingsHandler.writeSettingsObject(settings);
		}else if(action.equals("done")){
			// Ok, done editing. Back to overview.
			if(theJob.isRunning()){
				handler.kickUpdate(); //Just to make sure.
			}
			response.sendRedirect("/admin/jobs/refinements/overview.jsp?job="+theJob.getUID()+"&currDomain="+currDomain+"&message=Override changes saved");
			return;
		}else if(action.equals("goto")){
            // Goto another page of the job/profile settings
			response.sendRedirect(request.getParameter("subaction")+"&currDomain="+currDomain+"&reference="+reference);
			return;
		}
	}

	// Set page header.
	String title = "Add override filters";
	int tab = theJob.isProfile()?2:1;
	int jobtab = 1;
%>

<%@include file="/include/head.jsp"%>
<script type="text/javascript">
	function doSubmit(){
		document.frmFilters.submit();
	}
	
	function doGoto(where){
		document.frmFilters.action.value="goto";
		document.frmFilters.subaction.value=where;
		doSubmit();
	}
	
	function doMoveUp(filter,map){
		document.frmFilters.action.value = "filters";
		document.frmFilters.subaction.value = "moveup";
		document.frmFilters.map.value = map;
		document.frmFilters.filter.value = filter;
		doSubmit();
	}

	function doMoveDown(filter,map){
		document.frmFilters.action.value = "filters";
		document.frmFilters.subaction.value = "movedown";
		document.frmFilters.map.value = map;
		document.frmFilters.filter.value = filter;
		doSubmit();
	}

	function doRemove(filter,map){
		document.frmFilters.action.value = "filters";
		document.frmFilters.subaction.value = "remove";
		document.frmFilters.map.value = map;
		document.frmFilters.filter.value = filter;
		doSubmit();
	}

	function doAdd(map){
        if(document.getElementById(map+".name").value == ""){
            alert("Must enter a unique name for the filter");
        } else {
			document.frmFilters.action.value = "filters";
			document.frmFilters.subaction.value = "add";
			document.frmFilters.map.value = map;
			doSubmit();
		}
	}
</script>
	<p>
        <b>Refinement '<%=refinement.getReference()%>' on '<%=global?"global settings":currDomain%>' of
        <%=theJob.isProfile()?"profile":"job"%>
        <%=theJob.getJobName()%>:</b>
        <%@include file="/include/jobrefinementnav.jsp"%>
	<p>
	<form name="frmFilters" method="post" action="filters.jsp">
		<input type="hidden" name="currDomain" value="<%=currDomain%>">
		<input type="hidden" name="job" value="<%=theJob.getUID()%>">
		<input type="hidden" name="action" value="done">
		<input type="hidden" name="subaction" value="">
		<input type="hidden" name="map" value="">
		<input type="hidden" name="filter" value="">
        <input type="hidden" name="reference" value="<%=reference%>">
		<p>
			<b>Instructions:</b> It is possible to add filters to overrides and manipulate existing<br>
			refinement filters. It is not possible to remove filters defined in a super domain!
		<p>
		<table>
			<%=printFilters(crawlOrder,settings,"",false,false,false,null,false,CrawlJobHandler.loadOptions(CrawlJobHandler.MODULE_OPTIONS_FILE_FILTERS))%>
		</table>
	</form>
	<p>
        <%@include file="/include/jobrefinementnav.jsp"%>
<%@include file="/include/foot.jsp"%>


