<%
  /**
   * This pages allows the user to select what filters
   * are applied to what modules in the crawl order.
   *
   * @author Kristinn Sigurdsson
   */
%>
<%@include file="/include/handler.jsp"%>
<%@include file="/include/secure.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.datamodel.CredentialStore" %>
<%@ page import="org.archive.crawler.datamodel.settings.*" %>
<%@ page import="org.archive.crawler.framework.CrawlController" %>
<%@ page import="org.archive.crawler.framework.Filter" %>
<%@ page import="org.archive.util.TextUtils" %>

<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.management.MBeanInfo"%>
<%@ page import="javax.management.Attribute"%>
<%@ page import="javax.management.MBeanAttributeInfo"%>
<%@ page import="javax.management.AttributeNotFoundException"%>
<%@ page import="javax.management.MBeanException"%>
<%@ page import="javax.management.ReflectionException"%>

<%!
    /**
     * Builds the HTML to edit a map of modules
     *
     * @param map The map to edit
     * @param availibleOptions List of availible modules that can be added to the map
     *                         (full class names as Strings)
     * @param name A short name for the map (only alphanumeric chars.)
     *
     * @return the HTML to edit the specified modules map
     */
    public String buildModuleMap(ComplexType map, List availibleOptions, CrawlerSettings domain){
        StringBuffer ret = new StringBuffer();
        
        ret.append("<table cellspacing='0' cellpadding='2'>");
        
        MBeanInfo mapInfo = map.getMBeanInfo(domain);
        MBeanAttributeInfo m[] = mapInfo.getAttributes();
            
        // Printout modules in map.
        boolean alt = true;
        for(int n=0; n<m.length; n++) {
            ModuleAttributeInfo att = (ModuleAttributeInfo)m[n]; //The attributes of the current attribute.
            Object currentAttribute = null;
            Object localAttribute = null;

            try {
                currentAttribute = map.getAttribute(domain,att.getName());
                localAttribute = map.getLocalAttribute(domain,att.getName());
            } catch (Exception e1) {
                ret.append(e1.toString() + " " + e1.getMessage());
            }
    
            ret.append("<tr");
            if(alt){
                ret.append(" bgcolor='#EEEEFF'");
            }
            ret.append(">");
            
            if(localAttribute == null){
                // Inherited. Print for display only
                ret.append("<td><i>" + att.getName() + "</i></td><td><i>&nbsp;"+att.getType()+"</i></td>");
                ret.append("<td></td>");
                ret.append("<td><a href=\"javascript:alert('");
                ret.append(TextUtils.escapeForJavascript(att.getDescription()));
                ret.append("')\">Info</a></td>\n");
                ret.append("</tr>");
            } else {
	            ret.append("<td>" + att.getName() + "</td><td>&nbsp;"+att.getType()+"</td>");
	            ret.append("<td><a href=\"javascript:doRemove('"+att.getName()+"')\">Remove</a></td>");
	            ret.append("<td><a href=\"javascript:alert('");
	            ret.append(TextUtils.escapeForJavascript(att.getDescription()));
	            ret.append("')\">Info</a></td>\n");
	            ret.append("</tr>");
	        }
            alt = !alt;
        }
        
        // Find out which aren't being used.
        if(availibleOptions.size() > 0 ){
            ret.append("<tr><td>");
            ret.append("<input name='name'>");
            ret.append("</td><td>");
            ret.append("<select name='cboAdd'>");
            for(int i=0 ; i<availibleOptions.size() ; i++){
                String curr = ((Class)availibleOptions.get(i)).getName();
                ret.append("<option value='"+curr+"'>"+curr+"</option>");
            }
            ret.append("</select>");
            ret.append("</td><td>");
            ret.append("<input type='button' value='Add' onClick=\"doAdd()\">");
            ret.append("</td></tr>");
        }
        ret.append("</table>");
        return ret.toString();
    }

%>

<%
    // Load the job to manipulate   
    CrawlJob theJob = handler.getJob(request.getParameter("job"));
    // Load display level
    String currDomain = request.getParameter("currDomain");
    
    if(theJob == null)
    {
        // Didn't find any job with the given UID or no UID given.
        response.sendRedirect("/admin/jobs.jsp?message=No job selected");
        return;
    } else if(theJob.isReadOnly()){
        // Can't edit this job.
        response.sendRedirect("/admin/jobs.jsp?message=Can't edit modules on a read only job");
        return;
    }

    XMLSettingsHandler settingsHandler = (XMLSettingsHandler)theJob.getSettingsHandler();
    CrawlOrder crawlOrder = settingsHandler.getOrder();
    CrawlerSettings orderfile = settingsHandler.getSettingsObject(currDomain);
    ComplexType credstore = (ComplexType)crawlOrder.getAttribute(orderfile,CredentialStore.ATTR_NAME);
    MapType credmap = (MapType)credstore.getAttribute(orderfile,CredentialStore.ATTR_CREDENTIALS);

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
            response.sendRedirect("/admin/jobs/per/overview.jsp?job="+theJob.getUID()+"&currDomain="+currDomain+"&message=Override changes saved");
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


