<html>
	<head>
	</head>
	<body>
		<form name="frmProfiles" action="simplerequest.jsp" method="post">
		a
		<table border="0" cellspacing="0" cellpadding="0">
			<tr>
				<td colspan="2">
					<strong>SIMPLE CRAWL REQUEST</strong>
				</td>
			</tr>
			<tr>
				<td>
					<strong>Profile:</strong>
				</td>
				<td>
					<%
						String sProfile = request.getParameter("cboProfile");
						if(sProfile == null)
						{
							sProfile = "1";
						}
					%>
					<select name="cboProfile" onChange="document.frmProfiles.submit()">
						<option value="1" <%=(sProfile.equals("1")?"selected":"")%>>Page</option>
						<option value="2" <%=(sProfile.equals("2")?"selected":"")%>>Page+1</option>
						<option value="3" <%=(sProfile.equals("3")?"selected":"")%>>Path</option>
						<option value="4" <%=(sProfile.equals("4")?"selected":"")%>>Host</option>
						<option value="5" <%=(sProfile.equals("5")?"selected":"")%>>Domain</option>
						<option value="-1" <%=(sProfile.equals("-1")?"selected":"")%>>Manually configured</option>
					</select>
				</td>
			</tr>
			<%
				if(sProfile.equals("-1"))
				{
			%>
					<tr>
						<td valign="top">
							<strong>Crawl mode:</strong>
						</td>
						<td>
							<select name="cboFilterMode">
								<option value="domain">Path</option>
								<option value="domain">Host</option>
								<option value="domain">Domain</option>
								<option value="domain">Broad</option>
							</select>
						</td>
					</tr>
					<tr>
						<td valign="top">
							<strong>Max embed depth:</strong>
						</td>
						<td>
							<input> (max value: 8)
						</td>
					</tr>
					<tr>
						<td valign="top">
							<strong>Max link depth:</strong>
						</td>
						<td>
							<input> (max value: 8)
						</td>
					</tr>
					<tr>
						<td valign="top">
							<strong>Max run time (min):</strong>
						</td>
						<td>
							<input> (max value: 1,800 min)
						</td>
					</tr>
					<tr>
						<td valign="top">
							<strong>Max number of documents:</strong>
						</td>
						<td>
							<input> (max value: 100,000)
						</td>
					</tr>
					<tr>
						<td valign="top">
							<strong>Max amount of data (Mb):</strong>
						</td>
						<td>
							<input> (max value: 10,000 Mb)
						</td>
					</tr>
			<%
				}
				else
				{
					out.println("<tr><td></td><td>");
					switch(Integer.parseInt(sProfile))
					{
						case 1 : out.println("Description of a Page crawl");break;
						case 2 : out.println("Description of a Page+1 crawl");break;
						case 3 : out.println("Description of a Path crawl");break;
						case 4 : out.println("Description of a Host crawl");break;
						case 5 : out.println("Description of a Domain crawl");break;
					}
					out.println("</td></tr>");
				}
			%>
			<tr>
				<td valign="top">
					<strong>Seeds :</strong>
				</td>
				<td>
					<textarea name="seeds" wrap="off" cols="48" rows="8">http://archive.org</textarea>
				</td>
			</tr>
			<tr>
				<td colspan="2" align="right">
					<input type="button" value="Submit job">
				</td>
			</tr>
		</table>
		</form>
	</body>
</html>