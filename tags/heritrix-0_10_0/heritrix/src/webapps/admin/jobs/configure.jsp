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
<%@include file="/include/jobconfigure.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="java.io.FileReader" %>
<%@ page import="java.io.FileWriter" %>
<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.BufferedWriter" %>
<%@ page import="java.io.IOException" %>

<% 
	// Load the job to configure.
	CrawlJob theJob = handler.getJob(request.getParameter("job"));
    CrawlJobErrorHandler errorHandler = theJob.getErrorHandler();
	
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

    // Should we update with changes.
	if(request.getParameter("update") != null && request.getParameter("update").equals("true")){
		// Update values with new ones in the request
		errorHandler.clearErrors();
		writeNewOrderFile(crawlOrder,null,request,expert);
		orderfile.setDescription(request.getParameter("meta/description"));
		orderfile.setOperator(request.getParameter("meta/operator"));
		orderfile.setOrganization(request.getParameter("meta/organization"));
		orderfile.setAudience(request.getParameter("meta/audience"));
		
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
    }
    
    // Check for actions.
    String action = request.getParameter("action");
    if(action != null){
		if(action.equals("done")){
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
		}else if(action.equals("goto")){
            // Goto another page of the job/profile settings
			response.sendRedirect(request.getParameter("item"));
            return;
        } else if (action.equals("addMap")) {
            // Adding to a simple map
            String mapName = request.getParameter("update");
            MapType map = (MapType)settingsHandler.getComplexTypeByAbsoluteName(orderfile,mapName);
            String key = request.getParameter(mapName+".key");
            String value = request.getParameter(mapName+".value");
            SimpleType t = new SimpleType(key,"",value);
            map.addElement(orderfile,t);
            response.sendRedirect("configure.jsp?job="+theJob.getUID());
            return;
        } else if (action.equals("deleteMap")) {
            // Removing from a simple map
            String mapName = request.getParameter("update");
            String key = request.getParameter("item");
            MapType map = (MapType)settingsHandler.getComplexTypeByAbsoluteName(orderfile,mapName);
            map.removeElement(orderfile,key);
            response.sendRedirect("configure.jsp?job="+theJob.getUID());
            return;
		}else if(action.equals("updateexpert")){
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
	String inputForm=printMBean(crawlOrder,null,"",listsBuffer,expert,errorHandler);
	// The listsBuffer will have a trailing comma if not empty. Strip it off.
	String lists = listsBuffer.toString().substring(0,(listsBuffer.toString().length()>0?listsBuffer.toString().length()-1:0));

	// Settings for the page header
	String title = "Configure settings";
	int tab = theJob.isProfile()?2:1;
	int jobtab = 2;
%>

<%@include file="/include/head.jsp"%>

	<script type="text/javascript">
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
		
		function doGoto(where){
            document.frmConfig.action.value="goto";
            document.frmConfig.item.value = where;
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
		
        function setUpdate(){
            document.frmConfig.update.value = "true";
        }

        function setEdited(name){
            setUpdate();
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
		<input type="hidden" name="update" value="false">		
		<input type="hidden" name="action" value="done">
		<input type="hidden" name="item" value="">
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
			<tr>
				<td>
					Crawl Operator:
				</td>
				<td></td>
				<td>
					<input name="meta/operator" value="<%=orderfile.getOperator()%>" style="width: 320px">
				</td>
			</tr>
			<tr>
				<td>
					Crawl Organization:
				</td>
				<td></td>
				<td>
					<input name="meta/organization" value="<%=orderfile.getOrganization()%>" style="width: 320px">
				</td>
			</tr>
			<tr>
				<td>
					Crawl Job Recipient:
				</td>
				<td></td>
				<td>
					<input name="meta/audience" value="<%=orderfile.getAudience()%>" style="width: 320px">
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
					<textarea name="seeds" style="width: 320px" rows="8" onChange="setUpdate()"><%
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
