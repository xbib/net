package org.xbib.net.scheme;

/**
 *
 */
class WebSocketScheme extends HttpScheme {

    WebSocketScheme() {
        super("ws", 80);
    }

    WebSocketScheme(String name, int port) {
        super(name, port);
    }

}
