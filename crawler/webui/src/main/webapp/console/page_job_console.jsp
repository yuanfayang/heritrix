<%@ page import="org.archive.crawler.webui.Text" %>
<%@ page import="org.archive.crawler.webui.Crawler" %>
<%@ page import="org.archive.crawler.framework.StatisticsTracking" %>
<%@ page import="org.archive.crawler.framework.JobController" %>
<%@ page import="org.archive.util.ArchiveUtils" %>
<!-- @include file="/include/handler.jsp" -->

<% 

Crawler crawler = (Crawler)Text.get(request, "crawler");
StatisticsTracking stats = (StatisticsTracking)Text.get(request, "stats");
JobController controller = (JobController)Text.get(request, "controller");
String job = (String)Text.get(request, "job");

String qs = crawler.getQueryString() + "&job=" + job;

%>



<%
    String sAction = request.getParameter("action");
    if(sAction != null) {
        if(sAction.equalsIgnoreCase("logout")) {
            // Logging out.
            session = request.getSession();
            if (session != null) {
                session.invalidate();
                // Redirect back to here and we'll get thrown to the login
                // page.
                response.sendRedirect(request.getContextPath() + "/index.jsp"); 
            }
        }
    }

    String title = "Admin Console";
    int tab = 0;
%>

<!-- @include file="/include/head.jsp" -->
    
    <script type="text/javascript">
        function doTerminateCurrentJob(){
            if(confirm("Are you sure you wish to terminate the job currently being crawled?")){
                document.location = '<%out.print(request.getContextPath());%>/console/action.jsp?action=terminate';
            }
        }    
    </script>
    
    <table border="0" cellspacing="0" cellpadding="0"><tr><td>
    <fieldset style="width: 750px">
        <legend> 
        <b><span class="legendTitle">Crawler Status:</span> 
        <span class="status crawling"><%=controller.getCrawlStatusString()%></span>
        <!-- = handler.isRunning() 
            ? "<span class='status crawling'>CRAWLING JOBS</span></b> | "
              +"<a href='"+request.getContextPath()+"/console/action.jsp?action=stop'>Hold</a>"
            : "<span class='status holding'>HOLDING JOBS</span></b> | "
              +"<a href='"+request.getContextPath()+"/console/action.jsp?action=start'>Start</a>"
        --> </b>
        </legend>
        <div style="float:right;padding-right:50px;">
	        <b>Memory</b><br>
	        <div style="padding-left:20px">
		        <%=(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024%> KB 
		        used<br>
		        <%=(Runtime.getRuntime().totalMemory())/1024%> KB
		        current heap<br>
		        <%=(Runtime.getRuntime().maxMemory())/1024%> KB
		        max heap
	        </div>
	    </div>
        <b>Jobs</b>
        <div style="padding-left:20px">
            <%=controller.getCrawlStatusString()%>: <i><%=job%></i>
			<!--= handler.getCurrentJob()!=null
			    ? shortJobStatus+": <i>"
			      +handler.getCurrentJob().getJobName()+"</i>"
			    : ((handler.isRunning()) ? "None available" : "None running")
			 <br>
	        <= handler.getPendingJobs().size() > pending,
	        <= handler.getCompletedJobs().size() > completed -->
        </div>

        <b>Alerts:</b> 0 (0 new) <!-- FIXME
	        <a style="color: #000000" 
	            href="<=request.getContextPath()>/console/alerts.jsp">
	            <=heritrix.getAlertsCount()> (<=heritrix.getNewAlertsCount()> new)
	        </a> -->
	        
         </fieldset>
            <%
            	long begin, end;
	            if(stats != null) {
	                begin = stats.successfullyFetchedCount();
	                end = stats.totalCount();
	                if(end < 1) {
	                    end = 1;
	                }
	            } else {
                    begin = 0;
                    end = 1;
	            }
                
                if (true) //handler.getCurrentJob() != null)
                {
                    final long timeElapsed, timeRemain;
                    if(stats == null) {
                        timeElapsed= 0;
                        timeRemain = -1;
                    } else {
	                    timeElapsed = (stats.getCrawlerTotalElapsedTime());
	                    if(begin == 0) {
	                        timeRemain = -1;
	                    } else {
	                        timeRemain = ((long)(timeElapsed*end/(double)begin))-timeElapsed;
	                    }
                    }
            %>
            <fieldset style="width: 750px">
               <legend>
               <b><span class="legendTitle">Job Status:</span>
               <%=controller.getCrawlStatusString()%>
               <!--= 
               "<span class='status "
               +shortJobStatus+"'>"
               +shortJobStatus+"</span>"
               -->
               </b> 
           
<a>
<% String status = controller.getCrawlStatusString(); %>
<% if (status.equals("PREPARING")) { %>
    <a 
    title="Start the crawl."
    href="<%=request.getContextPath()%>/console/do_start.jsp?<%=qs%>">
    Start
    </a>
<% } else if (status.equals("PAUSED") || status.equals("PAUSING")) { %> 
    <a 
    title="Resume the crawl."
    href="<%=request.getContextPath()%>/console/do_resume.jsp?<%=qs%>">
    Resume
    </a>
    |
    <a 
    title="Checkpoint the crawl."
    href="<%=request.getContextPath()%>/console/do_checkpoint.jsp?<%=qs%>">
    Checkpoint
    </a>
<% } else if (!status.equals("CHECKPOINTING")) { %>
    <a 
    title="Pause the crawl."
    href="<%=request.getContextPath()%>/console/do_pause.jsp?<%=qs%>">
    Pause
    </a>
<% } %>
|
    <a 
    title="Terminates the crawl."
    href="<%=request.getContextPath()%>/console/do_stop.jsp?<%=qs%>">
    Terminate
    </a>

</legend>

                <%
                  if(stats != null)
                  {
                %>
                	<div style="float:right; padding-right:50px;">
                	    <b>Load</b>
            			<div style="padding-left:20px">
			            	<%=stats.activeThreadCount()%> active of <%=stats.threadCount()%> threads
			            	<br>
			            	<%=ArchiveUtils.doubleToString((double)stats.congestionRatio(),2)%>
			            	congestion ratio
			            	<br>
			            	<%=stats.deepestUri()%> deepest queue
			            	<br>
			            	<%=stats.averageDepth()%> average depth
						</div>
					</div>
	                <b>Rates</b>
	                <div style="padding-left:20px">
		                <%=ArchiveUtils.doubleToString(stats.currentProcessedDocsPerSec(),2)%> 		                
		                URIs/sec
		                (<%=ArchiveUtils.doubleToString(stats.processedDocsPerSec(),2)%> avg)
		                <br>
		                <%=stats.currentProcessedKBPerSec()%>
						KB/sec
						(<%=stats.processedKBPerSec()%> avg)
					</div>

                    <b>Time</b>
                    <div class='indent'>
	                    <%= ArchiveUtils.formatMillisecondsToConventional(timeElapsed,false) %>
						elapsed
						<br>
	                    <%
	                       if(timeRemain != -1) {
	                    %>
		                    <%= ArchiveUtils.formatMillisecondsToConventional(timeRemain,false) %>
		                    remaining (estimated)
		               	<%
	                       }
                   		%>
					</div>
                    <b>Totals</b>
                	<%
                          }
                }
                if(stats != null)
                {
	                int ratio = (int) (100 * begin / end);
            %>
                            <center>
                            <table border="0" cellpadding="0" cellspacing= "0" width="600"> 
                                <tr>
                                    <td align='right' width="25%">downloaded <%= begin %>&nbsp;</td>
                                    <td class='completedBar' width="<%= (int)ratio/2 %>%" align="right">
                                    <%= ratio > 50 ? "<b>"+ratio+"</b>%&nbsp;" : "" %>
                                    </td>
                                    <td class='queuedBar' align="left" width="<%= (int) ((100-ratio)/2) %>%">
                                    <%= ratio <= 50 ? "&nbsp;<b>"+ratio+"</b>%" : "" %>
                                    </td>
                                    <td width="25%" nowrap>&nbsp;<%=stats.queuedUriCount()%> queued</td>
                                </tr>
                            </table>
                            <%= end %> total downloaded and queued<br>      
                    		<%=ArchiveUtils.formatBytesForDisplay(stats.totalBytesWritten())%> uncompressed data received
                            </center>
            <%
                }
                if (controller.getCrawlStatusString().equals("PAUSED")) {
            %>
            		<b>Paused Operations</b>
            		<div class='indent'>
	                	<a href='<%= request.getContextPath() %>/console/frontier.jsp'>View or Edit Frontier URIs</a>
	                </div>
	        <%
            	}
            %>
    </fieldset>
    </td></tr>
    <tr><td>
    
	<a href="<%=request.getContextPath()%>/">Refresh</a>
    </td></tr>
    <tr><td>
        <p>
            &nbsp;
        <p>
            &nbsp;
    </td></tr>
    <tr><td>

        <!-- a href="<%=request.getContextPath()%>/index.jsp?action=logout">Logout</a -->
    </td></tr></table>
<!-- include file="/include/foot.jsp" -->
