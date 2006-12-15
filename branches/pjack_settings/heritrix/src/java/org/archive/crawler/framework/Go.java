package org.archive.crawler.framework;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.archive.settings.MemorySheetManager;
import org.archive.settings.SingleSheet;
import org.archive.settings.file.SheetFileReader;
import org.archive.settings.path.PathChange;
import org.archive.settings.path.PathChanger;

public class Go {

    
    public static void main(String args[]) throws Exception {
        MemorySheetManager mgr = new MemorySheetManager();
        
        FileReader fr = new FileReader("/Users/pjack/Desktop/settings.txt");
        SheetFileReader sfr = new SheetFileReader(fr);
        PathChanger changer = new PathChanger();
        changer.change(mgr.getDefault(), sfr);
        
        /*
        CrawlController cc = new CrawlController(mgr);
        mgr.setRoot(cc);
        
        SingleSheet def = mgr.getDefault();
        CrawlOrder order = new CrawlOrder(cc);
        def.set(cc, CrawlController.ORDER, order);

        Map<String,String> hh = new HashMap<String,String>();
        hh.put("from", "test@test.org");
        hh.put("user-agent", "blah (+http://www.archive.org)");        
        def.set(order, CrawlOrder.HTTP_HEADERS, hh);
        
        List<StatisticsTracking> loggers = new ArrayList<StatisticsTracking>();
        def.set(order, CrawlOrder.LOGGERS, loggers);

        cc.initialize();
        cc.requestCrawlStart();
        */
    }
    
    
}
