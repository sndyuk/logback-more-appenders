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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.amazonaws.services.kinesis.model.PutRecordsResultEntry;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.more.appenders.IntervalEmitter.EventMapper;
import ch.qos.logback.more.appenders.IntervalEmitter.IntervalAppender;

/**
 * Appender for Kinesis Stream. It appends entries for every emitInterval.
 * 
 * @author sndyuk
 *
 * @param <E>
 */
public class KinesisStreamLogbackAppender extends KinesisStreamAppenderBase<ILoggingEvent> {

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
        super.start();
        this.emitter = new IntervalEmitter<ILoggingEvent, PutRecordsRequestEntry>(emitInterval,
                new KinesisEventMapper(), new KinesisIntervalAppender());
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
                kinesis.shutdown();
            } catch (Exception e) {
                // pass
            }
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        emitter.append(eventObject);
    }

    private final class KinesisEventMapper implements EventMapper<ILoggingEvent, PutRecordsRequestEntry> {

        @Override
        public PutRecordsRequestEntry map(ILoggingEvent event) {
            PutRecordsRequestEntry entry  = new PutRecordsRequestEntry();
            entry.setData(ByteBuffer.wrap(encoder.encode(event)));
            entry.setPartitionKey(partitionKey.get(event));
            return entry;
        }
    }

    private final class KinesisIntervalAppender implements IntervalAppender<PutRecordsRequestEntry> {

        @Override
        public boolean append(List<PutRecordsRequestEntry> entries) {
            if (!started) {
                return false;
            }
            return append(entries, 0);
        }

        private boolean append(List<PutRecordsRequestEntry> entries, int retryCount) {
            if (retryCount > 3) {
                StringBuilder sb = new StringBuilder("Could not append the Kinesis stream entry. Failed entries:");
                for (PutRecordsRequestEntry entry : entries) {
                    sb.append(System.lineSeparator()).append(entry.getData());
                }
                addError(sb.toString());
                return true; // true to forcibly remove unexpected failed entries.
            }
            try {
                if (retryCount > 0) {
                    Thread.sleep(1000 * retryCount);
                }
                PutRecordsRequest putRecordsRequest  = new PutRecordsRequest();
                putRecordsRequest.setStreamName(streamName);
                putRecordsRequest.setRecords(entries);
                PutRecordsResult putRecordsResult  = kinesis.putRecords(putRecordsRequest);
                if (putRecordsResult.getFailedRecordCount() == 0) {
                    return true;
                }
                if (putRecordsResult.getFailedRecordCount() == entries.size()) {
                    return false; // Leave it to logback retry mechanism.
                }
                // Retry
                List<PutRecordsRequestEntry> failedEntries = new ArrayList<PutRecordsRequestEntry>(putRecordsResult.getFailedRecordCount());
                for (int i = 0; i < entries.size(); i++) {
                    PutRecordsResultEntry resultEntry = putRecordsResult.getRecords().get(i);
                    if (resultEntry.getErrorCode() != null) {
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
}
