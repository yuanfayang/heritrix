<%
  /**
   * This pages allows the user to edit the configuration 
   * of a refinement. 
   * That is set any af the 'values', but does not allow
   * users to change which 'modules' are used.
   *
   * @author Kristinn Sigurdsson
   */
%>
<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>
<%@include file="/include/jobconfigure.jsp"%>

<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@page import="org.archive.crawler.datamodel.settings.refinements.*"%>

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
		response.sendRedirect("/admin/jobs.jsp?message=Can't configure a running job");
		return;
	}

    // Load display level
    String currDomain = request.getParameter("currDomain");
    String reference = request.getParameter("reference");

    // Get the settings objects.
    XMLSettingsHandler settingsHandler = theJob.getSettingsHandler();
    CrawlOrder crawlOrder = settingsHandler.getOrder();
    boolean global = currDomain == null || currDomain.length() == 0;

    CrawlerSettings localSettings;
    
    if(global){
        localSettings = settingsHandler.getSettingsObject(null);
    } else {
        localSettings = settingsHandler.getSettingsObject(currDomain);
    }
    
    Refinement refinement = localSettings.getRefinement(reference);

    CrawlerSettings orderfile = refinement.getSettings();

    
	// Check for update.
	if(request.getParameter("update") != null && request.getParameter("update").equals("true")){
		// Update values with new ones in the request
		writeNewOrderFile(crawlOrder,orderfile,request,expert);
		settingsHandler.writeSettingsObject(orderfile);
	}
	
	// Check for actions
    String action = request.getParameter("action");
    if(action != null){
		if(action.equals("done")){
			if(theJob.isRunning()){
				handler.kickUpdate();
			}
			response.sendRedirect("/admin/jobs/refinements/overview.jsp?job="+theJob.getUID()+"&currDomain="+currDomain+"&message=Override changes saved");
            return;
		}else if(action.equals("goto")){
            // Goto another page of the job/profile settings
			response.sendRedirect(request.getParameter("where")+"&currDomain="+currDomain+"&reference="+reference);
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
	String inputForm=printMBean(crawlOrder,orderfile,"",listsBuffer,expert,errorHandler);
	// The listsBuffer will have a trailing comma if not empty. Strip it off.
	String lists = listsBuffer.toString().substring(0,(listsBuffer.toString().length()>0?listsBuffer.toString().length()-1:0));

	// Settings for the page header
	String title = "Configure refinement";
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
			setEdited(listName);
		}
		
		function doDeleteList(listName){
			theList = document.getElementById(listName);
			theList.options[theList.selectedIndex] = null;
			setEdited(listName);
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
		
		function doGoto(where){
			document.frmConfig.action.value="goto";
			document.frmConfig.where.value=where;
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
            checkbox = document.getElementById(name+".override");
            checkbox.checked = true;
            setUpdate();
        }
	</script>

	<p>
		<b>Override for the <%=theJob.isProfile()?"profile":"job"%> <%=theJob.getJobName()%> on domain '<%=currDomain%>'</b>
		<%@include file="/include/jobrefinementnav.jsp"%>
	<p>
	
	<form name="frmConfig" method="post" action="configure.jsp">
		<input type="hidden" name="update" value="true">		
		<input type="hidden" name="action" value="done">
		<input type="hidden" name="where" value="">
        <input type="hidden" name="expert" value="<%=expert%>">
		<input type="hidden" name="currDomain" value="<%=currDomain%>">
		<input type="hidden" name="job" value="<%=theJob.getUID()%>">
        <input type="hidden" name="reference" value="<%=reference%>">
	
		<p>	
			<b>Instructions:</b> To refine a setting, check the box in front of it and input new settings.<br>
			Unchecked settings are inherited settings. Changes to settings that do not have a<br>
			checked box will be discarded. Settings that can not be overridden will not have a<br>
			checkbox and will be displayed in a read only manner.
		<p>		
	        <% if(expert){ %>
	            <a href="javascript:setExpert('false')">Hide expert settings</a>
	        <% } else { %>
	            <a href="javascript:setExpert('true')">View expert settings</a>
	        <% } %>
	    <p>
		<table>
			<%=inputForm%>
		</table>
	</form>
	<p>
		<%@include file="/include/jobrefinementnav.jsp"%>
		
<%@include file="/include/foot.jsp"%>
