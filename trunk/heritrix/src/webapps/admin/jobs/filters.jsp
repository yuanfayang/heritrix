<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.datamodel.settings.*" %>
<%@ page import="org.archive.crawler.framework.CrawlController" %>
<%@ page import="org.archive.crawler.framework.Filter" %>

<%@ page import="java.lang.Boolean,java.util.Vector" %>
<%@ page import="javax.management.MBeanInfo, javax.management.Attribute, javax.management.MBeanAttributeInfo,javax.management.AttributeNotFoundException, javax.management.MBeanException,javax.management.ReflectionException"%>

<%!
	String[] availibleFilters = {"org.archive.crawler.filter.HopsFilter",
								 "org.archive.crawler.filter.OrFilter",
								 "org.archive.crawler.filter.PathDepthFilter",
								 "org.archive.crawler.filter.SeedExtensionFilter",
								 "org.archive.crawler.filter.TransclusionFilter",
								 "org.archive.crawler.filter.URIRegExpFilter"};

	public String printFilters(ComplexType mbean, String indent, boolean possible, boolean first, boolean last, String parent) throws Exception {
		if(mbean.isTransient()){
			return "";
		}
		StringBuffer p = new StringBuffer();
		MBeanInfo info = mbean.getMBeanInfo();

		MBeanAttributeInfo a[] = info.getAttributes();
		
		if(possible && mbean instanceof Filter){
			p.append("<tr><td nowrap>" + indent + mbean.getName() + "</td><td nowrap>");
			if(first==false){
				p.append("<a href=\"javascript:doMoveUp('"+mbean.getName()+"','"+parent+"')\">Move up</a>");
			}
			p.append("</td><td nowrap>");
			if(last==false){
				p.append("<a href=\"javascript:doMoveDown('"+mbean.getName()+"','"+parent+"')\">Move down</a>");
			}
			p.append("</td><td width='100%'><a href=\"javascript:doRemove('"+mbean.getName()+"','"+parent+"')\">Remove</a></td></tr>\n");
		} else {
			p.append("<tr><td colspan='4'><b>" + indent + mbean.getName() + "</b></td></tr>\n");
		}
		
		possible = mbean instanceof MapType;
		
		for(int n=0; n<a.length; n++) {
	        if(a[n] == null) {
                p.append("  ERROR: null attribute");
            } else {
	            Object currentAttribute = null;
				ModuleAttributeInfo att = (ModuleAttributeInfo)a[n]; //The attributes of the current attribute.
				try {
					currentAttribute = mbean.getAttribute(att.getName());
				} catch (Exception e1) {
					String error = e1.toString() + " " + e1.getMessage();
					return error;
				}

				if(currentAttribute instanceof ComplexType) {
			    	p.append(printFilters((ComplexType)currentAttribute,indent+"&nbsp;&nbsp;",possible,n==0,n==a.length-1,mbean.getAbsoluteName()));
			    	if(currentAttribute instanceof MapType)
			    	{
			    		MapType thisMap = (MapType)currentAttribute;
			    		if(thisMap.getContentType().getName().equals(Filter.class.getName())){
				    		p.append("<tr><td colspan='4'>\n"+indent+"&nbsp;&nbsp;");
				    		p.append("<input name='" + mbean.getAbsoluteName() + "/" + att.getName() + ".name'>\n");
				    		p.append("<select name='" + mbean.getAbsoluteName() + "/" + att.getName() + ".class'>\n");
				    		for(int i=0 ; i<availibleFilters.length ; i++){
					    		p.append("<option value='"+availibleFilters[i]+"'>"+availibleFilters[i]+"</option>\n");
					    	}
				    		p.append("</select>\n");
				    		p.append("<input type='button' value='Add' onClick=\"doAdd('" + mbean.getAbsoluteName() + "/" + att.getName() + "')\">\n");
				    		p.append("</td></tr>\n");
			    		}
			    	}
				}
		    }
		}
		return p.toString();
	}
%>

<%
	
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
		if(action.equals("filters")){
			//Doing something with the filters.
			String subaction = request.getParameter("subaction");
			String map = request.getParameter("map");
			if(map != null && map.length() > 0){
				String filter = request.getParameter("filter");
				MapType filterMap = (MapType)settingsHandler.getComplexTypeByAbsoluteName(settingsHandler.getSettingsObject(null),map);
				if(subaction.equals("add")){
					//Add filter
					String className = request.getParameter(map+".class");
					String typeName = request.getParameter(map+".name");
					filterMap.addElement(settingsHandler.getSettings(null),
									     SettingsHandler.instantiateCrawlerModuleFromClassName(typeName,className));
				} else if(subaction.equals("moveup")){
					// Move a filter down in a map
					if(filter != null && filter.length() > 0){
						filterMap.moveElementUp(settingsHandler.getSettings(null),filter);
					}
				} else if(subaction.equals("movedown")){
					// Move a filter up in a map
					if(filter != null && filter.length() > 0){
						filterMap.moveElementDown(settingsHandler.getSettings(null),filter);
					}
				} else if(subaction.equals("remove")){
					// Remove a filter from a map
					if(filter != null && filter.length() > 0){
						filterMap.removeElement(settingsHandler.getSettings(null),filter);
					}
				}
			}
			settingsHandler.writeSettingsObject(settingsHandler.getSettings(null));
		}else if(action.equals("done")){
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
		document.frmFilters.submit();
	}
	
	function doGotoConfigure(){
		document.frmFilters.action.value="configure";
		doSubmit();
	}
	
	function doGotoModules(){
		document.frmFilters.action.value="modules";
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
		document.frmFilters.action.value = "filters";
		document.frmFilters.subaction.value = "add";
		document.frmFilters.map.value = map;
		doSubmit();
	}
</script>
<p>
<form name="frmFilters" method="post" action="filters.jsp">
	<input type="hidden" name="job" value="<%=theJob.getUID()%>">
	<input type="hidden" name="action" value="done">
	<input type="hidden" name="subaction" value="">
	<input type="hidden" name="map" value="">
	<input type="hidden" name="filter" value="">
	<input type="button" value="Adjust modules" onClick="doGotoModules()">
	<input type="button" value="Configure settings" onClick="doGotoConfigure()">
	<input type="button" value="Done" onClick="doSubmit()">

<p>
<table>
	<%=printFilters(theJob.getSettingsHandler().getOrder(),"",false,false,false,null)%>
</table>
<p>	
	<input type="button" value="Adjust modules" onClick="doGotoModules()">
	<input type="button" value="Configure settings" onClick="doGotoConfigure()">
	<input type="button" value="Done" onClick="doSubmit()">
</form>
<%@include file="/include/foot.jsp"%>


