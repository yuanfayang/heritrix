<%
	/**
	 * An include file that handles the sub navigation of a 'job' page. 
	 * Include where the sub navigation should be displayed.
	 *
	 * The following variables must exist prior to this file being included:
	 *
	 * String theJob - The CrawlJob being manipulated.
	 * int jobtab - Which to display as 'selected'.
	 *          0 - Modules
	 *          1 - Filters
	 *          2 - Settings
	 *          3 - Overrides
	 *
	 * @author Kristinn Sigurdsson
	 */
%>
	<table cellspacing="0" cellpadding="0">
		<tr>
			<td bgcolor="#0000FF" height="1">
			</td>
		</tr>
		<tr>
			<td>
				<table cellspacing="0" cellpadding="0">
					<tr>
						<td nowrap>
							<b><%=theJob.isProfile()?"Profile":"Job"%> <%=theJob.getJobName()%>:</b>
						</td>
						<td class="tab_seperator">
						</td>
						<% if(theJob.isRunning()){ %>
							<td class="tab_inactive" nowrap>
								<a href="javascript:alert('Can not edit modules on running jobs!')" class="tab_text_inactive">Modules</a>
							</td>
						<% } else { %>
							<td class="tab<%=jobtab==0?"_selected":""%>" nowrap>
								<a href="javascript:doGoto('/admin/jobs/modules.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==0?"_selected":""%>">Modules</a>
							</td>
						<% } %>
						<td class="tab_seperator">
						</td>
						<td class="tab<%=jobtab==1?"_selected":""%>" nowrap>
							<a href="javascript:doGoto('/admin/jobs/filters.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==1?"_selected":""%>">Filters</a>
						</td>
						<td class="tab_seperator">
						</td>
						<td class="tab<%=jobtab==2?"_selected":""%>" nowrap>
							<a href="javascript:doGoto('/admin/jobs/configure.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==2?"_selected":""%>">Settings</a>
						</td>
						<td class="tab_seperator">
						</td>
						<td class="tab<%=jobtab==3?"_selected":""%>" nowrap>
							<a href="javascript:doGoto('/admin/jobs/per/overview.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==3?"_selected":""%>">Overrides</a>
						</td>
						<td class="tab_seperator">
						</td>
						<td class="tab">
							<a href="javascript:doSubmit()" class="tab_text"><%=theJob.isNew()?"Submit job":"Finished"%></a>
						</td>
						<td class="tab_seperator">
						</td>
					</tr>
				</table>
			</td>
		</tr>
		<tr>
			<td bgcolor="#0000FF" height="1">
			</td>
		</tr>
	</table>