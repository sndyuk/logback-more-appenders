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
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.*;

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
public class CloudWatchLogbackAppender<E> extends AwsAppender<E> {

    private IntervalEmitter<E, InputLogEvent> emitter;
    private AWSLogs awsLogs;
    private String logGroupName;
    private StreamName logStreamName;
    private boolean createLogDestination;
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
                awsLogs.shutdown();
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
             AWSLogsClientBuilder builder = AWSLogsClientBuilder.standard().withRegion(config.getRegion());
            if (credentials != null) {
                builder.withCredentials(new AWSStaticCredentialsProvider(credentials));
            } else if (credentialsProvider != null) {
                builder.withCredentials(credentialsProvider);
            }
            this.awsLogs = builder.build();
        }
        DescribeLogGroupsRequest request = new DescribeLogGroupsRequest().withLogGroupNamePrefix(logGroupName).withLimit(1);
        DescribeLogGroupsResult result = awsLogs.describeLogGroups(request);
        if (result.getLogGroups().size() == 1
          && result.getLogGroups().get(0).getLogGroupName().equals(logGroupName)) {
            return;
        }
        if (createLogDestination) {
            awsLogs.createLogGroup(new CreateLogGroupRequest(logGroupName));
        } else {
            throw new IllegalStateException(
                    "The specified log group does not exist: " + logGroupName);
        }
    }

    private String ensureLogStream(String name) {
        DescribeLogStreamsRequest request = new DescribeLogStreamsRequest()
                .withLogGroupName(logGroupName).withLogStreamNamePrefix(name).withLimit(1);
        DescribeLogStreamsResult result = awsLogs.describeLogStreams(request);
        if (result.getLogStreams().size() == 1 && result.getLogStreams().get(0).getLogStreamName().equals(name)) {
            return result.getLogStreams().get(0).getUploadSequenceToken();
        }
        if (createLogDestination) {
            awsLogs.createLogStream(new CreateLogStreamRequest(logGroupName, name));
            return null;
        } else {
            throw new IllegalStateException(
                    "The specified log stream does not exist: " + logStreamName);
        }
    }

    private final class CloudWatchEventMapper implements EventMapper<E, InputLogEvent> {
        @Override
        public InputLogEvent map(E event) {
            InputLogEvent logEvent = new InputLogEvent();
            logEvent.setTimestamp(System.currentTimeMillis());
            byte[] messageBytes = encoder.encode(event);
            String message = new String(messageBytes);
            if (messageBytes.length >= (1048576 - 26)) {
                // The maximum batch size is 1,048,576 bytes. This size is calculated as the sum of all event messages in UTF-8, plus 26 bytes for each log event.
                logEvent.setMessage(message.substring(0, 512) + "...(Omitted)");
                addWarn("Could not send all message to CloudWatch because of the message size limit(<= 1,048,576 bytes). original message = " + message);
            } else {
                logEvent.setMessage(message);
            }
            return logEvent;
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
                    String message = event.getMessage();
                    size += (message.length() * 4); // Approximately 4 times of the length >= bytes size of utf8
                    if (size > 1048576 || i + 1 == l) {
                        PutLogEventsRequest request;
                        if (i + 1 == l) {
                            request = new PutLogEventsRequest(logGroupName, streamName, events.subList(putIndex, i + 1));
                        } else {
                            request = new PutLogEventsRequest(logGroupName, streamName, events.subList(putIndex, i));
                            // Update putIndex before decreasing the event index
                            putIndex = i;
                            i -= 1;
                        }
                        size = 0;
                        if (sequenceToken != null) {
                            request.withSequenceToken(sequenceToken);
                        }
                        PutLogEventsResult result = awsLogs.putLogEvents(request);
                        sequenceToken = result.getNextSequenceToken();
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
