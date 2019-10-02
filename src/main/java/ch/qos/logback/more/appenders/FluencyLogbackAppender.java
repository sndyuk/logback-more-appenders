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

import static ch.qos.logback.core.CoreConstants.CODES_URL;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.komamitsu.fluency.EventTime;
import org.komamitsu.fluency.Fluency;
import org.komamitsu.fluency.fluentd.FluencyBuilderForFluentd;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;

public class FluencyLogbackAppender<E> extends FluentdAppenderBase<E> {

    private Fluency fluency;

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
        super.start();

        try {
            FluencyBuilderForFluentd builder = configureFluency();
            this.fluency = builder.build(configureServers());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void append(E event) {
        Map<String, Object> data = createData(event);

        try {
            String tag = this.tag == null ? "" : this.tag;
            if (this.isUseEventTime()) {
                EventTime eventTime = EventTime.fromEpochMilli(System.currentTimeMillis());
                fluency.emit(tag, eventTime, data);
            } else {
                fluency.emit(tag, data);
            }
        } catch (IOException e) {
            // pass
        }
    }

    @Override
    public void stop() {
        try {
            super.stop();
        } finally {
            try {
                fluency.close();
            } catch (Exception e) {
                // pass
            }
        }
    }

    private String tag;
    private String remoteHost;
    private int port;
    private RemoteServers remoteServers;
    private boolean ackResponseMode;
    private String fileBackupDir;
    private Integer bufferChunkInitialSize;
    private Integer bufferChunkRetentionSize;
    private Long maxBufferSize;
    private Integer connectionTimeoutMilli;
    private Integer readTimeoutMilli;
    private Integer waitUntilBufferFlushed;
    private Integer waitUntilFlusherTerminated;
    private Integer flushIntervalMillis;
    private Integer senderMaxRetryCount;
    private boolean useEventTime; // Flag to enable/disable usage of eventtime
    private boolean sslEnabled;

    public RemoteServers getRemoteServers() {
        return remoteServers;
    }

    public void setRemoteServers(RemoteServers remoteServers) {
        this.remoteServers = remoteServers;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
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

    public boolean isSslEnabled() { return sslEnabled; }

    public void setSslEnabled(boolean useSsl) { this.sslEnabled = useSsl; }

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

    public Integer getFlushIntervalMillis() {
        return flushIntervalMillis;
    }

    public void setFlushIntervalMillis(Integer flushIntervalMillis) {
        this.flushIntervalMillis = flushIntervalMillis;
    }

    public Integer getSenderMaxRetryCount() {
        return senderMaxRetryCount;
    }

    public void setSenderMaxRetryCount(Integer senderMaxRetryCount) {
        this.senderMaxRetryCount = senderMaxRetryCount;
    }

    /**
     * get the value for EventTime usage
     *
     * @return true if EventTime is used, false otherwise
     */
    public boolean isUseEventTime() { return this.useEventTime; }

    /**
     * Set the value for EventTime usage
     *
     * @param useEventTime the new value
     */
    public void setUseEventTime(boolean useEventTime) { this.useEventTime = useEventTime; }

    protected FluencyBuilderForFluentd configureFluency() {
        FluencyBuilderForFluentd builder = new FluencyBuilderForFluentd();

        builder.setAckResponseMode(ackResponseMode);
        if (fileBackupDir != null) { builder.setFileBackupDir(fileBackupDir); }
        if (bufferChunkInitialSize != null) { builder.setBufferChunkInitialSize(bufferChunkInitialSize); }
        if (bufferChunkRetentionSize != null) { builder.setBufferChunkRetentionSize(bufferChunkRetentionSize); }
        if (maxBufferSize != null) { builder.setMaxBufferSize(maxBufferSize); }
        if (connectionTimeoutMilli != null) { builder.setConnectionTimeoutMilli(connectionTimeoutMilli); }
        if (readTimeoutMilli != null) { builder.setReadTimeoutMilli(readTimeoutMilli); }
        if (waitUntilBufferFlushed != null) { builder.setWaitUntilBufferFlushed(waitUntilBufferFlushed); }
        if (waitUntilFlusherTerminated != null) { builder.setWaitUntilFlusherTerminated(waitUntilFlusherTerminated); }
        if (flushIntervalMillis != null) { builder.setFlushIntervalMillis(flushIntervalMillis); }
        if (senderMaxRetryCount != null) { builder.setSenderMaxRetryCount(senderMaxRetryCount); }
        builder.setSslEnabled(sslEnabled);

        return builder;
    }

    protected List<InetSocketAddress> configureServers() {
        List<InetSocketAddress> dest = new ArrayList<InetSocketAddress>();
        if (remoteHost != null && port > 0) {
            dest.add(new InetSocketAddress(remoteHost, port));
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

