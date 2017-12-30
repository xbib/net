package org.xbib.net.scheme;

/**
 * The RTSP scheme.
 *
 * @see <a href="https://www.ietf.org/rfc/rfc2326.txt">RTSP RFC</a>
 */
class RtspScheme extends AbstractScheme {

    RtspScheme() {
        super("rtsp", 554);
    }

}
