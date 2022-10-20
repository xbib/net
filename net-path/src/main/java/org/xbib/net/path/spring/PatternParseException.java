package org.xbib.net.path.spring;

import java.text.MessageFormat;

/**
 * Exception that is thrown when there is a problem with the pattern being parsed.
 */
@SuppressWarnings("serial")
public class PatternParseException extends IllegalArgumentException {

	private final int position;

	private final char[] pattern;

	private final PatternMessage messageType;

	private final Object[] inserts;

	public PatternParseException(int pos, char[] pattern, PatternMessage messageType, Object... inserts) {
		super(messageType.formatMessage(inserts));
		this.position = pos;
		this.pattern = pattern;
		this.messageType = messageType;
		this.inserts = inserts;
	}

	public PatternParseException(Throwable cause, int pos, char[] pattern, PatternMessage messageType, Object... inserts) {
		super(messageType.formatMessage(inserts), cause);
		this.position = pos;
		this.pattern = pattern;
		this.messageType = messageType;
		this.inserts = inserts;
	}

	/**
	 * Return a formatted message with inserts applied.
	 */
	@Override
	public String getMessage() {
		return this.messageType.formatMessage(this.inserts);
	}

	/**
	 * Return a detailed message that includes the original pattern text
	 * with a pointer to the error position, as well as the error message.
	 */
	public String toDetailedString() {
		return String.valueOf(this.pattern) + '\n' +
				" ".repeat(Math.max(0, this.position)) +
				"^\n" +
				getMessage();
	}

	public int getPosition() {
		return this.position;
	}

	public PatternMessage getMessageType() {
		return this.messageType;
	}

	public Object[] getInserts() {
		return this.inserts;
	}


	/**
	 * The messages that can be included in a {@link PatternParseException} when there is a parse failure.
	 */
	public enum PatternMessage {

		MISSING_CLOSE_CAPTURE("Expected close capture character after variable name '}'"),
		MISSING_OPEN_CAPTURE("Missing preceding open capture character before variable name'{'"),
		ILLEGAL_NESTED_CAPTURE("Not allowed to nest variable captures"),
		ILLEGAL_CHARACTER_AT_START_OF_CAPTURE_DESCRIPTOR("Char ''{0}'' not allowed at start of captured variable name"),
		ILLEGAL_CHARACTER_IN_CAPTURE_DESCRIPTOR("Char ''{0}'' is not allowed in a captured variable name"),
		NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST("No more pattern data allowed after '{*...}' or '**' pattern element"),
		MISSING_REGEX_CONSTRAINT("Missing regex constraint on capture"),
		ILLEGAL_DOUBLE_CAPTURE("Not allowed to capture ''{0}'' twice in the same pattern"),
		REGEX_PATTERN_SYNTAX_EXCEPTION("Exception occurred in regex pattern compilation"),
		CAPTURE_ALL_IS_STANDALONE_CONSTRUCT("'{*...}' can only be preceded by a path separator");

		private final String message;

		PatternMessage(String message) {
			this.message = message;
		}

		public String formatMessage(Object... inserts) {
			return MessageFormat.format(this.message, inserts);
		}
	}

}
