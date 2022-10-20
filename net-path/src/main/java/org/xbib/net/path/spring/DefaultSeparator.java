package org.xbib.net.path.spring;

class DefaultSeparator implements PathContainer.Separator {

    private final String separator;

    private final String encodedSequence;

    DefaultSeparator(char separator, String encodedSequence) {
        this.separator = String.valueOf(separator);
        this.encodedSequence = encodedSequence;
    }

    @Override
    public String value() {
        return this.separator;
    }

    public String encodedSequence() {
        return this.encodedSequence;
    }
}
