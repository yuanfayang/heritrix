/**
 * 
 */
package org.archive.settings.path;

import java.util.List;
import java.util.Map;

import org.archive.state.Key;
import org.archive.state.KeyManager;

/**
 * Test class #2.  Contains complex keys.
 * 
 * @author pjack
 */
public class Bar {

    
    final public static Key<Foo> FOO_AUTO = Key.makeAuto(Foo.class);
    
    final public static Key<Foo> FOO = Key.makeNull(Foo.class);
    
    final public static Key<List<Foo>> LIST = Key.makeList(Foo.class);
    
    final public static Key<Map<String,Foo>> MAP = Key.makeMap(Foo.class);
    
    final public static Key<List<String>> SLIST = Key.makeList(String.class);
    
    final public static Key<Map<String,String>> SMAP = 
        Key.makeMap(String.class);
    
    
    static {
        KeyManager.addKeys(Bar.class);
    }
}
