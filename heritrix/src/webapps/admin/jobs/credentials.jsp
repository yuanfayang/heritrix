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
<%@ page import="org.archive.crawler.settings.*" %>
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
     * Builds the HTML to edit a map of Credentials
     *
     * @param map The map to edit
     * @param availibleOptions List of availible modules that can be added to the map
     *                         (full class names as Strings)
     *
     * @return the HTML to edit the specified modules map
     */
    public String buildModuleMap(ComplexType map, List availibleOptions){
        StringBuffer ret = new StringBuffer();
        
        ret.append("<table cellspacing='0' cellpadding='2'>");
        
        MBeanInfo mapInfo = map.getMBeanInfo();
        MBeanAttributeInfo m[] = mapInfo.getAttributes();
            
        // Printout modules in map.
        boolean alt = true;
        for(int n=0; n<m.length; n++) {
            Object currentAttribute = null;
            ModuleAttributeInfo att = (ModuleAttributeInfo)m[n]; //The attributes of the current attribute.
    
            ret.append("<tr");
            if(alt){
                ret.append(" bgcolor='#EEEEFF'");
            }
            ret.append("><td>" + att.getName() + "</td><td>&nbsp;"+att.getType()+"</td>");
            ret.append("<td><a href=\"javascript:doRemove('"+att.getName()+"')\">Remove</a></td>");
            ret.append("<td><a href=\"javascript:alert('");
            ret.append(TextUtils.escapeForJavascript(att.getDescription()));
            ret.append("')\">Info</a></td>\n");
            ret.append("</tr>");
            alt = !alt;
        }
        
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
    ComplexType credstore = (ComplexType)settingsHandler.getOrder().getAttribute(CredentialStore.ATTR_NAME);
    MapType credmap = (MapType)credstore.getAttribute(CredentialStore.ATTR_CREDENTIALS);

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
                    credmap.addElement(settingsHandler.getSettings(null),
                                         SettingsHandler.instantiateModuleTypeFromClassName(typeName,className));
                }
            } else if(subaction.equals("remove")){
                // Remove a filter from a map
                if(credential != null && credential.length() > 0){
                    credmap.removeElement(settingsHandler.getSettings(null),credential);
                }
            }
            // Finally save the changes to disk
            settingsHandler.writeSettingsObject(settingsHandler.getSettings(null));
        }else if(action.equals("done")){
            // Ok, done editing.
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
            response.sendRedirect(request.getParameter("subaction"));
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
        <%@include file="/include/jobnav.jsp"%>
    <p>
    <form name="frmCredentials" method="post" action="credentials.jsp">
        <input type="hidden" name="job" value="<%=theJob.getUID()%>">
        <input type="hidden" name="action" value="done">
        <input type="hidden" name="subaction" value="">
        <input type="hidden" name="credential" value="">
        <p>
            <b>Add / Remove credentials</b>
        <p>
            <%
                List list = CredentialStore.getCredentialTypes();
            %>
            <table>
            <%=buildModuleMap(credmap, list)%>
            </table>
    </form>
    <p>
        <%@include file="/include/jobnav.jsp"%>
<%@include file="/include/foot.jsp"%>


