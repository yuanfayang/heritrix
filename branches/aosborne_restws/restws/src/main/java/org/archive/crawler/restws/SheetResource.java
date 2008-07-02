package org.archive.crawler.restws;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class SheetResource extends JobResource {

	private String sheetName;

	public SheetResource() {
		// TODO Auto-generated constructor stub
	}

	public SheetResource(Context context, Request request, Response response) {
		super(context, request, response);
		this.sheetName = (String)request.getAttributes().get("sheet");
	}

}
