<%
  /**
   * This pages allows the user to add filters to overrides.
   *
   * @author Kristinn Sigurdsson
   */
%>
<%@include file="/include/handler.jsp"%>

<%@page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@page import="org.archive.crawler.framework.Filter" %>
<%@page import="org.archive.crawler.admin.ui.JobConfigureUtils" %>
<%@page import="org.archive.crawler.settings.*"%>
<%@page import="org.archive.crawler.settings.refinements.*"%>

<%
    // Load display level
    String reference = request.getParameter("reference");
    String currDomain = request.getParameter("currDomain");
    // Load the job to manipulate   
    CrawlJob theJob = JobConfigureUtils.checkCrawlJob(
        handler.getJob(request.getParameter("job")), response,
        request.getContextPath() + "/jobs.jsp", currDomain);
    if (theJob == null) {
        return;
    }

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

    CrawlerSettings settings = refinement.getSettings();

    // See if we need to take any action
    if(request.getParameter("action") != null){
        // Need to take some action.
        String action = request.getParameter("action");
        if(action.equals("filters")){
            //Doing something with the filters.
            String subaction = request.getParameter("subaction");
            String map = request.getParameter("map");
            if(map != null && map.length() > 0){
                String filter = request.getParameter("filter");
                MapType filterMap = (MapType)settingsHandler.getComplexTypeByAbsoluteName(settings,map);
                if(subaction.equals("add")){
                    //Add filter
                    String className = request.getParameter(map+".class");
                    String typeName = request.getParameter(map+".name");
                    if(typeName != null && typeName.length() > 0 
                       && className != null && className.length() > 0 ){
                        ModuleType tmp = SettingsHandler.instantiateModuleTypeFromClassName(typeName,className);
                        filterMap.addElement(settings,tmp);
                    }
                } else if(subaction.equals("moveup")){
                    // Move a filter down in a map
                    if(filter != null && filter.length() > 0){
                        filterMap.moveElementUp(settings,filter);
                    }
                } else if(subaction.equals("movedown")){
                    // Move a filter up in a map
                    if(filter != null && filter.length() > 0){
                        filterMap.moveElementDown(settings,filter);
                    }
                } else if(subaction.equals("remove")){
                    // Remove a filter from a map
                    if(filter != null && filter.length() > 0){
                        filterMap.removeElement(settings,filter);
                    }
                }
            }
            // Finally save the changes to disk
            settingsHandler.writeSettingsObject(settings);
        }else if(action.equals("done")){
            // Ok, done editing. Back to overview.
            if(theJob.isRunning()){
                handler.kickUpdate(); //Just to make sure.
            }
            response.sendRedirect(request.getContextPath () +
                "/jobs/refinements/overview.jsp?job=" + theJob.getUID() +
                "&currDomain=" + currDomain +
                "&message=Override changes saved");
            return;
        }else if(action.equals("goto")){
            // Goto another page of the job/profile settings
            response.sendRedirect(request.getParameter("subaction")+"&currDomain="+currDomain+"&reference="+reference);
            return;
        }
    }

    // Set page header.
    String title = "Add override filters";
    int tab = theJob.isProfile()?2:1;
    int jobtab = 1;
%>

<%@include file="/include/head.jsp"%>
<%@include file="/include/filters_js.jsp"%>
    <p>
        <b>Refinement '<%=refinement.getReference()%>' on '<%=global?"global settings":currDomain%>' of
        <%=theJob.isProfile()?"profile":"job"%>
        <%=theJob.getJobName()%>:</b>
        <%@include file="/include/jobrefinementnav.jsp"%>
    <p>
    <form name="frmFilters" method="post" action="filters.jsp">
        <input type="hidden" name="currDomain" value="<%=currDomain%>">
        <input type="hidden" name="job" value="<%=theJob.getUID()%>">
        <input type="hidden" name="action" value="done">
        <input type="hidden" name="subaction" value="">
        <input type="hidden" name="map" value="">
        <input type="hidden" name="filter" value="">
        <input type="hidden" name="reference" value="<%=reference%>">
        <p>
            <b>Instructions:</b> It is possible to add filters to overrides and manipulate existing<br>
            refinement filters. It is not possible to remove filters defined in a super domain!
        <p>
        <table>
            <%=JobConfigureUtils.printOfType(
            	crawlOrder,
            	"",
            	false,false,false,null,false,
            	Filter.class, true)%>
        </table>
    </form>
    <p>
        <%@include file="/include/jobrefinementnav.jsp"%>
<%@include file="/include/foot.jsp"%>


