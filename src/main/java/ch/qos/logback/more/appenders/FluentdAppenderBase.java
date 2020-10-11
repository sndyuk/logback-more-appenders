/**
 * Copyright (c) 2018 sndyuk <sanada@sndyuk.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package ch.qos.logback.more.appenders;

import static ch.qos.logback.core.CoreConstants.CODES_URL;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Marker;

import ch.qos.logback.classic.pattern.CallerDataConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.more.appenders.marker.MapMarker;

public abstract class FluentdAppenderBase<E> extends AppenderBase<E> {
    private static final String DATA_MESSAGE = "message";
    private static final String DATA_LOGGER = "logger";
    private static final String DATA_THREAD = "thread";
    private static final String DATA_LEVEL = "level";
    private static final String DATA_MARKER = "marker";
    private static final String DATA_CALLER = "caller";
    private static final String DATA_THROWABLE = "throwable";

    protected Encoder<E> encoder;
    protected Map<String, String> additionalFields;
    protected boolean flattenMapMarker;
    protected String mapMarkerPrefix = DATA_MARKER;
    protected String messageFieldKeyName = DATA_MESSAGE;

    protected Map<String, Object> createData(E event) {
        Map<String, Object> data = new HashMap<String, Object>();
        if (event instanceof ILoggingEvent) {
            ILoggingEvent loggingEvent = (ILoggingEvent) event;
            data.put(messageFieldKeyName, encoder != null ? encoder.encode(event) : loggingEvent.getFormattedMessage());
            data.put(DATA_LOGGER, loggingEvent.getLoggerName());
            data.put(DATA_THREAD, loggingEvent.getThreadName());
            data.put(DATA_LEVEL, loggingEvent.getLevel().levelStr);

            Marker marker = loggingEvent.getMarker();
            if (marker != null) {
                if (marker instanceof MapMarker) {
                    extractMapMarker((MapMarker) marker, data);
                } else {
                    data.put(mapMarkerName(), marker.toString());
                    if (marker.hasReferences()) {
                        for (Iterator<Marker> iter = marker.iterator(); iter.hasNext();) {
                            Marker nestedMarker = iter.next();
                            if (nestedMarker instanceof MapMarker) {
                                extractMapMarker((MapMarker) nestedMarker, data);
                            }
                        }
                    }
                }
            }

            if (loggingEvent.hasCallerData()) {
                data.put(DATA_CALLER, new CallerDataConverter().convert(loggingEvent));
            }
            if (loggingEvent.getThrowableProxy() != null) {
                data.put(DATA_THROWABLE, ThrowableProxyUtil.asString(loggingEvent.getThrowableProxy()));
            }
            for (Map.Entry<String, String> entry : loggingEvent.getMDCPropertyMap().entrySet()) {
                data.put(entry.getKey(), entry.getValue());
            }
        } else {
            data.put(messageFieldKeyName, encoder != null ? encoder.encode(event) : event.toString());
        }

        if (additionalFields != null) {
            data.putAll(additionalFields);
        }
        return data;
    }

    private void extractMapMarker(MapMarker mapMarker, Map<String, Object> data) {
        if (flattenMapMarker) {
            data.putAll(mapMarker.getMap());
        } else
            data.put(mapMarkerName(mapMarker), mapMarker.getMap());
    }

    /**
     * Check if string is null or empty
     * 
     * @param string to check
     * @return true if null or empty
     */
    private boolean emptyString(String string) {
        return ((string == null) || (string.isEmpty()));
    }

    /**
     * Get map marker name if not a map
     * 
     * @return maprker name
     */
    private String mapMarkerName() {
        return (emptyString(mapMarkerPrefix)) ? DATA_MARKER : mapMarkerPrefix;
    }

    /**
     * Get map marker name if map is provided
     * 
     * @param mapMarker
     * @return
     */
    private String mapMarkerName(MapMarker mapMarker) {
        if ((mapMarker == null) || (emptyString(mapMarker.getName()))) {
            return mapMarkerName();
        }
        if (emptyString(mapMarkerPrefix)) {
            return mapMarker.getName();
        }
        return mapMarkerPrefix + "." + mapMarker.getName();
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

    /* formerly duplicated code */

    protected String tag;
    protected String remoteHost;
    protected int port;
    protected boolean useEventTime; // Flag to enable/disable usage of eventtime

    public void addAdditionalField(Field field) {
        if (additionalFields == null) {
            additionalFields = new HashMap<String, String>();
        }
        additionalFields.put(field.getKey(), field.getValue());
    }

    @Deprecated
    public void setLayout(Layout<E> layout) {
        addWarn("This appender no longer admits a layout as a sub-component, set an encoder instead.");
        addWarn("To ensure compatibility, wrapping your layout in LayoutWrappingEncoder.");
        addWarn("See also " + CODES_URL + "#layoutInsteadOfEncoder for details");
        LayoutWrappingEncoder<E> lwe = new LayoutWrappingEncoder<E>();
        lwe.setLayout(layout);
        lwe.setContext(context);
        this.setEncoder(lwe);
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * get the value for EventTime usage
     *
     * @return true if EventTime is used, false otherwise
     */
    public boolean isUseEventTime() {
        return this.useEventTime;
    }

    /**
     * Set the value for EventTime usage
     *
     * @param useEventTime the new value
     */
    public void setUseEventTime(boolean useEventTime) {
        this.useEventTime = useEventTime;
    }

    public Encoder<E> getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder<E> encoder) {
        this.encoder = encoder;
    }

    public String getMessageFieldKeyName() {
        return messageFieldKeyName;
    }

    public void setMessageFieldKeyName(String messageFieldKeyName) {
        this.messageFieldKeyName = messageFieldKeyName;
    }

    public boolean isFlattenMapMarker() {
        return flattenMapMarker;
    }

    public void setFlattenMapMarker(boolean flattenMapMarker) {
        this.flattenMapMarker = flattenMapMarker;
    }

    public String getMapMarkerPrefix() {
        return mapMarkerPrefix;
    }

    public void setMapMarkerPrefix(String mapMarkerPrefix) {
        this.mapMarkerPrefix = (mapMarkerPrefix != null) ? mapMarkerPrefix : DATA_MARKER;
    }
}
