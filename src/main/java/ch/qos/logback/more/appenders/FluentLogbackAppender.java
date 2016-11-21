/**
 * Copyright (c) 2012 sndyuk <sanada@sndyuk.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.more.appenders;

import java.util.HashMap;
import java.util.Map;

import org.fluentd.logger.FluentLogger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public class FluentLogbackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final int MSG_SIZE_LIMIT = 65535;

    private FluentLogger fluentLogger;

    @Override
    public void start() {
        super.start();

        this.fluentLogger = FluentLogger.getLogger(label != null ? tag : null, remoteHost, port);
    }

    @Override
    protected void append(ILoggingEvent rawData) {
        String msg = null;
        if (layout != null) {
            msg = layout.doLayout(rawData);
        } else {
            msg = rawData.toString();
        }
        if (msg != null && msg.length() > MSG_SIZE_LIMIT) {
            msg = msg.substring(0, MSG_SIZE_LIMIT);
        }
        Map<String, Object> data = new HashMap<String, Object>(1);
        data.put("msg", msg);
        if (label == null) {
            fluentLogger.log(tag, data);
        } else {
            fluentLogger.log(label, data);
        }
    }

    @Override
    public void stop() {
        try {
            super.stop();
        } finally {
            if (fluentLogger != null) {
                fluentLogger.close();
            }
        }
    }

    private String tag;
    private String label;
    private String remoteHost;
    private int port;
    private Layout<ILoggingEvent> layout;

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

    public Layout<ILoggingEvent> getLayout() {
        return layout;
    }

    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }
}