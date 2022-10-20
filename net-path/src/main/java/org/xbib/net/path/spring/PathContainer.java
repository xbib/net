package org.xbib.net.path.spring;

import org.xbib.net.path.spring.util.MultiValueMap;

import java.util.List;

/**
 * Structured representation of a URI path parsed via {@link #parsePath(String)}
 * into a sequence of {@link Separator} and {@link PathSegment} elements.
 *
 * Each {@link PathSegment} exposes its content in decoded form and with path
 * parameters removed. This makes it safe to match one path segment at a time
 * without the risk of decoded reserved characters altering the structure of
 * the path.
 *
 */
public interface PathContainer {

	/**
	 * The original path from which this instance was parsed.
	 */
	String value();

	/**
	 * The contained path elements, either {@link Separator} or {@link PathSegment}.
	 */
	List<Element> elements();

	/**
	 * Extract a sub-path from the given offset into the elements list.
	 * @param index the start element index (inclusive)
	 * @return the sub-path
	 */
	default PathContainer subPath(int index) {
		return subPath(index, elements().size());
	}

	/**
	 * Extract a sub-path from the given start offset into the element list
	 * (inclusive) and to the end offset (exclusive).
	 * @param startIndex the start element index (inclusive)
	 * @param endIndex the end element index (exclusive)
	 * @return the sub-path
	 */
	default PathContainer subPath(int startIndex, int endIndex) {
		return DefaultPathContainer.subPath(this, startIndex, endIndex);
	}

	/**
	 * Parse the path value into a sequence of {@code "/"} {@link Separator Separator}
	 * and {@link PathSegment PathSegment} elements.
	 * @param path the encoded, raw path value to parse
	 * @return the parsed path
	 */
	static PathContainer parsePath(String path) {
		return DefaultPathContainer.createFromUrlPath(path, Options.HTTP_PATH);
	}

	/**
	 * Parse the path value into a sequence of {@link Separator Separator} and
	 * {@link PathSegment PathSegment} elements.
	 * @param path the encoded, raw path value to parse
	 * @param options to customize parsing
	 * @return the parsed path
	 */
	static PathContainer parsePath(String path, Options options) {
		return DefaultPathContainer.createFromUrlPath(path, options);
	}

	/**
	 * A path element, either separator or path segment.
	 */
	interface Element {

		/**
		 * The unmodified, original value of this element.
		 */
		String value();
	}

	/**
	 * Path separator element.
	 */
	interface Separator extends Element {
	}

	/**
	 * Path segment element.
	 */
	interface PathSegment extends Element {

		/**
		 * Return the path segment value, decoded and sanitized, for path matching.
		 */
		String valueToMatch();

		/**
		 * Expose {@link #valueToMatch()} as a character array.
		 */
		char[] valueToMatchAsChars();

		/**
		 * Path parameters associated with this path segment.
		 * @return an unmodifiable map containing the parameters
		 */
		MultiValueMap<String, String> parameters();
	}

	/**
	 * Options to customize parsing based on the type of input path.
	 */
	class Options {

		/**
		 * Options for HTTP URL paths.
		 * Separator '/' with URL decoding and parsing of path parameters.
		 */
		public final static Options HTTP_PATH = Options.create('/', true);

		/**
		 * Options for a message route.
		 * Separator '.' with neither URL decoding nor parsing of path parameters.
		 * Escape sequences for the separator character in segment values are still
		 * decoded.
		 */
		public final static Options MESSAGE_ROUTE = Options.create('.', false);

		private final char separator;

		private final boolean decodeAndParseSegments;

		private Options(char separator, boolean decodeAndParseSegments) {
			this.separator = separator;
			this.decodeAndParseSegments = decodeAndParseSegments;
		}

		public char separator() {
			return this.separator;
		}

		public boolean shouldDecodeAndParseSegments() {
			return this.decodeAndParseSegments;
		}

		/**
		 * Create an {@link Options} instance with the given settings.
		 * @param separator the separator for parsing the path into segments;
		 * currently this must be slash or dot.
		 * @param decodeAndParseSegments whether to URL decode path segment
		 * values and parse path parameters. If set to false, only escape
		 * sequences for the separator char are decoded.
		 */
		public static Options create(char separator, boolean decodeAndParseSegments) {
			return new Options(separator, decodeAndParseSegments);
		}
	}
}
