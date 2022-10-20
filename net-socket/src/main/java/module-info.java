module org.xbib.net.socket {
    requires java.logging;
    requires com.sun.jna;
    exports org.xbib.net.socket;
    exports org.xbib.net.socket.v4;
    exports org.xbib.net.socket.v4.bsd;
    exports org.xbib.net.socket.v4.datagram;
    exports org.xbib.net.socket.v4.icmp;
    exports org.xbib.net.socket.v4.ip;
    exports org.xbib.net.socket.v4.ping;
    exports org.xbib.net.socket.v4.unix;
    exports org.xbib.net.socket.v6;
    exports org.xbib.net.socket.v6.bsd;
    exports org.xbib.net.socket.v6.datagram;
    exports org.xbib.net.socket.v6.icmp;
    exports org.xbib.net.socket.v6.ping;
    exports org.xbib.net.socket.v6.unix;
}
