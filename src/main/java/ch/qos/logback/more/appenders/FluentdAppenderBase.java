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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Marker;
import ch.qos.logback.classic.pattern.CallerDataConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.more.appenders.marker.MapMarker;

abstract class FluentdAppenderBase<E> extends UnsynchronizedAppenderBase<E> {

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
    protected boolean flattenMapMarker;

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
                    extractMapMarker((MapMarker) marker, data);
                } else {
                    data.put(DATA_MARKER, marker.toString());
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

    private void extractMapMarker(MapMarker mapMarker, Map<String, Object> data) {
        if (flattenMapMarker) {
            data.putAll(mapMarker.getMap());
        } else {
            data.put(DATA_MARKER + "." + mapMarker.getName(), mapMarker.getMap());
        }
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
