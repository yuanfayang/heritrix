<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.datamodel.settings.*" %>
<%@ page import="org.archive.crawler.framework.CrawlController" %>

<%@ page import="java.io.*,java.lang.Boolean" %>
<%@ page import="javax.management.MBeanInfo, javax.management.Attribute, javax.management.MBeanAttributeInfo,javax.management.AttributeNotFoundException, javax.management.MBeanException,javax.management.ReflectionException"%>

<%!
	StringBuffer lists = new StringBuffer();
	public String printMBean(ComplexType mbean, String indent) throws Exception {
		StringBuffer p = new StringBuffer();
		MBeanInfo info = mbean.getMBeanInfo();

		p.append("<tr><td colspan='2'><b>" + indent + mbean.getName() + "</b></td></tr>\n");

		MBeanAttributeInfo a[] = info.getAttributes();
		
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
			    	p.append(printMBean((ComplexType)currentAttribute,indent+"&nbsp;&nbsp;"));
				}
				else if(currentAttribute instanceof ListType){
					// Some type of list.
					ListType list = (ListType)currentAttribute;
					p.append("<tr><td valign='top'>" + indent + att.getName() + ":&nbsp;</td>\n");
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
					
					p.append("<tr><td>" + indent + att.getName() + ":&nbsp;</td><td>");
					
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
	
	public void writeNewOrderFile(ComplexType mbean, HttpServletRequest request){
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

			if(currentAttribute instanceof ComplexType) {
		    	writeNewOrderFile((ComplexType)currentAttribute, request);
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

%>


<%
	/**
	 * This page enables customized jobs to be scheduled.
	 * (order.xml is used for default values)
	 */
	String message = "";

	// Get the default settings.
	
	CrawlJob theJob = handler.getJob(request.getParameter("job"));
	
	
	if(theJob == null)
	{
		// Didn't find any job with the given UID or no UID given.
		response.sendRedirect("/admin/jobs.jsp?message=No job selected");
		return;
	} else if(theJob.isReadOnly()){
		// Can't edit this job.
		response.sendRedirect("/admin/jobs.jsp?message=Can't configure a running job");
		return;
	}

	XMLSettingsHandler settingsHandler = (XMLSettingsHandler)theJob.getSettingsHandler();

	boolean isNew = theJob.isNew();
	
	CrawlOrder crawlOrder = settingsHandler.getOrder();
    CrawlerSettings orderfile = settingsHandler.getSettingsObject(null);

	if(request.getParameter("update") != null && request.getParameter("update").equals("true")){
		// Update values with new ones in the request
		writeNewOrderFile(crawlOrder,request);
		orderfile.setDescription(request.getParameter("meta/description"));
		
		settingsHandler.writeSettingsObject(orderfile);
		
        BufferedWriter writer;
        try {
			String seedfile = (String)((ComplexType)settingsHandler.getOrder().getAttribute("scope")).getAttribute("seedsfile");
            writer = new BufferedWriter(new FileWriter(new File(settingsHandler.getPathRelativeToWorkingDirectory(seedfile))));
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
				response.sendRedirect("/admin/jobs.jsp?message=Job modified");
			}
		}else{
			response.sendRedirect("/admin/jobs/editmodules.jsp?session=true");
		}
		return;
	}	

	String inputForm=printMBean(crawlOrder,"");
	
	String title = "Configure crawl order";
	int tab = 1;
%>

<%@include file="/include/head.jsp"%>

		<script type="text/javascript">
			function doAddList(listName)
			{
				newItem = document.getElementById(listName+"/add");
				theList = document.getElementById(listName);
				
				if(newItem.value.length > 0)
				{
					insertLocation = theList.length;
					theList.options[insertLocation] = new Option(newItem.value, newItem.value, false, false);
					newItem.value = "";
				}
			}
			
			function doDeleteList(listName)
			{
				theList = document.getElementById(listName);
				theList.options[theList.selectedIndex] = null;
			}
			
			function doSubmit()
			{
				lists = new Array(<%=lists.toString().substring(0,(lists.toString().length()>0?lists.toString().length()-1:0))%>);
				for(i=0 ; i<lists.length ; i++)
				{
					theList = document.getElementById(lists[i]);
					for(j=0 ; j < theList.length ; j++)
					{
						theList.options[j].selected = true;
					}
				}
				document.frmConfig.submit();
			}
			
			function doGotoModules()
			{
				document.frmConfig.action.value="modules";
				doSubmit();
			}
		</script>

		<p><font color="red"><%=message%></font>
		
		<form name="frmConfig" method="post" action="configure.jsp">
			<input type="hidden" name="update" value="true">		
			<input type="hidden" name="action" value="done">
			<input type="hidden" name="job" value="<%=theJob.getUID()%>">

			<% if(theJob.isRunning() == false){ %>
				<input type="button" value="Adjust modules" onClick="doGotoModules()">
			<% } %>
			<input type="button" value="Done" onClick="doSubmit()">
			<p>			
			<table>
				<tr>
					<td colspan="2">
						<b>Meta data</b>
					</td>
				</tr>
				<tr>
					<td>
						Description:
					</td>
					<td>
						<input name="meta/description" value="<%=orderfile.getDescription()%>" style="width: 320px">
					</td>
				</tr>
				<%=inputForm%>
				<tr>
					<td colspan="2">
						<b>Seeds</b>
					</td>
				</tr>
				<tr>
					<td valign="top">
						Seeds:
					</td>
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
			<p>
			<% if(theJob.isRunning() == false){ %>
				<input type="button" value="Adjust modules" onClick="doGotoModules()">
			<% } %>
			<input type="button" value="Done" onClick="doSubmit()">
		
		</form>
		
<%@include file="/include/foot.jsp"%>
