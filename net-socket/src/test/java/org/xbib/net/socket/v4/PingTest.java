package org.xbib.net.socket.v4;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xbib.net.socket.v4.ping.Ping;
import java.net.Inet4Address;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Disabled("RHEL selinux blocks Java JDK from acessing icmp_socket")
class PingTest {

    private static final Logger logger = Logger.getLogger(PingTest.class.getName());

    @Test
    void ping() throws Exception {
        Inet4Address address = (Inet4Address) Inet4Address.getByName("www.google.de");
        if (address != null) {
            Ping ping = new Ping(1234);
            logger.log(Level.INFO, "address=" + address);
            ping.execute(1234, address);
            logger.log(Level.INFO, ping.getMetric().getSummary(TimeUnit.MILLISECONDS));
            ping.close();
        }
    }
}
