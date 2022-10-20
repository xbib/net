package org.xbib.net.socket.v6;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xbib.net.socket.v6.ping.Ping;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Disabled("RHEL selinux blocks Java JDK from acessing icmp_socket")
class PingTest {

    private static final Logger logger = Logger.getLogger(PingTest.class.getName());

    @Test
    void ping() throws Exception {
        Inet6Address address = getAddress("www.google.de");
        if (address != null) {
            logger.log(Level.INFO, "address=" + address);
            Ping ping = new Ping(1234);
            ping.execute(1234, address);
            logger.log(Level.INFO, ping.getMetric().getSummary(TimeUnit.MILLISECONDS));
            ping.close();
        }
    }

    private Inet6Address getAddress(String host) throws UnknownHostException {
        for (InetAddress addr : InetAddress.getAllByName(host)) {
            if (addr instanceof Inet6Address) {
                return (Inet6Address) addr;
            }
        }
        return null;
    }
}
