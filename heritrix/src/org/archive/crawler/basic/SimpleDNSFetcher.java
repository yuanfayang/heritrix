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
public class SimpleDNSFetcher extends Processor {
 	
    // when storing dns info in a CrawlURI's AList use this label
	public static final String RRECORDS_ALIST_LABEL 				= "dnsrecords";
	public static final String DNSFETCH_TIMESTAMP_LABEL	= "dns_fetch_timestamp";	
	
	// set to false for performance, true if your URIs will contain useful type/class info (usually they won't)
	public static final boolean DO_CLASS_TYPE_CHECKING = true;

	// defaults
 	private short ClassType = DClass.IN;
 	private short TypeType = Type.A;

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void process(CrawlURI curi) {
		super.process(curi);
		
		Record[] RRecordSet = null; 					// store retrieved dns records
		long now = System.currentTimeMillis(); 	// the time this operation happened
				
		if(!curi.getUURI().getUri().getScheme().equals("dns")) {
			// only handles dns
			return;
		}
		
		// get the host we want to look up
		//TODO currently the dns uri spec uses inconsistent syntax, this means we currently only support the syntax "dns:dnsname", resolve this issue with Joseffson, or support both semantic structures
		String DnsName = curi.getUURI().getUri().getSchemeSpecificPart();
		
		//TODO add support for type and class specifications in query string, for now always use defaults
		/* if(SimpleDNSFetcher.DO_CLASS_TYPE_CHECKING){
		} */

		// do the lookup and store the results back to the curi
		RRecordSet = dns.getRecords(DnsName, TypeType, ClassType);
		curi.getAList().putObject(SimpleDNSFetcher.RRECORDS_ALIST_LABEL, RRecordSet);
		curi.getAList().putLong(SimpleDNSFetcher.DNSFETCH_TIMESTAMP_LABEL, now);

		// get TTL and IP info from the first A record (there may be multiple, e.g. www.washington.edu)
		// then update the CrawlHost
		for(int i=0;i < RRecordSet.length; i++){

			if(RRecordSet[i].getType() != Type.A)
				continue;
			
			ARecord AsA = (ARecord)RRecordSet[i];
			
			CrawlHost h = curi.getHost();
			h.setIP(AsA.getAddress());

			h.setIpExpires( (long)AsA.getTTL() + now);
			
			// only need to process one record
			break;
		}
	}
}
