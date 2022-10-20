package org.xbib.net.path.spring.element;

import org.xbib.net.path.spring.MatchingContext;
import org.xbib.net.path.spring.PathContainer;
import org.xbib.net.path.spring.util.LinkedMultiValueMap;
import org.xbib.net.path.spring.util.MultiValueMap;

import java.util.List;

/**
 * A path element representing capturing the rest of a path. In the pattern
 * '/foo/{*foobar}' the /{*foobar} is represented as a {@link CaptureTheRestPathElement}.
 */
public class CaptureTheRestPathElement extends PathElement {

	private final String variableName;

	/**
	 * Create a new {@link CaptureTheRestPathElement} instance.
	 * @param pos position of the path element within the path pattern text
	 * @param captureDescriptor a character array containing contents like '{' '*' 'a' 'b' '}'
	 * @param separator the separator used in the path pattern
	 */
	public CaptureTheRestPathElement(int pos, char[] captureDescriptor, char separator) {
		super(pos, separator);
		this.variableName = new String(captureDescriptor, 2, captureDescriptor.length - 3);
	}


	@Override
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		// No need to handle 'match start' checking as this captures everything
		// anyway and cannot be followed by anything else
		// assert next == null

		// If there is more data, it must start with the separator
		if (pathIndex < matchingContext.pathLength && !matchingContext.isSeparator(pathIndex)) {
			return false;
		}
		if (matchingContext.determineRemainingPath) {
			matchingContext.remainingPathIndex = matchingContext.pathLength;
		}
		if (matchingContext.extractingVariables) {
			// Collect the parameters from all the remaining segments
			MultiValueMap<String,String> parametersCollector = null;
			for (int i = pathIndex; i < matchingContext.pathLength; i++) {
				PathContainer.Element element = matchingContext.pathElements.get(i);
				if (element instanceof PathContainer.PathSegment) {
					MultiValueMap<String, String> parameters = ((PathContainer.PathSegment) element).parameters();
					if (!parameters.isEmpty()) {
						if (parametersCollector == null) {
							parametersCollector = new LinkedMultiValueMap<>();
						}
						parametersCollector.addAll(parameters);
					}
				}
			}
			matchingContext.set(this.variableName, pathToString(pathIndex, matchingContext.pathElements),
					parametersCollector == null?NO_PARAMETERS:parametersCollector);
		}
		return true;
	}

	private String pathToString(int fromSegment, List<PathContainer.Element> pathElements) {
		StringBuilder sb = new StringBuilder();
		for (int i = fromSegment, max = pathElements.size(); i < max; i++) {
			PathContainer.Element element = pathElements.get(i);
			if (element instanceof PathContainer.PathSegment) {
				sb.append(((PathContainer.PathSegment)element).valueToMatch());
			}
			else {
				sb.append(element.value());
			}
		}
		return sb.toString();
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public char[] getChars() {
		return ("/{*" + this.variableName + "}").toCharArray();
	}

	@Override
	public int getWildcardCount() {
		return 0;
	}

	@Override
	public int getCaptureCount() {
		return 1;
	}

	@Override
	public String toString() {
		return "CaptureTheRest(/{*" + this.variableName + "})";
	}

}
