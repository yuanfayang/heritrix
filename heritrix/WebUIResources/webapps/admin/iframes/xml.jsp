<%@include file="/include/secure.jsp"%>
<%@ page import="java.io.*" %>

<%
	String inputFile = request.getParameter("file");

	if(inputFile == null)
	{
		out.println("No file");
		return;
	}
	
	response.setContentType("text/html");
	
	FileReader fr = new FileReader(inputFile);
	BufferedReader bf = new BufferedReader(fr, 8192);
	
	String line = null;
	while ((line = bf.readLine()) != null) {
		out.println(line);
	}

%>