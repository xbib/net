package org.xbib.net.path.structure;

import java.util.List;
import java.util.regex.Pattern;

public class PathSegment implements Comparable<PathSegment> {

    private String string;

    private Pattern pattern;

    private List<String> parameterNames;

    private boolean isCatchAll;

    public PathSegment() {
    }

    public void setString(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setParameterNames(List<String> parameterNames) {
        this.parameterNames = parameterNames;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }

    public void setCatchAll(boolean catchAll) {
        this.isCatchAll = catchAll;
    }

    public boolean isCatchAll() {
        return isCatchAll;
    }

    @Override
    public String toString() {
        return "PathSegment[" +
                "string='" + string + '\'' +
                ", pattern=" + pattern +
                ", parameterNames=" + parameterNames +
                ", isCatchAll=" + isCatchAll +
                "]";
    }

    @Override
    public int compareTo(PathSegment o) {
        Integer prio1 = isCatchAll ? 0 : pattern != null ? 1 : string != null ? 2 : -1;
        Integer prio2 = o.isCatchAll ? 0 : o.pattern != null ? 1 : o.string != null ? 2 : -1;
        return prio1.compareTo(prio2);
    }
}
