/**
 * Copyright (c) 2012 sndyuk <sanada@sndyuk.com>
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.more.appenders;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.komamitsu.fluency.EventTime;
import org.komamitsu.fluency.Fluency;
import org.komamitsu.fluency.fluentd.FluencyBuilderForFluentd;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class FluencyLogbackAppender<E> extends FluentdAppenderBase<E> {

    private Fluency fluency;

    @Override
    public void start() {
        try {
            FluencyBuilderForFluentd builder = configureFluency();
            if (getRemoteHost() != null && getPort() > 0
                    && (remoteServers == null || remoteServers.getRemoteServers().size() == 0)) {
                this.fluency = builder.build(getRemoteHost(), getPort());
            } else {
                this.fluency = builder.build(configureServers());
            }
            super.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void append(E event) {
        Map<String, Object> data = createData(event);
        try {
            String tag = getTag() == null ? "" : getTag();
            if (isUseEventTime()) {
                EventTime eventTime;
                if (event instanceof ILoggingEvent) {
                    long timeStampInMs = ((ILoggingEvent) event).getTimeStamp();
                    eventTime = EventTime.fromEpochMilli(timeStampInMs);
                } else if (event instanceof IAccessEvent) {
                    long timeStampInMs = ((IAccessEvent) event).getTimeStamp();
                    eventTime = EventTime.fromEpochMilli(timeStampInMs);
                } else {
                    eventTime = EventTime.fromEpochMilli(System.currentTimeMillis());
                }
                fluency.emit(tag, eventTime, data);
            } else {
                fluency.emit(tag, data);
            }
        } catch (IOException e) {
            // pass
            addError("Fluency throws the error and the message has been omitted. " + data, e);
        }
    }

    @Override
    public void stop() {
        try {
            super.stop();
        } finally {
            try {
                fluency.flush();
                long maxWaitMillis = Math.min((waitUntilBufferFlushed != null ? waitUntilBufferFlushed : 1)
                        + (waitUntilFlusherTerminated != null ? waitUntilFlusherTerminated : 1), 5) * 1000;
                Thread.sleep(maxWaitMillis);
                fluency.close();
            } catch (Exception e) {
                // pass
            }
        }
    }

    private RemoteServers remoteServers;
    private boolean ackResponseMode;
    private String fileBackupDir;
    private Integer bufferChunkInitialSize;
    private Integer bufferChunkRetentionSize;
    private Integer bufferChunkRetentionTimeMillis;
    private Long maxBufferSize;
    private Integer connectionTimeoutMilli;
    private Integer readTimeoutMilli;
    private Integer waitUntilBufferFlushed;
    private Integer waitUntilFlusherTerminated;
    private Integer flushAttemptIntervalMillis;
    private Integer senderMaxRetryCount;
    private boolean sslEnabled;
    private Boolean jvmHeapBufferMode;

    public RemoteServers getRemoteServers() {
        return remoteServers;
    }

    public void setRemoteServers(RemoteServers remoteServers) {
        this.remoteServers = remoteServers;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean useSsl) {
        this.sslEnabled = useSsl;
    }

    public boolean isAckResponseMode() {
        return ackResponseMode;
    }

    public void setAckResponseMode(boolean ackResponseMode) {
        this.ackResponseMode = ackResponseMode;
    }

    public String getFileBackupDir() {
        return fileBackupDir;
    }

    public void setFileBackupDir(String fileBackupDir) {
        this.fileBackupDir = fileBackupDir;
    }

    public Integer getBufferChunkInitialSize() {
        return bufferChunkInitialSize;
    }

    public void setBufferChunkInitialSize(Integer bufferChunkInitialSize) {
        this.bufferChunkInitialSize = bufferChunkInitialSize;
    }

    public Integer getBufferChunkRetentionSize() {
        return bufferChunkRetentionSize;
    }

    public void setBufferChunkRetentionSize(Integer bufferChunkRetentionSize) {
        this.bufferChunkRetentionSize = bufferChunkRetentionSize;
    }

    public Integer getBufferChunkRetentionTimeMillis() {
        return bufferChunkRetentionTimeMillis;
    }

    public void setBufferChunkRetentionTimeMillis(Integer bufferChunkRetentionTimeMillis) {
        this.bufferChunkRetentionTimeMillis = bufferChunkRetentionTimeMillis;
    }

    public Long getMaxBufferSize() {
        return maxBufferSize;
    }

    public void setMaxBufferSize(Long maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
    }

    public Integer getConnectionTimeoutMilli() {
        return connectionTimeoutMilli;
    }

    public void setConnectionTimeoutMilli(Integer connectionTimeoutMilli) {
        this.connectionTimeoutMilli = connectionTimeoutMilli;
    }

    public Integer getReadTimeoutMilli() {
        return readTimeoutMilli;
    }

    public void setReadTimeoutMilli(Integer readTimeoutMilli) {
        this.readTimeoutMilli = readTimeoutMilli;
    }

    public Integer getWaitUntilBufferFlushed() {
        return waitUntilBufferFlushed;
    }

    public void setWaitUntilBufferFlushed(Integer waitUntilBufferFlushed) {
        this.waitUntilBufferFlushed = waitUntilBufferFlushed;
    }

    public Integer getWaitUntilFlusherTerminated() {
        return waitUntilFlusherTerminated;
    }

    public void setWaitUntilFlusherTerminated(Integer waitUntilFlusherTerminated) {
        this.waitUntilFlusherTerminated = waitUntilFlusherTerminated;
    }

    public Integer getFlushAttemptIntervalMillis() {
        return flushAttemptIntervalMillis;
    }

    public void setFlushAttemptIntervalMillis(Integer flushAttemptIntervalMillis) {
        this.flushAttemptIntervalMillis = flushAttemptIntervalMillis;
    }

    public Integer getSenderMaxRetryCount() {
        return senderMaxRetryCount;
    }

    public void setSenderMaxRetryCount(Integer senderMaxRetryCount) {
        this.senderMaxRetryCount = senderMaxRetryCount;
    }

    public Boolean getJvmHeapBufferMode() {
        return jvmHeapBufferMode;
    }

    public void setJvmHeapBufferMode(Boolean jvmHeapBufferMode) {
        this.jvmHeapBufferMode = jvmHeapBufferMode;
    }

    protected FluencyBuilderForFluentd configureFluency() {
        FluencyBuilderForFluentd builder = new FluencyBuilderForFluentd();

        builder.setAckResponseMode(ackResponseMode);
        if (fileBackupDir != null) {
            builder.setFileBackupDir(fileBackupDir);
        }
        if (bufferChunkInitialSize != null) {
            builder.setBufferChunkInitialSize(bufferChunkInitialSize);
        }
        if (bufferChunkRetentionSize != null) {
            builder.setBufferChunkRetentionSize(bufferChunkRetentionSize);
        }
        if (bufferChunkRetentionTimeMillis != null) {
            builder.setBufferChunkRetentionTimeMillis(bufferChunkRetentionTimeMillis);
        }
        if (maxBufferSize != null) {
            builder.setMaxBufferSize(maxBufferSize);
        }
        if (connectionTimeoutMilli != null) {
            builder.setConnectionTimeoutMilli(connectionTimeoutMilli);
        }
        if (readTimeoutMilli != null) {
            builder.setReadTimeoutMilli(readTimeoutMilli);
        }
        if (waitUntilBufferFlushed != null) {
            builder.setWaitUntilBufferFlushed(waitUntilBufferFlushed);
        }
        if (waitUntilFlusherTerminated != null) {
            builder.setWaitUntilFlusherTerminated(waitUntilFlusherTerminated);
        }
        if (flushAttemptIntervalMillis != null) {
            builder.setFlushAttemptIntervalMillis(flushAttemptIntervalMillis);
        }
        if (senderMaxRetryCount != null) {
            builder.setSenderMaxRetryCount(senderMaxRetryCount);
        }
        builder.setSslEnabled(sslEnabled);
        if (jvmHeapBufferMode != null) {
            builder.setJvmHeapBufferMode(jvmHeapBufferMode);
        }

        return builder;
    }

    protected List<InetSocketAddress> configureServers() {
        List<InetSocketAddress> dest = new ArrayList<InetSocketAddress>();
        if (getRemoteHost() != null && getPort() > 0) {
            dest.add(new InetSocketAddress(getRemoteHost(), getPort()));
        }
        if (remoteServers != null) {
            for (RemoteServer server : remoteServers.getRemoteServers()) {
                dest.add(new InetSocketAddress(server.getHost(), server.getPort()));
            }
        }
        return dest;
    }

    public static class RemoteServers {

        private List<RemoteServer> remoteServers;

        public RemoteServers() {
            remoteServers = new ArrayList<RemoteServer>();
        }

        public List<RemoteServer> getRemoteServers() {
            return remoteServers;
        }

        public void addRemoteServer(RemoteServer remoteServer) {
            remoteServers.add(remoteServer);
        }
    }

    public static class RemoteServer {

        private String host;
        private int port;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }
}
