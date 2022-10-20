package org.xbib.net.path.spring.element;

import org.xbib.net.path.spring.MatchingContext;

/**
 * A separator path element. In the pattern '/foo/bar' the two occurrences
 * of '/' will be represented by a SeparatorPathElement (if the default
 * separator of '/' is being used).
 */
public class SeparatorPathElement extends PathElement {

	public SeparatorPathElement(int pos, char separator) {
		super(pos, separator);
	}

	/**
	 * Matching a separator is easy, basically the character at candidateIndex
	 * must be the separator.
	 */
	@Override
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		if (pathIndex < matchingContext.pathLength && matchingContext.isSeparator(pathIndex)) {
			if (isNoMorePattern()) {
				if (matchingContext.determineRemainingPath) {
					matchingContext.remainingPathIndex = pathIndex + 1;
					return true;
				}
				else {
					return (pathIndex + 1 == matchingContext.pathLength);
				}
			}
			else {
				pathIndex++;
				return (this.next != null && this.next.matches(pathIndex, matchingContext));
			}
		}
		return false;
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public char[] getChars() {
		return new char[] {this.separator};
	}


	@Override
	public String toString() {
		return "Separator(" + this.separator + ")";
	}

}
