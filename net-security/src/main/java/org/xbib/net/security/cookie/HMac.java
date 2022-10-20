package org.xbib.net.security.cookie;

public enum HMac {
    HMAC_SHA1("HMacSHA1"),
    HMAC_SHA256("HMacSHA256");

    String algo;

    HMac(String algo) {
        this.algo = algo;
    }

    public String getAlgo() {
        return algo;
    }
}
