<%
  /**
   * This pages allows the user to select all submodules which appear
   * in collections inside other modules 
   *
   * @author Kristinn Sigurdsson
   */
%>
<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.admin.ui.JobConfigureUtils" %>
<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.url.canonicalize.BaseRule" %>

<%
    CrawlJob theJob = JobConfigureUtils.handleJobAction(handler, request,
            response, request.getContextPath() + "/jobs.jsp", null);
    int tab = theJob.isProfile()?2:1;
%>

<%
    // Set page header.
    String title = "Submodules";
    int jobtab = 7;
%>

<%@include file="/include/head.jsp"%>
<%@include file="/include/filters_js.jsp"%>
    <p>
        <%@include file="/include/jobpernav.jsp"%>
    <p>
        <p>
            <b>Add/Remove/Order Submodules</b>
        <p>

    <form name="frmFilters" method="post" 
            action="submodules.jsp">
        <input type="hidden" name="job" value="<%=theJob.getUID()%>">
        <input type="hidden" name="action" value="done">
        <input type="hidden" name="subaction" value="continue">
        <input type="hidden" name="map" value="">
        <input type="hidden" name="filter" value="">

            <%=JobConfigureUtils.printAllMaps(theJob.getSettingsHandler().getOrder(),
                   false, null)%>
    </form>
    <p>
<%@include file="/include/jobnav.jsp"%>
<%@include file="/include/foot.jsp"%>


