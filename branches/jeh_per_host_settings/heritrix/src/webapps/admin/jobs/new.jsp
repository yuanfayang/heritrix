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

		p.append("<tr><td colspan='2'><b>" + indent + info.getDescription() + "</b></td></tr>\n");

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
	
	XMLSettingsHandler settingsHandler = new XMLSettingsHandler(new File(handler.getDefaultSettingsFilename()));
	settingsHandler.initialize();
	CrawlOrder crawlOrder = settingsHandler.getOrder();
    CrawlerSettings orderfile = settingsHandler.getSettingsObject(null);

	if(request.getParameter("action") != null && request.getParameter("action").equals("new")){
		// Update values with new ones in the request
		writeNewOrderFile(crawlOrder,request);
		orderfile.setName(request.getParameter("meta/name"));
		orderfile.setDescription(request.getParameter("meta/description"));
		
		// Get a UID.
		String newUID = handler.getNextJobUID();
		
		// Create filenames etc.
		File f = new File("jobs"+File.separator+newUID);
		f.mkdirs();
		String seedfile = "jobs"+File.separator+newUID+File.separator+"seeds-"+orderfile.getName()+".txt";
		((ComplexType)settingsHandler.getOrder().getAttribute("scope")).setAttribute(new Attribute("seedsfile",seedfile));
		File newFile = new File("jobs"+File.separator+newUID+File.separator+"job-"+orderfile.getName()+"-1.xml");
		settingsHandler.writeSettingsObject(orderfile,newFile);
		
		try{
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(seedfile)));
            if (writer != null) {
                writer.write(request.getParameter("seeds"));
                writer.close();
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }

		// Make new job
		CrawlJob cj = new CrawlJob(newUID,
		                           orderfile.getName(),
		                           new XMLSettingsHandler(newFile),
		                           CrawlJob.PRIORITY_AVERAGE);
		handler.addJob(cj);
		response.sendRedirect("/admin/jobs.jsp?message=Job created");
	}	

	String inputForm=printMBean(crawlOrder,"");
	
	String title = "New crawl job";
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
		</script>

		<p><font color="red"><%=message%></font>
		
		<form name="frmConfig" method="post" action="new.jsp">
			<input type="hidden" name="action" value="new">		
			<table>
				<tr>
					<td colspan="2">
						<b>Meta data</b>
					</td>
				<tr>
					<td>
						Name:
					</td>
					<td>
						<input name="meta/name" value="<%=orderfile.getName()%>" style="width: 320px">
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
						<textarea name="seeds" style="width: 320px" rows="8">#Input seeds here</textarea>
					</td>
				</tr>
			</table>
		
		<input type="button" value="Submit job" onClick="doSubmit()">
		
		</form>
		
<%@include file="/include/foot.jsp"%>
