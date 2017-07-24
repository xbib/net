package org.xbib.net.scheme;

/**
 *
 */
class SecureWebSocketScheme extends WebSocketScheme {

    SecureWebSocketScheme() {
        super("wss", 443);
    }

}
