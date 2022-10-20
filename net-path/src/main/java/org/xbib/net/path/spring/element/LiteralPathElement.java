package org.xbib.net.path.spring.element;

import org.xbib.net.path.spring.MatchingContext;
import org.xbib.net.path.spring.PathContainer;

/**
 * A literal path element. In the pattern '/foo/bar/goo' there are three
 * literal path elements 'foo', 'bar' and 'goo'.
 */
public class LiteralPathElement extends PathElement {

	private final char[] text;

	private final int len;

	private final boolean caseSensitive;

	public LiteralPathElement(int pos, char[] literalText, boolean caseSensitive, char separator) {
		super(pos, separator);
		this.len = literalText.length;
		this.caseSensitive = caseSensitive;
		if (caseSensitive) {
			this.text = literalText;
		}
		else {
			// Force all the text lower case to make matching faster
			this.text = new char[literalText.length];
			for (int i = 0; i < this.len; i++) {
				this.text[i] = Character.toLowerCase(literalText[i]);
			}
		}
	}

	@Override
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		if (pathIndex >= matchingContext.pathLength) {
			// no more path left to match this element
			return false;
		}
		PathContainer.Element element = matchingContext.pathElements.get(pathIndex);
		if (!(element instanceof PathContainer.PathSegment)) {
			return false;
		}
		String value = ((PathContainer.PathSegment)element).valueToMatch();
		if (value.length() != this.len) {
			// Not enough data to match this path element
			return false;
		}

		if (this.caseSensitive) {
			for (int i = 0; i < this.len; i++) {
				if (value.charAt(i) != this.text[i]) {
					return false;
				}
			}
		}
		else {
			for (int i = 0; i < this.len; i++) {
				// TODO revisit performance if doing a lot of case insensitive matching
				if (Character.toLowerCase(value.charAt(i)) != this.text[i]) {
					return false;
				}
			}
		}

		pathIndex++;
		if (isNoMorePattern()) {
			if (matchingContext.determineRemainingPath) {
				matchingContext.remainingPathIndex = pathIndex;
				return true;
			}
			else {
				if (pathIndex == matchingContext.pathLength) {
					return true;
				}
				else {
					return (matchingContext.isMatchOptionalTrailingSeparator() &&
							(pathIndex + 1) == matchingContext.pathLength &&
							matchingContext.isSeparator(pathIndex));
				}
			}
		}
		else {
			return (this.next != null && this.next.matches(pathIndex, matchingContext));
		}
	}

	@Override
	public int getNormalizedLength() {
		return this.len;
	}

	@Override
	public char[] getChars() {
		return this.text;
	}


	@Override
	public String toString() {
		return "Literal(" + String.valueOf(this.text) + ")";
	}

}
