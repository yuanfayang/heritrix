<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.settings.*" %>
<%@ page import="org.archive.crawler.framework.CrawlController" %>
<%@ page import="org.archive.util.TextUtils" %>

<%@ page import="java.io.*,java.lang.Boolean,java.util.ArrayList" %>
<%@ page import="javax.management.MBeanInfo, javax.management.Attribute, javax.management.MBeanAttributeInfo,javax.management.AttributeNotFoundException, javax.management.MBeanException,javax.management.ReflectionException"%>

<%!
	/**
	 * Builds the HTML for selecting an implementation of a specific crawler module 
	 * 
	 * @param module The MBeanAttributeInfo on the currently set module
	 * @param availibleOptions A list of the availibe implementations (full class names as Strings)
	 * @param name The name of the module
	 *
	 * @return the HTML for selecting an implementation of a specific crawler module
	 */
	public String buildModuleSetter(MBeanAttributeInfo module, ArrayList availibleOptions, String name, String currentDescription){
		StringBuffer ret = new StringBuffer();
		
		ArrayList unusedOptions = new ArrayList();

		// Let's figure out which are not being used.
		for(int i=0 ; i<availibleOptions.size() ; i++){
			if(module.getType().equals((String)availibleOptions.get(i))==false){
				unusedOptions.add(availibleOptions.get(i));
			}
		}
		
		ret.append("<table><tr><td>&nbsp;Current selection:</td><td>");
		ret.append(module.getType());
		ret.append("</td><td></td></tr>");
		ret.append("<tr><td></td><td width='100' colspan='2'><i>" + currentDescription + "</i></td>");
		
		if(unusedOptions.size()>0){ 
			ret.append("<tr><td>&nbsp;Availible alternatives:</td><td>");
			ret.append("<select name='cbo" + name + "'>");
			for(int i=0 ; i<unusedOptions.size() ; i++){
				ret.append("<option value='"+unusedOptions.get(i)+"'>");
				ret.append(unusedOptions.get(i)+"</option>");
			}
			ret.append("</select>");
			ret.append("</td><td>");
			ret.append("<input type='button' value='Change' onClick='doSetModule(\"" + name + "\")'>");
			ret.append("</td></tr>");
		}
		ret.append("</table>");
		return ret.toString();
	}
	
	/**
	 * Builds the HTML to edit a map of modules
	 *
	 * @param map The map to edit
	 * @param availibleOptions List of availible modules that can be added to the map
	 *                         (full class names as Strings)
	 * @param name A short name for the map (only alphanumeric chars.)
	 *
	 * @return the HTML to edit the specified modules map
	 */
	public String buildModuleMap(ComplexType map, ArrayList availibleOptions, String name){
		StringBuffer ret = new StringBuffer();
		
		ret.append("<table cellspacing='0' cellpadding='2'>");
		
		ArrayList unusedOptions = new ArrayList();
		MBeanInfo mapInfo = map.getMBeanInfo();
		MBeanAttributeInfo m[] = mapInfo.getAttributes();
			
		// Printout modules in map.
		boolean alt = false;
		for(int n=0; n<m.length; n++) {
	        Object currentAttribute = null;
			ModuleAttributeInfo att = (ModuleAttributeInfo)m[n]; //The attributes of the current attribute.
	
			ret.append("<tr");
			if(alt){
				ret.append(" bgcolor='#EEEEFF'");
			}
			ret.append("><td>&nbsp;"+att.getType()+"</td>");
			if(n!=0){
				ret.append("<td><a href=\"javascript:doMoveMapItemUp('" + name + "','"+att.getName()+"')\">Move up</a></td>");
			} else {
				ret.append("<td></td>");
			}
			if(n!=m.length-1){
				ret.append("<td><a href=\"javascript:doMoveMapItemDown('" + name + "','"+att.getName()+"')\">Move down</a></td>");
			} else {
				ret.append("<td></td>");
			}
			ret.append("<td><a href=\"javascript:doRemoveMapItem('" + name + "','"+att.getName()+"')\">Remove</a></td>");
			ret.append("<td><a href=\"javascript:alert('");
			ret.append(TextUtils.escapeForJavascript(att.getDescription()));
			ret.append("')\">Info</a></td>\n");
			ret.append("</tr>");
			alt = !alt;
		}
		
		// Find out which aren't being used.
		for(int i=0 ; i<availibleOptions.size() ; i++){
			boolean isIncluded = false;
			
			for(int n=0; n<m.length; n++) {
	            Object currentAttribute = null;
				ModuleAttributeInfo att = (ModuleAttributeInfo)m[n]; //The attributes of the current attribute.
	
				try {
					currentAttribute = map.getAttribute(att.getName());
				} catch (Exception e1) {
					ret.append(e1.toString() + " " + e1.getMessage());
				}
				String typeAndName = att.getType()+"|"+att.getName();
				if(typeAndName.equals(availibleOptions.get(i))){
					//Found it
					isIncluded = true;
					break;
				}
			}
			if(isIncluded == false){
				// Yep the current one is unused.
				unusedOptions.add(availibleOptions.get(i));
			}
		}
		if(unusedOptions.size() > 0 ){
			ret.append("<tr><td>");
			ret.append("<select name='cboAdd" + name + "'>");
			for(int i=0 ; i<unusedOptions.size() ; i++){
				String curr = (String)unusedOptions.get(i);
				ret.append("<option value='"+curr+"'>"+curr.substring(0,curr.indexOf("|"))+"</option>");
			}
			ret.append("</select>");
			ret.append("</td><td>");
			ret.append("<input type='button' value='Add' onClick=\"doAddMapItem('" + name + "')\">");
			ret.append("</td></tr>");
		}
		ret.append("</table>");
		return ret.toString();
	}
%>

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
		}else if(action.equals("goto")){
            // Goto another page of the job/profile settings
			response.sendRedirect(request.getParameter("subaction"));
			return;
		}else if(action.equals("module")){
			// Setting a module
			String item = request.getParameter("item");
			String className = request.getParameter("cbo"+item);
						
			ModuleType tmp = null;
			if(item.equals("Frontier")){
				// Changing URI frontier
				tmp = SettingsHandler.instantiateModuleTypeFromClassName("frontier",className);
			} else if(item.equals("Scope")){
				// Changing Scope
				tmp = SettingsHandler.instantiateModuleTypeFromClassName(org.archive.crawler.framework.CrawlScope.ATTR_NAME,className);
			} 
			if(tmp != null){
				// If tmp is null then something went wrong but we'll ignore it.
				settingsHandler.getOrder().setAttribute(tmp);
				// Write changes
				settingsHandler.writeSettingsObject(settingsHandler.getSettings(null));
			}
		}else if(action.equals("map")){
			//Doing something with a map
			String subaction = request.getParameter("subaction");
			String item = request.getParameter("item");
			if(subaction != null && item != null){
				// Do common stuff
				String subitem = request.getParameter("subitem");
				MapType map = null;
				if(item.equals("PreFetchProcessors")){
					// Editing processors map
					map = ((MapType)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_PRE_FETCH_PROCESSORS));
                } else if(item.equals("Fetchers")){
                    // Editing processors map
                    map = ((MapType)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_FETCH_PROCESSORS));
                } else if(item.equals("Extractors")){
                    // Editing processors map
                    map = ((MapType)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_EXTRACT_PROCESSORS));
                } else if(item.equals("Writers")){
                    // Editing processors map
                    map = ((MapType)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_WRITE_PROCESSORS));
                } else if(item.equals("Postprocessors")){
                    // Editing processors map
                    map = ((MapType)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_POST_PROCESSORS));
				} else if(item.equals("StatisticsTracking")){
					// Editing Statistics Tracking map
					map = ((MapType)settingsHandler.getOrder().getAttribute("loggers"));
				}
				if(map != null){
					// Figure out what to do
					if(subaction.equals("up")){
						// Move selected processor up
						map.moveElementUp(settingsHandler.getSettings(null),subitem);
					}else if(subaction.equals("down")){
						// Move selected processor down			
						map.moveElementDown(settingsHandler.getSettings(null),subitem);
					}else if(subaction.equals("remove")){
						// Remove selected processor
						map.removeElement(settingsHandler.getSettings(null),subitem);
					}else if(subaction.equals("add")){
						String className = request.getParameter("cboAdd"+item);
						String typeName = className.substring(className.indexOf("|")+1);
						className = className.substring(0,className.indexOf("|"));
	
						map.addElement(settingsHandler.getSettings(null),
										 SettingsHandler.instantiateModuleTypeFromClassName(typeName,className));
					}
					// Write changes
					settingsHandler.writeSettingsObject(settingsHandler.getSettings(null));
				}
			}
		}		
	}

	String title = "Adjust modules";
	int tab = theJob.isProfile()?2:1;
	int jobtab = 0;
%>

<%@include file="/include/head.jsp"%>
<script type="text/javascript">
	function doSubmit(){
		document.frmModules.submit();
	}
	
    function doGoto(where){
        document.frmModules.action.value="goto";
        document.frmModules.subaction.value = where;
        doSubmit();
    }
    
	function doSetModule(name){
		document.frmModules.action.value="module";
		document.frmModules.item.value=name;
		doSubmit();
	}
	
	function doMoveMapItemUp(name, item){
		document.frmModules.action.value="map";
		document.frmModules.subaction.value="up";
		document.frmModules.item.value=name;
		document.frmModules.subitem.value=item;
		doSubmit();
	}

	function doMoveMapItemDown(name, item){
		document.frmModules.action.value="map";
		document.frmModules.subaction.value="down";
		document.frmModules.item.value=name;
		document.frmModules.subitem.value=item;
		doSubmit();
	}
	
	function doRemoveMapItem(name, item){
		document.frmModules.action.value="map";
		document.frmModules.subaction.value="remove";
		document.frmModules.item.value=name;
		document.frmModules.subitem.value=item;
		doSubmit();
	}
	
	function doAddMapItem(name){
		document.frmModules.action.value="map";
		document.frmModules.subaction.value="add";
		document.frmModules.item.value=name;
		doSubmit();
	}
</script>

	<p>
		<%@include file="/include/jobnav.jsp"%>
	<form name="frmModules" method="post" action="modules.jsp">
		<input type="hidden" name="job" value="<%=theJob.getUID()%>">
		<input type="hidden" name="action" value="done">
		<input type="hidden" name="subaction" value="done">
		<input type="hidden" name="item" value="">
		<input type="hidden" name="subitem" value="">
		<p>
			<b>Select URI Frontier</b>
		<p>
			<%=buildModuleSetter(
				settingsHandler.getOrder().getAttributeInfo("frontier"),
				CrawlJobHandler.loadOptions(CrawlJobHandler.MODULE_OPTIONS_FILE_FRONTIERS),
				"Frontier",
				((ComplexType)settingsHandler.getOrder().getAttribute("frontier")).getMBeanInfo().getDescription())%>

	
		<p>
			<b>Select crawl scope</b>
		<p>
			<%=buildModuleSetter(
				settingsHandler.getOrder().getAttributeInfo("scope"),
				CrawlJobHandler.loadOptions(CrawlJobHandler.MODULE_OPTIONS_FILE_SCOPES),
				"Scope",
				((ComplexType)settingsHandler.getOrder().getAttribute("scope")).getMBeanInfo().getDescription())%>
				
		<p>
			<b>Select Pre Processors</b> <i>Processors that should be run before any fetching occurs</i>
		<p>
			<%=buildModuleMap((ComplexType)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_PRE_FETCH_PROCESSORS), CrawlJobHandler.loadOptions(CrawlJobHandler.MODULE_OPTIONS_FILE_PROCESSORS), "PreFetchProcessors")%>
        <p>
            <b>Select Fetchers</b> <i>Processors that fetch documents from various protocols</i>
        <p>
            <%=buildModuleMap((ComplexType)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_FETCH_PROCESSORS), CrawlJobHandler.loadOptions(CrawlJobHandler.MODULE_OPTIONS_FILE_PROCESSORS), "Fetchers")%>
        <p>
            <b>Select Extractors</b> <i>Processors that extracts links from URIs</i>
        <p>
            <%=buildModuleMap((ComplexType)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_EXTRACT_PROCESSORS), CrawlJobHandler.loadOptions(CrawlJobHandler.MODULE_OPTIONS_FILE_PROCESSORS), "Extractors")%>
        <p>
            <b>Select Writers</b> <i>Processors that write documents to archive files</i>
        <p>
            <%=buildModuleMap((ComplexType)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_WRITE_PROCESSORS), CrawlJobHandler.loadOptions(CrawlJobHandler.MODULE_OPTIONS_FILE_PROCESSORS), "Writers")%>
        <p>
            <b>Select Post Processors</b> <i>Processors that do cleanup and feeds the frontier with new URIs</i>
        <p>
            <%=buildModuleMap((ComplexType)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_POST_PROCESSORS), CrawlJobHandler.loadOptions(CrawlJobHandler.MODULE_OPTIONS_FILE_PROCESSORS), "Postprocessors")%>

		<p>
			<b>Select Statistics Tracking</b>
		<p>
			<%=buildModuleMap((ComplexType)settingsHandler.getOrder().getAttribute("loggers"), CrawlJobHandler.loadOptions(CrawlJobHandler.MODULE_OPTIONS_FILE_TRACKERS), "StatisticsTracking")%>
	</form>
	<p>
		<%@include file="/include/jobnav.jsp"%>
<%@include file="/include/foot.jsp"%>


