<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%@page import="java.util.List"%>
<%@page import="org.archive.crawler.admin.CrawlJob"%>
<%@page import="org.archive.crawler.datamodel.settings.XMLSettingsHandler"%>

<%
	if(request.getParameter("default")!=null){
		CrawlJob defaultJob = handler.getJob(request.getParameter("default"));
		if(defaultJob != null && defaultJob.isProfile()){
			handler.setDefaultProfile(defaultJob);
		}
	}
	String title = "Profiles";
	int tab = 2;
%>

<%@include file="/include/head.jsp"%>
<% if(request.getParameter("message")!=null && request.getParameter("message").length() >0){ %>
	<p>
		<font color="red"><b><%=request.getParameter("message")%></b></font>
<% } %>
<table border="0">
	<tr>
		<th>
			Profile name
		</th>
		<th>
			Actions
		</th>
	</tr>
	<%
		List profiles = handler.getProfiles();
		CrawlJob defaultProfile = handler.getDefaultProfile();
		for(int i=0 ; i<profiles.size() ; i++){
			CrawlJob profile = (CrawlJob)profiles.get(i);
	%>
	<tr>
		<td width="150">
			<%if(defaultProfile.getJobName().equals(profile.getJobName())){out.println("<b>");}%>
			<%=profile.getJobName()%>
			<%if(defaultProfile.getJobName().equals(profile.getJobName())){out.println("</b>");}%>
		</td>
		<td>
			<a href="/admin/jobs/configure.jsp?job=<%=profile.getUID()%>">Edit</a>
			<a href="/admin/jobs/new.jsp?job=<%=profile.getUID()%>">New job based on it</a>
			<a href="/admin/jobs/new.jsp?job=<%=profile.getUID()%>&profile=true">New profile based on it</a>
			<%if(defaultProfile.getJobName().equals(profile.getJobName())==false){%>
			<a href="/admin/profiles.jsp?default=<%=profile.getUID()%>">Set as default</a>
			<!--a href="">Delete</a-->
			<%}%>
		</td>
	</tr>
	<%
		}
	%>
</table>
<%@include file="/include/foot.jsp"%>
