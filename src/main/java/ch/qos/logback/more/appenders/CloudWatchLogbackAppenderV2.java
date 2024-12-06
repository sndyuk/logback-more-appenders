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

import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.more.appenders.IntervalEmitter.EventMapper;
import ch.qos.logback.more.appenders.IntervalEmitter.IntervalAppender;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Appender for CloudWatch. It appends logs for every emitInterval.
 *
 * @author sndyuk
 *
 * @param <E>
 */
public class CloudWatchLogbackAppenderV2<E> extends AwsAppenderV2<E> {

    private IntervalEmitter<E, InputLogEvent> emitter;
    private CloudWatchLogsClient awsLogs;
    private String logGroupName;
    private StreamName logStreamName;
    private boolean createLogDestination;
    private int maxEventCount = Integer.MAX_VALUE;
    private long emitInterval = 10000;
    private Encoder<E> encoder = new EchoEncoder<E>();

    public void setAwsConfig(AwsConfig config) {
        this.config = config;
    }

    public void setLogGroupName(String logGroupName) {
        this.logGroupName = logGroupName;
    }

    public void setLogStreamName(String logStreamName) {
        this.logStreamName = new StaticStreamName(logStreamName);
    }

    public void setLogStreamRolling(StreamName streamName) {
        this.logStreamName = streamName;
    }

    public void setCreateLogDestination(boolean createLogDestination) {
        this.createLogDestination = createLogDestination;
    }

    public void setMaxEventCount(int maxEventCount) {
        this.maxEventCount = maxEventCount;
    }

    public void setEmitInterval(long emitInterval) {
        this.emitInterval = emitInterval;
    }

    public void setEncoder(Encoder<E> encoder) {
        this.encoder = encoder;
    }

    @Override
    public void start() {
        if (logGroupName == null || logGroupName.length() == 0 || logStreamName == null) {
            throw new IllegalArgumentException("logGroupName and logStreamName must be defined.");
        }
        this.emitter = new IntervalEmitter<E, InputLogEvent>(emitInterval,
                new CloudWatchEventMapper(), new CloudWatchIntervalAppender());
        super.start();
    }

    @Override
    public void stop() {
        try {
            emitter.emitForShutdown(10000, 10);
        } catch (Exception e) {
            // Ignore
        }
        try {
            super.stop();
        } finally {
            try {
                awsLogs.close();
            } catch (Exception e) {
                // pass
            }
        }
    }

    @Override
    protected void append(E eventObject) {
        emitter.append(eventObject);
    }

    private void ensureLogGroup() {
        if (this.awsLogs == null) {
            CloudWatchLogsClientBuilder builder = CloudWatchLogsClient.builder()
                    .region(Region.of(config.getRegion()));
            if (credentials != null) {
                builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
            } else if (credentialsProvider != null) {
                builder.credentialsProvider(credentialsProvider);
            }
            this.awsLogs = builder.build();
        }
        DescribeLogGroupsRequest request = DescribeLogGroupsRequest.builder().logGroupNamePrefix(logGroupName).limit(1).build();
        DescribeLogGroupsResponse result = awsLogs.describeLogGroups(request);
        if (result.logGroups().size() == 1
          && result.logGroups().get(0).logGroupName().equals(logGroupName)) {
            return;
        }
        if (createLogDestination) {
            awsLogs.createLogGroup(CreateLogGroupRequest.builder().logGroupName(logGroupName).build());
        } else {
            throw new IllegalStateException(
                    "The specified log group does not exist: " + logGroupName);
        }
    }

    private String ensureLogStream(String name) {
        DescribeLogStreamsRequest request = DescribeLogStreamsRequest.builder()
                .logGroupName(logGroupName).logStreamNamePrefix(name).limit(1).build();
        DescribeLogStreamsResponse result = awsLogs.describeLogStreams(request);
        if (result.logStreams().size() == 1 && result.logStreams().get(0).logStreamName().equals(name)) {
            return result.logStreams().get(0).uploadSequenceToken();
        }
        if (createLogDestination) {
            awsLogs.createLogStream(CreateLogStreamRequest.builder().logGroupName(logGroupName).logStreamName(name).build());
            return null;
        } else {
            throw new IllegalStateException(
                    "The specified log stream does not exist: " + logStreamName);
        }
    }

    private final class CloudWatchEventMapper implements EventMapper<E, InputLogEvent> {
        @Override
        public InputLogEvent map(E event) {
            InputLogEvent.Builder logEvent = InputLogEvent.builder();
            logEvent.timestamp(System.currentTimeMillis());
            byte[] messageBytes = encoder.encode(event);
            String message = new String(messageBytes);
            if (messageBytes.length >= (1048576 - 26)) {
                // The maximum batch size is 1,048,576 bytes. This size is calculated as the sum of all event messages in UTF-8, plus 26 bytes for each log event.
                logEvent.message(message.substring(0, 512) + "...(Omitted)");
                addWarn("Could not send all message to CloudWatch because of the message size limit(<= 1,048,576 bytes). original message = " + message);
            } else {
                logEvent.message(message);
            }
            return logEvent.build();
        }
    }

    private final class CloudWatchIntervalAppender implements IntervalAppender<InputLogEvent> {
        private String sequenceToken;
        private boolean initialized = false;
        private boolean switchingStream = false;
        private String currentStreamName = logStreamName.get(Collections.<InputLogEvent>emptyList());

        @Override
        public boolean append(List<InputLogEvent> events) {
            if (!initialized) {
                ensureLogGroup();
                initialized = true;
            }
            if (switchingStream) {
                return false;
            }
            String streamName = logStreamName.get(events);
            if (!streamName.equals(currentStreamName)) {
                if (switchingStream) {
                    return false;
                }
                switchingStream = true;
                sequenceToken = ensureLogStream(streamName);
                currentStreamName = streamName;
                switchingStream = false;
            }
            try {
                long size = 0;
                int putIndex = 0;
                for (int i = 0, l = events.size(); i < l; i++) {
                    InputLogEvent event = events.get(i);
                    String message = event.message();
                    size += (message.length() * 4); // Approximately 4 times of the length >= bytes size of utf8

                    boolean eventCountLimitExceeded = (i - putIndex) == maxEventCount;
                    boolean eventSizeLimitExceeded = size > 1048576;
                    boolean lastRecordReached = i + 1 == l;
                    if (eventCountLimitExceeded || eventSizeLimitExceeded || lastRecordReached) {
                        PutLogEventsRequest.Builder request;

                        // When (eventCountLimitExceeded || eventSizeLimitExceeded) && lastRecordReached, give precedence
                        // to the limits (discard the current event)
                        if (eventCountLimitExceeded || eventSizeLimitExceeded) {
                            request = PutLogEventsRequest.builder().logGroupName(logGroupName)
                                .logStreamName(streamName).logEvents(events.subList(putIndex, i));
                            // Update putIndex before decreasing the event index
                            putIndex = i;
                            i -= 1;
                        } else {
                            request = PutLogEventsRequest.builder().logGroupName(logGroupName)
                                .logStreamName(streamName).logEvents(events.subList(putIndex, i + 1));
                        }
                        size = 0;
                        if (sequenceToken != null) {
                            request.sequenceToken(sequenceToken);
                        }
                        PutLogEventsResponse result = awsLogs.putLogEvents(request.build());
                        sequenceToken = result.nextSequenceToken();
                    }
                }
            } catch (RuntimeException e) {
                sequenceToken = null;
                addWarn("Skip sending logs to CloudWatch", e);
            }
            return true;
        }
    }

    public interface StreamName {
        String get(List<InputLogEvent> events);
    }

    public static class StaticStreamName implements StreamName {
        private String name;

        public StaticStreamName(String name) {
            this.name = name;
        }

        @Override
        public String get(List<InputLogEvent> events) {
            return name;
        }
    }

    public static class CountBasedStreamName implements StreamName {
        private long count = 0;
        private long limit = 1000;
        private String baseName = "";
        private String currentName;

        public void setBaseName(String baseName) {
            this.baseName = baseName;
        }

        public void setLimit(long limit) {
            this.limit = limit;
            this.count = limit + 1;
        }

        @Override
        public String get(List<InputLogEvent> events) {
            if (currentName == null) {
                currentName = baseName + UUID.randomUUID();
                return currentName;
            }
            count += events.size();
            if (count > limit) {
                currentName = baseName + UUID.randomUUID();
                count = events.size();
            }
            return currentName;
        }
    }
}
