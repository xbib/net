package org.xbib.net.socket.notify;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SystemdTest {

    @Test
    public void testSsystemd() throws IOException {
        SystemdNotify.sendNotify();
    }
}
