<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.datamodel.settings.*" %>
<%@ page import="org.archive.crawler.framework.CrawlController" %>

<%@ page import="java.io.*,java.lang.Boolean,java.util.Vector" %>
<%@ page import="javax.management.MBeanInfo, javax.management.Attribute, javax.management.MBeanAttributeInfo,javax.management.AttributeNotFoundException, javax.management.MBeanException,javax.management.ReflectionException"%>

<%
	String[] availibleURIFrontiers = {"org.archive.crawler.basic.Frontier"};
	String[] availibleProcessors = {"org.archive.crawler.basic.Preselector",
									"org.archive.crawler.basic.PreconditionEnforcer",
									"org.archive.crawler.fetcher.FetchDNS",
									"org.archive.crawler.fetcher.FetchHTTP",
									"org.archive.crawler.extractor.ExtractorHTTP",
									"org.archive.crawler.extractor.ExtractorHTML",
									"org.archive.crawler.extractor.ExtractorHTML2",
									"org.archive.crawler.extractor.ExtractorSWF",
									"org.archive.crawler.extractor.ExtractorJS",
									"org.archive.crawler.extractor.ExtractorPDF",
									"org.archive.crawler.extractor.ExtractorDOC",
									"org.archive.crawler.extractor.ExtractorUniversal",
									"org.archive.crawler.basic.ARCWriterProcessor",
									"org.archive.crawler.basic.CrawlStateUpdater",
									"org.archive.crawler.basic.Postselector"
									};

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

	String title = "Edit job modules";
	int tab = 1;
%>

<%@include file="/include/head.jsp"%>

<p><b>Chose URI Frontier</b>
<p>
<table>
	<tr>
		<td>
			<select>
			<%
				MBeanAttributeInfo frontier = settingsHandler.getOrder().getAttributeInfo("frontier");
				for(int i=0 ; i<availibleURIFrontiers.length ; i++){
					out.print("<option value='"+availibleURIFrontiers[i]+"'");
					if(frontier.getType().equals((String)availibleURIFrontiers[i])){
						out.print(" selected");
					}
					out.println(">"+ availibleURIFrontiers[i]+"</option>");
				}
			%>
			</select>
		</td>
		<td>
			<input type="button" value="Set URI Frontier">
		</td>
	</tr>
</table>

<p>
	<b>Select Processors</b>
<p>
<form name="frmProcessors" method="post" action="modules.jsp">
<table>
<%
	Vector unusedProcessors = new Vector();
	ComplexType procs = ((ComplexType)settingsHandler.getOrder().getAttribute("processors"));
	MBeanInfo procInfo = procs.getMBeanInfo();
	MBeanAttributeInfo p[] = procInfo.getAttributes();
	
	// Printout used proc.
	for(int n=0; n<p.length; n++) {
        Object currentAttribute = null;
		ModuleAttributeInfo att = (ModuleAttributeInfo)p[n]; //The attributes of the current attribute.

		out.println("<tr><td>"+att.getType()+"</td><td><a href=\"doMoveUpProcessor('"+att.getName()+"')\">Move up</a> <a href=\"doMoveDownProcessor('"+att.getName()+"')\">Move down</a> <a href=\"doMoveRemoveProcessor('"+att.getName()+"')\">Remove</a></td></tr>");
	}

	// Find out which aren't being used.
	for(int i=0 ; i<availibleProcessors.length ; i++){
		boolean isIncluded = false;
		
		for(int n=0; n<p.length; n++) {
            Object currentAttribute = null;
			ModuleAttributeInfo att = (ModuleAttributeInfo)p[n]; //The attributes of the current attribute.

			try {
				currentAttribute = procs.getAttribute(att.getName());
			} catch (Exception e1) {
				out.println(e1.toString() + " " + e1.getMessage());
			}
			if(att.getType().equals(availibleProcessors[i])){
				//Found it
				isIncluded = true;
				break;
			}
		}
		if(isIncluded == false){
			// Yep the current one is unused.
			unusedProcessors.add(availibleProcessors[i]);
		}
	}
%>
	<tr>
		<td>
			<select name="cboAddProcessor">
			<%
				for(int i=0 ; i<unusedProcessors.size() ; i++){
					out.println("<option value='"+unusedProcessors.get(i)+"'>"+unusedProcessors.get(i)+"</option>");
				}
			%>
			</select>
		</td>
		<td>
			<input type="button" value="Add">
		</td>
	</tr>
</table>
</form>

<p><b>Select Statistics Tracking</b>

<%@include file="/include/foot.jsp"%>


