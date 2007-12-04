package org.archive.i18n;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import junit.framework.TestCase;

public class LocaleCacheTest extends TestCase {

    
    static Map<String,String> ENGLISH;
    static Map<String,String> CANADA;
    static Map<String,String> JAPANESE;
    static Map<String,String> BORK_BORK_BORK;


    static {
        ENGLISH = new HashMap<String,String>();
        ENGLISH.put("hello", "Hello, world!");
        String s = "Most modern calendars mar the sweet simplicity of our " +
        "lives by reminding us that each day that passes is the anniversary " +
        "of some perfectly uninteresting event.";
        ENGLISH.put("multiline", s);
        
        CANADA = new HashMap<String,String>();
        CANADA.put("hello", "Hello world, eh?");
        s = "Most modern calendars mar the sweet simplicity of our " +
        "lives by reminding us that each day that passes is the anniversary " +
        "of some perfectly uninteresting event, eh?";
        CANADA.put("multiline", s);

        BORK_BORK_BORK = new HashMap<String,String>();
        s = "Yorn desh born, der ritt de gitt der gue, " +
        "orn desh, dee born desh, de umn, b\u00F8rk b\u00F8rk b\u00F8rk!";
        BORK_BORK_BORK.put("hello", s);
        s = "Must mudern celenders mer zee sveet seemplicity " +
        "ooff oooor leefes by remeending us thet iech dey thet pesses is " +
        "zee unneefersery ooff sume-a perffectly uneenteresting ifent, " +
        "b\u00F8rk b\u00F8rk b\u00F8rk!";
        BORK_BORK_BORK.put("multiline", s);
                
        JAPANESE = new HashMap<String,String>();
        s = "\u3053\u3093\u306b\u3061\u306f\u3001\u4e16\u754c!";
        JAPANESE.put("hello", s);
        s = "\u307b\u3068\u3093\u3069\u306e\u73fe\u4ee3\u30ab\u30ec" +
        "\u30f3\u30c0\u30fc\u306f\u79c1\u9054\u306b\u305d\u308c\u6bce\u65e5" +
        "\u601d\u3044\u51fa\u3055\u305b\u308b\u3053\u3068\u306b\u3088\u3063" +
        "\u3066\u79c1\u9054\u306e\u751f\u547d\u306e\u7518\u3044\u7c21\u6613" +
        "\u6027\u3092\u50b7\u3064\u3051\u308b\u30d1\u30b9\u304c\u0020\u5b8c" +
        "\u5168\u306b\u9000\u5c48\u306a\u3067\u304d\u4e8b\u306e\u8a18\u5ff5" +
        "\u65e5\u3067\u3042\u308b\u3053\u3068\u3002";
        JAPANESE.put("multiline", s);
    }
    
    
    public void testSearchOrder() {
        try {
            xtestSearchOrder2();
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
    
    private void xtestSearchOrder2() {
        // Test language/country/variant that does exist.
        Locale locale = new Locale("en", "US", "borkborkbork");
        Map<String,String> props = LocaleCache.load(LocaleCache.class, locale);
        assertEquals(BORK_BORK_BORK, props);
        
        // Test language/country/variant that does not exist, but the
        // language/country does.
        locale = new Locale("en", "CA", "sarcastic");
        props = LocaleCache.load(LocaleCache.class, locale);
        assertEquals(CANADA, props);
        
        // Test language/country/variant that does not exist, and neither
        // does language/country, but language-by-itself does exist.
        locale = new Locale("jp", "JP", "haiku");
        props = LocaleCache.load(LocaleCache.class, locale);
        assertEquals(JAPANESE, props);
        
        // Test language/country that does exist.
        locale = new Locale("en", "CA");
        props = LocaleCache.load(LocaleCache.class, locale);
        assertEquals(CANADA, props);
        
        // Test language/country that does not exist, but 
        // language-by-itself does exist.
        locale = new Locale("jp", "JP");
        props = LocaleCache.load(LocaleCache.class, locale);
        assertEquals(JAPANESE, props);
        
        // Test language that does exist.
        locale = new Locale("jp");
        props = LocaleCache.load(LocaleCache.class, locale);
        assertEquals(JAPANESE, props);
        
        // From this point on, the test assumes that the JVM's default 
        // locale is something in English.  That might not be the case, so
        // check here.
        if (!Locale.getDefault().getLanguage().equals("en")) {
            return;
        }
        
        // Test that when a language/country/variant does not exist, nor 
        // the language/country, nor even the language-by-itself, that the
        // default locale is returned.
        locale = new Locale("de", "DE", "schnitzelpusskronkengesheitmier");
        props = LocaleCache.load(LocaleCache.class, locale);
        assertEquals(ENGLISH, props);
        
        // Test that when a language/country does not exist, nor the
        // language-by-itself, that the default locale is returned.
        locale = new Locale("de", "DE");
        props = LocaleCache.load(LocaleCache.class, locale);
        assertEquals(ENGLISH, props);
        
        // Test that when a language-by-itself does not exist, that the
        // default locale is returned.
        locale = new Locale("de");
        props = LocaleCache.load(LocaleCache.class, locale);
        assertEquals(ENGLISH, props);
    }


}
