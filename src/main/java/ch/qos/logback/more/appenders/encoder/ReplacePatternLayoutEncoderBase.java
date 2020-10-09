package ch.qos.logback.more.appenders.encoder;

import java.lang.reflect.Field;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.FormattingConverter;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;

public abstract class ReplacePatternLayoutEncoderBase extends PatternLayoutEncoderBase<ILoggingEvent> {

    public interface Writer {
        void write(Converter<ILoggingEvent> converter, ILoggingEvent event, StringBuilder buf);
    }

    protected static class ReplacePattern {

        private final char[][] from;
        private final char[][] to;

        public ReplacePattern(char[][] from, char[][] to) {
            this.from = from;
            this.to = to;
        }
    }

    protected abstract ReplacePattern getReplacePattern();
    protected abstract Writer getWriter();

    private char[][] from;
    private char[][] to;

    @Override
    public void start() {
        ReplacePattern replacePattern = getReplacePattern();
        this.from = replacePattern.from;
        this.to = replacePattern.to;
        ReplacePatternLayout patternLayout =
                new ReplacePatternLayout(getWriter());
        patternLayout.setContext(context);
        patternLayout.setPattern(getPattern());
        patternLayout.setOutputPatternAsHeader(outputPatternAsHeader);
        patternLayout.start();
        this.layout = patternLayout;
        super.start();
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        return super.encode(event);
    }

    public void replace(StringBuilder buff, int start, int end) {
        int modified = 0;
        CHARS: for (int i = start; i < end + modified; i++) {
            FROM: for (int j = 0; j < from.length; j++) {
                char[] f = from[j];
                if (i + f.length > buff.length()) {
                    continue FROM;
                }
                for (int k = 0; k < f.length; k++) {
                    if (f[k] != buff.charAt(i + k)) {
                        continue FROM;
                    }
                }
                // Found
                buff.delete(i, i + f.length);
                buff.insert(i, to[j]);
                int d = (to[j].length - f.length);
                i = i + d;
                modified = modified + d;
                continue CHARS;
            }
        }
    }

    protected static class ReplacePatternLayout extends PatternLayout {
        Converter<ILoggingEvent> head;
        private Writer writer;

        public ReplacePatternLayout(Writer writer) {
            this.writer = writer;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void start() {
            super.start();
            try {
                Field headField = PatternLayoutBase.class.getDeclaredField("head");
                headField.setAccessible(true);
                head = (Converter<ILoggingEvent>) headField.get(this);
                headField.setAccessible(false);
            } catch (Exception e) {
                throw new RuntimeException("Please raise the issue.", e);
            }
        }

        @Override
        protected String writeLoopOnConverters(ILoggingEvent event) {
            super.writeLoopOnConverters(event);
            StringBuilder buf = new StringBuilder(256);
            Converter<ILoggingEvent> c = head;
            while (c != null) {
                writer.write(c, event, buf);
                c = c.getNext();
            }
            return buf.toString();
        }
    }
}
