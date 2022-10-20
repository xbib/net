package org.xbib.net;

import org.junit.jupiter.api.Test;

import java.net.UnixDomainSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkUtilsTest {

    private static final Logger logger = Logger.getLogger(NetworkUtilsTest.class.getName());

    @Test
    public void testInterfaceProperties() {
        logger.log(Level.INFO, NetworkUtils.createProperties().toString());
    }

    @Test
    public void testInterfaceDisplay() {
        logger.log(Level.INFO, NetworkUtils.getNetworkInterfacesAsString());
    }

    @Test
    public void testIP() {
        logger.log(Level.INFO, Boolean.toString(NetworkUtils.isIpv4Available()));
        logger.log(Level.INFO, Boolean.toString(NetworkUtils.isIpv4Active()));
        logger.log(Level.INFO, Boolean.toString(NetworkUtils.isIpv6Available()));
        logger.log(Level.INFO, Boolean.toString(NetworkUtils.isIpv6Active()));
    }
}
