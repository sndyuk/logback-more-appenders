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

import java.util.ArrayList;
import java.util.List;

public class IntervalEmitter<E, R> {
    private long lastEmit = -1;
    private long maxInterval;
    private final List<R> events;
    private final EventMapper<E, R> eventMapper;
    private final IntervalAppender<R> appender;

    IntervalEmitter(long maxInterval, EventMapper<E, R> eventMapper, IntervalAppender<R> appender) {
        this.events = new ArrayList<R>();
        this.maxInterval = maxInterval;
        this.eventMapper = eventMapper;
        this.appender = appender;
    }

    void append(E event) {
        events.add(eventMapper.map(event));

        long now = System.currentTimeMillis();
        if (now > lastEmit + maxInterval) {
            emit();
            lastEmit = now;
        }
    }

    public void emit() {
        if (!events.isEmpty() && appender.append(events)) {
            events.clear();
        }
    }

    public interface EventMapper<E, R> {
        R map(E event);
    }

    public interface IntervalAppender<R> {
        boolean append(List<R> events);
    }
}
