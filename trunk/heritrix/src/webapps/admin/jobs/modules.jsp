<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.datamodel.settings.*" %>
<%@ page import="org.archive.crawler.framework.CrawlController" %>

<%@ page import="java.io.*,java.lang.Boolean,java.util.Vector,java.util.ArrayList" %>
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
	public String buildModuleSetter(MBeanAttributeInfo module, ArrayList availibleOptions, String name){
		StringBuffer ret = new StringBuffer();
		
		ArrayList unusedOptions = new ArrayList();
		
		// Let's figure out which are not being used.
		for(int i=0 ; i<availibleOptions.size() ; i++){
			if(module.getType().equals((String)availibleOptions.get(i))==false){
				unusedOptions.add(availibleOptions.get(i));
			}
		}
		
		ret.append("<table><tr><td>Current selection:</td><td>");
		ret.append("<i>" + module.getType() + "</i>");
		ret.append("</td><td></td></tr>");
		
		if(unusedOptions.size()>0){ 
			ret.append("<tr><td>Availible alternatives:</td><td>");
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
		
		Vector unusedOptions = new Vector();
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
		}else if(action.equals("configure")){
			// Go to configure settings.
			response.sendRedirect("/admin/jobs/configure.jsp?job="+theJob.getUID());
			return;
		}else if(action.equals("filters")){
			// Go to select filters.
			response.sendRedirect("/admin/jobs/filters.jsp?job="+theJob.getUID());
			return;
		}else if(action.equals("per")){
			// Go to select filters.
			response.sendRedirect("/admin/jobs/per/overview.jsp?job="+theJob.getUID());
			return;
		}else if(action.equals("module")){
			// Setting a module
			String item = request.getParameter("item");
			String className = request.getParameter("cbo"+item);
						
			CrawlerModule tmp = null;
			if(item.equals("Frontier")){
				// Changing URI frontier
				tmp = SettingsHandler.instantiateCrawlerModuleFromClassName("frontier",className);
			} else if(item.equals("Scope")){
				// Changing Scope
				tmp = SettingsHandler.instantiateCrawlerModuleFromClassName(org.archive.crawler.framework.CrawlScope.ATTR_NAME,className);
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
				if(item.equals("Processors")){
					// Editing processors map
					map = ((MapType)settingsHandler.getOrder().getAttribute("processors"));
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
										 SettingsHandler.instantiateCrawlerModuleFromClassName(typeName,className));
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
	
	function doGotoConfigure(){
		document.frmModules.action.value="configure";
		doSubmit();
	}
	
	function doGotoFilters(){
		document.frmModules.action.value="filters";
		doSubmit();
	}
	
	function doGotoPer(){
		document.frmModules.action.value="per";
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
			<%=buildModuleSetter(settingsHandler.getOrder().getAttributeInfo("frontier"),CrawlJobHandler.loadOptions("urifrontiers.options"),"Frontier")%>

	
		<p>
			<b>Select crawl scope</b>
		<p>
			<%=buildModuleSetter(settingsHandler.getOrder().getAttributeInfo("scope"),CrawlJobHandler.loadOptions("scopes.options"),"Scope")%>
				
		<p>
			<b>Select Processors</b>
		<p>
			<%=buildModuleMap((ComplexType)settingsHandler.getOrder().getAttribute("processors"), CrawlJobHandler.loadOptions("processors.options"), "Processors")%>

		<p>
			<b>Select Statistics Tracking</b>
		<p>
			<%=buildModuleMap((ComplexType)settingsHandler.getOrder().getAttribute("loggers"), CrawlJobHandler.loadOptions("trackers.options"), "StatisticsTracking")%>
	</form>
	<p>
		<%@include file="/include/jobnav.jsp"%>
<%@include file="/include/foot.jsp"%>


