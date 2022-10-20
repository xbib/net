package org.xbib.net.path.spring;

import org.xbib.net.path.spring.util.LinkedMultiValueMap;
import org.xbib.net.path.spring.util.MultiValueMap;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link PathContainer}.
 */
final class DefaultPathContainer implements PathContainer {

	private static final PathContainer EMPTY_PATH = new DefaultPathContainer("", Collections.emptyList());

	private static final Map<Character, DefaultSeparator> SEPARATORS = new HashMap<>(2);

	static {
		SEPARATORS.put('/', new DefaultSeparator('/', "%2F"));
		SEPARATORS.put('.', new DefaultSeparator('.', "%2E"));
	}

	private final String path;

	private final List<Element> elements;


	private DefaultPathContainer(String path, List<Element> elements) {
		this.path = path;
		this.elements = Collections.unmodifiableList(elements);
	}


	@Override
	public String value() {
		return this.path;
	}

	@Override
	public List<Element> elements() {
		return this.elements;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PathContainer)) {
			return false;
		}
		return value().equals(((PathContainer) other).value());
	}

	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

	@Override
	public String toString() {
		return value();
	}


	static PathContainer createFromUrlPath(String path, Options options) {
		if (path.isEmpty()) {
			return EMPTY_PATH;
		}
		char separator = options.separator();
		DefaultSeparator separatorElement = SEPARATORS.get(separator);
		if (separatorElement == null) {
			throw new IllegalArgumentException("Unexpected separator: '" + separator + "'");
		}
		List<Element> elements = new ArrayList<>();
		int begin;
		if (path.charAt(0) == separator) {
			begin = 1;
			elements.add(separatorElement);
		}
		else {
			begin = 0;
		}
		while (begin < path.length()) {
			int end = path.indexOf(separator, begin);
			String segment = (end != -1 ? path.substring(begin, end) : path.substring(begin));
			if (!segment.isEmpty()) {
				elements.add(options.shouldDecodeAndParseSegments() ?
						decodeAndParsePathSegment(segment) :
						DefaultPathSegment.from(segment, separatorElement));
			}
			if (end == -1) {
				break;
			}
			elements.add(separatorElement);
			begin = end + 1;
		}
		return new DefaultPathContainer(path, elements);
	}

	private static PathSegment decodeAndParsePathSegment(String segment) {
		Charset charset = StandardCharsets.UTF_8;
		int index = segment.indexOf(';');
		if (index == -1) {
			String valueToMatch = uriDecode(segment, charset);
			return DefaultPathSegment.from(segment, valueToMatch);
		}
		else {
			String valueToMatch = uriDecode(segment.substring(0, index), charset);
			String pathParameterContent = segment.substring(index);
			MultiValueMap<String, String> parameters = parsePathParams(pathParameterContent, charset);
			return DefaultPathSegment.from(segment, valueToMatch, parameters);
		}
	}

	private static MultiValueMap<String, String> parsePathParams(String input, Charset charset) {
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
		int begin = 1;
		while (begin < input.length()) {
			int end = input.indexOf(';', begin);
			String param = (end != -1 ? input.substring(begin, end) : input.substring(begin));
			parsePathParamValues(param, charset, result);
			if (end == -1) {
				break;
			}
			begin = end + 1;
		}
		return result;
	}

	private static void parsePathParamValues(String input, Charset charset, MultiValueMap<String, String> output) {
		if (hasText(input)) {
			int index = input.indexOf('=');
			if (index != -1) {
				String name = input.substring(0, index);
				name = uriDecode(name, charset);
				if (hasText(name)) {
					String value = input.substring(index + 1);
					for (String v : commaDelimitedListToStringArray(value)) {
						output.add(name, uriDecode(v, charset));
					}
				}
			}
			else {
				String name = uriDecode(input, charset);
				if (hasText(name)) {
					output.add(input, "");
				}
			}
		}
	}

	public static boolean hasText(String str) {
		return (str != null && !str.isEmpty() && containsText(str));
	}

	private static boolean containsText(CharSequence str) {
		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	public static List<String> commaDelimitedListToStringArray(String str) {
		return delimitedListToStringArray(str, ",");
	}

	private static List<String> delimitedListToStringArray(String str, String delimiter) {
		return delimitedListToStringArray(str, delimiter, null);
	}

	private static List<String> delimitedListToStringArray(String str, String delimiter, String charsToDelete) {

		if (str == null) {
			return List.of();
		}
		if (delimiter == null) {
			return List.of(str);
		}

		List<String> result = new ArrayList<>();
		if (delimiter.isEmpty()) {
			for (int i = 0; i < str.length(); i++) {
				result.add(deleteAny(str.substring(i, i + 1), charsToDelete));
			}
		}
		else {
			int pos = 0;
			int delPos;
			while ((delPos = str.indexOf(delimiter, pos)) != -1) {
				result.add(deleteAny(str.substring(pos, delPos), charsToDelete));
				pos = delPos + delimiter.length();
			}
			if (str.length() > 0 && pos <= str.length()) {
				// Add rest of String, but not in case of empty input.
				result.add(deleteAny(str.substring(pos), charsToDelete));
			}
		}
		return result;
	}

	private static String deleteAny(String inString, String charsToDelete) {
		if (!hasLength(inString) || !hasLength(charsToDelete)) {
			return inString;
		}

		int lastCharIndex = 0;
		char[] result = new char[inString.length()];
		for (int i = 0; i < inString.length(); i++) {
			char c = inString.charAt(i);
			if (charsToDelete.indexOf(c) == -1) {
				result[lastCharIndex++] = c;
			}
		}
		if (lastCharIndex == inString.length()) {
			return inString;
		}
		return new String(result, 0, lastCharIndex);
	}

	private static boolean hasLength(String str) {
		return (str != null && !str.isEmpty());
	}

	/**
	 * Decode the given encoded URI component value. Based on the following rules:
	 * <ul>
	 * <li>Alphanumeric characters {@code "a"} through {@code "z"}, {@code "A"} through {@code "Z"},
	 * and {@code "0"} through {@code "9"} stay the same.</li>
	 * <li>Special characters {@code "-"}, {@code "_"}, {@code "."}, and {@code "*"} stay the same.</li>
	 * <li>A sequence "{@code %<i>xy</i>}" is interpreted as a hexadecimal representation of the character.</li>
	 * </ul>
	 * @param source the encoded String
	 * @param charset the character set
	 * @return the decoded value
	 * @throws IllegalArgumentException when the given source contains invalid encoded sequences
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	private static String uriDecode(String source, Charset charset) {
		int length = source.length();
		if (length == 0) {
			return source;
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
		boolean changed = false;
		for (int i = 0; i < length; i++) {
			int ch = source.charAt(i);
			if (ch == '%') {
				if (i + 2 < length) {
					char hex1 = source.charAt(i + 1);
					char hex2 = source.charAt(i + 2);
					int u = Character.digit(hex1, 16);
					int l = Character.digit(hex2, 16);
					if (u == -1 || l == -1) {
						throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
					}
					baos.write((char) ((u << 4) + l));
					i += 2;
					changed = true;
				}
				else {
					throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
				}
			}
			else {
				baos.write(ch);
			}
		}
		return changed ? baos.toString(charset) : source;
	}

	static PathContainer subPath(PathContainer container, int fromIndex, int toIndex) {
		List<Element> elements = container.elements();
		if (fromIndex == 0 && toIndex == elements.size()) {
			return container;
		}
		if (fromIndex == toIndex) {
			return EMPTY_PATH;
		}

		//Assert.isTrue(fromIndex >= 0 && fromIndex < elements.size(), () -> "Invalid fromIndex: " + fromIndex);
		//Assert.isTrue(toIndex >= 0 && toIndex <= elements.size(), () -> "Invalid toIndex: " + toIndex);
		//Assert.isTrue(fromIndex < toIndex, () -> "fromIndex: " + fromIndex + " should be < toIndex " + toIndex);

		List<Element> subList = elements.subList(fromIndex, toIndex);
		String path = subList.stream().map(Element::value).collect(Collectors.joining(""));
		return new DefaultPathContainer(path, subList);
	}


	private static final class DefaultPathSegment implements PathSegment {

		private static final MultiValueMap<String, String> EMPTY_PARAMS =
				new LinkedMultiValueMap<>();

		private final String value;

		private final String valueToMatch;

		private final MultiValueMap<String, String> parameters;

		/**
		 * Factory for segments without decoding and parsing.
		 */
		static DefaultPathSegment from(String value, DefaultSeparator separator) {
			String valueToMatch = value.contains(separator.encodedSequence()) ?
					value.replaceAll(separator.encodedSequence(), separator.value()) : value;
			return from(value, valueToMatch);
		}

		/**
		 * Factory for decoded and parsed segments.
		 */
		static DefaultPathSegment from(String value, String valueToMatch) {
			return new DefaultPathSegment(value, valueToMatch, EMPTY_PARAMS);
		}

		/**
		 * Factory for decoded and parsed segments.
		 */
		static DefaultPathSegment from(String value, String valueToMatch, MultiValueMap<String, String> params) {
			return new DefaultPathSegment(value, valueToMatch, params);
		}

		private DefaultPathSegment(String value, String valueToMatch, MultiValueMap<String, String> params) {
			this.value = value;
			this.valueToMatch = valueToMatch;
			this.parameters = params;
		}


		@Override
		public String value() {
			return this.value;
		}

		@Override
		public String valueToMatch() {
			return this.valueToMatch;
		}

		@Override
		public char[] valueToMatchAsChars() {
			return this.valueToMatch.toCharArray();
		}

		@Override
		public MultiValueMap<String, String> parameters() {
			return this.parameters;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof PathSegment)) {
				return false;
			}
			return value().equals(((PathSegment) other).value());
		}

		@Override
		public int hashCode() {
			return this.value.hashCode();
		}

		@Override
		public String toString() {
			return "[value='" + this.value + "']";
		}
	}

}

