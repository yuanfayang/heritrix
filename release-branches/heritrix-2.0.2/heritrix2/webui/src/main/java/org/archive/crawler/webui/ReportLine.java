package org.archive.crawler.webui;

public class ReportLine implements Comparable<ReportLine>{
    public String legend;
    public long numberOfURIS;
    public long bytes;
    public long lastActive;
    
    long orderby;
    
    public int compareTo(ReportLine o) {
        if(this.orderby!=o.orderby){
            if(this.orderby>o.orderby){
                return -1;
            } else {
                return 1;
            }
        }
        // Are ordering by name or URI count equal            
        return this.legend.compareTo(o.legend);
    }
}
