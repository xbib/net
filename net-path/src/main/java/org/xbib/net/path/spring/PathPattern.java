package org.xbib.net.path.spring;

import org.xbib.net.path.spring.element.CaptureTheRestPathElement;
import org.xbib.net.path.spring.element.PathElement;
import org.xbib.net.path.spring.element.SeparatorPathElement;
import org.xbib.net.path.spring.element.WildcardPathElement;
import org.xbib.net.path.spring.element.WildcardTheRestPathElement;

import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

/**
 * Representation of a parsed path pattern. Includes a chain of path elements
 * for fast matching and accumulates computed state for quick comparison of
 * patterns.
 *
 * <p>{@code PathPattern} matches URL paths using the following rules:<br>
 * <ul>
 * <li>{@code ?} matches one character</li>
 * <li>{@code *} matches zero or more characters within a path segment</li>
 * <li>{@code **} matches zero or more <em>path segments</em> until the end of the path</li>
 * <li><code>{spring}</code> matches a <em>path segment</em> and captures it as a variable named "spring"</li>
 * <li><code>{spring:[a-z]+}</code> matches the regexp {@code [a-z]+} as a path variable named "spring"</li>
 * <li><code>{*spring}</code> matches zero or more <em>path segments</em> until the end of the path
 * and captures it as a variable named "spring"</li>
 * </ul>
 *
 * <p><strong>Note:</strong> In contrast to
 * {@code org.springframework.util.AntPathMatcher}, {@code **} is supported only
 * at the end of a pattern. For example {@code /pages/{**}} is valid but
 * {@code /pages/{**}/details} is not. The same applies also to the capturing
 * variant <code>{*spring}</code>. The aim is to eliminate ambiguity when
 * comparing patterns for specificity.
 *
 * <h3>Examples</h3>
 * <ul>
 * <li>{@code /pages/t?st.html} &mdash; matches {@code /pages/test.html} as well as
 * {@code /pages/tXst.html} but not {@code /pages/toast.html}</li>
 * <li>{@code /resources/*.png} &mdash; matches all {@code .png} files in the
 * {@code resources} directory</li>
 * <li><code>/resources/&#42;&#42;</code> &mdash; matches all files
 * underneath the {@code /resources/} path, including {@code /resources/image.png}
 * and {@code /resources/css/spring.css}</li>
 * <li><code>/resources/{&#42;path}</code> &mdash; matches all files
 * underneath the {@code /resources/}, as well as {@code /resources}, and captures
 * their relative path in a variable named "path"; {@code /resources/image.png}
 * will match with "path" &rarr; "/image.png", and {@code /resources/css/spring.css}
 * will match with "path" &rarr; "/css/spring.css"</li>
 * <li><code>/resources/{filename:\\w+}.dat</code> will match {@code /resources/spring.dat}
 * and assign the value {@code "spring"} to the {@code filename} variable</li>
 * </ul>
 *
 * @see PathContainer
 */
public class PathPattern implements Comparable<PathPattern> {

	private static final PathContainer EMPTY_PATH = PathContainer.parsePath("");

	/**
	 * Comparator that sorts patterns by specificity as follows:
	 * <ol>
	 * <li>Null instances are last.
	 * <li>Catch-all patterns are last.
	 * <li>If both patterns are catch-all, consider the length (longer wins).
	 * <li>Compare wildcard and captured variable count (lower wins).
	 * <li>Consider length (longer wins)
	 * </ol>
	 */
	public static final Comparator<PathPattern> SPECIFICITY_COMPARATOR =
			Comparator.nullsLast(
					Comparator.<PathPattern>
							comparingInt(p -> p.isCatchAll() ? 1 : 0)
							.thenComparingInt(p -> p.isCatchAll() ? scoreByNormalizedLength(p) : 0)
							.thenComparingInt(PathPattern::getScore)
							.thenComparingInt(PathPattern::scoreByNormalizedLength)
			);


	/** The text of the parsed pattern. */
	private final String patternString;

	/** The parser used to construct this pattern. */
	private final PathPatternParser parser;

	/** The options to use to parse a pattern. */
	private final PathContainer.Options pathOptions;

	/** If this pattern has no trailing slash, allow candidates to include one and still match successfully. */
	final boolean matchOptionalTrailingSeparator;

	/** Will this match candidates in a case sensitive way? (case sensitivity  at parse time). */
	private final boolean caseSensitive;

	/** First path element in the parsed chain of path elements for this pattern. */
	
	private final PathElement head;

	/** How many variables are captured in this pattern. */
	private int capturedVariableCount;

	/**
	 * The normalized length is trying to measure the 'active' part of the pattern. It is computed
	 * by assuming all captured variables have a normalized length of 1. Effectively this means changing
	 * your variable name lengths isn't going to change the length of the active part of the pattern.
	 * Useful when comparing two patterns.
	 */
	private int normalizedLength;

	/**
	 * Does the pattern end with '&lt;separator&gt;'.
	 */
	private boolean endsWithSeparatorWildcard = false;

	/**
	 * Score is used to quickly compare patterns. Different pattern components are given different
	 * weights. A 'lower score' is more specific. Current weights:
	 * <ul>
	 * <li>Captured variables are worth 1
	 * <li>Wildcard is worth 100
	 * </ul>
	 */
	private int score;

	/** Does the pattern end with {*...}. */
	private boolean catchAll = false;

	public PathPattern(String patternText, PathPatternParser parser,  PathElement head) {
		this.patternString = patternText;
		this.parser = parser;
		this.pathOptions = parser.getPathOptions();
		this.matchOptionalTrailingSeparator = parser.isMatchOptionalTrailingSeparator();
		this.caseSensitive = parser.isCaseSensitive();
		this.head = head;
		// Compute fields for fast comparison
		PathElement elem = head;
		while (elem != null) {
			this.capturedVariableCount += elem.getCaptureCount();
			this.normalizedLength += elem.getNormalizedLength();
			this.score += elem.getScore();
			if (elem instanceof CaptureTheRestPathElement || elem instanceof WildcardTheRestPathElement) {
				this.catchAll = true;
			}
			if (elem instanceof SeparatorPathElement && elem.next instanceof WildcardPathElement && elem.next.next == null) {
				this.endsWithSeparatorWildcard = true;
			}
			elem = elem.next;
		}
	}

	/**
	 * Return the original String that was parsed to create this PathPattern.
	 */
	public String getPatternString() {
		return this.patternString;
	}

	/**
	 * Whether the pattern string contains pattern syntax that would require
	 * use of {@link #matches(PathContainer)}, or if it is a regular String that
	 * could be compared directly to others.
	 */
	public boolean hasPatternSyntax() {
		return (this.score > 0 || this.catchAll || this.patternString.indexOf('?') != -1);
	}

	/**
	 * Whether this pattern matches the given path.
	 * @param pathContainer the candidate path to attempt to match against
	 * @return {@code true} if the path matches this pattern
	 */
	public boolean matches(PathContainer pathContainer) {
		if (this.head == null) {
			return !hasLength(pathContainer) ||
				(this.matchOptionalTrailingSeparator && pathContainerIsJustSeparator(pathContainer));
		}
		else if (!hasLength(pathContainer)) {
			if (this.head instanceof WildcardTheRestPathElement || this.head instanceof CaptureTheRestPathElement) {
				pathContainer = EMPTY_PATH; // Will allow CaptureTheRest to bind the variable to empty
			}
			else {
				return false;
			}
		}
		MatchingContext matchingContext = new MatchingContext(this, pathContainer, false);
		return this.head.matches(0, matchingContext);
	}

	/**
	 * Match this pattern to the given URI path and return extracted URI template
	 * variables as well as path parameters (matrix variables).
	 * @param pathContainer the candidate path to attempt to match against
	 * @return info object with the extracted variables, or {@code null} for no match
	 */
	public PathMatchInfo matchAndExtract(PathContainer pathContainer) {
		if (this.head == null) {
			return (hasLength(pathContainer) &&
					!(this.matchOptionalTrailingSeparator && pathContainerIsJustSeparator(pathContainer)) ?
					null : PathMatchInfo.EMPTY);
		}
		else if (!hasLength(pathContainer)) {
			if (this.head instanceof WildcardTheRestPathElement || this.head instanceof CaptureTheRestPathElement) {
				pathContainer = EMPTY_PATH; // Will allow CaptureTheRest to bind the variable to empty
			}
			else {
				return null;
			}
		}
		MatchingContext matchingContext = new MatchingContext(this, pathContainer, true);
		return this.head.matches(0, matchingContext) ? matchingContext.getPathMatchResult() : null;
	}

	/**
	 * Match the beginning of the given path and return the remaining portion
	 * not covered by this pattern. This is useful for matching nested routes
	 * where the path is matched incrementally at each level.
	 * @param pathContainer the candidate path to attempt to match against
	 * @return info object with the match result or {@code null} for no match
	 */
	
	public PathRemainingMatchInfo matchStartOfPath(PathContainer pathContainer) {
		if (this.head == null) {
			return new PathRemainingMatchInfo(EMPTY_PATH, pathContainer);
		}
		else if (!hasLength(pathContainer)) {
			return null;
		}

		MatchingContext matchingContext = new MatchingContext(this, pathContainer, true);
		matchingContext.setMatchAllowExtraPath();
		boolean matches = this.head.matches(0, matchingContext);
		if (!matches) {
			return null;
		}
		else {
			PathContainer pathMatched;
			PathContainer pathRemaining;
			if (matchingContext.remainingPathIndex == pathContainer.elements().size()) {
				pathMatched = pathContainer;
				pathRemaining = EMPTY_PATH;
			}
			else {
				pathMatched = pathContainer.subPath(0, matchingContext.remainingPathIndex);
				pathRemaining = pathContainer.subPath(matchingContext.remainingPathIndex);
			}
			return new PathRemainingMatchInfo(pathMatched, pathRemaining, matchingContext.getPathMatchResult());
		}
	}

	/**
	 * Determine the pattern-mapped part for the given path.
	 * <p>For example: <ul>
	 * <li>'{@code /docs/cvs/commit.html}' and '{@code /docs/cvs/commit.html} &rarr; ''</li>
	 * <li>'{@code /docs/*}' and '{@code /docs/cvs/commit}' &rarr; '{@code cvs/commit}'</li>
	 * <li>'{@code /docs/cvs/*.html}' and '{@code /docs/cvs/commit.html} &rarr; '{@code commit.html}'</li>
	 * <li>'{@code /docs/**}' and '{@code /docs/cvs/commit} &rarr; '{@code cvs/commit}'</li>
	 * </ul>
	 * <p><b>Notes:</b>
	 * <ul>
	 * <li>Assumes that {@link #matches} returns {@code true} for
	 * the same path but does <strong>not</strong> enforce this.
	 * <li>Duplicate occurrences of separators within the returned result are removed
	 * <li>Leading and trailing separators are removed from the returned result
	 * </ul>
	 * @param path a path that matches this pattern
	 * @return the subset of the path that is matched by pattern or "" if none
	 * of it is matched by pattern elements
	 */
	public PathContainer extractPathWithinPattern(PathContainer path) {
		List<PathContainer.Element> pathElements = path.elements();
		int pathElementsCount = pathElements.size();

		int startIndex = 0;
		// Find first path element that is not a separator or a literal (i.e. the first pattern based element)
		PathElement elem = this.head;
		while (elem != null) {
			if (elem.getWildcardCount() != 0 || elem.getCaptureCount() != 0) {
				break;
			}
			elem = elem.next;
			startIndex++;
		}
		if (elem == null) {
			// There is no pattern piece
			return PathContainer.parsePath("");
		}

		// Skip leading separators that would be in the result
		while (startIndex < pathElementsCount && (pathElements.get(startIndex) instanceof PathContainer.Separator)) {
			startIndex++;
		}
		int endIndex = pathElements.size();
		// Skip trailing separators that would be in the result
		while (endIndex > 0 && (pathElements.get(endIndex - 1) instanceof PathContainer.Separator)) {
			endIndex--;
		}
		boolean multipleAdjacentSeparators = false;
		for (int i = startIndex; i < (endIndex - 1); i++) {
			if ((pathElements.get(i) instanceof PathContainer.Separator) && (pathElements.get(i+1) instanceof PathContainer.Separator)) {
				multipleAdjacentSeparators=true;
				break;
			}
		}
		PathContainer resultPath = null;
		if (multipleAdjacentSeparators) {
			// Need to rebuild the path without the duplicate adjacent separators
			StringBuilder sb = new StringBuilder();
			int i = startIndex;
			while (i < endIndex) {
				PathContainer.Element e = pathElements.get(i++);
				sb.append(e.value());
				if (e instanceof PathContainer.Separator) {
					while (i < endIndex && (pathElements.get(i) instanceof PathContainer.Separator)) {
						i++;
					}
				}
			}
			resultPath = PathContainer.parsePath(sb.toString(), this.pathOptions);
		}
		else if (startIndex >= endIndex) {
			resultPath = PathContainer.parsePath("");
		}
		else {
			resultPath = path.subPath(startIndex, endIndex);
		}
		return resultPath;
	}

	/**
	 * Compare this pattern with a supplied pattern: return -1,0,+1 if this pattern
	 * is more specific, the same or less specific than the supplied pattern.
	 * The aim is to sort more specific patterns first.
	 */
	@Override
	public int compareTo( PathPattern otherPattern) {
		int result = SPECIFICITY_COMPARATOR.compare(this, otherPattern);
		return (result == 0 && otherPattern != null ?
				this.patternString.compareTo(otherPattern.patternString) : result);
	}

	/**
	 * Combine this pattern with another.
	 */
	public PathPattern combine(PathPattern pattern2string) {
		// If one of them is empty the result is the other. If both empty the result is ""
		if (!hasLength(this.patternString)) {
			if (!hasLength(pattern2string.patternString)) {
				return this.parser.parse("");
			}
			else {
				return pattern2string;
			}
		}
		else if (!hasLength(pattern2string.patternString)) {
			return this;
		}

		// /* + /hotel => /hotel
		// /*.* + /*.html => /*.html
		// However:
		// /usr + /user => /usr/user
		// /{foo} + /bar => /{foo}/bar
		if (!this.patternString.equals(pattern2string.patternString) && this.capturedVariableCount == 0 &&
				matches(PathContainer.parsePath(pattern2string.patternString))) {
			return pattern2string;
		}

		// /hotels/* + /booking => /hotels/booking
		// /hotels/* + booking => /hotels/booking
		if (this.endsWithSeparatorWildcard) {
			return this.parser.parse(concat(
					this.patternString.substring(0, this.patternString.length() - 2),
					pattern2string.patternString));
		}

		// /hotels + /booking => /hotels/booking
		// /hotels + booking => /hotels/booking
		int starDotPos1 = this.patternString.indexOf("*.");  // Are there any file prefix/suffix things to consider?
		if (this.capturedVariableCount != 0 || starDotPos1 == -1 || getSeparator() == '.') {
			return this.parser.parse(concat(this.patternString, pattern2string.patternString));
		}

		// /*.html + /hotel => /hotel.html
		// /*.html + /hotel.* => /hotel.html
		String firstExtension = this.patternString.substring(starDotPos1 + 1);  // looking for the first extension
		String p2string = pattern2string.patternString;
		int dotPos2 = p2string.indexOf('.');
		String file2 = (dotPos2 == -1 ? p2string : p2string.substring(0, dotPos2));
		String secondExtension = (dotPos2 == -1 ? "" : p2string.substring(dotPos2));
		boolean firstExtensionWild = (firstExtension.equals(".*") || firstExtension.isEmpty());
		boolean secondExtensionWild = (secondExtension.equals(".*") || secondExtension.isEmpty());
		if (!firstExtensionWild && !secondExtensionWild) {
			throw new IllegalArgumentException(
					"Cannot combine patterns: " + this.patternString + " and " + pattern2string);
		}
		return this.parser.parse(file2 + (firstExtensionWild ? secondExtension : firstExtension));
	}

	public static boolean hasLength(String str) {
		return (str != null && !str.isEmpty());
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof PathPattern)) {
			return false;
		}
		PathPattern otherPattern = (PathPattern) other;
		return (this.patternString.equals(otherPattern.getPatternString()) &&
				getSeparator() == otherPattern.getSeparator() &&
				this.caseSensitive == otherPattern.caseSensitive);
	}

	@Override
	public int hashCode() {
		return (this.patternString.hashCode() + getSeparator()) * 17 + (this.caseSensitive ? 1 : 0);
	}

	@Override
	public String toString() {
		return this.patternString;
	}

	int getScore() {
		return this.score;
	}

	boolean isCatchAll() {
		return this.catchAll;
	}

	/**
	 * The normalized length is trying to measure the 'active' part of the pattern. It is computed
	 * by assuming all capture variables have a normalized length of 1. Effectively this means changing
	 * your variable name lengths isn't going to change the length of the active part of the pattern.
	 * Useful when comparing two patterns.
	 */
	int getNormalizedLength() {
		return this.normalizedLength;
	}

	char getSeparator() {
		return this.pathOptions.separator();
	}

	int getCapturedVariableCount() {
		return this.capturedVariableCount;
	}

	String toChainString() {
		StringJoiner stringJoiner = new StringJoiner(" ");
		PathElement pe = this.head;
		while (pe != null) {
			stringJoiner.add(pe.toString());
			pe = pe.next;
		}
		return stringJoiner.toString();
	}

	/**
	 * Return the string form of the pattern built from walking the path element chain.
	 * @return the string form of the pattern
	 */
	String computePatternString() {
		StringBuilder sb = new StringBuilder();
		PathElement pe = this.head;
		while (pe != null) {
			sb.append(pe.getChars());
			pe = pe.next;
		}
		return sb.toString();
	}
	
	PathElement getHeadSection() {
		return this.head;
	}

	/**
	 * Join two paths together including a separator if necessary.
	 * Extraneous separators are removed (if the first path
	 * ends with one and the second path starts with one).
	 * @param path1 first path
	 * @param path2 second path
	 * @return joined path that may include separator if necessary
	 */
	private String concat(String path1, String path2) {
		boolean path1EndsWithSeparator = (path1.charAt(path1.length() - 1) == getSeparator());
		boolean path2StartsWithSeparator = (path2.charAt(0) == getSeparator());
		if (path1EndsWithSeparator && path2StartsWithSeparator) {
			return path1 + path2.substring(1);
		}
		else if (path1EndsWithSeparator || path2StartsWithSeparator) {
			return path1 + path2;
		}
		else {
			return path1 + getSeparator() + path2;
		}
	}

	/**
	 * Return if the container is not null and has more than zero elements.
	 * @param container a path container
	 * @return {@code true} has more than zero elements
	 */
	private boolean hasLength( PathContainer container) {
		return container != null && container.elements().size() > 0;
	}

	private static int scoreByNormalizedLength(PathPattern pattern) {
		return -pattern.getNormalizedLength();
	}

	private boolean pathContainerIsJustSeparator(PathContainer pathContainer) {
		return pathContainer.value().length() == 1 &&
				pathContainer.value().charAt(0) == getSeparator();
	}
}
