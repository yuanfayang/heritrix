		<table border="0">
			<tr>
				<td>
					Crawl name:
				</td>
				<td>
					<input name="<%=handler.XP_CRAWL_ORDER_NAME%>" value="<%=crawlOrder.getStringAt(handler.XP_CRAWL_ORDER_NAME)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Comment:
				</td>
				<td>
					<input name="<%=handler.XP_CRAWL_COMMENT%>" value="<%=crawlOrder.getStringAt(handler.XP_CRAWL_COMMENT)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Maximum link hops:
				</td>
				<td>
					<input name="<%=handler.XP_MAX_LINK_HOPS%>" value="<%=crawlOrder.getStringAt(handler.XP_MAX_LINK_HOPS)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Maximum trans hops:
				</td>
				<td>
					<input name="<%=handler.XP_MAX_TRANS_HOPS%>" value="<%=crawlOrder.getStringAt(handler.XP_MAX_TRANS_HOPS)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Mode:
				</td>
				<td>
					<select name="<%=handler.XP_CRAWL_MODE%>">
						<option value="broad" <%=crawlOrder.getStringAt(handler.XP_CRAWL_MODE).equals("broad")?"selected":""%>>Broad</option>
						<option value="domain" <%=crawlOrder.getStringAt(handler.XP_CRAWL_MODE).equals("domain")?"selected":""%>>Domain</option>
						<option value="host" <%=crawlOrder.getStringAt(handler.XP_CRAWL_MODE).equals("host")?"selected":""%>>Host</option>
						<option value="path" <%=crawlOrder.getStringAt(handler.XP_CRAWL_MODE).equals("path")?"selected":""%>>Path</option>
					</select>
				</td>
			</tr>
			<tr>
				<td>
					Disk path:
				</td>
				<td>
					<input name="<%=handler.XP_DISK_PATH%>" value="<%=crawlOrder.getStringAt(handler.XP_DISK_PATH)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					ARC Prefix:
				</td>
				<td>
					<input name="<%=handler.XP_ARC_PREFIX%>" value="<%=crawlOrder.getStringAt(handler.XP_ARC_PREFIX)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					ARC Use compression:
				</td>
				<td>
					<input name="<%=handler.XP_ARC_COMPRESSION_IN_USE%>" value="<%=crawlOrder.getStringAt(handler.XP_ARC_COMPRESSION_IN_USE)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Max ARC size:
				</td>
				<td>
					<input name="<%=handler.XP_MAX_ARC_SIZE%>" value="<%=crawlOrder.getStringAt(handler.XP_MAX_ARC_SIZE)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					User agent:
				</td>
				<td>
					<input name="<%=handler.XP_HTTP_USER_AGENT%>" value="<%=crawlOrder.getStringAt(handler.XP_HTTP_USER_AGENT)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					From:
				</td>
				<td>
					<input name="<%=handler.XP_HTTP_FROM%>" value="<%=crawlOrder.getStringAt(handler.XP_HTTP_FROM)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Maximum number of toe threads:
				</td>
				<td>
					<input name="<%=handler.XP_MAX_TOE_THREADS%>" value="<%=crawlOrder.getStringAt(handler.XP_MAX_TOE_THREADS)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td>
					Seeds file:
				</td>
				<td>
					<input name="<%=handler.XP_SEEDS_FILE%>" value="<%=crawlOrder.getStringAt(handler.XP_SEEDS_FILE)%>" size="<%=iInputSize%>">
				</td>
			</tr>
			<tr>
				<td valign="top">
					Seeds:
				</td>
				<td>
					<textarea name="<%=handler.XP_SEEDS%>" rows="8" cols="<%=iInputSize%>"><%=crawlOrder.getStringAt(handler.XP_SEEDS)%></textarea>
				</td>
			</tr>
		</table>
