<%
  /**
   * This pages allows the user to select what filters
   * are applied to what modules in the crawl order.
   *
   * @author Kristinn Sigurdsson
   */
%>
<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.datamodel.settings.*" %>
<%@ page import="org.archive.crawler.framework.CrawlController" %>
<%@ page import="org.archive.crawler.framework.Filter" %>

<%@ page import="java.util.Vector" %>
<%@ page import="javax.management.MBeanInfo"%>
<%@ page import="javax.management.Attribute"%>
<%@ page import="javax.management.MBeanAttributeInfo"%>
<%@ page import="javax.management.AttributeNotFoundException"%>
<%@ page import="javax.management.MBeanException"%>
<%@ page import="javax.management.ReflectionException"%>

<%!
	String[] availibleFilters = {"org.archive.crawler.filter.HopsFilter",
								 "org.archive.crawler.filter.OrFilter",
								 "org.archive.crawler.filter.PathDepthFilter",
								 "org.archive.crawler.filter.SeedExtensionFilter",
								 "org.archive.crawler.filter.TransclusionFilter",
								 "org.archive.crawler.filter.URIRegExpFilter"};
	/**
	 * Generates the HTML code to display and allow manipulation of which
	 * filters are attached to the crawl order. Will work it's way 
	 * recursively down the crawlorder.
	 *
	 * @param mbean The ComplexType representing the crawl order or one 
	 *              of it's subcomponents.
	 * @param indent A string to prefix to the current ComplexType to 
	 *               visually indent it.
	 * @param possible If true then the current ComplexType MAY be a 
	 *                 configurable filter. (Generally this means that
	 *                 the current ComplexType belongs to a Map)
	 * @param first True if mbean is the first element of a Map.
	 * @param last True if mbean is the last element of a Map.
	 * @parent The absolute name of the ComplexType that contains the
	 *         current ComplexType (i.e. parent).
	 * @alt If true and mbean is a filter then an alternate background color
	 *      is used for displaying it.
	 *
	 * @return The variable part of the HTML code for selecting filters.
	 */
	public String printFilters(ComplexType mbean, String indent, boolean possible, boolean first, boolean last, String parent, boolean alt) throws Exception {
		if(mbean.isTransient()){
			return "";
		}
		StringBuffer p = new StringBuffer();
		MBeanInfo info = mbean.getMBeanInfo();

		MBeanAttributeInfo a[] = info.getAttributes();
		
		if(possible && mbean instanceof Filter){
			p.append("<tr");
			if(alt){
				p.append(" bgcolor='#EEEEFF'");
			}
			alt = !alt;
			p.append("><td nowrap>" + indent + mbean.getName() + "</td><td nowrap>");
			if(first==false){
				p.append("<a href=\"javascript:doMoveUp('"+mbean.getName()+"','"+parent+"')\">Move up</a>");
			}
			p.append("</td><td nowrap>");
			if(last==false){
				p.append("<a href=\"javascript:doMoveDown('"+mbean.getName()+"','"+parent+"')\">Move down</a>");
			}
			p.append("</td><td><a href=\"javascript:doRemove('"+mbean.getName()+"','"+parent+"')\">Remove</a></td>");
			p.append("<td><i>"+mbean.getClass().getName()+"</i></td></tr>\n");
		} else {
			p.append("<tr><td colspan='5'><b>" + indent + mbean.getName() + "</b></td></tr>\n");
		}
		
		possible = mbean instanceof MapType;
		alt=false;
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
			    	p.append(printFilters((ComplexType)currentAttribute,indent+"&nbsp;&nbsp;",possible,n==0,n==a.length-1,mbean.getAbsoluteName(),alt));
			    	if(currentAttribute instanceof MapType)
			    	{
			    		MapType thisMap = (MapType)currentAttribute;
			    		if(thisMap.getContentType().getName().equals(Filter.class.getName())){
				    		p.append("<tr><td colspan='5'>\n"+indent+"&nbsp;&nbsp;");
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
				alt = !alt;
		    }
		}
		return p.toString();
	}
%>

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

	XMLSettingsHandler settingsHandler = (XMLSettingsHandler)theJob.getSettingsHandler();

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
				MapType filterMap = (MapType)settingsHandler.getComplexTypeByAbsoluteName(settingsHandler.getSettingsObject(null),map);
				if(subaction.equals("add")){
					//Add filter
					String className = request.getParameter(map+".class");
					String typeName = request.getParameter(map+".name");
					if(typeName != null && typeName.length() > 0 
					   && className != null && className.length() > 0 ){
						filterMap.addElement(settingsHandler.getSettings(null),
										     SettingsHandler.instantiateCrawlerModuleFromClassName(typeName,className));
					}
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
			// Finally save the changes to disk
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
		}else if(action.equals("per")){
			// Go to modules
			response.sendRedirect("/admin/jobs/per/overview.jsp?job="+theJob.getUID());
			return;
		}
	}

	// Set page header.
	String title = "Select filters";
	int tab = theJob.isProfile()?2:1;
	int jobtab = 1;
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
	
	function doGotoPer(){
		document.frmFilters.action.value="per";
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
		<%@include file="/include/jobnav.jsp"%>
	<p>
	<form name="frmFilters" method="post" action="filters.jsp">
		<input type="hidden" name="job" value="<%=theJob.getUID()%>">
		<input type="hidden" name="action" value="done">
		<input type="hidden" name="subaction" value="">
		<input type="hidden" name="map" value="">
		<input type="hidden" name="filter" value="">
		<table>
			<%=printFilters(theJob.getSettingsHandler().getOrder(),"",false,false,false,null,false)%>
		</table>
	</form>
	<p>
		<%@include file="/include/jobnav.jsp"%>
<%@include file="/include/foot.jsp"%>


