/* Copyright (C) 2003 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * UriProcessingFormatter.java
 * Created on Jun 10, 2003
 *
 * $Header$
 */
package org.archive.crawler.io;

import java.text.DecimalFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.util.ArchiveUtils;

/**
 * Formmatter for 'crawl.log'. Expects completed CrawlURI as parameter.  
 *   
 * @author gojomo
 *
 */
public class UriProcessingFormatter extends Formatter implements CoreAttributeConstants {
	static String NA = ".";
	static DecimalFormat STATUS_FORMAT = new DecimalFormat("-####");
	static DecimalFormat LENGTH_FORMAT = new DecimalFormat("#,##0");
	
	/* (non-Javadoc)
	 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	 */
	public String format(LogRecord lr) {
		CrawlURI curi = (CrawlURI) lr.getParameters()[0];

		String length = NA;
		String mime = NA;
		String uri = curi.getUURI().getUriString();
		if ( curi.getAList().containsKey(A_HTTP_TRANSACTION)) {
			GetMethod get = (GetMethod) curi.getAList().getObject(A_HTTP_TRANSACTION);
				
			if(curi.getContentLength()>=0) {
				length = Long.toString(curi.getContentLength());
			} else if (curi.getContentSize()>0) {
				length = Long.toString(curi.getContentSize());
			}
			
			if (get.getResponseHeader("Content-Type")!=null) {
				mime = get.getResponseHeader("Content-Type").getValue();
			}
		} else {
			if (curi.getContentSize()>0) {
				length = Long.toString(curi.getContentSize());
 
			}
			if (curi.getContentType() != null) {
				mime = curi.getContentType();
			} 
		}
		long time;
		String duration;
		if(curi.getAList().containsKey(A_FETCH_COMPLETED_TIME)) {
			time = curi.getAList().getLong(A_FETCH_COMPLETED_TIME);
			duration = Long.toString(time-curi.getAList().getLong(A_FETCH_BEGAN_TIME));
		} else {
			time = System.currentTimeMillis();
			duration = NA;
		}
		
		
		Object via = curi.getVia();
		if (via instanceof CandidateURI) {
			via = ((CandidateURI)via).getUURI().getUriString();
		}
		if (via instanceof UURI) {
			via = ((UURI)via).getUriString();
		}
		
		// allow get to be GC'd
		curi.getAList().remove(A_HTTP_TRANSACTION);

		return ArchiveUtils.get17DigitDate(time)
			+ " "
			+ ArchiveUtils.padTo(curi.getFetchStatus(),5)
			+ " "
			+ ArchiveUtils.padTo(length,10)
			+ " "
			+ "#"
			+ curi.getThreadNumber()
			+ " "
			+ uri
			+ " "
			+ duration
			+ " "
			+ mime
			+ "\n"
			+ "  "
			+ curi.getPathFromSeed()
			+ " "
			+ via
			+ "\n";
	}

}


