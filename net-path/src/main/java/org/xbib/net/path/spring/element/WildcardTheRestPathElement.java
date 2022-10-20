package org.xbib.net.path.spring.element;

import org.xbib.net.path.spring.MatchingContext;

/**
 * A path element representing wildcarding the rest of a path. In the pattern
 * '/foo/**' the /** is represented as a {@link WildcardTheRestPathElement}.
 *
 */
public class WildcardTheRestPathElement extends PathElement {

	public WildcardTheRestPathElement(int pos, char separator) {
		super(pos, separator);
	}

	@Override
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		// If there is more data, it must start with the separator
		if (pathIndex < matchingContext.pathLength && !matchingContext.isSeparator(pathIndex)) {
			return false;
		}
		if (matchingContext.determineRemainingPath) {
			matchingContext.remainingPathIndex = matchingContext.pathLength;
		}
		return true;
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public char[] getChars() {
		return (this.separator + "**").toCharArray();
	}

	@Override
	public int getWildcardCount() {
		return 1;
	}

	@Override
	public String toString() {
		return "WildcardTheRest(" + this.separator + "**)";
	}
}
