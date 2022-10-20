package org.xbib.net.security.signatures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public enum Signatures {
    ;

    /**
     * Create a canonicalized string representation of the HTTP request. It is used
     * as the input to calculate the signature of the HTTP request.
     *
     * @param required The list of headers that should be included in the HTTP signature.
     * @param method   The HTTP method.
     * @param uri      The HTTP request URI.
     * @param headers  A map of header names to header values.
     */
    public static String createSigningString(final List<String> required, final String method, final String uri, final Map<String, String> headers) {
        return createSigningString(required, method, uri, headers, null, null);
    }

    /**
     * Create a canonicalized string representation of the HTTP request. It is used
     * as the input to calculate the signature of the HTTP request.
     * <p>
     * The provided method, path and query values are used to generate the optional
     * (request-target) field.
     *
     * @param required              The list of headers that should be included in the HTTP signature.
     * @param method                The HTTP method.
     * @param uri                   The path and query of the request target of the message.
     *                              The value must already be encoded exactly as it will be sent in the
     *                              request line of the HTTP message. No URL encoding is performed by this method.
     * @param headers               A map of header names to header values.
     * @param signatureCreationTime The signature creation time in milliseconds since the epoch.
     * @param signatureExpiryTime   The signature expiration time in milliseconds since the epoch.
     */
    public static String createSigningString(final List<String> required, String method, final String uri, Map<String, String> headers,
                                             final Long signatureCreationTime, final Long signatureExpiryTime) {
        headers = lowercase(headers);

        final List<String> list = new ArrayList<String>(required.size());

        for (final String key : required) {
            if ("(request-target)".equals(key)) {
                method = lowercase(method);
                list.add(Joiner.join(" ", "(request-target):", method, uri));
            } else if ("(created)".equals(key)) {
                // The "created" parameter contains the signature's Creation Time.
                // This parameter is useful when signers are not capable of controlling
                // the "Date" HTTP Header such as when operating in certain web
                // browser environments.
                // Its canonicalized value is an Integer String containing the
                // signature's Creation Time expressed as the number of seconds since
                // the Epoch
                if (signatureCreationTime == null) {
                    throw new InvalidCreatedFieldException("(created) field requested but signature creation time is not set");
                }
                list.add(key + ": " + TimeUnit.MILLISECONDS.toSeconds(signatureCreationTime));
            } else if ("(expires)".equals(key)) {
                // The "expires" parameter contains the signature's Expiration Time.
                // If the signature does not have an Expiration Time, this parameter "MUST"
                // be omitted.  If not specified, the signature's Expiration Time is
                // undefined.
                // Its canonicalized value is a Decimal String containing the
                // signature's Expiration Time expressed as the number of seconds since
                // the Epoch.
                if (signatureExpiryTime == null) {
                    throw new InvalidExpiresFieldException("(expires) field requested but signature expiration time is not set");
                }
                final double expires = signatureExpiryTime / 1000.0;
                list.add(key + ": " + String.format("%.3f", expires));
            } else {
                final String value = headers.get(key);
                if (value == null) throw new MissingRequiredHeaderException(key);

                list.add(key + ": " + value);
            }
        }

        return Joiner.join("\n", list);
    }

    private static Map<String, String> lowercase(final Map<String, String> headers) {
        final Map<String, String> map = new HashMap<String, String>();
        for (final Map.Entry<String, String> entry : headers.entrySet()) {
            map.put(entry.getKey().toLowerCase(), entry.getValue());
        }

        return map;
    }

    private static String lowercase(final String spec) {
        return spec.toLowerCase();
    }
}
