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
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;

public class IntervalEmitter<E, R> {
    private volatile long lastEmit = -1;
    private long maxInterval;
    private Timer timer;
    // A single buffer for the emitter's whole lifetime. It is never swapped, so an event added
    // concurrently with a flush either goes out in this flush or stays for the next one.
    private final ConcurrentLinkedDeque<R> events = new ConcurrentLinkedDeque<R>();
    // Serializes flushes (the appender thread and the timer thread) so batches are still sent
    // one at a time. Adding events never takes this lock.
    private final ReentrantLock flushLock = new ReentrantLock();
    private final EventMapper<E, R> eventMapper;
    private final IntervalAppender<R> appender;

    IntervalEmitter(long maxInterval, EventMapper<E, R> eventMapper, IntervalAppender<R> appender) {
        this.maxInterval = maxInterval;
        this.eventMapper = eventMapper;
        this.appender = appender;
        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                IntervalEmitter.this.append(null);
            }
        }, this.maxInterval, this.maxInterval);
    }

    void append(E event) {
        if (event != null) {
            events.add(eventMapper.map(event));
        }
        long now = System.currentTimeMillis();
        // If another flush is running, skip: the event is already buffered and will be sent next.
        if (now > lastEmit + maxInterval && flushLock.tryLock()) {
            try {
                if (emit(drain())) {
                    lastEmit = now;
                }
            } finally {
                flushLock.unlock();
            }
        }
    }

    private List<R> drain() {
        List<R> batch = new ArrayList<R>();
        for (R e; (e = events.poll()) != null;) {
            batch.add(e);
        }
        return batch;
    }

    public boolean emit(List<R> batch) {
        if (batch.isEmpty()) {
            return true;
        }
        if (appender.append(batch)) {
            return true;
        }
        // Put the failed batch back at the head, keeping its order, so it is retried first.
        for (ListIterator<R> it = batch.listIterator(batch.size()); it.hasPrevious();) {
            events.addFirst(it.previous());
        }
        return false;
    }

    public void emitForShutdown(long waitMillis, int retry) {
        flushLock.lock();
        try {
            for (int i = 0; i < retry; i++) {
                if (emit(drain())) {
                    return;
                }
                Thread.sleep(waitMillis);
            }
        } catch (InterruptedException e) {
            // Ignore
        } finally {
            flushLock.unlock();
        }
        System.err.println("Could not write the log: " + events);
    }

    public interface EventMapper<E, R> {
        R map(E event);
    }

    public interface IntervalAppender<R> {
        // Threadsafe
        boolean append(List<R> events);
    }
}
