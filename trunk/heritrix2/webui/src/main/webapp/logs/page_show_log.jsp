<%@ page import="org.archive.crawler.webui.Log"%>
<%@ page import="org.archive.crawler.webui.Text"%>
<%@ page import="org.archive.crawler.webui.Crawler"%>
<%@ page import="org.archive.crawler.webui.CrawlJob"%>
<%@ page import="org.archive.crawler.framework.JobStage"%>
<%@ page import="org.archive.util.TextUtils"%>
<%@ page import="org.archive.crawler.webui.Log.Mode"%>
<%@ page import="org.archive.crawler.util.Logs"%>

<%
	Log log = (Log)Text.get(request, "log"); 
	Crawler crawler = (Crawler)Text.get(request, "crawler");
	
	int refreshInterval = log.getRefreshInterval();
	Logs currentLog = log.getCurrentLog();     
	Mode mode = log.getMode(); 
	int linesToShow = log.getLinesToShow();
	CrawlJob crawlJob = (CrawlJob)Text.get(request, "job");
%>
<html>
<head>
	<%@include file="/include/header.jsp"%>
	<title>Heritrix <%=Text.html(crawler.getLegend())%></title>
</head>
<body>
<%@include file="/include/nav.jsp"%>

<% if(log.getRefreshInterval()>0){ %>
    <meta http-equiv=Refresh 
          content=<%=refreshInterval%> 
          URL=do_show_log.jsp?<%=crawler.getQueryString()+"&"+log.getQueryString(log.getCurrentLog(),log.getMode())%>">
<% } %>

<form method="GET" action="do_show_log.jsp">
<input type="hidden" name="host" value="<%=crawler.getHost()%>">
<input type="hidden" name="port" value="<%=crawler.getPort()%>">
<input type="hidden" name="id" value="<%=crawler.getIdentityHashCode()%>">
<input type="hidden" name="job" value="<%=log.getJob()%>">
<input type="hidden" name="log" value="<%=currentLog%>">
<input type="hidden" name="mode" value="<%=mode%>">

<table border="0" cellspacing="0" cellpadding="0">
    <tr>
        <td height="3"></td>
    </tr>
    <tr>
        <td valign="top" width="210">
            <table border="0" cellspacing="0" cellpadding="0">
                <tr>
                    <td width="50" align="right" valign="top">
                        &nbsp;<b>View:</b>&nbsp;
                    </td>
                    <td align="left" valign="top" width="160">
                     <% for(Logs logtype : Logs.values()){ %>
                    	<a href="do_show_log.jsp?<%=crawler.getQueryString()+"&"+log.getQueryString(logtype,log.getMode())%>" <%=currentLog==logtype?"class='plain'":""%>><%=logtype.getFilename()%></a><br>
	                <% } %>
                    </td>
                </tr>
            </table>
        </td>
        <td valign="top" width="170">
            <table border="0" cellspacing="0" cellpadding="0">
                <tr>
                    <td width="20" align="right" valign="top">
                        &nbsp;<b>By:</b>&nbsp;
                    </td>
                    <td align="left" valign="top" width="150">
                       	<% for(Log.Mode modeType : Log.Mode.values()){ %>
                        	<a href="do_show_log.jsp?<%=crawler.getQueryString()+"&"+log.getQueryString(log.getCurrentLog(),modeType)%>" <%=mode==modeType?"class='plain'":""%>><%=modeType.getDescription()%></a><br>
                        <% } %>
                        <input type="hidden" name="mode" value="<%=mode%>">
                    </td>
                </tr>
            </table>
        </td>
        <td valign="top">
        <% if(mode==Log.Mode.TAIL){ %>
            <table border="0" cellspacing="0" cellpadding="0">
                <tr>
                    <td>
                        <b>Refresh time:</b>&nbsp;
                    </td>
                    <td>
                        <select id="time" onChange="location.href='do_show_log.jsp?time=' + document.getElementById('time').value + '&<%=crawler.getQueryString()+"&"+log.getQueryString(log.getCurrentLog(),log.getMode())%>'" >
                            <option value="-1" <%=refreshInterval==-1?"selected":""%>>No refresh</option>
                            <option value="2" <%=refreshInterval==2?"selected":""%>>2 sec.</option>
                            <option value="5" <%=refreshInterval==5?"selected":""%>>5 sec.</option>
                            <option value="10" <%=refreshInterval==10?"selected":""%>>10 sec.</option>
                            <option value="20" <%=refreshInterval==20?"selected":""%>>20 sec.</option>
                            <option value="30" <%=refreshInterval==30?"selected":""%>>30 sec.</option>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td>
                        <b>Lines to show:</b>&nbsp;
                    </td>
                    <td>
                        <input size="4" name="linesToShow" value="<%=linesToShow%>">
                    </td>
                </tr>
            </table>
        <% } else if(mode==Log.Mode.LINE_NUMBER){ %>
            <table border="0" cellspacing="0" cellpadding="0">
                <tr>
                    <td nowrap align="right">
                        <b>Line number:</b>&nbsp;
                    </td>
                    <td>
                        <input size="4" value="<%=log.getLinenumber()%>" name="linenumber">&nbsp;<input type="submit" value="Get">
                    </td>
                </tr>
                <tr>
                    <td>
                        <b>Lines to show:</b>&nbsp;
                    </td>
                    <td>
                        <input size="4" name="linesToShow" value="<%=linesToShow%>">
                    </td>
                </tr>
            </table>
        <% } else if(mode==Log.Mode.TIMESTAMP){ %>
            <table border="0" cellspacing="0" cellpadding="0">
                <tr>
                    <td nowrap align="right" valign="top">
                        <b>Timestamp:</b>&nbsp;
                    </td>
                    <td>
                        <input value="<%=request.getParameter("timestamp")==null?"":request.getParameter("timestamp")%>" name="timestamp" align="absmiddle" size="21">&nbsp;<input type="submit" value="Get"><br>
                        (YYYYMMDDHHMMSS)
                    </td>
                </tr>
                <tr>
                    <td>
                        <b>Lines to show:</b>&nbsp;
                    </td>
                    <td>
                        <input size="4" name="linesToShow" value="<%=linesToShow%>">
                    </td>
                </tr>
            </table>
        <% } else if(mode==Log.Mode.REGEXPR){ %>
            <table border="0" cellspacing="0" cellpadding="0">
                <tr>
                    <td nowrap align="right">
                        <b>Regular expression:</b>&nbsp;
                    </td>
                    <td width="100%" colspan="4">
                        <input size="50" name="regexpr" value="<%=request.getParameter("regexpr")==null?"":request.getParameter("regexpr")%>" align="absmiddle">&nbsp;<input type="submit" value="Get">
                    </td>
                </tr>
                <tr>
                    <td align="right">
                        <font size="-2">(<a href="<%=request.getContextPath()%>/help/regexpr.jsp">about java reg.expr.</a>)</font>&nbsp;
                    </td>
                    <td nowrap colspan="2">
                        Start at match:&nbsp; <input size="4" name="linesToSkip" value="<%=log.getLinesToSkip()%>">
                    </td>
                    <td nowrap colspan="2" width="100%">
                        &nbsp;&nbsp;Show matches:&nbsp;<input size="4" name="linesToShow" value="<%=linesToShow%>"> (0 = all)
                    </td>
                </tr>
                <tr>
                    <td></td>
                    <td nowrap>
                        <input name="ln" value="true" type="checkbox" <%=request.getParameter("ln")!=null&&request.getParameter("ln").equalsIgnoreCase("true")?"checked":""%>><input type="hidden" name="linesToShow" value="<%=linesToShow%>">
                    </td>
                    <td nowrap>
                        &nbsp;Line numbers&nbsp;&nbsp;
                    </td>
                    <td nowrap>
                        &nbsp;<input name="grep" value="true" type="checkbox" <%=request.getParameter("grep")!=null&&request.getParameter("grep").equalsIgnoreCase("true")?"checked":""%>>
                    </td>
                    <td width="100%">
                        &nbsp;Grep style&nbsp;&nbsp;
                    </td>
                </tr>
                <tr>
                    <td>
                    </td>
                    <td nowrap>
                        <input name="indent" value="true" type="checkbox" <%=request.getParameter("indent")!=null&&request.getParameter("indent").equalsIgnoreCase("true")?"checked":""%>>
                    </td>
                    <td width="100%" colspan="3">
                        &nbsp;Include following indented lines&nbsp;&nbsp;
                    </td>
                </tr>
            </table>
        <% } %>
        </td>
    </tr>
</table>
<p>
<table border="0" cellspacing="0" cellpadding="0">
    <tr>
        <td colspan="2"></td>
        <td height="5" colspan="3">
        </td>
        <td colspan="2"></td>
    </tr>
    <tr>
        <td rowspan="5" width="3" nowrap >
        <td rowspan="5" width="1" nowrap bgcolor="#0c0c0c">
        </td>
        <td height="1" colspan="4" bgcolor="#0c0c0c">
        </td>
        <td rowspan="5" width="1" nowrap bgcolor="#0c0c0c">
        <td rowspan="5" width="3" nowrap >
    </tr>
    <tr>
        <td colspan="2">
            &nbsp;<%=currentLog.getFilename()%> for <%=log.getJob()%>
        </td>
        <td colspan="1" align="right">
            <%=log.getLog()[1]%>
        </td>
        <td>&nbsp;
        </td>
    </tr>
    <tr>
        <td height="1" colspan="4" bgcolor="#0c0c0c">
        </td>
    </tr>
    <tr>
        <td colspan="4" class="main" width="400" height="100" valign="top">
                <pre><% TextUtils.writeEscapedForHTML(log.getLog()[0],out); %></pre>
        </td>
    </tr>
    <tr>
        <td height="1" colspan="4" bgcolor="#0c0c0c">
        </td>
    </tr>
    <tr>
        <td height="5" colspan="4">
        </td>
    </tr>
</table>
</form>

<% if (crawlJob.getJobStage() == JobStage.ACTIVE) { %>

<hr>

<h3>Rotate Log Files</h3>

<form 
   method="post" 
   action="<%=request.getContextPath()%>/console/do_rotate_log_files.jsp?<%=Text.jobQueryString(request)%>"
   >
   <input type="checkbox" name="confirm" value="yes">Confirm<br/>
   <input type="submit" value="Rotate Log Files">
</form>

<% } %>

</body>
</html>