<%@include file="/include/secure.jsp"%>
<%@include file="/include/handler.jsp"%>

<%
	String title = "About Heritrix";
	int tab = 5;
%>

<%@include file="/include/head.jsp"%>

	<pre>
	
	Heritrix - version <%=Heritrix.getVersion()%>
	
	Copyright (C) 2003 Internet Archive.
	
	Heritrix is free software; you can redistribute it and/or modify
	it under the terms of the GNU Lesser Public License as published by
	the Free Software Foundation; either version 2.1 of the License, or
	any later version.
	
	Heritrix is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser Public License for more details.
	
	You should have received a copy of the GNU Lesser Public License
	along with Heritrix; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
	
	For more information see:
	<a target="_blank" href="http://crawler.archive.org/">http://crawler.archive.org/</a>
	</pre>

<%@include file="/include/foot.jsp"%>
