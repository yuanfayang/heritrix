package org.archive.crawler2.settings;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * A list of NamedObjects that ensures all elements have unique names.
 * 
 * @author pjack
 */
public class NamedObjectArrayList extends AbstractList<NamedObject> {

    /** For serialization. */
    private static final long serialVersionUID = 1L;

    /** The delegate list. */
    private ArrayList<NamedObject> delegate;

    /** The names of the elements in the delegate list. */
    private Set<String> names;
    

    /**
     * Constructor.
     */
    public NamedObjectArrayList() {
        this.delegate = new ArrayList<NamedObject>();
        this.names = new HashSet<String>();
    }
    
    
    /**
     * Constructor.
     * 
     * @param c  the elements with which to populate this list
     */
    public NamedObjectArrayList(Collection<NamedObject> c) {
        this.delegate = new ArrayList<NamedObject>(c);
    }
    
    
    @Override
    public int size() {
        return delegate.size();
    }
    
    
    @Override
    public NamedObject get(int index) {
        return delegate.get(index);
    }


    @Override
    public NamedObject set(int index, NamedObject no) {
        String name = no.getName();
        if (get(index).getName().equals(name)) {
            return delegate.set(index, no);
        } 
        
        if (!names.add(no.getName())) {
            throw new IllegalArgumentException(name + " already exists.");
        }
        NamedObject result = delegate.set(index, no);
        names.remove(result.getName());
        names.add(name);
        return result;
    }


    @Override
    public void add(int index, NamedObject no) {
        String name = no.getName();
        if (!names.add(name)) {
            throw new IllegalArgumentException(name + " already exists.");
        }
        delegate.add(index, no);
    }
    
    
    @Override
    public NamedObject remove(int index) {
        NamedObject result = delegate.remove(index);
        names.remove(result.getName());
        return result;
    }
}
