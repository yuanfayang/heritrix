<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%@ page import="org.archive.crawler.framework.URIFrontierMarker"%>
<%@ page import="org.archive.crawler.framework.exceptions.InvalidURIFrontierMarkerException"%>

<%@ page import="java.util.ArrayList"%>

<%
    /**
     * This page allows users to inspect URIs in the Frontier of a paused
     * crawl. It also allows them to delete those URIs based on regular
     * expressions.
     */
    
    String title = "Inspect Frontier";
    int tab = 0;
    
%>

<%@include file="/include/head.jsp"%>
<%
    if( handler.getCurrentJob() != null && handler.getCurrentJob().getStatus().equals(CrawlJob.STATUS_PAUSED) ){
        // Have a paused job.

        String regexpr = "";
        if(request.getParameter("match") != null ){
            regexpr = request.getParameter("match");
        }
        
        int numberOfMatches = 1000;
        try {
            if(request.getParameter("numberOfMatches") != null ){
                numberOfMatches = Integer.parseInt(request.getParameter("numberOfMatches"));
            }
        } catch ( Exception e ){
            numberOfMatches = 1000;
        }
        
        boolean verbose = request.getParameter("verbose") != null && request.getParameter("verbose").equals("true");

        boolean grep = request.getParameter("grep") != null && request.getParameter("grep").equals("true");
%>
    <script type="text/javascript">
        function doDisplayInitial(){
            document.frmFrontierList.action.value = "initial";
            document.frmFrontierList.submit();
        }
        
        function doDisplayNext(){
            document.frmFrontierList.action.value = "next";
            document.frmFrontierList.submit();
        }
        
        function doDelete(){
            if(confirm("This action will delete ALL URIs in the Frontier that match the specified regular expression!\nAre you sure you wish to proceed?")){
	            document.frmFrontierList.action.value = "delete";
	            document.frmFrontierList.submit();
	        }
        }
    </script>
    <form name="frmFrontierList" method="POST" action="frontier.jsp">
    <input type="hidden" name="action" value="">
    <table cellspacing="0" cellpadding="0" width="100%">
        <tr>
            <td nowrap>
                Regular expression:
            </td>
            <td colspan="3">
                <input name="match" size="33" value="<%=regexpr%>">
            </td>
            <td nowrap>
                &nbsp;<a href="/admin/help/regexpr.jsp">?</a>&nbsp;&nbsp;
            </td>
            <td nowrap>
                <input type="button" value="Display URIs" onClick="doDisplayInitial()">&nbsp;&nbsp;&nbsp;<input type="button" value="Delete URIs" onClick="doDelete()">
            </td>
            <td width="100%">
            </td>
        </tr>
        <tr>
            <td nowrap>
                Display matches:
            </td>
            <td colspan="4">
                <input name="numberOfMatches" size="6" value="<%=numberOfMatches%>">
            </td>
        </tr>
        <tr>
            <td nowrap>
                Verbose description:
            </td>
            <td>
                <input type="checkbox" value="true" name="verbose" <%=verbose?"checked":""%>>
            </td>
            <td align="right">
                Grep style:
            </td>
            <td align="right" width="20">
                <input type="checkbox" value="true" name="grep" <%=grep?"checked":""%>>
            </td>
            <td></td>
        </tr>
        <tr><td height="5"></td></tr>
        <tr bgColor="black">
            <td bgcolor="#000000" height="1" colspan="7">
            </td>
        </tr>
<%        
                String action = request.getParameter("action");
                StringBuffer outputString = new StringBuffer();
                if ( action != null ) {
                    
                    URIFrontierMarker marker = null;
			        if(grep){
			            if(regexpr.length() > 0){
			                regexpr = ".*" + regexpr + ".*";
			            } else {
			                regexpr = ".*";
			            }
			        }
	                
	                if(action.equals("initial")){
                       // Get initial marker.
                       marker = handler.getInitialMarker(regexpr,false);
                       session.setAttribute("marker",marker);
	                } else if(action.equals("next")) {
	                   // Reuse old marker.
	                   marker = (URIFrontierMarker)session.getAttribute("marker");
	                   regexpr = marker.getMatchExpression();
	                } else if(action.equals("delete")){
	                   // Delete based on regexpr.
	                   long numberOfDeletes = handler.deleteURIsFromPending(regexpr);
                       out.println("<tr><td height='5'></td></tr>");
	                   out.println("<tr><td colspan='7'><b>All " + numberOfDeletes + " URIs matching</b> <code>" + regexpr + "</code> <b>were deleted.</b></td></tr>");
                       out.println("<tr><td height='5'></td></tr>");
	                }
	                
	                if (marker != null) {             

                        int found = 0;
                        try{
                            ArrayList list = handler.getPendingURIsList(marker,numberOfMatches,verbose);
                            found = list.size();
                            for(int i=0 ; i < list.size() ; i++){
                                outputString.append((String)list.get(i)+"\n");
                            }
                        } catch ( InvalidURIFrontierMarkerException e ) {
                            session.removeAttribute("marker");
                            outputString.append("Invalid marker");
                        }

                        long from = 1;
                        long to = marker.getNextItemNumber()-1;
                        boolean hasNext = marker.hasNext();
                        
                        if(marker.getNextItemNumber() > numberOfMatches+1){
                            // Not starting from 1.
                            from = to-found;
                        }
%>
				        <tr><td height="5"></td></tr>
				        <tr>
				            <td colspan="7">
                                <% if(to>0) { %> Displaying URIs <%=from%> - <%=to%> matching <% } else { %> No URIs found matching <% } %> expression '<code><%=regexpr%></code>'.  <% if(hasNext){ %> <a href="javascript:doDisplayNext()">Get next set of matches &gt;&gt;</a> <% } %>
				            </td>
				        </tr>
				        <tr><td height="5"></td></tr>
				        <tr bgColor="black">
				            <td bgcolor="#000000" height="1" colspan="7">
				            </td>
				        </tr>
				        <tr><td height="5"></td></tr>
				        <tr>
				            <td colspan="7"><pre><%=outputString.toString()%></pre></td>
				        </tr>
				        <tr><td height="5"></td></tr>
				        <tr bgColor="black">
				            <td bgcolor="#000000" height="1" colspan="7">
				            </td>
				        </tr>
				        <tr><td height="5"></td></tr>
				        <tr>
				            <td colspan="7">
                                <% if(to>0) { %> Displaying URIs <%=from%> - <%=to%> matching <% } else { %> No URIs found matching <% } %> expression '<code><%=regexpr%></code>'.  <% if(hasNext){ %> <a href="javascript:doDisplayNext()">Get next set of matches &gt;&gt;</a> <% } %>
				            </td>
				        </tr>
				        <tr><td height="5"></td></tr>
<%
				        out.println("</pre>");
				    }
			    }
%>
        <tr bgColor="black">
            <td bgcolor="#000000" height="1" colspan="7">
            </td>
        </tr>
        <tr><td height="5"></td></tr>
    </table>
    </form>
<%
    } else { 
%>
        <b>You can only inspect and manipulate the URIs list of a paused job.</b>
<%  
    } 
%>

<%@include file="/include/foot.jsp"%>