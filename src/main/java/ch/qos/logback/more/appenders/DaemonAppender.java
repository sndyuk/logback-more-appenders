/**
 * Copyright (c) 2012 sndyuk <sanada@sndyuk.com>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
            LOG.warn("Message queue is full. Ignore the message.");
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
