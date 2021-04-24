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
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.KinesisClientBuilder;
import software.amazon.awssdk.services.kinesis.model.*;

public abstract class KinesisStreamAppenderBaseV2<E> extends AwsAppenderV2<E> {

    protected KinesisClient kinesis;
    protected String streamName;
    protected int shardCount;
    protected boolean createStreamDestination;
    protected Encoder<E> encoder = new EchoEncoder<E>();
    protected volatile boolean active;

    @Override
    public void start() {
        if (streamName == null || streamName.length() == 0) {
            throw new IllegalArgumentException("streamName must be defined.");
        }
        super.start();
    }

    @Override
    public void stop() {
        try {
            super.stop();
        } finally {
            try {
                kinesis.close();
            } catch (Exception e) {
                // pass
            }
        }
    }

    protected void ensureKinesisStream() {
        if (this.kinesis == null) {
            KinesisClientBuilder builder = KinesisClient.builder()
                    .region(Region.of(config.getRegion()));
            if (credentials != null) {
                builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
            } else if (credentialsProvider != null) {
                builder.credentialsProvider(credentialsProvider);
            }
            kinesis = builder.build();
        }
        try {
            DescribeStreamRequest request = DescribeStreamRequest.builder()
                    .streamName(streamName)
                    .build();
            kinesis.describeStream(request);
            active = true;
            return;
        } catch (ResourceNotFoundException e) {
            // pass
        }
        // Watch the stream becomes ACTIVE.
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                CreateStreamRequest createStreamRequest = CreateStreamRequest.builder()
                        .streamName(streamName)
                        .shardCount(shardCount).build();
                kinesis.createStream(createStreamRequest);

                DescribeStreamRequest describeStreamRequest = DescribeStreamRequest.builder()
                        .streamName(streamName)
                        .build();

                long startTime = System.currentTimeMillis();
                long endTime = startTime + (120 * 1000);
                while (true) {
                    try {
                        Thread.sleep(5 * 1000);
                    } catch (Exception e) {
                        // pass
                    }
                    try {
                        DescribeStreamResponse describeStreamResponse =
                                kinesis.describeStream(describeStreamRequest);
                        StreamStatus streamStatus =
                                describeStreamResponse.streamDescription().streamStatus();
                        if (streamStatus.equals(StreamStatus.ACTIVE)) {
                            active = true;
                            return;
                        }
                    } catch (ResourceNotFoundException e) {
                        // pass
                    }
                    if (System.currentTimeMillis() >= endTime) {
                        addError("Stream " + streamName + " never went active.");
                        return;
                    }
                }
            }
        });
        th.setDaemon(true);
        th.start();
    }

    public interface PartitionKey<E> {
        String get(E event);
    }
}
