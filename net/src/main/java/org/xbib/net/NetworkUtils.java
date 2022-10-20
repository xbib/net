package org.xbib.net;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class for Java networking.
 */
public class NetworkUtils {

    private static final String lf = System.lineSeparator();

    private static final char[] hexDigit = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private static final String IPV4_SETTING = "java.net.preferIPv4Stack";

    private static final String IPV6_SETTING = "java.net.preferIPv6Addresses";

    private static final CountDownLatch latch = new CountDownLatch(1);

    private static final InterfaceWaiter interfaceWaiter = new InterfaceWaiter();

    private static InetAddress localAddress;

    private NetworkUtils() {
        throw new UnsupportedOperationException();
    }

    public static Properties createProperties() {
        Properties properties = new Properties();
        try {
            InetAddress address = getLocalAddress();
            properties.put("net.localhost", address.getCanonicalHostName());
            String hostname = address.getHostName();
            properties.put("net.hostname", hostname);
            InetAddress[] hostnameAddresses = InetAddress.getAllByName(hostname);
            int i = 0;
            for (InetAddress hostnameAddress : hostnameAddresses) {
                properties.put("net.host." + (i++) + "." + hostnameAddress.getCanonicalHostName(), hostnameAddress.getHostAddress());
            }
            for (InetAddress inetAddress : getAddresses(getActiveInterfaces(), NetworkUtils::isNonLoopBack, NetworkProtocolVersion.IPV4)) {
                    properties.put("net." + inetAddress.getCanonicalHostName(), inetAddress.getHostAddress());
            }
        } catch (Throwable e) {
            Logger.getLogger("network").log(Level.WARNING, e.getMessage(), e);
        }
        return properties;
    }

    public static boolean isPreferIPv4() {
        return Boolean.getBoolean(System.getProperty(IPV4_SETTING));
    }

    public static boolean isPreferIPv6() {
        return Boolean.getBoolean(System.getProperty(IPV6_SETTING));
    }

    public static InetAddress getIPv4Localhost() throws UnknownHostException {
        return getLocalhost(NetworkProtocolVersion.IPV4);
    }

    public static InetAddress getIPv6Localhost() throws UnknownHostException {
        return getLocalhost(NetworkProtocolVersion.IPV6);
    }

    public static InetAddress getLocalhost(NetworkProtocolVersion ipversion) throws UnknownHostException {
        return ipversion == NetworkProtocolVersion.IPV4 ?
                InetAddress.getByName("127.0.0.1") : InetAddress.getByName("::1");
    }

    public static String getLocalHostName(String defaultHostName) {
        String hostName = getLocalAddress().getHostName();
        if (hostName == null) {
            return defaultHostName;
        }
        return hostName;
    }

    public static String getLocalHostAddress(String defaultHostAddress) {
        String hostAddress = getLocalAddress().getHostAddress();
        if (hostAddress == null) {
            return defaultHostAddress;
        }
        return hostAddress;
    }

    public static NetworkClass getNetworkClass(InetAddress address) {
        if (address == null) {
            return NetworkClass.ANY;
        }
        if (address.isLoopbackAddress()) {
            return NetworkClass.LOOPBACK;
        }
        // link local unicast address
        if (address.isLinkLocalAddress()) {
            return NetworkClass.LOCAL;
        }
        // site local unicast address
        if (address.isSiteLocalAddress()) {
            return NetworkClass.SITE;
        }
        // wildcard address
        if (address.isAnyLocalAddress()) {
            return NetworkClass.ANY;
        }
        // other, must be public
        return NetworkClass.PUBLIC;
    }

    public static boolean matchesNetwork(NetworkClass given, NetworkClass expected) {
        switch (expected) {
            case ANY:
                return EnumSet.of(NetworkClass.LOOPBACK, NetworkClass.LOCAL, NetworkClass.SITE, NetworkClass.PUBLIC, NetworkClass.ANY)
                        .contains(given);
            case PUBLIC:
                return EnumSet.of(NetworkClass.LOOPBACK, NetworkClass.LOCAL, NetworkClass.SITE, NetworkClass.PUBLIC)
                        .contains(given);
            case SITE:
                return EnumSet.of(NetworkClass.LOOPBACK, NetworkClass.LOCAL, NetworkClass.SITE)
                        .contains(given);
            case LOCAL:
                return EnumSet.of(NetworkClass.LOOPBACK, NetworkClass.LOCAL)
                        .contains(given);
            case LOOPBACK:
                return NetworkClass.LOOPBACK == given;
        }
        return false;
    }

    public static List<InetAddress> getAllAvailableAddresses() {
        List<InetAddress> allAddresses = new ArrayList<>();
        for (NetworkInterface networkInterface : getAllNetworkInterfaces()) {
            Enumeration<InetAddress> addrs = networkInterface.getInetAddresses();
            while (addrs.hasMoreElements()) {
                allAddresses.add(addrs.nextElement());
            }
        }
        sortAddresses(allAddresses);
        return allAddresses;
    }

    public static boolean isIpv4Available() {
        return getAllAvailableAddresses().stream().anyMatch(addr -> addr instanceof Inet4Address);
    }

    public static boolean isIpv6Available() {
        return getAllAvailableAddresses().stream().anyMatch(addr -> addr instanceof Inet6Address);
    }

    public static List<InetAddress> getAllActiveAddresses() {
        List<InetAddress> allAddresses = new ArrayList<>();
        for (NetworkInterface networkInterface : getActiveInterfaces()) {
            Enumeration<InetAddress> addrs = networkInterface.getInetAddresses();
            while (addrs.hasMoreElements()) {
                allAddresses.add(addrs.nextElement());
            }
        }
        sortAddresses(allAddresses);
        return allAddresses;
    }

    public static boolean isIpv4Active() {
        return getAllActiveAddresses().stream().anyMatch(addr -> addr instanceof Inet4Address);
    }

    public static boolean isIpv6Active() {
        return getAllActiveAddresses().stream().anyMatch(addr -> addr instanceof Inet6Address);
    }

    public static List<NetworkInterface> getAllNetworkInterfaces() {
        return getInterfaces(n -> true);
    }

    public static List<NetworkInterface> getNonLoopbackNetworkInterfaces() {
        return getInterfaces(NetworkUtils::isNonLoopBack);
    }

    public static List<NetworkInterface> getActiveInterfaces() {
        return getInterfaces(NetworkUtils::isUp);
    }

    public static List<InetAddress> getAddresses(List<NetworkInterface> networkInterfaces,
                                                 Predicate<NetworkInterface> predicate,
                                                 NetworkProtocolVersion networkProtocolVersion) {
        List<InetAddress> addresses = new ArrayList<>();
        for (NetworkInterface networkInterface : networkInterfaces) {
            if (predicate.test(networkInterface)) {
                Enumeration<InetAddress> addrs = networkInterface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress address = addrs.nextElement();
                    if ((address instanceof Inet4Address && networkProtocolVersion == NetworkProtocolVersion.IPV4) ||
                            (address instanceof Inet6Address && networkProtocolVersion == NetworkProtocolVersion.IPV6)) {
                        addresses.add(address);
                    }
                }
            }
        }
        return addresses;
    }

    public static InetAddress getFirstAddress(List<NetworkInterface> networkInterfaces, NetworkProtocolVersion networkProtocolVersion) {
        List<InetAddress> addresses = getAddresses(networkInterfaces, n-> true, networkProtocolVersion);
        return addresses.isEmpty() ? null : addresses.get(0);
    }

    public static InetAddress getFirstNonLoopbackAddress(List<NetworkInterface> networkInterfaces, NetworkProtocolVersion networkProtocolVersion) {
        List<InetAddress> addresses = getAddresses(networkInterfaces, NetworkUtils::isNonLoopBack, networkProtocolVersion);
        return addresses.isEmpty() ? null : addresses.get(0);
    }

    public static boolean interfaceSupports(NetworkInterface networkInterface, NetworkProtocolVersion ipVersion) {
        boolean supportsVersion = false;
        if (networkInterface != null) {
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if ((address instanceof Inet4Address && (ipVersion == NetworkProtocolVersion.IPV4)) ||
                        (address instanceof Inet6Address && (ipVersion == NetworkProtocolVersion.IPV6))) {
                    supportsVersion = true;
                    break;
                }
            }
        }
        return supportsVersion;
    }

    public static NetworkProtocolVersion getProtocolVersion() {
        switch (findAvailableProtocols()) {
            case IPV4:
                return NetworkProtocolVersion.IPV4;
            case IPV6:
                return NetworkProtocolVersion.IPV6;
            case IPV46:
                if (Boolean.getBoolean(System.getProperty(IPV4_SETTING))) {
                    return NetworkProtocolVersion.IPV4;
                }
                if (Boolean.getBoolean(System.getProperty(IPV6_SETTING))) {
                    return NetworkProtocolVersion.IPV6;
                }
                return NetworkProtocolVersion.IPV6;
            default:
                break;
        }
        return NetworkProtocolVersion.NONE;
    }

    public static NetworkProtocolVersion findAvailableProtocols() {
        boolean hasIPv4 = false;
        boolean hasIPv6 = false;
        for (InetAddress addr : getAllAvailableAddresses()) {
            if (addr instanceof Inet4Address) {
                hasIPv4 = true;
            }
            if (addr instanceof Inet6Address) {
                hasIPv6 = true;
            }
        }
        if (hasIPv4 && hasIPv6) {
            return NetworkProtocolVersion.IPV46;
        }
        if (hasIPv4) {
            return NetworkProtocolVersion.IPV4;
        }
        if (hasIPv6) {
            return NetworkProtocolVersion.IPV6;
        }
        return NetworkProtocolVersion.NONE;
    }

    public static InetAddress resolveInetAddress(String hostname, String defaultValue) throws IOException {
        String host = hostname;
        if (host == null) {
            host = defaultValue;
        }
        String origHost = host;
        // strip port
        int pos = host.indexOf(':');
        if (pos > 0) {
            host = host.substring(0, pos - 1);
        }
        if ((host.startsWith("#") && host.endsWith("#")) || (host.startsWith("_") && host.endsWith("_"))) {
            host = host.substring(1, host.length() - 1);
            if ("loopback".equals(host)) {
                return InetAddress.getLoopbackAddress();
            } else if ("local".equals(host)) {
                return getLocalAddress();
            } else if (host.startsWith("non_loopback")) {
                if (host.toLowerCase(Locale.ROOT).endsWith(":ipv4")) {
                    return getFirstNonLoopbackAddress(getActiveInterfaces(), NetworkProtocolVersion.IPV4);
                } else if (host.toLowerCase(Locale.ROOT).endsWith(":ipv6")) {
                    return getFirstNonLoopbackAddress(getActiveInterfaces(), NetworkProtocolVersion.IPV6);
                } else {
                    return getFirstNonLoopbackAddress(getActiveInterfaces(), getProtocolVersion());
                }
            } else {
                NetworkProtocolVersion networkProtocolVersion = getProtocolVersion();
                String reducedHost = host.substring(0, host.length() - 5);
                if (host.toLowerCase(Locale.ROOT).endsWith(":ipv4")) {
                    networkProtocolVersion = NetworkProtocolVersion.IPV4;
                    host = reducedHost;
                } else if (host.toLowerCase(Locale.ROOT).endsWith(":ipv6")) {
                    networkProtocolVersion = NetworkProtocolVersion.IPV6;
                    host = reducedHost;
                }
                for (NetworkInterface networkInterface : getActiveInterfaces()) {
                    if (host.equals(networkInterface.getName()) || host.equals(networkInterface.getDisplayName())) {
                        if (networkInterface.isLoopback()) {
                            return getFirstAddress(List.of(networkInterface), networkProtocolVersion);
                        } else {
                            return getFirstNonLoopbackAddress(List.of(networkInterface), networkProtocolVersion);
                        }
                    }
                }
            }
            throw new IOException("failed to find network interface for [" + origHost + "]");
        }
        if ("0.0.0.0".equals(host) || "::".equals(host)) {
            return new InetSocketAddress(0).getAddress();
        }
        return InetAddress.getByName(host);
    }

    public static InetAddress resolvePublicHostAddress(String host) throws IOException {
        InetAddress address = resolveInetAddress(host, null);
        if (address == null || address.isAnyLocalAddress()) {
            address = getFirstNonLoopbackAddress(getActiveInterfaces(), NetworkProtocolVersion.IPV4);
            if (address == null) {
                address = getFirstNonLoopbackAddress(getActiveInterfaces(), getProtocolVersion());
                if (address == null) {
                    address = getLocalAddress();
                    if (address == null) {
                        return getLocalhost(NetworkProtocolVersion.IPV4);
                    }
                }
            }
        }
        return address;
    }

    public static List<NetworkInterface> getInterfaces(Predicate<NetworkInterface> predicate) {
        List<NetworkInterface> networkInterfaces = new ArrayList<>();
        for (NetworkInterface networkInterface : waitForNetworkInterfaces()) {
            if (predicate.test(networkInterface)) {
                networkInterfaces.add(networkInterface);
                Enumeration<NetworkInterface> subInterfaces = networkInterface.getSubInterfaces();
                while (subInterfaces.hasMoreElements()) {
                    networkInterfaces.add(subInterfaces.nextElement());
                }
            }
        }
        sortInterfaces(networkInterfaces);
        return networkInterfaces;
    }

    public static List<NetworkInterface> waitForNetworkInterfaces() {
        return waitForNetworkInterfaces(30L, TimeUnit.SECONDS);
    }

    public static List<NetworkInterface> waitForNetworkInterfaces(long period, TimeUnit timeUnit) {
        if (latch.getCount() == 0L) {
            return interfaceWaiter.interfaces;
        }
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> future = service.scheduleAtFixedRate(interfaceWaiter, 0L, period, timeUnit);
        try {
            latch.await();
            future.cancel(true);
            service.shutdownNow();
            return interfaceWaiter.interfaces;
        } catch (InterruptedException e) {
            throw new IllegalStateException("timeout while waiting for network interfaces");
        }
    }

    private static class InterfaceWaiter implements Runnable {

        private final List<NetworkInterface> interfaces = new ArrayList<>();

        private final Logger logger = Logger.getLogger("network");

        @Override
        public void run() {
            try {
                interfaces.clear();
                logger.log(Level.INFO, "waiting for network interfaces");
                Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
                if (networkInterfaceEnumeration.hasMoreElements()) {
                    do {
                        NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();
                        interfaces.add(networkInterface);
                    } while (networkInterfaceEnumeration.hasMoreElements());
                    logger.log(Level.INFO, "network interfaces present: " + interfaces);
                    if (!interfaces.isEmpty()) {
                        latch.countDown();
                    }
                }
            } catch (Exception e) {
                // getNetworkInterfaces() throws socket exception if no network is configured
                logger.log(Level.WARNING, e.getMessage());
            }
        }
    }

    public static String getNetworkInterfacesAsString() {
        StringBuilder sb = new StringBuilder();
        for (NetworkInterface nic : getAllNetworkInterfaces()) {
            sb.append(getNetworkInterfaceAsString(nic));
        }
        return sb.toString();
    }

    public static String getNetworkInterfaceAsString(NetworkInterface nic) {
        StringBuilder sb = new StringBuilder();
        sb.append(lf).append(nic.getName()).append(lf);
        if (!nic.getName().equals(nic.getDisplayName())) {
            sb.append("\t").append(nic.getDisplayName()).append(lf);
        }
        sb.append("\t").append("flags ");
        List<String> flags = new ArrayList<>();
        try {
            if (nic.isUp()) {
                flags.add("UP");
            }
            if (nic.supportsMulticast()) {
                flags.add("MULTICAST");
            }
            if (nic.isLoopback()) {
                flags.add("LOOPBACK");
            }
            if (nic.isPointToPoint()) {
                flags.add("POINTTOPOINT");
            }
            if (nic.isVirtual()) {
                flags.add("VIRTUAL");
            }
        } catch (Exception e) {
            Logger.getLogger("network").log(Level.WARNING, e.getMessage(), e);
        }
        sb.append(String.join(",", flags));
        try {
            sb.append(" mtu ").append(nic.getMTU()).append(lf);
        } catch (SocketException e) {
            Logger.getLogger("network").log(Level.WARNING, e.getMessage(), e);
        }
        List<InterfaceAddress> addresses = nic.getInterfaceAddresses();
        for (InterfaceAddress address : addresses) {
            sb.append("\t").append(formatAddress(address)).append(lf);
        }
        try {
            byte[] b = nic.getHardwareAddress();
            if (b != null) {
                sb.append("\t").append("ether ");
                for (int i = 0; i < b.length; i++) {
                    if (i > 0) {
                        sb.append(":");
                    }
                    sb.append(hexDigit[(b[i] >> 4) & 0x0f]).append(hexDigit[b[i] & 0x0f]);
                }
                sb.append(lf);
            }
        } catch (SocketException e) {
            Logger.getLogger("network").log(Level.WARNING, e.getMessage(), e);
        }
        return sb.toString();
    }

    public static InetAddress getLocalAddress() {
        if (localAddress != null) {
            return localAddress;
        }
        InetAddress address;
        try {
            address = InetAddress.getLocalHost();
        } catch (Exception e) {
            Logger.getLogger("network").log(Level.WARNING, e.getMessage(), e);
            address = InetAddress.getLoopbackAddress();
        }
        localAddress = address;
        return localAddress;
    }


    public static String format(InetAddress address) {
        return format(address, -1);
    }

    public static String format(InetSocketAddress address) {
        return format(address.getAddress(), address.getPort());
    }

    public static String format(InetAddress address, int port) {
        Objects.requireNonNull(address);
        StringBuilder sb = new StringBuilder();
        if (port != -1 && address instanceof Inet6Address) {
            sb.append(toUriString(address));
        } else {
            sb.append(toAddrString(address));
        }
        if (port != -1) {
            sb.append(':').append(port);
        }
        return sb.toString();
    }

    public static String toUriString(InetAddress ip) {
        if (ip instanceof Inet6Address) {
            return "[" + toAddrString(ip) + "]";
        }
        return toAddrString(ip);
    }

    public static String toAddrString(InetAddress ip) {
        if (ip == null) {
            throw new NullPointerException("ip");
        }
        if (ip instanceof Inet4Address) {
            byte[] bytes = ip.getAddress();
            return (bytes[0] & 0xff) + "." + (bytes[1] & 0xff) + "." + (bytes[2] & 0xff) + "." + (bytes[3] & 0xff);
        }
        if (!(ip instanceof Inet6Address)) {
            throw new IllegalArgumentException("ip");
        }
        byte[] bytes = ip.getAddress();
        int[] hextets = new int[8];
        for (int i = 0; i < hextets.length; i++) {
            hextets[i] = (bytes[2 * i] & 255) << 8 | bytes[2 * i + 1] & 255;
        }
        compressLongestRunOfZeroes(hextets);
        return hextetsToIPv6String(hextets);
    }

    public static String formatAddress(InterfaceAddress interfaceAddress) {
        StringBuilder sb = new StringBuilder();
        InetAddress address = interfaceAddress.getAddress();
        if (address instanceof Inet6Address) {
            sb.append("inet6 ").append(format(address))
                    .append(" prefixlen:").append(interfaceAddress.getNetworkPrefixLength());
        } else {
            int netmask = 0xFFFFFFFF << (32 - interfaceAddress.getNetworkPrefixLength());
            byte[] b = new byte[] {
                    (byte) (netmask >>> 24),
                    (byte) (netmask >>> 16 & 0xFF),
                    (byte) (netmask >>> 8 & 0xFF),
                    (byte) (netmask & 0xFF)
            };
            sb.append("inet ").append(format(address));
            try {
                sb.append(" netmask:").append(format(InetAddress.getByAddress(b)));
            } catch (UnknownHostException e) {
                Logger.getLogger("network").log(Level.WARNING, e.getMessage(), e);
            }
            InetAddress broadcast = interfaceAddress.getBroadcast();
            if (broadcast != null) {
                sb.append(" broadcast:").append(format(broadcast));
            }
        }
        if (address.isLoopbackAddress()) {
            sb.append(" scope:host");
        } else if (address.isLinkLocalAddress()) {
            sb.append(" scope:link");
        } else if (address.isSiteLocalAddress()) {
            sb.append(" scope:site");
        }
        return sb.toString();
    }

    private static boolean isUp(NetworkInterface networkInterface) {
        try {
            return networkInterface.isUp();
        } catch (SocketException e) {
            return false;
        }
    }

    private static boolean isNonLoopBack(NetworkInterface networkInterface) {
        try {
            return !networkInterface.isLoopback();
        } catch (SocketException e) {
            return false;
        }
    }

    private static int compareBytes(byte[] left, byte[] right) {
        for (int i = 0, j = 0; i < left.length && j < right.length; i++, j++) {
            int a = left[i] & 0xff;
            int b = right[j] & 0xff;
            if (a != b) {
                return a - b;
            }
        }
        return left.length - right.length;
    }

    private static void compressLongestRunOfZeroes(int[] hextets) {
        int bestRunStart = -1;
        int bestRunLength = -1;
        int runStart = -1;
        for (int i = 0; i < hextets.length + 1; i++) {
            if (i < hextets.length && hextets[i] == 0) {
                if (runStart < 0) {
                    runStart = i;
                }
            } else if (runStart >= 0) {
                int runLength = i - runStart;
                if (runLength > bestRunLength) {
                    bestRunStart = runStart;
                    bestRunLength = runLength;
                }
                runStart = -1;
            }
        }
        if (bestRunLength >= 2) {
            Arrays.fill(hextets, bestRunStart, bestRunStart + bestRunLength, -1);
        }
    }

    private static String hextetsToIPv6String(int[] hextets) {
        StringBuilder sb = new StringBuilder(39);
        boolean lastWasNumber = false;
        for (int i = 0; i < hextets.length; i++) {
            boolean b = hextets[i] >= 0;
            if (b) {
                if (lastWasNumber) {
                    sb.append(':');
                }
                sb.append(Integer.toHexString(hextets[i]));
            } else {
                if (i == 0 || lastWasNumber) {
                    sb.append("::");
                }
            }
            lastWasNumber = b;
        }
        return sb.toString();
    }

    private static void sortInterfaces(List<NetworkInterface> interfaces) {
        interfaces.sort(Comparator.comparingInt(NetworkInterface::getIndex));
    }

    private static void sortAddresses(List<InetAddress> addressList) {
        addressList.sort((o1, o2) -> compareBytes(o1.getAddress(), o2.getAddress()));
    }
}
