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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import ch.qos.logback.more.appenders.IntervalEmitter.EventMapper;
import ch.qos.logback.more.appenders.IntervalEmitter.IntervalAppender;

public class IntervalEmitterTest {

    /**
     * The appender thread adds events while the emitter's Timer thread flushes and swaps the
     * buffer. Every appended event must be emitted exactly once.
     */
    @Test
    public void everyEventIsEmittedWhileTimerFlushesConcurrently() throws Exception {
        final long events = 2_000_000;
        final AtomicLong emitted = new AtomicLong();
        IntervalEmitter<Long, Long> emitter = new IntervalEmitter<Long, Long>(1,
                new EventMapper<Long, Long>() {
                    @Override
                    public Long map(Long event) {
                        return event;
                    }
                }, new IntervalAppender<Long>() {
                    @Override
                    public boolean append(List<Long> batch) {
                        emitted.addAndGet(batch.size());
                        return true;
                    }
                });

        for (long i = 0; i < events; i++) {
            emitter.append(i);
        }

        // Let the timer flush whatever is still buffered.
        long last = -1;
        for (int i = 0; i < 100 && emitted.get() != last; i++) {
            last = emitted.get();
            Thread.sleep(50);
        }

        assertEquals("events lost between append and timer flush", events, emitted.get());
    }

    /**
     * A batch the appender failed to send is retried before newer events, in its original order,
     * and nothing is lost or duplicated.
     */
    @Test
    public void failedBatchIsRetriedFirstInOrder() throws Exception {
        final int events = 100_000;
        final AtomicInteger failuresLeft = new AtomicInteger(3);
        final List<Integer> emitted = Collections.synchronizedList(new ArrayList<Integer>());
        IntervalEmitter<Integer, Integer> emitter = new IntervalEmitter<Integer, Integer>(1,
                new EventMapper<Integer, Integer>() {
                    @Override
                    public Integer map(Integer event) {
                        return event;
                    }
                }, new IntervalAppender<Integer>() {
                    @Override
                    public boolean append(List<Integer> batch) {
                        if (failuresLeft.getAndDecrement() > 0) {
                            return false;
                        }
                        emitted.addAll(batch);
                        return true;
                    }
                });

        for (int i = 0; i < events; i++) {
            emitter.append(i);
        }
        int last = -1;
        for (int i = 0; i < 100 && emitted.size() != last; i++) {
            last = emitted.size();
            Thread.sleep(50);
        }

        assertEquals(events, emitted.size());
        for (int i = 0; i < events; i++) {
            assertEquals(Integer.valueOf(i), emitted.get(i));
        }
    }
}
