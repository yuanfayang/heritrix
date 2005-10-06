<%@include file="/include/handler.jsp"%>

<%
    String title = "Frontier report";
    int tab = 4;
    String reportName = request.getParameter("name");
%>

<%@include file="/include/head.jsp"%>
        <pre><%=handler.getCurrentJob() != null?
            handler.getCurrentJob().getFrontierReport(reportName):
            "No current job"%></pre>
<%@include file="/include/foot.jsp"%>
