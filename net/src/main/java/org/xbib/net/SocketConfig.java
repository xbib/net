package org.xbib.net;

public class SocketConfig {

    /**
     * Keep alive.
     */
    private boolean keepAlive = true;

    /**
     * Default for SO_REUSEADDR.
     */
    private boolean reuseAddr = true;

    /**
     * Default for TCP_NODELAY.
     */
    private boolean tcpNodelay = true;

    /**
     * Set TCP send buffer to 64k per socket.
     */
    private int tcpSendBufferSize = 64 * 1024;

    /**
     * Set TCP receive buffer to 64k per socket.
     */
    private int tcpReceiveBufferSize = 64 * 1024;

    /**
     * Default for socket back log.
     */
    private int backLogSize = 10 * 1024;

    /**
     * The default socket timeout for SO_TIMEOUT in seconds.
     */
    private int socketTimeoutMillis = 60000;

    /**
     * Default connect timeout in milliseconds when socket is created.
     */
    private int connectTimeoutMillis = 5000;

    /**
     * Default read timeout in milliseconds per request.
     */
    private int readTimeoutMillis = 15000;

    /**
     * For SSL handshakes.
     */
    private int sslHandshakeTimeoutMillis = 15000;

    /**
     * Disable linger.
     */
    private int linger = -1;

    public SocketConfig() {
    }

    public SocketConfig setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public SocketConfig setReuseAddr(boolean reuseAddr) {
        this.reuseAddr = reuseAddr;
        return this;
    }

    public boolean isReuseAddr() {
        return reuseAddr;
    }

    public SocketConfig setTcpNodelay(boolean tcpNodelay) {
        this.tcpNodelay = tcpNodelay;
        return this;
    }

    public boolean isTcpNodelay() {
        return tcpNodelay;
    }

    public SocketConfig setTcpSendBufferSize(int tcpSendBufferSize) {
        this.tcpSendBufferSize = tcpSendBufferSize;
        return this;
    }

    public int getTcpSendBufferSize() {
        return tcpSendBufferSize;
    }

    public SocketConfig setTcpReceiveBufferSize(int tcpReceiveBufferSize) {
        this.tcpReceiveBufferSize = tcpReceiveBufferSize;
        return this;
    }

    public int getTcpReceiveBufferSize() {
        return tcpReceiveBufferSize;
    }

    public SocketConfig setBackLogSize(int backLogSize) {
        this.backLogSize = backLogSize;
        return this;
    }

    public int getBackLogSize() {
        return backLogSize;
    }

    public SocketConfig setSocketTimeoutMillis(int socketTimeoutMillis) {
        this.socketTimeoutMillis = socketTimeoutMillis;
        return this;
    }

    public int getSocketTimeoutMillis() {
        return socketTimeoutMillis;
    }

    public SocketConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        return this;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public SocketConfig setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
        return this;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }


    public SocketConfig setSslHandshakeTimeoutMillis(int sslHandshakeTimeoutMillis) {
        this.sslHandshakeTimeoutMillis = sslHandshakeTimeoutMillis;
        return this;
    }

    public int getSslHandshakeTimeoutMillis() {
        return sslHandshakeTimeoutMillis;
    }

    public SocketConfig setLinger(int seconds) {
        this.linger = seconds;
        return this;
    }

    public int getLinger() {
        return linger;
    }
}
