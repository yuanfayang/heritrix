<%@include file="/include/handler.jsp"%>

<%
    String title = "Frontier report";
    int tab = 4;
%>

<%@include file="/include/head.jsp"%>
        <pre><%=handler.getCurrentJob() != null?
            handler.getCurrentJob().getFrontierReport().replaceAll("<","&lt;"):
            "No current job"%></pre>
<%@include file="/include/foot.jsp"%>
