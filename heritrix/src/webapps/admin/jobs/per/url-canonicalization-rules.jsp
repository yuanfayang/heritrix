<%
  /**
   * This pages allows the user to add filters to overrides.
   *
   * @author Kristinn Sigurdsson
   */
%>
<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.admin.ui.JobConfigureUtils" %>
<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.url.canonicalize.BaseRule" %>

<%@include file="/include/jobfilters.jsp"%>

<%
    String currDomain = request.getParameter("currDomain");
    CrawlJob theJob = JobConfigureUtils.handleJobAction(handler, request,
            response, "/admin/jobs/per/overview.jsp", currDomain);
    int tab = theJob.isProfile()?2:1;
%>
<%
    // Set page header.
    String title = "Add Url Canonicalization Override";
    int jobtab = 6;
%>

<%@include file="/include/head.jsp"%>
<%@include file="/include/filters_js.jsp"%>
    <p>
        <b>Override rules for the
        <%=theJob.isProfile()?"profile":"job"%> <%=theJob.getJobName()%> on
        domain '<%=currDomain%>'</b>
        <%@include file="/include/jobpernav.jsp"%>
    <p>
    <form name="frmFilters" method="post" 
            action="url-canonicalization-rules.jsp">
        <input type="hidden" name="currDomain" value="<%=currDomain%>">
        <input type="hidden" name="job" value="<%=theJob.getUID()%>">
        <input type="hidden" name="action" value="done">
        <input type="hidden" name="subaction" value="">
        <input type="hidden" name="map" value="">
        <input type="hidden" name="filter" value="">
        <p>
            <b>Instructions:</b> It is possible to add rules to overrides and
            manipulate existing<br>
            override rules. It is not possible to remove rules defined in a
            super domain!
        <p>
        <table>
            <%=printFilters(theJob.getSettingsHandler().getOrder(),
                    ((XMLSettingsHandler)theJob.getSettingsHandler()).
                        getSettingsObject(currDomain), "", false,
                    false, false, null, false,
                    CrawlJobHandler.loadOptions(CrawlJobHandler.
                        MODULE_OPTIONS_URL_CANONICALIZATION_RULES), 
                   BaseRule.class, false)%>
        </table>
    </form>
    <p>
<%@include file="/include/jobpernav.jsp"%>
<%@include file="/include/foot.jsp"%>
