/**
 * 
 */
package org.archive.settings.path;

import org.archive.state.KeyManager;

/**
 * @author pjack
 *
 */
public class Baz extends Foo {

    
    public Baz() {
    }
    
    
    public Baz(String name) {
        super(name);
    }
    
    
    static {
        KeyManager.addKeys(Baz.class);
    }
}
