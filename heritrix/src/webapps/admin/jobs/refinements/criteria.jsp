<%
  /**
   * Add or remove criteria
   *
   * @author Kristinn Sigurdsson
   */
%>
<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%@page import="java.util.ListIterator"%>
<%@page import="java.util.regex.Pattern" %>

<%@page import="org.archive.crawler.datamodel.settings.CrawlerSettings"%>
<%@page import="org.archive.crawler.datamodel.settings.XMLSettingsHandler"%>
<%@page import="org.archive.crawler.datamodel.settings.refinements.*"%>

<%
    String message = request.getParameter("message");
    int deleteCriteriaNumber = -1;
    // Load the job.
    CrawlJob theJob = handler.getJob(request.getParameter("job"));
    
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
    boolean global = currDomain == null || currDomain.length() == 0;

    CrawlerSettings localSettings;
    
    if(global){
        localSettings = settingsHandler.getSettingsObject(null);
    } else {
        localSettings = settingsHandler.getSettingsObject(currDomain);
    }
    
    Refinement refinement = localSettings.getRefinement(reference);
    
    // Check for actions
    String action = request.getParameter("action");
    if(action != null){
        String item = request.getParameter("item");
        // Need to do something!
        if(action.equals("done")){
            // Ok, done editing.
            if(theJob.isRunning()){
                handler.kickUpdate();
            }
            response.sendRedirect("/admin/jobs/refinements/overview.jsp?job="+theJob.getUID()+"&currDomain="+currDomain+"&message=Refinement changes saved");
            return;
        } else if(action.equals("add")){
            // Add new criteria
            Criteria newCrit = null;
            if(item.equals("timeOfDay")) {
                // Add new time of day criteria
                String from = request.getParameter("todFrom");
                String to = request.getParameter("todTo");
                try{
                    newCrit = new TimespanCriteria(from,to);
                } catch( java.text.ParseException e){
                    message = e.getMessage();
                }
            } else if(item.equals("regExpr")) {
                // Add new regular expr. criteria
                newCrit = new RegularExpressionCriteria(request.getParameter("regexpr"));
            } else if(item.equals("port")) {
                // Add new port critera
                newCrit = new PortnumberCriteria(request.getParameter("port"));
            }
            if(newCrit != null){
                refinement.addCriteria(newCrit);
            }
        } else if(action.equals("delete")){
            // Delete criteria
            try{
                deleteCriteriaNumber = Integer.parseInt(item);
            } catch(NumberFormatException e){
                message = "Invalid criteria number!!!"; // This should normally not happen.
            }
        } else if(action.equals("goto")){
            // Goto another page of the job/profile settings
            response.sendRedirect(item+"&currDomain="+currDomain+"&reference="+reference);
            return;
        }
    }
    
    String title = "Refinements";
    int tab = theJob.isProfile()?2:1;
    int jobtab = 5;
%>
<%@include file="/include/head.jsp"%>
    <script type="text/javascript">
        function doSubmit(){
            document.frmCriteria.submit();
        }
        
        function doGoto(where){
            document.frmCriteria.action.value="goto";
            document.frmCriteria.item.value = where;
            doSubmit();
        }
        
        function doDelete(criteria){
            document.frmCriteria.action.value="delete";
            document.frmCriteria.item.value = criteria;
            doSubmit();
        }
        
        function doAdd(type){
            document.frmCriteria.action.value="add";
            document.frmCriteria.item.value = type;
            doSubmit();
        }
    </script>   
    <% if(message != null && message.length() > 0){ %>
        <p>
            <font color="red"><b><%=message%></b></font>
    <% } %>
    <p>
        <b>Refinement '<%=refinement.getReference()%>' on '<%=global?"global settings":currDomain%>' of
        <%=theJob.isProfile()?"profile":"job"%>
        <%=theJob.getJobName()%>:</b>
        <%@include file="/include/jobrefinementnav.jsp"%>
    <p>
    <form name="frmCriteria" method="post" action="criteria.jsp">
        <input type="hidden" name="action" value="done">
        <input type="hidden" name="item" value="">
        <input type="hidden" name="job" value="<%=theJob.getUID()%>">
        <input type="hidden" name="currDomain" value="<%=currDomain%>">
        <input type="hidden" name="reference" value="<%=reference%>">
        <table border="0" cellpadding="1" cellspacing="0" width="450">
            <tr>
                <td colspan="2" style="background-color: #0000FF; color: #FFFFFF">
                    &nbsp;<b>Existing criteria</b>
                </td>
            </tr>
            <%
                ListIterator criteria = refinement.criteriaIterator();
                boolean alt = true;
                int i = 0;
                while(criteria.hasNext()){
                    Criteria cr = (Criteria)criteria.next();
                    if(i==deleteCriteriaNumber){
                        // Need to delete this criteria
                        criteria.remove();
                        deleteCriteriaNumber=-1;
                    } else {
            %>
	                    <tr <%=alt?"bgcolor='#EEEEFF'":""%>>
	                        <td width="400">
	                            <b><%=cr.getName()%></b>&nbsp;
	                        </td>
	                        <td nowrap>
	                            <a href="javascript:doDelete('<%=i++%>')" style="color: #003399;" class="underLineOnHover">Remove</a>&nbsp;
	                        </td>
	                    </tr>
	                    <tr <%=alt?"bgcolor='#EEEEFF'":""%>>
	                        <td>
	                            <table border="0" cellpadding="0" cellspacing="0" width="100%">
	                                <tr>
	                                    <td>&nbsp;&nbsp;&nbsp;</td>
	                                    <td width="100%">
	                                        <i><%=cr.getDescription()%></i>
	                                    </td>
	                                </tr>
	                            </table>
	                        </td>
	                        <td>&nbsp;</td>
	                    </tr>
            <%
                        alt = !alt;
                    }
                }
            %>
        </table>
        
        <p>
          
        <table border="0" cellpadding="1" cellspacing="0" width="450">
            <tr>
                <td colspan="6" style="background-color: #0000FF; color: #FFFFFF">
                    &nbsp;<b>Add Criteria</b>
                </td>
            </tr>
            <tr>
                <td>
                    <b>Port number</b>:
                </td>
                <td>
                    Port: 
                </td>
                <td colspan="3">
                    <input name="port" style="width: 50px">
                </td>
                <td>
                    <input type="button" value="Add" onClick="doAdd('port')">
                </td>
            </tr>
            <tr>
                <td>
                    <b>Time of day</b>:
                </td>
                <td>
                    From: 
                </td>
                <td>
                    <input name="todFrom" style="width: 40px" maxlength="4">
                    <a class='help' href="javascript:alert('Time format is HHMM (hours and minutes). All times are GMT')">?</a>
                </td>
                <td>
                    To: 
                </td>
                <td width="140">
                    <input name="todTo" style="width: 40px" maxlength="4">
                    <a class='help' href="javascript:alert('Time format is HHMM (hours and minutes). All times are GMT')">?</a>
                </td>
                <td>
                    <input type="button" value="Add" onClick="doAdd('timeOfDay')">
                </td>
            </tr>
            <tr>
                <td>
                    <b>Regular expr.</b>:&nbsp;
                </td>
                <td>
                    Reg.expr: 
                </td>
                <td colspan="3">    
                    <input name="regexpr" style="width: 230px">
                </td>
                <td>
                    <input type="button" value="Add" onClick="doAdd('regExpr')">
                </td>
            </tr>
        </table>
        <p>
            <%@include file="/include/jobrefinementnav.jsp"%>
    </form>
<%@include file="/include/foot.jsp"%>