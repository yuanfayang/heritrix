<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%
	String title = "Threads report";
	int tab = 4;
	
	String action = request.getParameter("action");
	String message = null;
	if(action != null && action.equals("killkillkill")){
        // Kill thread.
        try{
            handler.killThread(Integer.parseInt(request.getParameter("threadNumber")),
                    (request.getParameter("replace")!=null && 
                        request.getParameter("replace").equals("replace")));
            message = "Kill message sent to thread #" + request.getParameter("threadNumber");
        } catch(NumberFormatException e){
            message = "Kill operation failed";
        }
	}
%>

<%@include file="/include/head.jsp"%>
	<script type="text/javascript">
        function doKill(){
            thread = document.frmThread.threadNumber.value;
            if(confirm("Are you sure you wish to kill thread number " +
                            thread + "?\nThe action is irreversible " +
                            " and can potentially destablize the crawler."))
            {
                document.frmThread.action.value = "killkillkill";
                document.frmThread.submit();
            }
        }
    </script>
    <pre><%=handler.getThreadsReport()%></pre>
    <hr>
    <form name="frmThread" method="post" action="threads.jsp">
        <input type="hidden" name="action">
        <b>Thread number:</b> <input name="threadNumber" size="3"> <input type="checkbox" name="replace" value="replace"> Replace thread <input type="button" onClick="doKill()" value="Kill thread">
    </form>

<%@include file="/include/foot.jsp"%>
