/*
 * SimpleDNSFetcher.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.fetcher;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Processor;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;
import org.xbill.DNS.dns;


/**
 * @author gojomo
 *
 */
public class FetchDNS extends Processor implements CoreAttributeConstants, FetchStatusCodes {
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.FetcherDNS");
	
 	// set to false for performance, true if your URIs will contain useful type/class info (usually they won't)
	public static final boolean DO_CLASS_TYPE_CHECKING = true;

	// defaults
 	private short ClassType = DClass.IN;
 	private short TypeType = Type.A;
 	
 	protected InetAddress serverInetAddr = null;
 	// protected CrawlServer dnsServer = null;

  	public void initialize(CrawlController c){
  		super.initialize(c);
  		
  		// lookup nameserver
//		String nameServer = FindServer.server();
//		
//		try {
//			// if we're local get something more useful than the loopback
//			if (nameServer.equals("127.0.0.1")) {
//				serverInetAddr = InetAddress.getLocalHost();
//			} else {
//				serverInetAddr = InetAddress.getByName(nameServer);
//			}
//			
//			// create a dns host to attach to dns records
//			dnsServer = new CrawlServer(nameServer);
//			dnsServer.getHost().setIP(serverInetAddr);
//			
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//		}
  	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	protected void innerProcess(CrawlURI curi) {
		
		Record[] rrecordSet = null; 		// store retrieved dns records
		long now; 									// the time this operation happened
		CrawlServer targetServer = null;
		String dnsName = parseTargetDomain(curi);	
			
		if (!curi.getUURI().getUri().getScheme().equals("dns")) {
			// only handles dns
			return;
		}
		
		// curi.setServer(dnsServer);
		
		// make sure we're in "normal operating mode", e.g. a cache + controller exist to assist us
		if (controller != null && controller.getServerCache() != null) {
			targetServer = controller.getServerCache().getServerFor(dnsName);
		} else {
			// standalone operation (mostly for test cases/potential other uses)
			targetServer = new CrawlServer(dnsName);
		}
		
		// if it's an ip no need to do a lookup
		if (dnsName.matches("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")) {
			// ideally this branch would never be reached: no CrawlURI
			// would be created for numerical IPs
			logger.warning("unnecessary DNS CrawlURI created: "+curi);
			
			try {
				String[] octets = dnsName.split("\\.");

				targetServer.getHost().setIP(
					InetAddress.getByAddress(
						dnsName,
						new byte[] {
							(byte) (new Integer(octets[0])).intValue(),
							(byte) (new Integer(octets[1])).intValue(),
							(byte) (new Integer(octets[2])).intValue(),
							(byte) (new Integer(octets[3])).intValue()})
				);
				
//				if(targetHost.getIP() == null){
//					System.out.println("crapped out creating ip address for " + dnsName);
//				}

			} catch (UnknownHostException e) {
				// this should never happen as a dns lookup is not made
			
				e.printStackTrace();
			}
			
			// don't expire numeric IPs
			targetServer.getHost().setIpExpires(Long.MAX_VALUE);
			curi.setFetchStatus(S_DNS_SUCCESS);
			
			// no further lookup necessary
			return;
		}
				
//		if(curi.getFetchAttempts() >= MAX_DNS_FETCH_ATTEMPTS){
//			curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
//			return;
//		}
					
		// give it a go    
		//curi.incrementFetchAttempts();

		//TODO add support for type and class specifications in query string, for now always use defaults
		/* if(SimpleDNSFetcher.DO_CLASS_TYPE_CHECKING){
		} */
		
		now = System.currentTimeMillis();
		curi.getAList().putLong(A_FETCH_BEGAN_TIME, now);
			
		// try to get the records for this host (assume domain name)
		rrecordSet = dns.getRecords(dnsName, TypeType, ClassType);
		
		targetServer.getHost().setHasBeenLookedUp();
		
		if (rrecordSet != null) {
			curi.setFetchStatus(S_DNS_SUCCESS);
			curi.getAList().putString(A_CONTENT_TYPE, "text/dns");
			curi.getAList().putObject(A_RRECORD_SET_LABEL, rrecordSet);

			// get TTL and IP info from the first A record (there may be multiple, e.g. www.washington.edu)
			// then update the CrawlServer
			for (int i = 0; i < rrecordSet.length; i++) {

				if (rrecordSet[i].getType() != Type.A) {
					continue;
				}

				ARecord AsA = (ARecord) rrecordSet[i];
				targetServer.getHost().setIP(AsA.getAddress());
				targetServer.getHost().setIpExpires(1000 * (long) AsA.getTTL() + now);
				break; // only need to process one record
			}
		} else {
			curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
		}
		
		curi.getAList().putLong(A_FETCH_COMPLETED_TIME, System.currentTimeMillis());
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
