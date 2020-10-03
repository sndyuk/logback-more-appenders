/**
 * Copyright (c) 2012 sndyuk <sanada@sndyuk.com>
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

import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import org.fluentd.logger.FluentLogger;

import java.util.HashMap;
import java.util.Map;

import static ch.qos.logback.core.CoreConstants.CODES_URL;

public class DataFluentAppender<E> extends FluentdAppenderBase<E> {
    private FluentLogger fluentLogger;

    @Deprecated
    public void setLayout(Layout<E> layout) {
        addWarn("This appender no longer admits a layout as a sub-component, set an encoder instead.");
        addWarn("To ensure compatibility, wrapping your layout in LayoutWrappingEncoder.");
        addWarn("See also " + CODES_URL + "#layoutInsteadOfEncoder for details");
        LayoutWrappingEncoder<E> lwe = new LayoutWrappingEncoder<E>();
        lwe.setLayout(layout);
        lwe.setContext(context);
        this.encoder = lwe;
    }

    public void setEncoder(Encoder<E> encoder) {
        this.encoder = encoder;
    }

    public void setMessageFieldKeyName(String messageFieldKeyName) { this.messageFieldKeyName = messageFieldKeyName; }

    public void addAdditionalField(Field field) {
        if (additionalFields == null) {
            additionalFields = new HashMap<String, String>();
        }
        additionalFields.put(field.getKey(), field.getValue());
    }

    public boolean isFlattenMapMarker() { return flattenMapMarker; }
    public void setFlattenMapMarker(boolean flattenMapMarker) { this.flattenMapMarker = flattenMapMarker; }

    @Override
    public void start() {
        fluentLogger = FluentLogger.getLogger(label != null ? tag : null, remoteHost, port, getTimeout(), getBufferCapacity());
        super.start();
    }


    @Override
    public void stop() {
        try {
            super.stop();
        } finally {
            try {
                FluentLogger.closeAll();
            } catch (Exception e) {
                // pass
            }
        }
    }

    @Override
    protected void append(E event) {
        Map<String, Object> data = createData(event);

        if (useEventTime) {
            fluentLogger.log(label == null ? tag : label, data, System.currentTimeMillis() / 1000);
        } else {
            fluentLogger.log(label == null ? tag : label, data);
        }
    }

    private String tag;
    private String label;
    private String remoteHost;
    private int port;
    private boolean useEventTime;
    private Integer timeout;
    private Integer bufferCapacity;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public boolean isUseEventTime() {
        return this.useEventTime;
    }

    public void setUseEventTime(boolean useEventTime) {
        this.useEventTime = useEventTime;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout != null ? timeout : 1000;
    }

    public void setBufferCapacity(Integer bufferCapacity) {
        this.bufferCapacity = bufferCapacity;
    }

    public int getBufferCapacity() {
        return bufferCapacity != null ? bufferCapacity : 16777216;
    }
}
