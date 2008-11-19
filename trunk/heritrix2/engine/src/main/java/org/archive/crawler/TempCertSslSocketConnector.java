package org.archive.crawler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.mortbay.jetty.security.SslSocketConnector;

public class TempCertSslSocketConnector extends SslSocketConnector 
{
    private static final Logger logger =
        Logger.getLogger(TempCertSslSocketConnector.class.getName());

    protected static String KEY_ALGORITHM = "RSA";
    protected static String SIGNATURE_ALGORITHM = "SHA1WithRSA";
    protected static int KEY_SIZE = 1024;
    protected static long VALIDITY_DAYS = 365l;

    protected static String OU = "Heritrix";
    protected static String O = "Internet Archive";
    protected static String L = "San Francisco";
    protected static String ST = "California";
    protected static String C = "US";

    // these values aren't allowed to be null, but they really don't matter
    // because the keystore isn't saved
    protected static String ALIAS = "heritrix";
    protected static char[] PASSWORD = "heritrix".toCharArray();
    
    protected String cn = "localhost";
    protected SSLContext sslContext;
    
    // do all the reflection stuff here so we can fail early
    public TempCertSslSocketConnector(String commonName) throws Exception {
        super();
        if (commonName != null) {
            this.cn = commonName;
        }
        
        initSslContext();
    }

    protected void initSslContext() throws Exception {
        logger.info("generating self-signed certificate with " + KEY_SIZE + " bit key (this could take a few moments...)");

        // CertAndKeyGen keypair = new CertAndKeyGen(KEY_ALGORITHM, SIGNATURE_ALGORITHM, getProvider());
        Class<?> keypairClass = Class.forName("sun.security.x509.CertAndKeyGen");
        Constructor<?> keypairConstructor = keypairClass.getConstructor(String.class, String.class, String.class);
        Object keypair = keypairConstructor.newInstance(KEY_ALGORITHM, SIGNATURE_ALGORITHM, getProvider());

        // keypair.generate(KEY_SIZE);
        keypairClass.getMethod("generate", Integer.TYPE).invoke(keypair, KEY_SIZE);

        // X500Name x500Name = new X500Name(cn, OU, O, L, ST, C);
        Class<?> x500NameClass = Class.forName("sun.security.x509.X500Name");
        Constructor<?> x500NameConstructor = x500NameClass.getConstructor(String.class, String.class, String.class, String.class, String.class, String.class);
        Object x500Name = x500NameConstructor.newInstance(cn, OU, O, L, ST, C);

        // X509Certificate cert = keypair.getSelfCertificate(x500Name, new Date(), VALIDITY_DAYS*24L*60L*60L);
        Method getSelfCertificateMethod = keypairClass.getMethod("getSelfCertificate", x500NameClass, Date.class, Long.TYPE);
        X509Certificate cert = (X509Certificate) getSelfCertificateMethod.invoke(keypair, x500Name, new Date(), VALIDITY_DAYS*24L*60L*60L);
        
        logger.info("generated self-signed certificate with subject: " + cert.getSubjectX500Principal());
        
        // PrivateKey privateKey = keypair.getPrivateKey();
        PrivateKey privateKey = (PrivateKey) keypairClass.getMethod("getPrivateKey").invoke(keypair);
        
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(null, null);
        keystore.setKeyEntry(ALIAS, privateKey, PASSWORD, new X509Certificate[] {cert});

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(getSslKeyManagerFactoryAlgorithm());        
        keyManagerFactory.init(keystore, PASSWORD);
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(getSslTrustManagerFactoryAlgorithm());
        trustManagerFactory.init(keystore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        SecureRandom secureRandom = null;
        if (getSecureRandomAlgorithm() != null) {
            secureRandom = SecureRandom.getInstance(getSecureRandomAlgorithm());
        }

        if (getProvider() != null) {
            sslContext = SSLContext.getInstance(getProtocol(), getProvider());
        } else {
            sslContext = SSLContext.getInstance(getProtocol());
        }
        
        sslContext.init(keyManagers, trustManagers, secureRandom);
    }

    @Override
    protected SSLServerSocketFactory createFactory() {
        return sslContext.getServerSocketFactory();
    }
}
