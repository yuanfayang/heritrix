/*
 * SimpleDNSFetcher.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.datamodel.CrawlHost;
import org.xbill.DNS.*;


/**
 * @author gojomo
 *
 */
public class SimpleDNSFetcher extends Processor implements org.archive.crawler.datamodel.CoreAttributeConstants {
 	
 	// set to false for performance, true if your URIs will contain useful type/class info (usually they won't)
	public static final boolean DO_CLASS_TYPE_CHECKING = true;

	// defaults
 	private short ClassType = DClass.IN;
 	private short TypeType = Type.A;

	String DnsName = null;		// store the name of the host we wish to look up

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void process(CrawlURI curi) {
		super.process(curi);
		
		Record[] rrecordSet = null; 						// store retrieved dns records
		long now; 													// the time this operation happened
				
				
		// TODO this should deny requests for non-dns URIs, for now this will figure out 'http' requests too
		if(!curi.getUURI().getUri().getScheme().equals("dns")) {
			
			DnsName = curi.getUURI().getUri().getAuthority();
			
			// only handles dns
			//return;
			// for now handle html as well

		}else{	
			DnsName = parseTargetDomain(curi);
		}
		
		//TODO add support for type and class specifications in query string, for now always use defaults
		/* if(SimpleDNSFetcher.DO_CLASS_TYPE_CHECKING){
		} */

		// do the lookup and store the results back to the curi
		now = System.currentTimeMillis();
		rrecordSet = dns.getRecords(DnsName, TypeType, ClassType);
		curi.getAList().putString(A_CONTENT_TYPE, DNS_CONTENT_TYPE);
		curi.getAList().putObject(RRECORD_SET_LABEL, rrecordSet);
		curi.getAList().putLong(FETCH_BEGAN_AT, now);

		// get TTL and IP info from the first A record (there may be multiple, e.g. www.washington.edu)
		// then update the CrawlHost
		for(int i=0;i < rrecordSet.length; i++){

			if(rrecordSet[i].getType() != Type.A)
				continue;
			
			ARecord AsA 	= (ARecord)rrecordSet[i];
			CrawlHost h 		= curi.getHost();
			
			h.setIP(AsA.getAddress());
			h.setIpExpires( (long)AsA.getTTL() + now);
			
			break; 	// only need to process one record
		}
	}
	
	// TODO should throw some sort of exception if it's passed
	// a non-dns uri.  currently assumes the caller knows what he/she is doing
	public static String parseTargetDomain(CrawlURI curi){

		// should look like "dns:" [ "//" hostport "/" ] dnsname [ "?" dnsquery ]
		String uri = curi.getURIString();

		// if it's not a dns uri
		if(!uri.startsWith("dns:")){
			return null;
		}
		
		uri = uri.substring(4);						// drop "dns:" prefix
			
		if(uri.startsWith("//")){						// drop hostport
			uri = uri.replaceFirst("//.+/", "");
		}
		
		// drop query string
		if(uri.indexOf("?") > -1){				
			uri = uri.substring(0, uri.indexOf("?"));
		}
		
		return uri;
	}
}
