package ch.qos.logback.more.appenders.encoder;

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
        public void write(Converter<ILoggingEvent> converter, ILoggingEvent event,
                StringBuilder buf) {
            try {
                int start = buf.length();
                converter.write(buf, event);
                int end = buf.length();
                if (compressSpace) {
                    if (converter instanceof LiteralConverter) {
                        
                        int segmentEndIndex = -1;
                        int segmentStartIndex = -1;
                        for (int i = start; i < end; i++) {
                            char c = buf.charAt(i);
                            if (segmentEndIndex == -1 && (c == '\n' || c == ' ')) {
                                segmentEndIndex = i;
                            }
                            if (c == '"' || c == '}') {
                                segmentStartIndex = i;
                            }
                            if (segmentEndIndex >= 0 && segmentStartIndex >= segmentEndIndex) {
                                buf.replace(segmentEndIndex, segmentStartIndex, " ");
                                end -= (segmentStartIndex - segmentEndIndex);
                                segmentEndIndex = -1;
                                segmentStartIndex = -1;
                            }
                        }
                    }
                }
                if (converter instanceof DynamicConverter) {
                    char[] sub = new char[end - start];
                    buf.getChars(start, end, sub, 0);
                    String to = replace(sub);
                    buf.replace(start, end, to != null ? to : "");
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
