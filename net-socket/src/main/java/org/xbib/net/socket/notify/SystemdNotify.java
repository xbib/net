package org.xbib.net.socket.notify;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SystemdNotify {

    public SystemdNotify() {
    }

    public static void sendNotify() throws IOException {
        sendNotify("READY=1");
    }

    public static void sendNotify(String text) throws IOException {
        String socketName = System.getenv("NOTIFY_SOCKET");
        if (socketName != null) {
            Path path = Paths.get(socketName);
            UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(path);
            try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
                channel.connect(socketAddress);
                channel.write(StandardCharsets.US_ASCII.encode(CharBuffer.wrap(text)));
            }
        }
    }
}
