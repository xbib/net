module org.xbib.net.security {
    requires org.xbib.net;
    provides java.security.Provider with org.xbib.net.security.eddsa.EdDSASecurityProvider;
    provides org.xbib.net.security.CertificateProvider with org.xbib.net.security.DefaultCertificateProvider;
    exports org.xbib.net.security;
    exports org.xbib.net.security.cookie;
    exports org.xbib.net.security.eddsa;
    exports org.xbib.net.security.eddsa.math;
    exports org.xbib.net.security.eddsa.math.bigint;
    exports org.xbib.net.security.eddsa.math.ed25519;
    exports org.xbib.net.security.eddsa.spec;
    exports org.xbib.net.security.signatures;
    exports org.xbib.net.security.ssl;
    exports org.xbib.net.security.util;
}
