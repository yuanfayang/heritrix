<font color="red">
<h4>Status Message: 
<center>
<% 
String mesg = request.getParameter("message");
if( !mesg.equals("null")) {
 	out.print(mesg);
}else{
	out.print("Welcome");
}
%>
</center>
</h4>
</font>
