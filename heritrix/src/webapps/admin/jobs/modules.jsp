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

	XMLSettingsHandler settingsHandler = (XMLSettingsHandler)handler.getJob(request.getParameter("job")).getSettingsHandler();
	settingsHandler.initialize();


	String title = "Edit job modules";
	int tab = 1;
%>

<%@include file="/include/head.jsp"%>

<p><b>Chose URI Frontier</b>

<p><b>Select Processors</b>
<p>
<table>
<%
	Vector unusedProcessors = new Vector();
	for(int i=0 ; i<availibleProcessors.length ; i++){
	
		ComplexType mbean = ((ComplexType)settingsHandler.getOrder().getAttribute("processors"));
		MBeanInfo procInfo = mbean.getMBeanInfo();
		MBeanAttributeInfo a[] = procInfo.getAttributes();
		
		boolean isIncluded = false;
		
		for(int n=0; n<a.length; n++) {
            Object currentAttribute = null;
			ModuleAttributeInfo att = (ModuleAttributeInfo)a[n]; //The attributes of the current attribute.
			try {
				currentAttribute = mbean.getAttribute(att.getName());
			} catch (Exception e1) {
				out.println(e1.toString() + " " + e1.getMessage());
			}
			if(att.getType().equals(availibleProcessors[i])){
				//Found it
				isIncluded = true;
				break;
			}
		}
		if(isIncluded){
			out.println("<tr><td>"+availibleProcessors[i]+"</td><td><a href=''>Move up</a> <a href=''>Move down</a> <a href=''>Remove</a></td></tr>");
		}
		else{
			unusedProcessors.add(availibleProcessors[i]);
		}
	}
%>
	<tr>
		<td>
			<select>
			<%
				for(int i=0 ; i<unusedProcessors.size() ; i++){
					out.println("<option>"+unusedProcessors.get(i)+"</option>");
				}
			%>
			</select>
		</td>
		<td>
			<input type="button" value="Add">
		</td>
	</tr>

</table>

<p><b>Select Statistics Tracking</b>

<%@include file="/include/foot.jsp"%>


