<%
  /**
   * This pages allows the user to select what filters
   * are applied to what modules in the crawl order.
   *
   * @author Kristinn Sigurdsson
   */
%>
<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.datamodel.CredentialStore" %>

<%@include file="/include/jobcredentials.jsp"%>

<%
    // Load the job to manipulate   
    CrawlJob theJob = handler.getJob(request.getParameter("job"));
    // Load display level
    String currDomain = request.getParameter("currDomain");
    if(theJob == null) {
        // Didn't find any job with the given UID or no UID given.
        response.sendRedirect(request.getContextPath() +
            "/jobs.jsp?message=No job selected");
        return;
    } else if(theJob.isReadOnly()) {
        // Can't edit this job.
        response.sendRedirect(request.getContextPath() +
            "/jobs.jsp?message=Can't edit modules on a read only job");
        return;
    }

    XMLSettingsHandler settingsHandler =
        (XMLSettingsHandler)theJob.getSettingsHandler();
    CrawlOrder crawlOrder = settingsHandler.getOrder();
    CrawlerSettings orderfile = settingsHandler.getSettingsObject(currDomain);
    ComplexType credstore = (ComplexType)crawlOrder.
        getAttribute(orderfile,CredentialStore.ATTR_NAME);
    MapType credmap = (MapType)credstore.
        getAttribute(orderfile,CredentialStore.ATTR_CREDENTIALS);

    // See if we need to take any action
    if(request.getParameter("action") != null){
        // Need to take some action.
        String action = request.getParameter("action");
        if(action.equals("credentials")){
            //Doing something with the filters.
            String subaction = request.getParameter("subaction");
            String credential = request.getParameter("credential");
            if(subaction.equals("add")){
                //Add filter
                String className = request.getParameter("cboAdd");
                String typeName = request.getParameter("name");
                if(typeName != null && typeName.length() > 0 
                   && className != null && className.length() > 0 ){
                    credmap.addElement(orderfile,
                            SettingsHandler.instantiateModuleTypeFromClassName(typeName,className));
                }
            } else if(subaction.equals("remove")){
                // Remove a filter from a map
                if(credential != null && credential.length() > 0){
                    credmap.removeElement(orderfile,credential);
                }
            }
            // Finally save the changes to disk
            settingsHandler.writeSettingsObject(orderfile);
        }else if(action.equals("done")){
            // Ok, done editing.
            if(theJob.isRunning()){
                handler.kickUpdate();
            }
            response.sendRedirect(request.getContextPath() +
                "/jobs/per/overview.jsp?job=" + theJob.getUID() +
                "&currDomain=" + currDomain +
                "&message=Override changes saved");
            return;
        }else if(action.equals("goto")){
            // Goto another page of the job/profile settings
            response.sendRedirect(request.getParameter("subaction")+"&currDomain="+currDomain);
            return;
        }
    }

    // Set page header.
    String title = "Credentials";
    int tab = theJob.isProfile()?2:1;
    int jobtab = 4;
%>

<%@include file="/include/head.jsp"%>
<script type="text/javascript">
    function doSubmit(){
        document.frmCredentials.submit();
    }
    
    function doGoto(where){
        document.frmCredentials.action.value="goto";
        document.frmCredentials.subaction.value = where;
        doSubmit();
    }
    
    function doRemove(credential){
        document.frmCredentials.action.value = "credentials";
        document.frmCredentials.subaction.value = "remove";
        document.frmCredentials.credential.value = credential;
        doSubmit();
    }

    function doAdd(){
        if(document.frmCredentials.name.value == ""){
            alert("Must enter a unique name for the Credential");
        } else {
            document.frmCredentials.action.value = "credentials";
            document.frmCredentials.subaction.value = "add";
            doSubmit();
        }
    }
</script>
    <p>
        <b>Override for the <%=theJob.isProfile()?"profile":"job"%> <%=theJob.getJobName()%> on domain '<%=currDomain%>'</b>
        <%@include file="/include/jobpernav.jsp"%>
    <p>
    <form name="frmCredentials" method="post" action="credentials.jsp">
        <input type="hidden" name="job" value="<%=theJob.getUID()%>">
        <input type="hidden" name="action" value="done">
        <input type="hidden" name="subaction" value="">
        <input type="hidden" name="credential" value="">
        <input type="hidden" name="currDomain" value="<%=currDomain%>">
        <p>
            <b>Add / Remove credentials</b>
        <p>
            <%
                List list = CredentialStore.getCredentialTypes();
            %>
            <table>
            <%=buildModuleMap(credmap, list, orderfile)%>
            </table>
    </form>
    <p>
        <%@include file="/include/jobpernav.jsp"%>
<%@include file="/include/foot.jsp"%>

