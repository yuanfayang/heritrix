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
import org.xbill.DNS.FindServer;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.HostCache;
import org.archive.crawler.datamodel.FetchStatusCodes;

import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * @author gojomo
 *
 */
public class SimpleDNSFetcher extends Processor implements CoreAttributeConstants, FetchStatusCodes {
 	
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
		
		String DnsName = null;				// store the name of the host we wish to look up		
		Record[] rrecordSet = null; 		// store retrieved dns records
		long now; 									// the time this operation happened
				
				
		// TODO this should deny requests for non-dns URIs, for now this will figure out 'http' requests too
		if(!curi.getUURI().getUri().getScheme().equals("dns")) {
			// only handles dns
			return;
		}
		
		// give it a go
		curi.incrementFetchAttempts();

		//TODO add support for type and class specifications in query string, for now always use defaults
		/* if(SimpleDNSFetcher.DO_CLASS_TYPE_CHECKING){
		} */

		// if the dns server hasn't been looked up yet populate it's crawlhost
		if( ! curi.getHost().hasBeenLookedUp() ){
			
			// now we know we've at least tried
			curi.getHost().setHasBeenLookedUp();
			
			String nameServer = FindServer.server();
			InetAddress serverInetAddr = null;
			
			try{
				serverInetAddr = InetAddress.getByName(nameServer);

				// TODO define success status codes
				curi.setFetchStatus(1);
			
			}catch(UnknownHostException e){
				curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
				e.printStackTrace();
			}
			
			curi.getHost().setIP(serverInetAddr);
		}

		// do the lookup and store the results back to the curi
		now = System.currentTimeMillis();
		DnsName = parseTargetDomain(curi);
		rrecordSet = dns.getRecords(DnsName, TypeType, ClassType);
		curi.getAList().putString(A_CONTENT_TYPE, "text/dns");
		curi.getAList().putLong(A_FETCH_BEGAN_TIME, now);

		// get the CrawlHost object for the host we're looking up (not this curi's host, which is the nameserver)
		CrawlHost targetHost = controller.getHostCache().getHostFor(parseTargetDomain(curi));
		targetHost.setHasBeenLookedUp();
		
		if(rrecordSet != null){

			curi.getAList().putObject(A_RRECORD_SET_LABEL, rrecordSet);

			// get TTL and IP info from the first A record (there may be multiple, e.g. www.washington.edu)
			// then update the CrawlHost
			for(int i=0;i < rrecordSet.length; i++){

				if(rrecordSet[i].getType() != Type.A)
					continue;
			
				ARecord AsA 	= (ARecord)rrecordSet[i];
							
				targetHost.setIP(AsA.getAddress());
				targetHost.setIpExpires( (long)AsA.getTTL() + now);

				break; 	// only need to process one record
			}
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
