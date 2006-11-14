package org.archive.crawler2.settings.path;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class Paths {

    final public static Set<Class> SIMPLE_TYPES
    = Collections.unmodifiableSet(new HashSet<Class>(Arrays.asList(
        new Class[] { 
            Boolean.class,
            Byte.class,
            Character.class,
            Double.class,
            Float.class,
            Integer.class,
            Long.class,
            Short.class,
            BigInteger.class,
            BigDecimal.class,
            Pattern.class,
            Date.class,
    })));

    
    private Paths() {
    }

    
    public static boolean isSimple(Class c) {
        if (c.isPrimitive()) {
            return true;
        }
        return SIMPLE_TYPES.contains(c);
    }
    
}
