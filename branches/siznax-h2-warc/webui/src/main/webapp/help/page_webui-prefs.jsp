<%@ page pageEncoding="UTF-8" %> 
<%@ page import="org.archive.crawler.Heritrix" %>
<html>
<head>
    <%@include file="/include/header.jsp"%>
    <title>Heritrix Change Password</title>
</head>
<body>

<%@include file="/include/nav.jsp"%>

<h1>Change Password</h1>

<form method="POST" action="do_change_password.jsp" accept-charset='UTF-8'>
    
Enter the current password:<br/>
<input type="password" name="old" value="">
    
<p>
Enter the new password:<br/>
<input type="password" name="new1" value="">
    
<p>
Confirm the new password:<br/>
<input type="password" name="new2" value=""> 

<p>
<input type="submit" value="Change Password">
</form>
</body>
</html>
