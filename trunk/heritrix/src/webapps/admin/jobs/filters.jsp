<%
  /**
   * This pages allows the user to select what filters
   * are applied to what modules in the crawl order.
   *
   * @author Kristinn Sigurdsson
   */
%>
<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.admin.ui.JobConfigureUtils" %>
<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.framework.Filter" %>

<%@include file="/include/filters.jsp"%>
<%
    CrawlJob theJob = JobConfigureUtils.handleJobAction(handler, request,
        response, request.getContextPath() + "/jobs.jsp", null);
    int tab = theJob.isProfile()?2:1;
%>

<%
    // Set page header.
    String title = "Select Filters";
    int jobtab = 1;
%>

<%@include file="/include/head.jsp"%>

<%@include file="/include/filters_js.jsp"%>
    <p>
        <%@include file="/include/jobnav.jsp"%>
    <p>
    <form name="frmFilters" method="post" action="filters.jsp">
        <input type="hidden" name="job" value="<%=theJob.getUID()%>">
        <input type="hidden" name="action" value="done">
        <input type="hidden" name="subaction" value="">
        <input type="hidden" name="map" value="">
        <input type="hidden" name="filter" value="">
        <table>
            <%=printFilters(theJob.getSettingsHandler().getOrder(), "",
                    false, false, false, null, false, 
                    CrawlJobHandler.loadOptions(
                        CrawlJobHandler.MODULE_OPTIONS_FILE_FILTERS),
                    Filter.class, true)%>
        </table>
    </form>
    <p>

<%@include file="/include/jobnav.jsp"%>
<%@include file="/include/foot.jsp"%>
