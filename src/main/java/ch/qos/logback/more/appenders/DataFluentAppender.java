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

import java.util.Map;

import org.fluentd.logger.FluentLogger;

public class DataFluentAppender<E> extends FluentdAppenderBase<E> {
    private FluentLogger fluentLogger;

    @Override
    public void start() {
        fluentLogger = FluentLogger.getLogger(getLabel() != null ? getTag() : null, getRemoteHost(), getPort(),
                getTimeout(), getBufferCapacity());
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

        if (isUseEventTime()) {
            fluentLogger.log(getLabel() == null ? getTag() : getLabel(), data, System.currentTimeMillis() / 1000);
        } else {
            fluentLogger.log(getLabel() == null ? getTag() : getLabel(), data);
        }
    }

    private Integer timeout;
    private Integer bufferCapacity;
    private String label;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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
