<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.datamodel.settings.*" %>
<%@ page import="org.archive.crawler.framework.CrawlController" %>

<%@ page import="java.io.*,java.lang.Boolean,java.util.Vector,java.util.ArrayList" %>
<%@ page import="javax.management.MBeanInfo, javax.management.Attribute, javax.management.MBeanAttributeInfo,javax.management.AttributeNotFoundException, javax.management.MBeanException,javax.management.ReflectionException"%>

<%
	ArrayList availibleURIFrontiers = CrawlJobHandler.loadOptions("urifrontiers.options");
	ArrayList availibleProcessors = CrawlJobHandler.loadOptions("processors.options");
	ArrayList availibleTrackers = CrawlJobHandler.loadOptions("trackers.options");
	ArrayList availibleScopes = CrawlJobHandler.loadOptions("scopes.options");
	
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
		}else if(action.equals("frontier")){
			// Change URI frontier
			String className = request.getParameter("cboFrontier");
			CrawlerModule tmp = SettingsHandler.instantiateCrawlerModuleFromClassName("frontier",className);
			settingsHandler.getOrder().setAttribute(tmp);
			// Write changes
			settingsHandler.writeSettingsObject(settingsHandler.getSettings(null));
		}else if(action.equals("scope")){
			// Change scope
			String className = request.getParameter("cboScope");
			CrawlerModule tmp = SettingsHandler.instantiateCrawlerModuleFromClassName(org.archive.crawler.framework.CrawlScope.ATTR_NAME,className);
			settingsHandler.getOrder().setAttribute(tmp);
			// Write changes
			settingsHandler.writeSettingsObject(settingsHandler.getSettings(null));
		}else if(action.equals("processors")){
			//Doing something with the processors
			String subaction = request.getParameter("subaction");
			if(subaction != null){
				// Do common stuff
				String procName = request.getParameter("item");
				MapType procs = ((MapType)settingsHandler.getOrder().getAttribute("processors"));
				// Figure out what to do
				if(subaction.equals("up")){
					// Move selected processor up
					procs.moveElementUp(settingsHandler.getSettings(null),procName);
				}else if(subaction.equals("down")){
					// Move selected processor down			
					procs.moveElementDown(settingsHandler.getSettings(null),procName);
				}else if(subaction.equals("remove")){
					// Remove selected processor
					procs.removeElement(settingsHandler.getSettings(null),procName);
				}else if(subaction.equals("add")){
					String className = request.getParameter("cboAddProcessor");
					String typeName = className.substring(className.indexOf("|")+1);
					className = className.substring(0,className.indexOf("|"));

					procs.addElement(settingsHandler.getSettings(null),
									 SettingsHandler.instantiateCrawlerModuleFromClassName(typeName,className));
				}
				// Write changes
				settingsHandler.writeSettingsObject(settingsHandler.getSettings(null));
			}
		}else if(action.equals("trackers")){
			//Doing something with the statistics trackers
			String subaction = request.getParameter("subaction");
			if(subaction != null){
				// Do common stuff
				String trackerName = request.getParameter("item");
				MapType trackers = ((MapType)settingsHandler.getOrder().getAttribute("loggers"));
				// Figure out what to do
				if(subaction.equals("up")){
					// Move selected tracker up
					trackers.moveElementUp(settingsHandler.getSettings(null),trackerName);
				}else if(subaction.equals("down")){
					// Move selected tracker down			
					trackers.moveElementDown(settingsHandler.getSettings(null),trackerName);
				}else if(subaction.equals("remove")){
					// Remove selected processor
					trackers.removeElement(settingsHandler.getSettings(null),trackerName);
				}else if(subaction.equals("add")){
					String className = request.getParameter("cboAddTrackers");
					String typeName = className.substring(className.indexOf("|")+1);
					className = className.substring(0,className.indexOf("|"));

					trackers.addElement(settingsHandler.getSettings(null),
									 SettingsHandler.instantiateCrawlerModuleFromClassName(typeName,className));
				}
				// Write changes
				settingsHandler.writeSettingsObject(settingsHandler.getSettings(null));
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
	
	function doSetURIFrontier(){
		document.frmModules.action.value="frontier";
		doSubmit();
	}
	
	function doSetScope(){
		document.frmModules.action.value="scope";
		doSubmit();
	}
	
	function doMoveUpProcessor(proc){
		document.frmModules.action.value="processors";
		document.frmModules.subaction.value="up";
		document.frmModules.item.value=proc;
		doSubmit();
	}

	function doMoveDownProcessor(proc){
		document.frmModules.action.value="processors";
		document.frmModules.subaction.value="down";
		document.frmModules.item.value=proc;
		doSubmit();
	}
	
	function doRemoveProcessor(proc){
		document.frmModules.action.value="processors";
		document.frmModules.subaction.value="remove";
		document.frmModules.item.value=proc;
		doSubmit();
	}
	
	function doAddProcessor(){
		document.frmModules.action.value="processors";
		document.frmModules.subaction.value="add";
		doSubmit();
	}

	function doMoveUpTracker(track){
		document.frmModules.action.value="trackers";
		document.frmModules.subaction.value="up";
		document.frmModules.item.value=track;
		doSubmit();
	}

	function doMoveDownTracker(track){
		document.frmModules.action.value="trackers";
		document.frmModules.subaction.value="down";
		document.frmModules.item.value=track;
		doSubmit();
	}
	
	function doRemoveTracker(track){
		document.frmModules.action.value="trackers";
		document.frmModules.subaction.value="remove";
		document.frmModules.item.value=track;
		doSubmit();
	}
	
	function doAddTracker(){
		document.frmModules.action.value="trackers";
		document.frmModules.subaction.value="add";
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
		<p>
			<b>Select URI Frontier</b>
		<p>
		<table>
			<tr>
				<td>
					<select name="cboFrontier">
					<%
						MBeanAttributeInfo frontier = settingsHandler.getOrder().getAttributeInfo("frontier");
						for(int i=0 ; i<availibleURIFrontiers.size() ; i++){
							out.print("<option value='"+availibleURIFrontiers.get(i)+"'");
							if(frontier.getType().equals((String)availibleURIFrontiers.get(i))){
								out.print(" selected");
							}
							out.println(">"+ availibleURIFrontiers.get(i)+"</option>");
						}
					%>
					</select>
				</td>
				<td>
					<input type="button" value="Set URI Frontier" onClick="doSetURIFrontier()">
				</td>
			</tr>
		</table>
	
		<p>
			<b>Select crawl scope</b>
		<p>
		<table>
			<tr>
				<td>
					<select name="cboScope">
					<%
						MBeanAttributeInfo scope = settingsHandler.getOrder().getAttributeInfo("scope");
						for(int i=0 ; i<availibleScopes.size() ; i++){
							out.print("<option value='"+availibleScopes.get(i)+"'");
							if(scope.getType().equals((String)availibleScopes.get(i))){
								out.print(" selected");
							}
							out.println(">"+ availibleScopes.get(i)+"</option>");
						}
					%>
					</select>
				</td>
				<td>
					<input type="button" value="Set scope" onClick="doSetScope()">
				</td>
			</tr>
		</table>
				
		<p>
			<b>Select Processors</b>
		<p>
		<table cellspacing="0" cellpadding="2">
		<%
			Vector unusedProcessors = new Vector();
			ComplexType procs = ((ComplexType)settingsHandler.getOrder().getAttribute("processors"));
			MBeanInfo procInfo = procs.getMBeanInfo();
			MBeanAttributeInfo p[] = procInfo.getAttributes();
			
			// Printout used proc.
			boolean alt = false;
			for(int n=0; n<p.length; n++) {
		        Object currentAttribute = null;
				ModuleAttributeInfo att = (ModuleAttributeInfo)p[n]; //The attributes of the current attribute.
		
				out.println("<tr");
				if(alt){
					out.println(" bgcolor='#EEEEFF'");
				}
				out.println("><td>&nbsp;"+att.getType()+"</td>");
				if(n!=0){
					out.println("<td><a href=\"javascript:doMoveUpProcessor('"+att.getName()+"')\">Move up</a></td>");
				} else {
					out.println("<td></td>");
				}
				if(n!=p.length-1){
					out.println("<td><a href=\"javascript:doMoveDownProcessor('"+att.getName()+"')\">Move down</a></td>");
				} else {
					out.println("<td></td>");
				}
				out.println("<td><a href=\"javascript:doRemoveProcessor('"+att.getName()+"')\">Remove</a></td>");
				out.println("</tr>");
				alt = !alt;
			}
		
			// Find out which aren't being used.
			for(int i=0 ; i<availibleProcessors.size() ; i++){
				boolean isIncluded = false;
				
				for(int n=0; n<p.length; n++) {
		            Object currentAttribute = null;
					ModuleAttributeInfo att = (ModuleAttributeInfo)p[n]; //The attributes of the current attribute.
		
					try {
						currentAttribute = procs.getAttribute(att.getName());
					} catch (Exception e1) {
						out.println(e1.toString() + " " + e1.getMessage());
					}
					String typeAndName = att.getType()+"|"+att.getName();
					if(typeAndName.equals(availibleProcessors.get(i))){
						//Found it
						isIncluded = true;
						break;
					}
				}
				if(isIncluded == false){
					// Yep the current one is unused.
					unusedProcessors.add(availibleProcessors.get(i));
				}
			}
			if(unusedProcessors.size() > 0 ){
		%>
			<tr>
				<td>
					<select name="cboAddProcessor">
					<%
						for(int i=0 ; i<unusedProcessors.size() ; i++){
							String curr = (String)unusedProcessors.get(i);
							out.println("<option value='"+curr+"'>"+curr.substring(0,curr.indexOf("|"))+"</option>");
						}
					%>
					</select>
				</td>
				<td>
					<input type="button" value="Add" onClick="doAddProcessor()">
				</td>
			</tr>
		<%	}	%>
		</table>

		<p>
			<b>Select Statistics Tracking</b>
		<p>
		<table cellspacing="0" cellpadding="2">
		<%
			Vector unusedTrackers = new Vector();
			ComplexType trackers = ((ComplexType)settingsHandler.getOrder().getAttribute("loggers"));
			MBeanInfo trackersInfo = trackers.getMBeanInfo();
			MBeanAttributeInfo t[] = trackersInfo.getAttributes();
			
			// Printout used trackers.
			alt = false;
			for(int n=0; n<t.length; n++) {
		        Object currentAttribute = null;
				ModuleAttributeInfo att = (ModuleAttributeInfo)t[n]; //The attributes of the current attribute.
		
				out.println("<tr");
				if(alt){
					out.println(" bgcolor='#EEEEFF'");
				}
				out.println("><td>&nbsp;"+att.getType()+"</td>");
				if(n!=0){
					out.println("<td><a href=\"javascript:doMoveUpTracker('"+att.getName()+"')\">Move up</a></td>");
				} else {
					out.println("<td></td>");
				}
				if(n!=t.length-1){
					out.println("<td><a href=\"javascript:doMoveDownTracker('"+att.getName()+"')\">Move down</a></td>");
				} else {
					out.println("<td></td>");
				}
				out.println("<td><a href=\"javascript:doRemoveTracker('"+att.getName()+"')\">Remove</a></td>");
				out.println("</tr>");
				alt = !alt;
			}
		
			// Find out which aren't being used.
			for(int i=0 ; i<availibleTrackers.size() ; i++){
				boolean isIncluded = false;
				
				for(int n=0; n<t.length; n++) {
		            Object currentAttribute = null;
					ModuleAttributeInfo att = (ModuleAttributeInfo)t[n]; //The attributes of the current attribute.
		
					try {
						currentAttribute = trackers.getAttribute(att.getName());
					} catch (Exception e1) {
						out.println(e1.toString() + " " + e1.getMessage());
					}
					String typeAndName = att.getType()+"|"+att.getName();
					if(typeAndName.equals(availibleTrackers.get(i))){
						//Found it
						isIncluded = true;
						break;
					}
				}
				if(isIncluded == false){
					// Yep the current one is unused.
					unusedTrackers.add(availibleTrackers.get(i));
				}
			}
			if(unusedTrackers.size() > 0 ){
		%>
			<tr>
				<td>
					<select name="cboAddTrackers">
					<%
						for(int i=0 ; i<unusedTrackers.size() ; i++){
							String curr = (String)unusedTrackers.get(i);
							out.println("<option value='"+curr+"'>"+curr.substring(0,curr.indexOf("|"))+"</option>");
						}
					%>
					</select>
				</td>
				<td>
					<input type="button" value="Add" onClick="doAddTracker()">
				</td>
			</tr>
		<%	}	%>
		</table>
	</form>
	<p>
		<%@include file="/include/jobnav.jsp"%>
<%@include file="/include/foot.jsp"%>


