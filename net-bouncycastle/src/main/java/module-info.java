import org.xbib.net.security.CertificateProvider;
import org.xbib.net.bouncycastle.BouncyCastleCertificateProvider;

module org.xbib.net.bouncycastle {
    requires transitive org.xbib.net.security;
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    exports org.xbib.net.bouncycastle;
    provides CertificateProvider with BouncyCastleCertificateProvider;
}
