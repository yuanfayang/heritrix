<%@include file="/include/handler.jsp"%>

<%
    String title = "Frontier report";
    int tab = 4;
%>

<%@include file="/include/head.jsp"%>
        <pre><%=handler.getFrontierReport().replaceAll("<","&lt;")%></pre>
<%@include file="/include/foot.jsp"%>
