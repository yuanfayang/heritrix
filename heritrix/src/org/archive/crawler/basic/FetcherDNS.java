/*
 * SimpleDNSFetcher.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Processor;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.FindServer;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;
import org.xbill.DNS.dns;


/**
 * @author gojomo
 *
 */
public class FetcherDNS extends Processor implements CoreAttributeConstants, FetchStatusCodes {
 	
 	// set to false for performance, true if your URIs will contain useful type/class info (usually they won't)
	public static final boolean DO_CLASS_TYPE_CHECKING = true;
	
	public static int MAX_DNS_FETCH_ATTEMPTS = 3;

	// defaults
 	private short ClassType = DClass.IN;
 	private short TypeType = Type.A;
 	
 	protected InetAddress serverInetAddr = null;

  	public void initialize(CrawlController c){
  		super.initialize(c);
  		
  		// lookup nameserver
		String nameServer = FindServer.server();
		
		try{
			// if we're local get something more useful than the loopback
			if(nameServer.equals("127.0.0.1")){
				serverInetAddr = InetAddress.getLocalHost();	
			}else{
				serverInetAddr = InetAddress.getByName(nameServer);
			}
		}catch(UnknownHostException e){
			e.printStackTrace();
		}	
  	}


	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	protected void innerProcess(CrawlURI curi) {
		
		Record[] rrecordSet = null; 		// store retrieved dns records
		long now; 									// the time this operation happened
		CrawlHost targetHost = null;
		String DnsName = parseTargetDomain(curi);	
			
		// TODO this should deny requests for non-dns URIs, for now this will figure out 'http' requests too
		if(!curi.getUURI().getUri().getScheme().equals("dns")) {
			// only handles dns
			return;
		}
		
		// make sure we're in "normal operating mode", e.g. a cache + controller exist to assist us
		if(controller != null && controller.getHostCache() != null){
			targetHost = controller.getHostCache().getHostFor(DnsName);
		
		// standalone operation (mostly for test cases/potential other uses)
		}else{
			targetHost = new CrawlHost(DnsName);
		}
		
		// if it's an ip no need to do a lookup
		if (DnsName.matches("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")) {
			try {
				String[] octets = DnsName.split("\\.");

				targetHost.setIP(
					InetAddress.getByAddress(
						DnsName,
						new byte[] {
							(byte) (new Integer(octets[0])).intValue(),
							(byte) (new Integer(octets[1])).intValue(),
							(byte) (new Integer(octets[2])).intValue(),
							(byte) (new Integer(octets[3])).intValue()})
				);

			} catch (UnknownHostException e) {
				// this should never happen as a dns lookup is not made
				e.printStackTrace();
			}
			
			// don't expire IPs
			targetHost.setIpExpiresFromTTL(Long.MAX_VALUE);
			curi.setFetchStatus(1);
			
			// no need to continue with the lookup
			curi.cancelFurtherProcessing();
		}
		
		
		
		// we've successfully looked up this host, don't do it again
		if(targetHost.hasBeenLookedUp() && targetHost.getIP() != null){
			return;
		}
		
		if(curi.getFetchAttempts() >= MAX_DNS_FETCH_ATTEMPTS){
			curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
		}
					
		// give it a go    
		curi.incrementFetchAttempts();

		//TODO add support for type and class specifications in query string, for now always use defaults
		/* if(SimpleDNSFetcher.DO_CLASS_TYPE_CHECKING){
		} */

		// add the nameserver as the curi's ip, to indicate the machine that did the lookup
		curi.getHost().setIP(serverInetAddr);
		
		now = System.currentTimeMillis();
		curi.getAList().putString(A_CONTENT_TYPE, "text/dns");
		curi.getAList().putLong(A_FETCH_BEGAN_TIME, now);
			
		// try to get the records for this host (assume domain name)
		rrecordSet = dns.getRecords(DnsName, TypeType, ClassType);
		targetHost.setHasBeenLookedUp();
		
		// on failure check if it's an ip (silly looking but likely more 
		// effecient than using a regexp to examine the uri for every call
		if(rrecordSet==null){
			rrecordSet = dns.getRecordsByAddress(DnsName, TypeType);
		}
		if(rrecordSet != null){

			curi.setFetchStatus(1);
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

		}else{
				curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
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
