package org.xbib.net.socket.v6.ping;

import java.net.Inet6Address;

public interface PingResponseListener {

	void onPingResponse(Inet6Address inetAddress, PingResponse reply);
}
