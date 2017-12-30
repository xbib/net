package org.xbib.net.scheme;

import org.xbib.net.URL;

/**
 * Interface implemented by custom scheme parsers.
 */
public interface Scheme {

    String DNS = "dns";
    String FILE = "file";
    String FTP = "ftp";
    String GIT = "git";
    String GIT_HTTPS = "git+https";
    String GOPHER = "gopher";
    String HTTP = "http";
    String HTTPS = "https";
    String IMAP = "imap";
    String IMAPS = "imaps";
    String IRC = "irc";
    String LDAP = "ldap";
    String LDAPS = "ldaps";
    String MAILTO = "mailto";
    String NEWS = "news";
    String NNTP = "nntp";
    String POP3 = "pop3";
    String POP3S = "pop3s";
    String REDIS = "redis";
    String RSYNC = "rsync";
    String RTMP = "rtmp";
    String RTSP = "rtsp";
    String SFTP = "sftp";
    String SMTP = "smtp";
    String SMTPS = "smtps";
    String SNEWS = "snews";
    String SSH = "ssh";
    String TELNET = "telnet";
    String TFTP = "tftp";
    String URN = "urn";
    String WS = "ws";
    String WSS = "wss";

    String getName();

    int getDefaultPort();

    URL normalize(URL url);
}
