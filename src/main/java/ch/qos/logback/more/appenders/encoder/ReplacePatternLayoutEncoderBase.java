package ch.qos.logback.more.appenders.encoder;

import java.lang.reflect.Field;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.Converter;
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

    public String replace(char[] chars) {
        StringBuilder replaced = new StringBuilder((int)(chars.length * 1.5));
        CHARS: for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            FROM: for (int j = 0; j < from.length; j++) {
                char[] f = from[j];
                for (int k = 0; k < f.length; k++) {
                    if (f[k] != chars[i + k]) {
                        continue FROM;
                    }
                }
                // Found
                replaced.append(to[j]);
                continue CHARS;
            }
            // Not found
            replaced.append(c);
        }
        return replaced.toString();
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
