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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.more.appenders.IntervalEmitter.EventMapper;
import ch.qos.logback.more.appenders.IntervalEmitter.IntervalAppender;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResultEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Appender for Kinesis Stream. It appends entries for every emitInterval.
 * 
 * @author sndyuk
 */
public class KinesisStreamLogbackAppenderV2 extends KinesisStreamAppenderBaseV2<ILoggingEvent> {

    private IntervalEmitter<ILoggingEvent, PutRecordsRequestEntry> emitter;
    private PartitionKey<ILoggingEvent> partitionKey = new RandomPartitionKey();
    private long emitInterval = 10000;

    public void setAwsConfig(AwsConfig config) {
        this.config = config;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public void setShardCount(int shardCount) {
        this.shardCount = shardCount;
    }

    public void setCreateStreamDestination(boolean createStreamDestination) {
        this.createStreamDestination = createStreamDestination;
    }

    public void setPartitionKey(PartitionKey<ILoggingEvent> partitionKey) {
        this.partitionKey = partitionKey;
    }

    public void setEmitInterval(long emitInterval) {
        this.emitInterval = emitInterval;
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

    @Override
    public void start() {
        this.emitter = new IntervalEmitter<ILoggingEvent, PutRecordsRequestEntry>(emitInterval,
                new KinesisEventMapper(), new KinesisIntervalAppender());
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
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        emitter.append(eventObject);
    }

    private final class KinesisEventMapper implements EventMapper<ILoggingEvent, PutRecordsRequestEntry> {

        @Override
        public PutRecordsRequestEntry map(ILoggingEvent event) {
            PutRecordsRequestEntry entry  = PutRecordsRequestEntry.builder()
                    .data(SdkBytes.fromByteArray(encoder.encode(event)))
                            .partitionKey(partitionKey.get(event))
                    .build();
            return entry;
        }
    }

    private final class KinesisIntervalAppender implements IntervalAppender<PutRecordsRequestEntry> {

        @Override
        public boolean append(List<PutRecordsRequestEntry> entries) {
            if (!active) {
                ensureKinesisStream();
            }
            return append(entries, 0);
        }

        private boolean append(List<PutRecordsRequestEntry> entries, int retryCount) {
            if (retryCount > 3) {
                StringBuilder sb = new StringBuilder("Could not append the Kinesis stream entry. Failed entries:");
                for (PutRecordsRequestEntry entry : entries) {
                    sb.append(System.lineSeparator()).append(entry.data());
                }
                addError(sb.toString());
                return true; // true to forcibly remove unexpected failed entries.
            }
            try {
                if (retryCount > 0) {
                    Thread.sleep(1000 * retryCount);
                }
                PutRecordsRequest putRecordsRequest  = PutRecordsRequest.builder()
                        .streamName(streamName)
                        .records(entries)
                        .build();
                PutRecordsResponse putRecordsResult  = kinesis.putRecords(putRecordsRequest);
                if (putRecordsResult.failedRecordCount() == 0) {
                    return true;
                }
                if (putRecordsResult.failedRecordCount() == entries.size()) {
                    return false; // Leave it to logback retry mechanism.
                }
                // Retry
                List<PutRecordsRequestEntry> failedEntries = new ArrayList<PutRecordsRequestEntry>(putRecordsResult.failedRecordCount());
                for (int i = 0; i < entries.size(); i++) {
                    PutRecordsResultEntry resultEntry = putRecordsResult.records().get(i);
                    if (resultEntry.errorCode() != null) {
                        failedEntries.add(entries.get(i));
                    }
                    append(failedEntries, retryCount + 1);
                }
            } catch (RuntimeException e) {
                addError("Unexpected runtime error while appending kinesis entries.", e);
            } catch (InterruptedException e) {
                // pass
            }
            return true;
        }
    }

    public static class RandomPartitionKey implements PartitionKey<ILoggingEvent> {
        private Random rand;
        private int maxPartition;

        public RandomPartitionKey() {
            this.rand = new Random();
            this.maxPartition = 10000;
        }

        @Override
        public String get(ILoggingEvent event) {
            return String.valueOf(rand.nextInt(maxPartition));
        }
    }

    public static class SinglePartitionKey implements PartitionKey<ILoggingEvent> {
        @Override
        public String get(ILoggingEvent event) {
            return "0";
        }
    }
}
