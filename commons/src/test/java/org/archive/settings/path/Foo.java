/**
 * 
 */
package org.archive.settings.path;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.Path;

/**
 * Test module #1.  Contains only simple types.
 * 
 * @author pjack
 */
public class Foo {

    public static enum Zoo {
        DOG, CAT, MOOSE, SQUIRREL
    }
    
    final public static Key<Boolean> ZERO = Key.make(false);
    
    final public static Key<Byte> ONE = Key.make((byte)1);
    
    final public static Key<Character> TWO = Key.make('2');
    
    final public static Key<Double> THREE = Key.make(3.0);
    
    final public static Key<Float> FOUR = Key.make((float)4.0);
    
    final public static Key<Integer> FIVE = Key.make(5);
    
    final public static Key<Long> SIX = Key.make(6L);
    
    final public static Key<Short> SEVEN = Key.make((short)7);
    
    final public static Key<BigInteger> EIGHT = Key.make(new BigInteger("8"));
    
    final public static Key<BigDecimal> NINE = Key.make(new BigDecimal("9.0"));
    
    final public static Key<String> TEN = Key.make("ten");
    
    final public static Key<Pattern> ELEVEN = Key.make(Pattern.compile("^11$"));
    
    final public static Key<Path> TWELVE = Key.make(new Path("/12"));
    
    final public static Key<Zoo> MOOSE = Key.make(Zoo.MOOSE);

    private String name;
    
    public Foo() {
        
    }
    
    
    public Foo(String name) {
        this.name = name;
    }
    
    public String toString() {
        return name;
    }
    
    
    static {
        KeyManager.addKeys(Foo.class);
    }
}
