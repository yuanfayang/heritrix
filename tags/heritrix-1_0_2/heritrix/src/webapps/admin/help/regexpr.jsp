<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%
	String title = "Help";
	int tab = 6;
%>

<%@include file="/include/head.jsp"%>

<p>
	<b>Heritrix online help:</b> Regular expressions in java
<p>
<table width="600">
	<tr>
		<td>
			All regular expressions used by Heritrix are Java regular expressions.
			<p>
			Java regular expressions differ from those used in Perl, for example, in 
			several ways. For detailed info on Java regular expressions see the 
			Java API for <code>java.util.regex.Pattern</code> on Sun's homepage 
			(<a href="http://java.sun.com">java.sun.com</a>). 
			<p>
			For API of Java SE v1.4.2 see
			<a href="http://java.sun.com/j2se/1.4.2/docs/api/index.html">
			http://java.sun.com/j2se/1.4.2/docs/api/index.html</a>,
			it is recommended you lookup the API for the version of Java that is
			being used to run Heritrix.
		</td>
	</tr>
</table>

<%@include file="/include/foot.jsp"%>