<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.admin.CrawlJob,org.archive.crawler.admin.StatisticsTracker,org.archive.util.LongWrapper,java.util.*" %>
<%@ page import="org.archive.crawler.datamodel.CrawlURI"%>

<%
    /**
     * Page allows user to view the information on seeds in the
     * StatisticsTracker for a completed job.
     * Parameter: job - UID for the job.
     */
    String job = request.getParameter("job");
    CrawlJob cjob = (job != null)? handler.getJob(job): handler.getCurrentJob();
    
    String title = "Seeds report";
    int tab = 4;
    
%>

<%@include file="/include/head.jsp"%>

<%
    if(cjob == null)
    {
        // NO JOB SELECTED - ERROR
%>
        <p>&nbsp;<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Invalid job selected</b>
<%
    }
    else if(stats == null)
    {
        out.println("<b>No statistics associated with job.</b><p><b>Job status:</b> " + cjob.getStatus());            
        if(cjob.getErrorMessage()!=null){
            out.println("<p><pre><font color='red'>"+cjob.getErrorMessage()+"</font></pre>");
        }
    }
    else
    {
    String ignoredSeeds = cjob.getIgnoredSeeds(); 
    if(ignoredSeeds!=null&&ignoredSeeds.length()>0) {
%>
	<b style="color:red">Items in seed specification were ignored. 
	<a href="#ignored">See below</a> for details.</b><p>
<%    
    }
%>

        <table>
            <tr>
                <th>
                    Seeds for job '<%=cjob.getJobName()%>'
                </th>
                <th>
                    Status code
                </th>
                <th>
                    Disposition
                </th>
            </tr>
            <%
                Iterator seeds = stats.getSeedsSortedByStatusCode();
                while(seeds.hasNext()){
                    String UriString = (String)seeds.next();
                    String disposition = stats.getSeedDisposition(UriString);
                    int code = stats.getSeedStatusCode(UriString);
                    String statusCode = code==0 ? "" : CrawlURI.fetchStatusCodesToString(code);
                    String statusColor = "black";
                    if(code<0 || code >=400){
                        statusColor = "red";
                    }else if(code == 200){
                        statusColor = "green";
                    }
            %>
                    <tr>
                        <td>
                            <%=UriString%>
                        </td>
                        <td align="left">
                            &nbsp;<font color="<%=statusColor%>"><%=statusCode%></font>&nbsp;
                        </td>
                        <td>
                            <a href="<%=request.getContextPath()%>/logs.jsp?job=<%=cjob.getUID()%>&log=crawl.log&mode=regexpr&regexpr=^[^ ].*<%=UriString%>&grep=true" style="text-decoration: none;"><%=disposition%></a>
                        </td>
                    </tr>
            <%
                }
            %>
        </table>

<%
    if(ignoredSeeds!=null&&ignoredSeeds.length()>0) {
%>
	<p>
	<a name="ignored"></a>
	Some items in seed specification were ignored. This may not indicate any 
	problem, but the ignored items are displayed here for reference:<p>
	
	<div style="border:2px solid pink;margin-right:50px;margin-left:50px;padding:25px">
<pre>
<%=ignoredSeeds%>
</pre>
	</div>
<%    
    }

    }
%>

<%@include file="/include/foot.jsp"%>
