<%
  /**
   * This pages allows the user to select what url canonicalization rules
   * are applied to urls.
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
    String title = "URL Canonicalization Rules";
    int jobtab = 6;
%>

<%@include file="/include/head.jsp"%>
<%@include file="/include/filters_js.jsp"%>
    <p>
        <%@include file="/include/jobnav.jsp"%>
    <p>
        <p>
            <b>Add/Remove/Order URL Canonicalization Rules</b>
        <p>

    <form name="frmFilters" method="post" 
            action="url-canonicalization-rules.jsp">
        <input type="hidden" name="job" value="<%=theJob.getUID()%>">
        <input type="hidden" name="action" value="done">
        <input type="hidden" name="subaction" value="">
        <input type="hidden" name="map" value="">
        <input type="hidden" name="filter" value="">
        <table>
            <%=JobConfigureUtils.printOfType(theJob.getSettingsHandler().getOrder(), "", false,
                   false, false, null, false, BaseRule.class, false)%>
        </table>
    </form>
    <p>
<%@include file="/include/jobnav.jsp"%>
<%@include file="/include/foot.jsp"%>


