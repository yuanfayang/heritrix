package org.archive.crawler2.settings;


import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.archive.state.Key;


/**
 * A bundle of sheets.
 * 
 * @author pjack
 */
public class SheetBundle extends Sheet {


    /** 
     * The sheets contained in this bundle.
     */
    private List<Sheet> sheets;


    /**
     * Constructor.
     * 
     * @param manager
     * @param sheets
     */
    SheetBundle(SheetManager manager, Collection<Sheet> sheets) {
        super(manager);
        this.sheets = new CopyOnWriteArrayList<Sheet>(sheets);
    }


    /**
     * Returns the first non-null value returned by this bundle's list of
     * sheets.  The sheets are consulted in order starting at the beginning
     * of the list.  If any particular sheet contains a non-null value for
     * the given processor/key combination, then that value is returned and
     * any remaining sheets in the bundle are ignored.
     */
    public <T> T get(Object processor, Key<T> key) {
        for (Sheet sheet: sheets) {
            T result = sheet.get(processor, key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    
    public <T> Resolved<T> resolve(Object processor, Key<T> key) {
        Resolved<T> r = resolve(this, processor, key);
        if (r == null) {
            return resolveDefault(processor, key);
        } else {
            return r;
        }
    }
    
    
    private <T> Resolved<T> resolve(SheetBundle bundle, Object processor, Key<T> key) {
        for (Sheet sheet: bundle.getSheets()) {
            if (sheet instanceof SheetBundle) {
                SheetBundle sb = (SheetBundle)sheet;
                Resolved<T> r = resolve(sb, processor, key);
                if (r != null) {
                    return r;
                }
            } else {
                SingleSheet ss = (SingleSheet)sheet;
                T value = ss.get(processor, key);
                if (value != null) {
                    return new Resolved<T>(ss, processor, key, value);
                }
            }
        }
        return null;
    }
    
    /**
     * Returns the list of sheets contained in this bundle.
     * 
     * @return  the list of sheets
     */
    public List<Sheet> getSheets() {
        return sheets;
    }


}
