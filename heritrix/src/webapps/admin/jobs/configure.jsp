<%
  /**
   * This pages allows the user to edit the configuration 
   * of a crawl order. 
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
<%@ page import="org.archive.util.TextUtils" %>
<%@ page import="java.io.FileReader" %>
<%@ page import="java.io.FileWriter" %>
<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.BufferedWriter" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.util.regex.*"%>
<%@ page import="javax.management.MBeanInfo"%>
<%@ page import="javax.management.Attribute"%>
<%@ page import="javax.management.MBeanAttributeInfo"%>

<%!
	/**
	 * Builds up the the HTML code to display any ComplexType attribute
	 * of the settings in an editable form. Uses recursion.
	 *
	 * @param mbean The ComplexType to build a display
	 * @param indent A string that will be added in front to indent the
	 *               current type.
	 * @param lists All 'lists' encountered will have their name added	 
	 *              to this StringBuffer followed by a comma.
	 * @param expert if true then expert settings will be included, else
	 *               they will be hidden.
	 * @returns The HTML code described above.
	 */
	public String printMBean(ComplexType mbean, String indent, StringBuffer lists, boolean expert) throws Exception {
		if(mbean.isTransient() || (mbean.isExpertSetting() && expert == false)){
			return "";
		}
		StringBuffer p = new StringBuffer();
		MBeanInfo info = mbean.getMBeanInfo();
        MBeanAttributeInfo[] a = info.getAttributes();
        
        if( mbean instanceof MapType && a.length ==0 ){
            // Empty map, ignore it.
            return "";
        }
        
		p.append("<tr><td><b>" + indent + mbean.getName() + "</b></td>\n");
		p.append("<td><a class='help' href=\"javascript:doPop('");
		p.append(TextUtils.escapeForJavascript(mbean.getDescription()));
		p.append("')\">?</a></td>");

		String shortDescription = mbean.getDescription();
		// Need to cut off everything after the first sentance.
		Pattern firstSentance = Pattern.compile("^[^\\.)]*\\.\\s");
 		Matcher m = firstSentance.matcher(mbean.getDescription());
 		if(m.find()){
	 		shortDescription = m.group(0);
		}
 		
		p.append("<td><font size='-2'>" + shortDescription + "</font></td></tr>\n");

		for(int n=0; n<a.length; n++) {
	        if(a[n] == null) {
                p.append("  ERROR: null attribute");
            } else {
	            Object currentAttribute = null;
				ModuleAttributeInfo att = (ModuleAttributeInfo)a[n]; //The attributes of the current attribute.

                if(att.isTransient()==false && (att.isExpertSetting()==false || expert)){
					try {
						currentAttribute = mbean.getAttribute(att.getName());
					} catch (Exception e1) {
						String error = e1.toString() + " " + e1.getMessage();
						return error;
					}
	
					if(currentAttribute instanceof ComplexType) {
				    	p.append(printMBean((ComplexType)currentAttribute,indent+"&nbsp;&nbsp;",lists,expert));
					}
					else if(currentAttribute instanceof ListType){
						// Some type of list.
						ListType list = (ListType)currentAttribute;
						p.append("<tr><td valign='top'>" + indent + "&nbsp;&nbsp;" + att.getName() + ":&nbsp;</td>");
						p.append("<td valign='top'><a class='help' href=\"javascript:doPop('");
						p.append(TextUtils.escapeForJavascript(att.getDescription()));
						p.append("')\">?</a>&nbsp;</td>\n");
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
						
						p.append("<tr><td>" + indent + "&nbsp;&nbsp;" + att.getName() + ":&nbsp;</td>");
						p.append("<td ><a class='help' href=\"javascript:doPop('");
						p.append(TextUtils.escapeForJavascript(att.getDescription()));
						p.append("')\">?</a>&nbsp;</td><td>\n");
						
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
						else if(currentAttribute instanceof TextField){
							// Text area
							p.append("<textarea name='" + mbean.getAbsoluteName() + "/" + att.getName() + "' style='width: 320px' rows='4'>");
							p.append(currentAttribute + "\n");
							p.append("</textarea>\n");
						}
						else{
							//Input box
							p.append("<input name='" + mbean.getAbsoluteName() + "/" + att.getName() + "' value='" + currentAttribute + "' style='width: 320px'>\n");
						}
						
						p.append("</td></tr>\n");
					}
				}
		    }
		}
		return p.toString();
	}
	
	/**
	 * This methods updates a ComplexType with information passed to it
	 * by a HttpServletRequest. It assumes that for every 'simple' type
	 * there is a corrisponding parameter in the request. A recursive
	 * call will be made for any nested ComplexTypes.
	 * 
	 * @param mbean The ComplexType to update
	 * @param request The HttpServletRequest to use to update the 
	 *                ComplexType
     * @param expert if true then expert settings are included, else
     *               they should be ignored
	 */
	public void writeNewOrderFile(ComplexType mbean, HttpServletRequest request, boolean expert){
		if(mbean.isTransient() || (mbean.isExpertSetting() && expert == false)){
			return;
		}
		MBeanInfo info = mbean.getMBeanInfo();
		MBeanAttributeInfo a[] = info.getAttributes();
		for(int n=0; n<a.length; n++) {
            Object currentAttribute = null;
			ModuleAttributeInfo att = (ModuleAttributeInfo)a[n]; //The attributes of the current attribute.
			try {
				currentAttribute = mbean.getAttribute(att.getName());
			} catch (Exception e1) {
				return;
			}

            if (att.isTransient() == false && (att.isExpertSetting()==false || expert)) {
				if(currentAttribute instanceof ComplexType) {
			    	writeNewOrderFile((ComplexType)currentAttribute, request, expert);
				}
				else if(currentAttribute instanceof ListType){
					ListType list = (ListType)currentAttribute;
					list.clear();
					String[] elems = request.getParameterValues(mbean.getAbsoluteName() + "/" + att.getName());
					for(int i=0 ; elems != null && i < elems.length ; i++){
						list.add(elems[i]);
					}
				}
				else{
				    try{
					   mbean.setAttribute(new Attribute(att.getName(),request.getParameter(mbean.getAbsoluteName() + "/" + att.getName())));
    				} catch (Exception e1) {
	   				    e1.printStackTrace();
					   return;
				    }
				}
			}
		}
	}
%>
<%
	// Load the job to configure.
	CrawlJob theJob = handler.getJob(request.getParameter("job"));
	
	boolean expert = false;
    if(getCookieValue(request.getCookies(),"expert","false").equals("true")){
        expert = true;
    }
    
	if(theJob == null)
	{
		// Didn't find any job with the given UID or no UID given.
		response.sendRedirect("/admin/jobs.jsp?message=No job selected "+request.getParameter("job"));
		return;
	} else if(theJob.isReadOnly()){
		// Can't edit this job.
		response.sendRedirect("/admin/jobs.jsp?message=Can't configure a read only job");
		return;
	}

	// Get the settings objects.
	XMLSettingsHandler settingsHandler = theJob.getSettingsHandler();
	CrawlOrder crawlOrder = settingsHandler.getOrder();
    CrawlerSettings orderfile = settingsHandler.getSettingsObject(null);

	// Check for actions.
	if(request.getParameter("update") != null && request.getParameter("update").equals("true")){
		// Update values with new ones in the request
		writeNewOrderFile(crawlOrder,request,expert);
		orderfile.setDescription(request.getParameter("meta/description"));
		
		settingsHandler.writeSettingsObject(orderfile);
		
        BufferedWriter writer;
        try {
			String seedfile = (String)((ComplexType)settingsHandler.getOrder().getAttribute("scope")).getAttribute("seedsfile");
            writer = new BufferedWriter(new FileWriter(settingsHandler.getPathRelativeToWorkingDirectory(seedfile)));
            if (writer != null) {
                // TODO Read seeds from profile.
                writer.write(request.getParameter("seeds"));
                writer.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

		if(request.getParameter("action").equals("done")){
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
		}else if(request.getParameter("action").equals("modules")){
			response.sendRedirect("/admin/jobs/modules.jsp?job="+theJob.getUID());
            return;
		}else if(request.getParameter("action").equals("filters")){
			response.sendRedirect("/admin/jobs/filters.jsp?job="+theJob.getUID());
            return;
        }else if(request.getParameter("action").equals("per")){
            response.sendRedirect("/admin/jobs/per/overview.jsp?job="+theJob.getUID());
            return;
        }else if(request.getParameter("action").equals("updateexpert")){
		    if(request.getParameter("expert") != null){
		        if(request.getParameter("expert").equals("true")){
		            expert = true;
		        } else {
		            expert = false;
		        }
		        // Save to cookie.
		        Cookie operatorCookie = new Cookie("expert", Boolean.toString(expert));
		        operatorCookie.setMaxAge(60*60*24*365);//One year
		        response.addCookie(operatorCookie);
		    }
		}
	}	


	// Get the HTML code to display the settigns.
	StringBuffer listsBuffer = new StringBuffer();
	String inputForm=printMBean(crawlOrder,"",listsBuffer,expert);
	// The listsBuffer will have a trailing comma if not empty. Strip it off.
	String lists = listsBuffer.toString().substring(0,(listsBuffer.toString().length()>0?listsBuffer.toString().length()-1:0));

	// Settings for the page header
	String title = "Configure settings";
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
		
		function doGotoModules(){
			document.frmConfig.action.value="modules";
			doSubmit();
		}
		
		function doGotoFilters(){
			document.frmConfig.action.value="filters";
			doSubmit();
		}
		
		function doGotoPer(){
			document.frmConfig.action.value="per";
			doSubmit();
		}
		
		function doPop(text){
			alert(text);
		}
		
		function setExpert(val){
            document.frmConfig.expert.value = val;
            document.frmConfig.action.value="updateexpert";
            doSubmit();
		}
	</script>

	<p>
		<%@include file="/include/jobnav.jsp"%>
	<p>
        <% if(expert){ %>
            <a href="javascript:setExpert('false')">Hide expert settings</a>
        <% } else { %>
            <a href="javascript:setExpert('true')">View expert settings</a>
        <% } %>
	<p>
	
	<form name="frmConfig" method="post" action="configure.jsp">
		<input type="hidden" name="update" value="true">		
		<input type="hidden" name="action" value="done">
		<input type="hidden" name="expert" value="<%=expert%>">
		<input type="hidden" name="job" value="<%=theJob.getUID()%>">
	
		<p>			
		<table>
			<tr>
				<td colspan="3">
					<b>Meta data</b>
				</td>
			</tr>
			<tr>
				<td>
					Description:
				</td>
				<td></td>
				<td>
					<input name="meta/description" value="<%=orderfile.getDescription()%>" style="width: 320px">
				</td>
			</tr>
			<%=inputForm%>
			<tr>
				<td colspan="3">
					<b>Seeds</b>
				</td>
			</tr>
			<tr>
				<td valign="top">
					Seeds:
				</td>
				<td></td>
				<td>
					<textarea name="seeds" style="width: 320px" rows="8"><%
						BufferedReader seeds = new BufferedReader(new FileReader(settingsHandler.getPathRelativeToWorkingDirectory((String)((ComplexType)settingsHandler.getOrder().getAttribute("scope")).getAttribute("seedsfile"))));
						String sout = seeds.readLine();
						while(sout!=null){
							out.println(sout);
							sout = seeds.readLine();
						}
					%></textarea>
				</td>
			</tr>
		</table>
	</form>
	<p>
		<%@include file="/include/jobnav.jsp"%>
		
<%@include file="/include/foot.jsp"%>
