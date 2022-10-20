package org.xbib.net.socket.v4.ping;

import java.net.Inet4Address;

public interface PingResponseListener {

	void onPingResponse(Inet4Address inetAddress, PingResponse reply);
}
