/*
 * Created on Jul 8, 2003
 *
 */
package org.archive.crawler.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpRecoverableException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderGroup;


/**

 *
 *  This class allows the caller to create a dummy GetMethod object,
 *  inserting data so that later processors can be led to think that the 
 *  object was actually created by a real http get request.
 *
 * @author Parker Thompson
 */
public class OfflineGet extends GetMethod {

	// point the response body at a file, rather than towards the network
	protected File responseBody = null;
	
	// if we're to modify this we need it to override (private in the super)
	protected HeaderGroup responseHeaders = new HeaderGroup();

	/* (non-Javadoc)
	 * @see org.apache.commons.httpclient.HttpMethod#execute(org.apache.commons.httpclient.HttpState, org.apache.commons.httpclient.HttpConnection)
	 */
	public int execute(HttpState arg0, HttpConnection arg1)
		throws
			HttpException,
			HttpRecoverableException,
			IOException,
			NullPointerException {
		// TODO Auto-generated method stub
		return super.execute(arg0, arg1);
	}
	
	/**
	 * Set the response body to read from a given file.
	 */
	public void setResponseBody(File f){
		responseBody = f;
	}
	
	/**
	 * Set the response body to read from a given file.
	 */
	public void setResponseBody(String s){
			responseBody = new File(s);
	}
	
	/**
	 * Set a response header of your choice.
	 */
	public void setResponseHeader(String header, String value){
		Header h = new Header(header, value);
		
		responseHeaders.addHeader(h);
	}
	
	/**
	 * Return our overridden repsonse headers.
	 */
	public Header[] gerResponseHeaders(){
		return responseHeaders.getAllHeaders();
	}
	
	/**
	 * Return our overridden response header.
	 */
	public Header getResponseHeader(String s){
		// return the first header matching their request
		return responseHeaders.getHeaders(s)[0];
	}
	
	/**
	 * Return an open stream for reading the response body
	 * from a file on the filesystem.
	 */
	public InputStream getResponseBodyAsStream(){
		try{
			return new FileInputStream(responseBody);	
		}catch(IOException e){
			return null;
		}
	}

}
