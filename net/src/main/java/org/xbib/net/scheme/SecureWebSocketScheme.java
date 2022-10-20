package org.xbib.net.scheme;

/**
 * Secure web socket scheme.
 */
class SecureWebSocketScheme extends WebSocketScheme {

    SecureWebSocketScheme() {
        super("wss", 443);
    }

}
