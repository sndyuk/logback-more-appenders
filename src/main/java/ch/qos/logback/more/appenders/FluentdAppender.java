package ch.qos.logback.more.appenders;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Marker;
import ch.qos.logback.classic.pattern.CallerDataConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.more.appenders.marker.MapMarker;

abstract class FluentdAppender<E> extends UnsynchronizedAppenderBase<E> {

    private static final String DATA_MSG = "msg";
    private static final String DATA_MESSAGE = "message";
    private static final String DATA_LOGGER = "logger";
    private static final String DATA_THREAD = "thread";
    private static final String DATA_LEVEL = "level";
    private static final String DATA_MARKER = "marker";
    private static final String DATA_CALLER = "caller";
    private static final String DATA_THROWABLE = "throwable";

    protected Encoder<E> encoder = new EchoEncoder<E>();
    protected Map<String, String> additionalFields;

    protected Map<String, Object> createData(E event) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(DATA_MSG, encoder.encode(event));

        if (event instanceof ILoggingEvent) {
            ILoggingEvent loggingEvent = (ILoggingEvent) event;
            data.put(DATA_MESSAGE, loggingEvent.getFormattedMessage());
            data.put(DATA_LOGGER, loggingEvent.getLoggerName());
            data.put(DATA_THREAD, loggingEvent.getThreadName());
            data.put(DATA_LEVEL, loggingEvent.getLevel().levelStr);

            Marker marker = loggingEvent.getMarker();
            if (marker != null) {
                if (marker instanceof MapMarker) {
                    MapMarker markerMap = (MapMarker) marker;
                    data.put(DATA_MARKER + "." + markerMap.getName(), markerMap.getMap());
                } else {
                    data.put(DATA_MARKER, marker.toString());
                }
            }
            if (loggingEvent.hasCallerData()) {
                data.put(DATA_CALLER, new CallerDataConverter().convert(loggingEvent));
            }
            if (loggingEvent.getThrowableProxy() != null) {
                data.put(DATA_THROWABLE,
                        ThrowableProxyUtil.asString(loggingEvent.getThrowableProxy()));
            }
            for (Map.Entry<String, String> entry : loggingEvent.getMDCPropertyMap().entrySet()) {
                data.put(entry.getKey(), entry.getValue());
            }
        }

        if (additionalFields != null) {
            data.putAll(additionalFields);
        }
        return data;
    }


    public static class Field {
        private String key;
        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
