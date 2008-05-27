<%
String favicon = (String)application.getAttribute("favicon");
if ((favicon == null) || (favicon.length() == 0)) {
    favicon = "h.ico";
}
%>

<link rel="stylesheet"  href="<%=request.getContextPath()%>/css/heritrix.css">
<link rel="icon" href="<%=request.getContextPath()%>/images/<%=favicon%>" type="image/x-icon" />
<link rel="shortcut icon" href="<%=request.getContextPath()%>/images/<%=favicon%>" type="image/x-icon" />

<script src="<%=request.getContextPath()%>/js/jquery-1.1.4.js"></script>
