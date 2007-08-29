<%@ page import="org.archive.crawler.webui.Text" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="java.util.Collection"%>

<% 

Crawler crawler = (Crawler)Text.get(request, "crawler");
CrawlJob job = (CrawlJob)Text.get(request, "job");
Collection<CrawlJob> completedJobs = (Collection)Text.get(request, "completedJobs"); 

String qs = crawler.getQueryString() + "&job=" + job.getName();

%>

<%@page import="org.archive.crawler.framework.CrawlController"%>
<html>
<head>
    <%@include file="/include/header.jsp"%>
    <title>Heritrix Import URIs to Frontier</title>
</head>
<body>

<script>
function updateSections() {
   if($('#source').val() == 'file') {
      $('#fileOptions').show('medium');
   } else {
      $('#fileOptions').hide('medium');
      $('#recoveryLog').attr('checked','checked');
   }
   if($('#recoveryLog').attr('checked')) {
      $('#recoveryOptions').show('medium');
   } else {
      $('#recoveryOptions').hide('medium');
   }
};
$(updateSections);
</script>

<%@include file="/include/nav.jsp"%>

<h1>Import URIs to Frontier</h1>

You may bulk import URIs to the current job's frontier from either a 
file (at the job's crawl engine machine) or a prior crawl's recovery 
log. 

<form method="post" action="do_frontier_import.jsp">
<% Text.printJobFormFields(request, out); %>

<h2>Source</h2>

<select id="source" name="source" 
        onChange="updateSections()">
<option value="file" selected>file at crawl engine</option>
<optgroup label="Or a recover log from job:">
<% for (CrawlJob c: completedJobs) { %>
<option value="<%=Text.attr(c.encode())%>">
<%=Text.html(c.getName())%>
</option>
<% } %>
</optgroup>
</select>

<div id="fileOptions">

<h2> File Import Options </h2>

<h3>File full path</h3>
<input type="text" size="60" name="path" value=""><br/>

<h3>File format</h3>
<input id="onePer" type="radio" name="format" checked value="onePer"
       onChange="updateSections()">
<label for="onePer">one URI per line</label><br/>
<input id="crawlLog" type="radio" name="format" value="crawlLog"
       onChange="updateSections()">
<label for="crawlLog">crawl.log style</label><br/>
<input id="recoveryLog" type="radio" name="format" value="recoveryLog"
       onChange="updateSections()">
<label for="recoveryLog">recovery log style</label>

</div>

<div id="generalOptions">

<h2> General Options </h2>
<input id="forceRevisit" type="checkbox" name="forceRevisit" value="false">
<label for="forceRevisit">Force revisit of URIs</label><br/>

<input id="scopeScheduleds" type="checkbox" name="scopeScheduleds" checked="true">
<label for="scopeScheduleds">Apply scope before scheduling</label>

</div>

<div id="recoveryOptions" style="display:none;">

<h2> Recovery Log Options </h2>

<h3> First (consider-included) pass</h3>

<input id="scopeIncludes" type="checkbox" name="scopeIncludes" checked="true">
<label for="scopeIncludes">Apply scope before including</label><br/>
<br/>
<input id="includeSuccesses" type="checkbox" name="includeSuccesses" checked="true">
<label for="includeSuccesses">Include log successes ('Fs' lines; usual)</label><br/>
<input id="includeFailures" type="checkbox" name="includeFailures">
<label for="includeFailures">Include log failures ('Ff' lines; possible)</label><br/>
<input id="includeScheduleds" type="checkbox" name="includeScheduleds">
<label for="includeScheduleds">Include log scheduleds ('F+' lines; unusual)</label><br/>

<h3> Second (schedule if new) pass</h3>
<input id="scheduleSuccesses" type="checkbox" name="scheduleSuccesses">
<label for="scheduleSuccesses">Include log successes ('Fs' lines; unusual)</label><br/>
<input id="scheduleFailures" type="checkbox" name="scheduleFailures">
<label for="scheduleFailures">Include log failures ('Ff' lines; possible)</label><br/>
<input id="scheduleScheduleds" type="checkbox" name="scheduleScheduleds" checked="true">
<label for="scheduleScheduleds">Include log scheduleds ('F+' lines; usual)</label><br/>
</div>

<br/>
<input type="submit" value="Import URIs">
</form>

</body>
</html>