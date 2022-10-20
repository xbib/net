package org.xbib.net.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Function;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

public class ThreadLoggingFormatter extends Formatter {

    private static final String key = "org.xbib.net.util.ThreadLoggingFormatter.format";

    private static final String DEFAULT_FORMAT =
            "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$-7s [%3$s] [%5$s] %6$s %7$s%n";

    ThreadMXBean threadMXBean;

    public ThreadLoggingFormatter() {
        this.threadMXBean = ManagementFactory.getThreadMXBean();
    }

    @Override
    public String format(LogRecord record) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());
        String source;
        if (record.getSourceClassName() != null) {
            source = record.getSourceClassName();
            if (record.getSourceMethodName() != null) {
                source += " " + record.getSourceMethodName();
            }
        } else {
            source = record.getLoggerName();
        }
        String message = formatMessage(record);
        String throwable = "";
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            pw.close();
            throwable = sw.toString();
        }
        return String.format(format,
                zdt,
                source,
                record.getLoggerName(),
                record.getLevel().getName(),
                threadMXBean.getThreadInfo(record.getLongThreadID()).getThreadName(),
                message,
                throwable);
    }

    private static String getLoggingProperty(String name) {
        return LogManager.getLogManager().getProperty(name);
    }

    private static final String format = getSimpleFormat(ThreadLoggingFormatter::getLoggingProperty);

    private static String getSimpleFormat(Function<String, String> defaultPropertyGetter) {
        String format = System.getProperty(key);
        if (format == null && defaultPropertyGetter != null) {
            format = defaultPropertyGetter.apply(key);
        }
        if (format != null) {
            try {
                // validate the user-defined format string
                String.format(format, ZonedDateTime.now(), "", "", "", "", "");
            } catch (IllegalArgumentException e) {
                // illegal syntax; fall back to the default format
                format = DEFAULT_FORMAT;
            }
        } else {
            format = DEFAULT_FORMAT;
        }
        return format;
    }

}
