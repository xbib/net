package org.xbib.net.security;

import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

public class CertificateReaderTest {

    private static final Logger logger = Logger.getLogger(CertificateReaderTest.class.getName());

    @Test
    public void testCert() throws Exception {
        InputStream inputStream = getClass().getResourceAsStream("/test.crt");
        if (inputStream != null) {
            CertificateReader certificateReader = new CertificateReader();
            X509Certificate certificate = certificateReader.readCertificate(inputStream);
            logger.log(Level.INFO, "" + certificate.getSerialNumber());
            logger.log(Level.INFO, "not before = " + certificate.getNotBefore());
            logger.log(Level.INFO, "not after = " + certificate.getNotAfter());
        }
    }
}
