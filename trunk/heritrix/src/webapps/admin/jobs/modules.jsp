<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.admin.ui.JobConfigureUtils" %>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.settings.*" %>
<%@ page import="org.archive.crawler.framework.CrawlController" %>
<%@ page import="org.archive.crawler.framework.Frontier" %>
<%@ page import="org.archive.crawler.framework.CrawlScope" %>
<%@ page import="org.archive.crawler.framework.Processor" %>
<%@ page import="org.archive.crawler.framework.StatisticsTracking" %>
<%@ page import="org.archive.util.TextUtils" %>

<%@ page import="java.io.*,java.lang.Boolean,java.util.ArrayList" %>
<%@ page import="javax.management.MBeanInfo, javax.management.Attribute, javax.management.MBeanAttributeInfo,javax.management.AttributeNotFoundException, javax.management.MBeanException,javax.management.ReflectionException"%>

<%
    // Get the default settings.
    
    CrawlJob theJob = handler.getJob(request.getParameter("job"));
    
    
    if(theJob == null)
    {
        // Didn't find any job with the given UID or no UID given.
        response.sendRedirect(request.getContextPath() +
            "/jobs.jsp?message=No job selected");
        return;
    } else if(theJob.isReadOnly() || theJob.isRunning()){
        // Can't edit this job.
        response.sendRedirect(request.getContextPath() +
            "/jobs.jsp?message=Can't edit modules on a running or read only" +
            " job");
        return;
    }

    XMLSettingsHandler settingsHandler = (XMLSettingsHandler)theJob.getSettingsHandler();

    if(request.getParameter("action") != null){
        // Need to take some action.
        String action = request.getParameter("action");
        if(action.equals("done")){
            // Ok, done editing.
            if(theJob.isNew()){            
                handler.addJob(theJob);
                response.sendRedirect(request.getContextPath() +
                    "/jobs.jsp?message=Job created");
            }else{
                if(theJob.isRunning()){
                    handler.kickUpdate();
                }
                if(theJob.isProfile()){
                    response.sendRedirect(request.getContextPath() +
                        "/profiles.jsp?message=Profile modified");
                }else{
                    response.sendRedirect(request.getContextPath() +
                        "/jobs.jsp?message=Job modified");
                }
            }
            return;
        }else if(action.equals("goto")){
            // Goto another page of the job/profile settings
            response.sendRedirect(request.getParameter("subaction"));
            return;
        }else if(action.equals("module")){
            // Setting a module
            String item = request.getParameter("item");
            String className = request.getParameter("cbo"+item);
                        
            ModuleType tmp = null;
            if(item.equals("Frontier")){
                // Changing URI frontier
                tmp = SettingsHandler.instantiateModuleTypeFromClassName("frontier",className);
            } else if(item.equals("Scope")){
                // Changing Scope
                tmp = SettingsHandler.instantiateModuleTypeFromClassName(org.archive.crawler.framework.CrawlScope.ATTR_NAME,className);
            } 
            if(tmp != null){
                // If tmp is null then something went wrong but we'll ignore it.
                settingsHandler.getOrder().setAttribute(tmp);
                // Write changes
                settingsHandler.writeSettingsObject(settingsHandler.getSettings(null));
            }
        }else if(action.equals("map")){
            //Doing something with a map
            String subaction = request.getParameter("subaction");
            String item = request.getParameter("item");
            if(subaction != null && item != null){
                // Do common stuff
                String subitem = request.getParameter("subitem");
                MapType map = null;
                if(item.equals("PreFetchProcessors")){
                    // Editing processors map
                    map = ((MapType)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_PRE_FETCH_PROCESSORS));
                } else if(item.equals("Fetchers")){
                    // Editing processors map
                    map = ((MapType)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_FETCH_PROCESSORS));
                } else if(item.equals("Extractors")){
                    // Editing processors map
                    map = ((MapType)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_EXTRACT_PROCESSORS));
                } else if(item.equals("Writers")){
                    // Editing processors map
                    map = ((MapType)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_WRITE_PROCESSORS));
                } else if(item.equals("Postprocessors")){
                    // Editing processors map
                    map = ((MapType)settingsHandler.getOrder().getAttribute(CrawlOrder.ATTR_POST_PROCESSORS));
                } else if(item.equals("StatisticsTracking")){
                    // Editing Statistics Tracking map
                    map = ((MapType)settingsHandler.getOrder().getAttribute("loggers"));
                }
                if(map != null){
                    // Figure out what to do
                    if(subaction.equals("up")){
                        // Move selected processor up
                        map.moveElementUp(settingsHandler.getSettings(null),subitem);
                    }else if(subaction.equals("down")){
                        // Move selected processor down            
                        map.moveElementDown(settingsHandler.getSettings(null),subitem);
                    }else if(subaction.equals("remove")){
                        // Remove selected processor
                        map.removeElement(settingsHandler.getSettings(null),subitem);
                    }else if(subaction.equals("add")){
                        String className = request.getParameter("cboAdd"+item);
                        String typeName = className.substring(className.indexOf("|")+1);
                        className = className.substring(0,className.indexOf("|"));
    
                        map.addElement(settingsHandler.getSettings(null),
                                         SettingsHandler.instantiateModuleTypeFromClassName(typeName,className));
                    }
                    // Write changes
                    settingsHandler.writeSettingsObject(settingsHandler.getSettings(null));
                }
            }
        }        
    }

    String title = "Adjust modules";
    int tab = theJob.isProfile()?2:1;
    int jobtab = 0;
%>

<%@include file="/include/head.jsp"%>
<script type="text/javascript">
    function doSubmit(){
        document.frmModules.submit();
    }
    
    function doGoto(where){
        document.frmModules.action.value="goto";
        document.frmModules.subaction.value = where;
        doSubmit();
    }
    
    function doSetModule(name){
        document.frmModules.action.value="module";
        document.frmModules.item.value=name;
        doSubmit();
    }
    
    function doMoveMapItemUp(name, item){
        document.frmModules.action.value="map";
        document.frmModules.subaction.value="up";
        document.frmModules.item.value=name;
        document.frmModules.subitem.value=item;
        doSubmit();
    }

    function doMoveMapItemDown(name, item){
        document.frmModules.action.value="map";
        document.frmModules.subaction.value="down";
        document.frmModules.item.value=name;
        document.frmModules.subitem.value=item;
        doSubmit();
    }
    
    function doRemoveMapItem(name, item){
        document.frmModules.action.value="map";
        document.frmModules.subaction.value="remove";
        document.frmModules.item.value=name;
        document.frmModules.subitem.value=item;
        doSubmit();
    }
    
    function doAddMapItem(name){
        document.frmModules.action.value="map";
        document.frmModules.subaction.value="add";
        document.frmModules.item.value=name;
        doSubmit();
    }
</script>

    <p>
        <%@include file="/include/jobnav.jsp"%>
    <form name="frmModules" method="post" action="modules.jsp">
        <input type="hidden" name="job" value="<%=theJob.getUID()%>">
        <input type="hidden" name="action" value="done">
        <input type="hidden" name="subaction" value="done">
        <input type="hidden" name="item" value="">
        <input type="hidden" name="subitem" value="">
        
        <p>
            <b>Select Crawl Scope</b>
        <p>
            <%=JobConfigureUtils.buildModuleSetter(
                settingsHandler.getOrder().getAttributeInfo("scope"),
                CrawlScope.class,
                "Scope",
                ((ComplexType)settingsHandler.getOrder().getAttribute("scope")).getMBeanInfo().getDescription())%>
                
        <p>
            <b>Select URI Frontier</b>
        <p>
            <%=JobConfigureUtils.buildModuleSetter(
                settingsHandler.getOrder().getAttributeInfo("frontier"),
                Frontier.class,
                "Frontier",
                ((ComplexType)settingsHandler.getOrder().getAttribute("frontier")).getMBeanInfo().getDescription())%>

    
        <p>
            <b>Select Pre Processors</b> 
            <i>Processors that should run before any fetching</i>
        <p>
            <%=JobConfigureUtils.buildModuleMap(
                (ComplexType)settingsHandler.getOrder().getAttribute(
                    CrawlOrder.ATTR_PRE_FETCH_PROCESSORS),
                    Processor.class,
                    "PreFetchProcessors")%>
        <p>
            <b>Select Fetchers</b> 
            <i>Processors that fetch documents using various protocols</i>
        <p>
            <%=JobConfigureUtils.buildModuleMap(
                (ComplexType)settingsHandler.getOrder().getAttribute(
                    CrawlOrder.ATTR_FETCH_PROCESSORS),
                    Processor.class,
                    "Fetchers")%>
        <p>
            <b>Select Extractors</b> 
            <i>Processors that extracts links from URIs</i>
        <p>
            <%=JobConfigureUtils.buildModuleMap(
                (ComplexType)settingsHandler.getOrder().getAttribute(
                    CrawlOrder.ATTR_EXTRACT_PROCESSORS),
                    Processor.class,
                    "Extractors")%>
        <p>
            <b>Select Writers</b> 
            <i>Processors that write documents to archive files</i>
        <p>
            <%=JobConfigureUtils.buildModuleMap(
                (ComplexType)settingsHandler.getOrder().getAttribute(
                    CrawlOrder.ATTR_WRITE_PROCESSORS),
                    Processor.class,
                    "Writers")%>
        <p>
            <b>Select Post Processors</b> 
            <i>Processors that do cleanup and feed the Frontier with new URIs</i>
        <p>
            <%=JobConfigureUtils.buildModuleMap(
                (ComplexType)settingsHandler.getOrder().getAttribute(
                    CrawlOrder.ATTR_POST_PROCESSORS),
                    Processor.class,
                    "Postprocessors")
            %>
        <p>
            <b>Select Statistics Tracking</b>
        <p>
            <%=JobConfigureUtils.buildModuleMap(
                (ComplexType)settingsHandler.getOrder().getAttribute("loggers"),
                StatisticsTracking.class,
                "StatisticsTracking")
            %>
       </form>
    <p>
        <%@include file="/include/jobnav.jsp"%>
<%@include file="/include/foot.jsp"%>


