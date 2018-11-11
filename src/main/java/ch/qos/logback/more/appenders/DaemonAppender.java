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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use UnsynchronizedAppenderBase instead.
 */
@Deprecated
public abstract class DaemonAppender<E> implements Runnable {
    private static ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

    private static final Logger LOG = LoggerFactory.getLogger(DaemonAppender.class);

    private AtomicBoolean start = new AtomicBoolean(false);
    private final BlockingQueue<E> queue;

    DaemonAppender(int maxQueueSize) {
        this.queue = new LinkedBlockingQueue<E>(maxQueueSize);
    }

    protected void execute() {
        if (THREAD_POOL.isShutdown()) {
            THREAD_POOL = Executors.newCachedThreadPool();
        }
        THREAD_POOL.execute(this);
    }

    void log(E eventObject) {
        if (!queue.offer(eventObject)) {
            LOG.warn("Message queue is full. Ignored the message:" + System.lineSeparator() + eventObject.toString());
        } else if (start.compareAndSet(false, true)) {
            execute();
        }
    }

    @Override
    public void run() {

        try {
            for (;;) {
                append(queue.take());
            }
        } catch (InterruptedException e) {
            // ignore the error and rerun.
            run();
        } catch (Exception e) {
            close();
        }
    }

    abstract protected void append(E rawData);

    protected void close() {
        synchronized (THREAD_POOL) {
            if (!THREAD_POOL.isShutdown()) {
                shutdownAndAwaitTermination(THREAD_POOL);
            }
        }
    }

    private static void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
