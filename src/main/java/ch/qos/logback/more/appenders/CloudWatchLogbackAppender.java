package ch.qos.logback.more.appenders;

import java.util.List;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.CreateLogGroupRequest;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsResult;
import com.amazonaws.services.logs.model.DescribeLogStreamsRequest;
import com.amazonaws.services.logs.model.DescribeLogStreamsResult;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.LogGroup;
import com.amazonaws.services.logs.model.LogStream;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.more.appenders.IntervalEmitter.EventMapper;
import ch.qos.logback.more.appenders.IntervalEmitter.IntervalAppender;

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
        super.start();
        if (logGroupName == null || logGroupName.length() == 0 || logStreamName == null) {
            throw new IllegalArgumentException("logGroupName and logStreamName must be defined.");
        }
        this.awsLogs = AWSLogsClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(config.getRegion()).build();
        this.emitter = new IntervalEmitter<E, InputLogEvent>(emitInterval,
                new CloudWatchEventMapper(), new CloudWatchIntervalAppender());
    }

    @Override
    public void stop() {
        try {
            emitter.emit();
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
        DescribeLogGroupsRequest request =
                new DescribeLogGroupsRequest().withLogGroupNamePrefix(logGroupName);
        DescribeLogGroupsResult result = awsLogs.describeLogGroups(request);
        for (LogGroup group : result.getLogGroups()) {
            if (logGroupName.equals(group.getLogGroupName())) {
                return;
            }
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
                .withLogGroupName(logGroupName).withLogStreamNamePrefix(name);
        DescribeLogStreamsResult result = awsLogs.describeLogStreams(request);
        for (LogStream stream : result.getLogStreams()) {
            if (name.equals(stream.getLogStreamName())) {
                return stream.getUploadSequenceToken();
            }
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
            if (event instanceof ILoggingEvent) {
                ILoggingEvent loggingEvent = (ILoggingEvent) event;
                logEvent.setTimestamp(loggingEvent.getTimeStamp());
            } else {
                logEvent.setTimestamp(System.currentTimeMillis());
            }
            logEvent.setMessage(new String(encoder.encode(event)));
            return logEvent;
        }
    }

    private final class CloudWatchIntervalAppender implements IntervalAppender<InputLogEvent> {
        private String sequenceToken;
        private boolean initialized = false;
        private boolean switchingStream = false;
        private String currentStreamName;

        @Override
        public boolean append(List<InputLogEvent> events) {
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        ensureLogGroup();
                        initialized = true;
                    }
                }
                return false;
            }
            if (switchingStream) {
                return false;
            }
            String streamName = logStreamName.get(events);
            if (!streamName.equals(currentStreamName)) {
                switchingStream = true;
                synchronized (this) {
                    if (switchingStream) {
                        sequenceToken = ensureLogStream(streamName);
                        currentStreamName = streamName;
                        switchingStream = false;
                    } else {
                        return false;
                    }
                }
            }
            try {
                PutLogEventsRequest request =
                        new PutLogEventsRequest(logGroupName, streamName, events);
                if (sequenceToken != null) {
                    request.withSequenceToken(sequenceToken);
                }
                PutLogEventsResult result = awsLogs.putLogEvents(request);
                sequenceToken = result.getNextSequenceToken();
                return true;
            } catch (RuntimeException e) {
                sequenceToken = null;
                e.printStackTrace();
                throw e;
            }
        }
    }

    protected interface StreamName {
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
}
