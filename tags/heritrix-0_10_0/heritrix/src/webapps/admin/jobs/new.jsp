<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.settings.ComplexType" %>
<%@ page import="org.archive.crawler.settings.CrawlerSettings" %>
<%@ page import="org.archive.crawler.settings.XMLSettingsHandler" %>

<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.FileReader" %>
<%@ page import="java.util.regex.Pattern" %>

<%
	/**
	 * Create a new job
	 */
	CrawlJob theJob = handler.getJob(request.getParameter("job"));
	boolean isProfile = false;
	if(request.getParameter("profile") != null && request.getParameter("profile").equals("true")){
		isProfile = true;
	}
	
	if(theJob == null)
	{
		//Ok, use default profile then.
		theJob = handler.getDefaultProfile();
		if(theJob == null){
			// ERROR - This should never happen. There must always be at least one (default) profile.
			out.println("ERROR: NO PROFILE FOUND");
			return;
		}
	} 

	XMLSettingsHandler settingsHandler = theJob.getSettingsHandler();
	CrawlOrder crawlOrder = settingsHandler.getOrder();
    CrawlerSettings orderfile = settingsHandler.getSettingsObject(null);
    
    String error = null;
    String metaName = request.getParameter("meta/name");
    
    if(request.getParameter("action") != null){
    	//Make new job.
    	CrawlJob newJob = null;

    	// Ensure we got a valid name. ([a-zA-Z][0-9][-_])
    	Pattern p = Pattern.compile("[a-zA-Z_\\-0-9]*");
    	if(p.matcher(metaName).matches()==false){
            // Illegal name!
            error = "Name can only contain alphanumeric chars, dash and underscore.<br>No spaces are allowed";
    	}
    	
    	if(error == null){
	    	if(isProfile){
	    		// Ensure unique name
	    		CrawlJob test = handler.getJob(metaName);
	    		if(test == null){
	                // unique name
	                newJob = handler.newProfile(theJob,metaName,request.getParameter("meta/description"),request.getParameter("seeds"));
	            } else {
	                // Need a unique name!
	                error = "Profile name must be unique!";
	            }
	    	}else{
	    		newJob = handler.newJob(theJob,metaName,request.getParameter("meta/description"),request.getParameter("seeds"),CrawlJob.PRIORITY_AVERAGE);
	    	}
    	}
    	
    	if(error == null && newJob != null){
	    	if(request.getParameter("action").equals("configure")){
	    		response.sendRedirect("/admin/jobs/configure.jsp?job="+newJob.getUID());
	    	} else if(request.getParameter("action").equals("modules")){
	    		response.sendRedirect("/admin/jobs/modules.jsp?job="+newJob.getUID());
	    	} else if(request.getParameter("action").equals("filters")){
	    		response.sendRedirect("/admin/jobs/filters.jsp?job="+newJob.getUID());
	    	} else if(request.getParameter("action").equals("override")){
	    		response.sendRedirect("/admin/jobs/per/overview.jsp?job="+newJob.getUID());
	    	} else {
	    		handler.addJob(newJob);
	    		response.sendRedirect("/admin/jobs.jsp?message=Job created");
	    	}
	    	return;
        }
    }
	
	String title = isProfile?"New profile":"New crawl job";
	int tab = isProfile?2:1;
	// TODO: Offer setting of priority.
%>

<%@include file="/include/head.jsp"%>

		<form name="frmNew" method="post" action="new.jsp">
			<input type="hidden" name="action" value="new">
			<input type="hidden" name="profile" value="<%=isProfile%>">
			<input type="hidden" name="job" value="<%=theJob.getUID()%>">
			<b>
				Create new 
			<% 	if(isProfile){ %>
				profile
			<%	} else { %>
				crawl job 
			<%	}	%>
				based on
			<% 	if(request.getParameter("job")==null){%>
				default profile
			<% 
				}else{ 
					if(theJob.isProfile()){
						out.println("profile ");					
					} else {
						out.println("job ");
					}
					out.println("'"+theJob.getJobName()+"'"); 
				}
			%>	
			</b>
			<p>			
			<table>
				<tr>
					<td>
						Name of new job:
					</td>
					<td>
						<input maxlength="38" name="meta/name" value="<%=error==null?orderfile.getName():metaName%>" style="width: 320px">
					</td>
				</tr>
				<% if(error != null){ %>
				    <tr>
				        <td>
				        </td>
				        <td>
				            <font color="red"><%=error%></font>
				        </td>
				    </tr>
				<% } %>
				<tr>
					<td>
						Description:
					</td>
					<td>
						<input name="meta/description" value="<%=error==null?orderfile.getDescription():request.getParameter("meta/description")%>" style="width: 320px">
					</td>
				</tr>
				<tr>
					<td valign="top">
						Seeds:
					</td>
					<td>
						<textarea name="seeds" style="width: 320px" rows="8"><%
							if(error==null){
								BufferedReader seeds = new BufferedReader(new FileReader(settingsHandler.getPathRelativeToWorkingDirectory((String)((ComplexType)settingsHandler.getOrder().getAttribute("scope")).getAttribute("seedsfile"))));
								String sout = seeds.readLine();
								while(sout!=null){
									out.println(sout);
									sout = seeds.readLine();
								}
                            } else {
                                out.println(request.getParameter("seeds"));
                            }
						%></textarea>
					</td>
				</tr>
			</table>
			<input type="button" value="Modules" onClick="document.frmNew.action.value='modules';document.frmNew.submit()">
			<input type="button" value="Filters" onClick="document.frmNew.action.value='filters';document.frmNew.submit()">
			<input type="button" value="Settings" onClick="document.frmNew.action.value='configure';document.frmNew.submit()">
			<input type="button" value="Overrides" onClick="document.frmNew.action.value='override';document.frmNew.submit()">
			<% if(isProfile == false){ %>
				<input type="submit" value="Submit job">
			<% } %>
		</form>
		
<%@include file="/include/foot.jsp"%>
