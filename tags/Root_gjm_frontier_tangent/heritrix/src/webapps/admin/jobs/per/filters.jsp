<%
  /**
   * This pages allows the user to add filters to overrides.
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

<%@ page import="java.util.Vector,java.util.ArrayList" %>
<%@ page import="javax.management.MBeanInfo"%>
<%@ page import="javax.management.Attribute"%>
<%@ page import="javax.management.MBeanAttributeInfo"%>
<%@ page import="javax.management.AttributeNotFoundException"%>
<%@ page import="javax.management.MBeanException"%>
<%@ page import="javax.management.ReflectionException"%>

<%!
	/** 
	 * Generates the HTML code to display and allow manipulation of which
	 * filters are attached to this override. Will work it's way 
	 * recursively down the crawlorder. Inherited filters are displayed,
	 * but no changes allowed. Local filters can be added and manipulated.
	 *
	 * @param mbean The ComplexType representing the crawl order or one 
	 *              of it's subcomponents.
	 * @param settings CrawlerSettings for the domain to override setting
	 *                 for.
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
	public String printFilters(ComplexType mbean, CrawlerSettings settings, String indent, boolean possible, boolean first, boolean last, String parent, boolean alt, ArrayList availibleFilters) throws Exception {
		if(mbean.isTransient()){
			return "";
		}
		StringBuffer p = new StringBuffer();
		MBeanInfo info = mbean.getMBeanInfo(settings);

		MBeanAttributeInfo a[] = info.getAttributes();
		
		if(possible && mbean instanceof Filter){
			// Have a local filter.
			p.append("<tr");
			if(alt){
				p.append(" bgcolor='#EEEEFF'");
			}
			alt = !alt;
			p.append("><td nowrap><b>" + indent + "</b>" + mbean.getName() + "</td><td nowrap>");
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
			// Not a filter, or an inherited filter.
			p.append("<tr><td colspan='5'><b>" + indent + mbean.getName() + "</b></td></tr>\n");
		}
		
		alt=false;
		boolean haveNotFoundFirstEditable = true;
		int firstEditable = -1;
		for(int n=0; n<a.length; n++) {
			possible = mbean instanceof MapType;
	        if(a[n] == null) {
                p.append("  ERROR: null attribute");
            } else {
	            Object currentAttribute = null;
	            Object localAttribute = null;
				ModuleAttributeInfo att = (ModuleAttributeInfo)a[n]; //The attributes of the current attribute.
				try {
					currentAttribute = mbean.getAttribute(settings, att.getName());
					localAttribute = mbean.getLocalAttribute(settings, att.getName());
				} catch (Exception e1) {
					String error = e1.toString() + " " + e1.getMessage();
					return error;
				}
		    	if(localAttribute == null){
		    		possible = false; //Not an editable filter.
		    	} else if(haveNotFoundFirstEditable) {
		    		firstEditable=n;
		    		haveNotFoundFirstEditable=false;
		    		alt = true;
		    	}

				if(currentAttribute instanceof ComplexType) {
			    	p.append(printFilters((ComplexType)currentAttribute,settings,indent+"&nbsp;&nbsp;",possible,n==firstEditable,n==a.length-1,mbean.getAbsoluteName(),alt,availibleFilters));
			    	if(currentAttribute instanceof MapType)
			    	{
			    		MapType thisMap = (MapType)currentAttribute;
			    		if(thisMap.getContentType().getName().equals(Filter.class.getName())){
				    		p.append("<tr><td colspan='5'>\n<b>"+indent+"&nbsp;&nbsp;</b>");
				    		p.append("<input name='" + mbean.getAbsoluteName() + "/" + att.getName() + ".name'>\n");
				    		p.append("<select name='" + mbean.getAbsoluteName() + "/" + att.getName() + ".class'>\n");
				    		for(int i=0 ; i<availibleFilters.size() ; i++){
					    		p.append("<option value='"+availibleFilters.get(i)+"'>"+availibleFilters.get(i)+"</option>\n");
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

	// Load display level
	String currDomain = request.getParameter("currDomain");
	
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
	CrawlOrder crawlOrder = settingsHandler.getOrder();
    CrawlerSettings settings = settingsHandler.getSettingsObject(currDomain);

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
						CrawlerModule tmp = SettingsHandler.instantiateCrawlerModuleFromClassName(typeName,className);
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
			response.sendRedirect("/admin/jobs/per/overview.jsp?job="+theJob.getUID()+"&currDomain="+currDomain+"&message=Override changes saved");
			return;
		}else if(action.equals("configure")){
			// Go to configure settings.
			response.sendRedirect("/admin/jobs/per/configure.jsp?job="+theJob.getUID()+"&currDomain="+currDomain);
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
	
	function doGotoConfigure(){
		document.frmFilters.action.value="configure";
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
		<b>Override for the <%=theJob.isProfile()?"profile":"job"%> <%=theJob.getJobName()%> on domain '<%=currDomain%>'</b>
		<%@include file="/include/jobpernav.jsp"%>
	<p>
	<form name="frmFilters" method="post" action="filters.jsp">
		<input type="hidden" name="currDomain" value="<%=currDomain%>">
		<input type="hidden" name="job" value="<%=theJob.getUID()%>">
		<input type="hidden" name="action" value="done">
		<input type="hidden" name="subaction" value="">
		<input type="hidden" name="map" value="">
		<input type="hidden" name="filter" value="">
		<p>
			<b>Instructions:</b> It is possible to add filters to overrides and manipulate existing<br>
			override filters. It is not possible to remove filters defined in a super domain!
		<p>
		<table>
			<%=printFilters(crawlOrder,settings,"",false,false,false,null,false,CrawlJobHandler.loadOptions(CrawlJobHandler.MODULE_OPTIONS_FILE_FILTERS))%>
		</table>
	</form>
	<p>
		<%@include file="/include/jobpernav.jsp"%>
<%@include file="/include/foot.jsp"%>


