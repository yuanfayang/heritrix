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
<%@ page import="org.archive.crawler.framework.Filter" %>

<%@include file="/include/jobfilters.jsp"%>

<%
    String currDomain = request.getParameter("currDomain");
    CrawlJob theJob = JobConfigureUtils.handleJobAction(handler, request,
            response, "/admin/jobs/per/overview.jsp", currDomain);
	int tab = theJob.isProfile()?2:1;
%>
<%
	// Set page header.
	String title = "Add override filters";
	int jobtab = 1;
%>

<%@include file="/include/head.jsp"%>
<%@include file="/include/filters_js.jsp"%>

	<p>
		<b>Override for the <%=theJob.isProfile()?"profile":"job"%> <%=theJob.getJobName()%> on domain '<%=currDomain%>'</b>
		<%@include file="/include/jobpernav.jsp"%>
	<p>
	<form name="frmFilters" method="post" action="filters.jsp">
		<input type="hidden" name="currDomain" value="<%=currDomain%>">
		<input type="hidden" name="job" value="<%=theJob.getUID()%>">
		<input type="hidden" name="action" value="done">
		<input type="hidden" name="subaction" value="">
		<input type="hidden" name="map" value="">
		<input type="hidden" name="filter" value="">
		<p>
			<b>Instructions:</b> It is possible to add filters to overrides and manipulate existing<br>
			override filters. It is not possible to remove filters defined in a super domain!
		<p>
		<table>
			<%=printFilters(
                    theJob.getSettingsHandler().getOrder(),
                    ((XMLSettingsHandler)theJob.getSettingsHandler()).
                        getSettingsObject(currDomain), "", false,
			        false, false, (String)null, false, 
                    CrawlJobHandler.loadOptions(CrawlJobHandler.
                        MODULE_OPTIONS_FILE_FILTERS), Filter.class, true)%>
		</table>
	</form>
	<p>
		<%@include file="/include/jobpernav.jsp"%>
<%@include file="/include/foot.jsp"%>


