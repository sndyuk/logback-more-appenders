package ch.qos.logback.more.appenders.encoder;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.DynamicConverter;
import ch.qos.logback.core.pattern.LiteralConverter;

/**
 * JSON encoder. It just escape meta characters of JSON not to lose performance. It doesn't handle
 * null value.
 *
 * @author sndyuk
 */
public class FastJsonEncoder extends ReplacePatternLayoutEncoderBase {

    private static final char[][] FROM = {{'\b'}, {'\f'}, {'\n'}, {'\r'}, {'\t'}, {'\"'}, {'\\'}};
    private static final char[][] TO = {{'\\', 'b'}, {'\\', 'f'}, {'\\', 'n'}, {'\\', 'r'},
            {'\\', 't'}, {'\\', '"'}, {'\\', '\\'}};

    private static final ReplacePattern JSON_REPLACE_PATTERN = new ReplacePattern(FROM, TO);

    private boolean compressSpace;

    public void setCompressSpace(boolean compressSpace) {
        this.compressSpace = compressSpace;
    }

    private final class JsonWriter implements Writer {
        @Override
        public void write(Converter<ILoggingEvent> converter, ILoggingEvent event, StringBuilder buf) {
            try {
                int start, end;
                if (converter instanceof ThrowableProxyConverter) {
                    if (event.getThrowableProxy() == null) {
                        return;
                    }
                    int closing = buf.lastIndexOf("}");
                    String closingStr = buf.substring(closing);
                    buf.delete(closing, buf.length()).append(",\"stacktrace\":\"");
                    start = buf.length();
                    converter.write(buf, event);
                    end = buf.length();
                    buf.append("\"");
                    buf.append(closingStr);
                } else {
                    start = buf.length();
                    converter.write(buf, event);
                    end = buf.length();
                }

                if (end == start) {
                    return;
                }
                if (compressSpace) {
                    if (converter instanceof LiteralConverter) {
                        int spaceStart = -1, spaceEnd = -1, spaceRemoved = 0;
                        boolean withinValue = false;
                        for (int i = start + 1; i < end - spaceRemoved; i++) {
                            char c = buf.charAt(i);
                            if (!withinValue) {
                              boolean space = c == '\r' || c == '\n' || c == ' ' || c == '\t';
                              if (spaceStart == -1 && space) {
                                spaceStart = i;
                              } else if (spaceStart >= 0 && !space) {
                                spaceEnd = i;
                              }
                              if (spaceEnd > spaceStart) {
                                buf.delete(spaceStart, spaceEnd);
                                spaceRemoved += spaceEnd - spaceStart;
                                spaceStart = -1;
                                spaceEnd = -1;
                              }
                            }
                            if (c == '"' && buf.charAt(i - 1) != '\\') {
                                withinValue = !withinValue;
                                if (withinValue) {
                                    spaceStart = -1;
                                    spaceEnd = -1;
                                }
                            }
                        }
                    }
                }
                if (converter instanceof DynamicConverter) {
                    replace(buf, start, end);
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    private final JsonWriter writer = new JsonWriter();

    @Override
    protected ReplacePattern getReplacePattern() {
        return JSON_REPLACE_PATTERN;
    }

    @Override
    protected Writer getWriter() {
        return writer;
    }
}
