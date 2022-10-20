package org.xbib.net.socket.v6;

import com.sun.jna.Platform;

public interface Constants {

    int AF_INET6 =  Platform.isLinux() ? 10
            : Platform.isMac() ? 30
            : Platform.isWindows() ? 23
            : Platform.isFreeBSD() ? 28
            : Platform.isSolaris() ? 26
            : -1;

    int IPPROTO_IPV6 = 41;

    int IPPROTO_ICMPV6 = 58;

    int IPV6_DONTFRAG = 62;

    int IPV6_TCLASS = Platform.isLinux() ? 67
            : Platform.isMac() ? 36
            : -1;
}
