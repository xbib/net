package org.xbib.net.scheme;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Registry of URL schemes.
 */
public final class SchemeRegistry {

    private static final SchemeRegistry registry = new SchemeRegistry();

    private final Map<String, Scheme> schemes;

    private SchemeRegistry() {
        schemes = new HashMap<>();
        schemes.put(Scheme.DNS , new DnsScheme());
        schemes.put(Scheme.FILE , new FileScheme());
        schemes.put(Scheme.FTP, new FtpScheme());
        schemes.put(Scheme.GIT, new GitScheme());
        schemes.put(Scheme.GIT_HTTPS, new GitSecureHttpScheme());
        schemes.put(Scheme.GOPHER, new GopherScheme());
        schemes.put(Scheme.HTTP, new HttpScheme());
        schemes.put(Scheme.HTTPS, new SecureHttpScheme());
        schemes.put(Scheme.IMAP, new ImapScheme());
        schemes.put(Scheme.IMAPS, new SecureImapScheme());
        schemes.put(Scheme.IRC, new IrcScheme());
        schemes.put(Scheme.LDAP, new LdapScheme());
        schemes.put(Scheme.LDAPS, new SecureLdapScheme());
        schemes.put(Scheme.MAILTO, new MailtoScheme());
        schemes.put(Scheme.NEWS, new NewsScheme());
        schemes.put(Scheme.NNTP, new NntpScheme());
        schemes.put(Scheme.POP3, new Pop3Scheme());
        schemes.put(Scheme.POP3S, new SecurePop3Scheme());
        schemes.put(Scheme.REDIS, new RedisScheme());
        schemes.put(Scheme.RSYNC, new RsyncScheme());
        schemes.put(Scheme.RTMP, new RtmpScheme());
        schemes.put(Scheme.RTSP, new RtspScheme());
        schemes.put(Scheme.SFTP, new SftpScheme());
        schemes.put(Scheme.SMTP, new SmtpScheme());
        schemes.put(Scheme.SMTPS, new SecureSmtpScheme());
        schemes.put(Scheme.SNEWS, new SecureNewsScheme());
        schemes.put(Scheme.SSH, new SshScheme());
        schemes.put(Scheme.TELNET, new TelnetScheme());
        schemes.put(Scheme.TFTP, new TftpScheme());
        schemes.put(Scheme.URN, new UrnScheme());
        schemes.put(Scheme.WS, new WebSocketScheme());
        schemes.put(Scheme.WSS, new SecureWebSocketScheme());
        for (Scheme scheme : ServiceLoader.load(Scheme.class)) {
            register(scheme);
        }
    }

    public static SchemeRegistry getInstance() {
        return registry;
    }

    public boolean register(Scheme scheme) {
        String name = scheme.getName();
        if (name == null) {
            return false;
        }
        if (!schemes.containsKey(name)) {
            schemes.put(name.toLowerCase(Locale.ROOT), scheme);
            return true;
        } else {
            return false;
        }
    }

    public Scheme getScheme(String scheme) {
        if (scheme == null) {
            return null;
        }
        Scheme s = schemes.get(scheme.toLowerCase(Locale.ROOT));
        return s != null ? s : new DefaultScheme(scheme);
    }
}
