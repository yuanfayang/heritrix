<%
  /**
   * This pages allows the user to edit the configuration 
   * of an override. 
   * That is set any af the 'values', but does not allow
   * users to change which 'modules' are used.
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
<%@ page import="org.archive.util.TextUtils" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.FileReader" %>
<%@ page import="java.io.FileWriter" %>
<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.BufferedWriter" %>
<%@ page import="java.io.IOException" %>
<%@ page import="javax.management.MBeanInfo"%>
<%@ page import="javax.management.Attribute"%>
<%@ page import="javax.management.MBeanAttributeInfo"%>
<%@ page import="javax.management.AttributeNotFoundException"%>
<%@ page import="javax.management.MBeanException"%>
<%@ page import="javax.management.ReflectionException"%>

<%!
	/** TODO: FIX JAVADOC
	 * Builds up the the HTML code to display any ComplexType attribute
	 * of the settings in an editable form. Uses recursion.
	 *
	 * @param mbean The ComplexType to build a display
	 * @param indent A string that will be added in front to indent the
	 *               current type.
	 * @param lists All 'lists' encountered will have their name added	 
	 *              to this StringBuffer followed by a comma.
	 * @returns The HTML code described above.
	 */
	public String printMBean(ComplexType mbean, CrawlerSettings settings, String indent, StringBuffer lists) throws Exception {
		if(mbean.isTransient()){
			return "";
		}
		StringBuffer p = new StringBuffer();
		MBeanInfo info = mbean.getMBeanInfo(settings);

		p.append("<tr><td><b>" + indent + mbean.getName() + "</b></td>\n");
		p.append("<td><a class='help' href=\"javascript:doPop('");
		p.append(TextUtils.escapeForJavascript(mbean.getDescription()));
		p.append("')\">?</a></td></tr>\n");

		MBeanAttributeInfo a[] = info.getAttributes();
		
		for(int n=0; n<a.length; n++) {
	        if(a[n] == null) {
                p.append("  ERROR: null attribute");
            } else {
	            Object currentAttribute = null;
	            Object localAttribute = null;
				ModuleAttributeInfo att = (ModuleAttributeInfo)a[n]; //The attributes of the current attribute.
				try {
					currentAttribute = mbean.getAttribute(settings,att.getName());
				} catch (Exception e1) {
					String error = e1.toString() + " " + e1.getMessage();
					return error;
				}
				try{
					//TODO: Exceptions are BAD. Stop disregarding them.
					localAttribute = mbean.getLocalAttribute(settings,att.getName());
				} catch (Exception e) {
					// Disregard exception.
					localAttribute = null;
				}

				if(currentAttribute instanceof ComplexType) {
			    	p.append(printMBean((ComplexType)currentAttribute,settings,indent+"&nbsp;&nbsp;",lists));
				}
				else if(currentAttribute instanceof ListType){
					// Some type of list.
					ListType list = (ListType)currentAttribute;
					p.append("<tr><td valign='top'>" + indent + att.getName() + ":&nbsp;</td>");
					p.append("<td valign='top'><a class='help' href=\"javascript:doPop('");
					p.append(TextUtils.escapeForJavascript(att.getDescription()));
					p.append("')\">?</a>&nbsp;</td>\n");
					p.append("<td valign='top'><input name='" + mbean.getAbsoluteName() + "/" + att.getName() + ".override' type='checkbox' value='true'");
					if(localAttribute != null){
						 p.append(" checked");
					}
					p.append("></td>\n");
					p.append("<td><table border='0' cellspacing='0' cellpadding='0'>\n");
					p.append("<tr><td><select multiple name='" + mbean.getAbsoluteName() + "/" + att.getName() + "' id='" + mbean.getAbsoluteName() + "/" + att.getName() + "' size='4' style='width: 320px'>\n");
					for(int i=0 ; i<list.size() ; i++){
						p.append("<option value='" + list.get(i) +"'>"+list.get(i)+"</option>\n");
					}
					p.append("</select></td>\n");
					p.append("<td valign='top'><input type='button' value='Delete' onClick=\"doDeleteList('" + mbean.getAbsoluteName() + "/" + att.getName() + "')\"></td></tr>\n");
					p.append("<tr><td><input name='" + mbean.getAbsoluteName() + "/" + att.getName() + "/add' id='" + mbean.getAbsoluteName() + "/" + att.getName() + "/add' style='width: 320px'></td>\n");
					p.append("<td><input type='button' value='Add' onClick=\"doAddList('" + mbean.getAbsoluteName() + "/" + att.getName() + "')\"></td></tr>\n");
					p.append("</table></td></tr>\n");

					lists.append("'"+mbean.getAbsoluteName() + "/" + att.getName()+"',");
				}
				else{
					Object[] legalValues = att.getLegalValues();
					
					p.append("<tr><td>" + indent + att.getName() + ":&nbsp;</td>");
					p.append("<td ><a class='help' href=\"javascript:doPop('");
					p.append(TextUtils.escapeForJavascript(att.getDescription()));
					p.append("')\">?</a>&nbsp;</td><td>\n");

					p.append("<input type='checkbox' value='true' name='" + mbean.getAbsoluteName() + "/" + att.getName() + ".override'");
					if(localAttribute != null){
						 p.append(" checked");
					}
					p.append("></td>\n<td>\n");
					if(legalValues != null && legalValues.length > 0){
						//Have legal values. Build combobox.
						p.append("<select name='" + mbean.getAbsoluteName() + "/" + att.getName() + "' style='width: 320px'>\n");
						for(int i=0 ; i < legalValues.length ; i++){
							p.append("<option value='"+legalValues[i]+"'");
							if(currentAttribute.equals(legalValues[i])){
								p.append(" selected");
							}
							p.append(">"+legalValues[i]+"</option>\n");
						}
						p.append("</select>\n");
					}
					else if(currentAttribute instanceof Boolean){
						// Boolean value
						p.append("<select name='" + mbean.getAbsoluteName() + "/" + att.getName() + "' style='width: 320px'>\n");
						p.append("<option value='False'"+ (currentAttribute.equals(new Boolean(false))?" selected":"") +">False</option>\n");
						p.append("<option value='True'"+ (currentAttribute.equals(new Boolean(true))?" selected":"") +">True</option>\n");
						p.append("</select>\n");
					}
					else{
						//Input box
						p.append("<input name='" + mbean.getAbsoluteName() + "/" + att.getName() + "' value='" + currentAttribute + "' style='width: 320px'>\n");
					}
					
					p.append("</td></tr>\n");
				}
		    }
		}
		return p.toString();
	}
	
	/** TODO: FIX JAVADOC
	 * This methods updates a ComplexType with information passed to it
	 * by a HttpServletRequest. It assumes that for every 'simple' type
	 * there is a corrisponding parameter in the request. A recursive
	 * call will be made for any nested ComplexTypes.
	 * 
	 * @param mbean The ComplexType to update
	 * @param request The HttpServletRequest to use to update the 
	 *                ComplexType
	 */
	public void writeNewOrderFile(ComplexType mbean, CrawlerSettings settings, HttpServletRequest request){
		if(mbean.isTransient()){
			return;
		}
		MBeanInfo info = mbean.getMBeanInfo(settings);
		MBeanAttributeInfo a[] = info.getAttributes();
		for(int n=0; n<a.length; n++) {
            Object currentAttribute = null;
			ModuleAttributeInfo att = (ModuleAttributeInfo)a[n]; //The attributes of the current attribute.
			try {
				currentAttribute = mbean.getAttribute(att.getName());
			} catch (Exception e1) {
				return;
			}

			if(currentAttribute instanceof ComplexType) {
		    	writeNewOrderFile((ComplexType)currentAttribute, settings, request);
			}
			else {
				// Have a 'setting'. Let's see if it's been overridden.
				if(request.getParameter(mbean.getAbsoluteName()+"/"+att.getName()+".override") != null
				   && request.getParameter(mbean.getAbsoluteName()+"/"+att.getName()+".override").equals("true")){
					//Is being overridden.
					if(currentAttribute instanceof ListType){
						ListType list = (ListType)currentAttribute;
						list.clear();
						String[] elems = request.getParameterValues(mbean.getAbsoluteName() + "/" + att.getName());
						for(int i=0 ; elems != null && i < elems.length ; i++){
							list.add(elems[i]);
						}
					}
					else{
						try{
							mbean.setAttribute(settings, new Attribute(att.getName(),request.getParameter(mbean.getAbsoluteName() + "/" + att.getName())));
						} catch (Exception e1) {
							e1.printStackTrace();
							return;
						}
					}
				} else {
					// Is not being overriden. 
					// TODO: Need to see if it WAS overridden and if so remove the override.
				}
			}
		}
	}
%>
<%
	// Load the job to configure.
	CrawlJob theJob = handler.getJob(request.getParameter("job"));

	// Load display level
	String currDomain = request.getParameter("currDomain");
	
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

	// Get the settings objects.
	XMLSettingsHandler settingsHandler = theJob.getSettingsHandler();
	CrawlOrder crawlOrder = settingsHandler.getOrder();
    CrawlerSettings orderfile = settingsHandler.getSettingsObject(currDomain);

	// Check for actions.
	if(request.getParameter("update") != null && request.getParameter("update").equals("true")){
		// Update values with new ones in the request
		writeNewOrderFile(crawlOrder,orderfile,request);
		settingsHandler.writeSettingsObject(orderfile);
		
		if(request.getParameter("action").equals("done")){
			if(theJob.isNew()){			
				handler.addJob(theJob);
				response.sendRedirect("/admin/jobs.jsp?message=Job created");
			}else{
				if(theJob.isRunning()){
					handler.kickUpdate();
				}
				response.sendRedirect("/admin/jobs/per/overview.jsp?job="+theJob.getUID()+"&currDomain="+currDomain);
			}
		}else if(request.getParameter("action").equals("filters")){
			response.sendRedirect("/admin/jobs/per/filters.jsp?job="+theJob.getUID()+"&currDomain="+currDomain);
		}
		return;
	}	

	// Get the HTML code to display the settigns.
	StringBuffer listsBuffer = new StringBuffer();
	String inputForm=printMBean(crawlOrder,orderfile,"",listsBuffer);
	// The listsBuffer will have a trailing comma if not empty. Strip it off.
	String lists = listsBuffer.toString().substring(0,(listsBuffer.toString().length()>0?listsBuffer.toString().length()-1:0));

	// Settings for the page header
	String title = "Configure override";
	int tab = theJob.isProfile()?2:1;
	int jobtab = 2;
%>

<%@include file="/include/head.jsp"%>

	<script type="text/javascript">
		function doAddList(listName){
			newItem = document.getElementById(listName+"/add");
			theList = document.getElementById(listName);
			
			if(newItem.value.length > 0){
				insertLocation = theList.length;
				theList.options[insertLocation] = new Option(newItem.value, newItem.value, false, false);
				newItem.value = "";
			}
		}
		
		function doDeleteList(listName){
			theList = document.getElementById(listName);
			theList.options[theList.selectedIndex] = null;
		}
		
		function doSubmit(){
			// Before the form can be submitted we must
			// ensure that ALL elements in ALL lists
			// are selected. Otherwise they will be lost.
			lists = new Array(<%=lists%>);
			for(i=0 ; i<lists.length ; i++){
				theList = document.getElementById(lists[i]);
				for(j=0 ; j < theList.length ; j++){
					theList.options[j].selected = true;
				}
			}
			document.frmConfig.submit();
		}
		
		function doGotoFilters(){
			document.frmConfig.action.value="filters";
			doSubmit();
		}
		
		function doPop(text){
			alert(text);
		}
	</script>

	<p>
		<b>Override for the <%=theJob.isProfile()?"profile":"job"%> <%=theJob.getJobName()%> on domain '<%=currDomain%>'</b>
		<%@include file="/include/jobpernav.jsp"%>
	<p>
	
	<form name="frmConfig" method="post" action="configure.jsp">
		<input type="hidden" name="update" value="true">		
		<input type="hidden" name="action" value="done">
		<input type="hidden" name="currDomain" value="<%=currDomain%>">
		<input type="hidden" name="job" value="<%=theJob.getUID()%>">
	
		<p>	
			<b>Instructions:</b> To override a setting, check the box in front of it and input new settings.<br>
			Unchecked settings are inherited settings from super domain. Changes to settings that<br>
			do not have a checked box will be discarded.
		<p>		
		<table>
			<%=inputForm%>
		</table>
	</form>
	<p>
		<%@include file="/include/jobpernav.jsp"%>
		
<%@include file="/include/foot.jsp"%>
