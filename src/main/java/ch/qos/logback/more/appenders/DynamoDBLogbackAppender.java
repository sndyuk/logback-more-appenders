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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public class DynamoDBLogbackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final class DynamoDBDaemonAppender extends
            DaemonAppender<ILoggingEvent> {

        private final String tableName;
        private final String instanceName;
        private long id;
        private final AmazonDynamoDB dynamoClient;
        private final Layout<ILoggingEvent> layout;

        DynamoDBDaemonAppender(String tableName, String instanceName,
                long lastId, AmazonDynamoDB dynamoClient,
                Layout<ILoggingEvent> layout, int maxQueueSize) {
            super(maxQueueSize);
            this.tableName = tableName;
            this.instanceName = instanceName;
            this.id = lastId;
            this.dynamoClient = dynamoClient;
            this.layout = layout;
        }

        @Override
        protected void append(ILoggingEvent rawData) {
            String msg = null;
            if (layout != null) {
                msg = layout.doLayout(rawData);
            } else {
                msg = rawData.toString();
            }

            Map<String, AttributeValue> data = new HashMap<String, AttributeValue>(4);
            data.put("instance", new AttributeValue().withS(instanceName));
            data.put("id", new AttributeValue().withN(String.valueOf(++id)));
            data.put("msg", new AttributeValue().withS(msg));

            PutItemRequest itemRequest = new PutItemRequest().withTableName(
                    tableName).withItem(data);
            dynamoClient.putItem(itemRequest);
        }

        @Override
        protected void close() {
            try {
                super.close();
            } finally {
                dynamoClient.shutdown();
            }
        }
    }

    private DaemonAppender<ILoggingEvent> appender;

    private boolean initializeAppender() {
        try {
            PropertiesCredentials credentials = new PropertiesCredentials(
                    getClass().getClassLoader().getResourceAsStream(dynamodbCredentialFilePath));
            AmazonDynamoDB dynamoClient = AmazonDynamoDBClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(dynamodbEndpoint, dynamodbRegion)).build();
            appender = new DynamoDBDaemonAppender(outputTableName, instanceName,
                    getLastId(outputTableName, instanceName, dynamoClient),
                    dynamoClient, layout, maxQueueSize);
            return true;
        } catch (Exception e) {
            System.err.println("Could not initialize " + DynamoDBLogbackAppender.class.getCanonicalName() + " ( will try to initialize again later ): " + e);
            return false;
        }
    }

    private static long getLastId(String tableName, String instanceName,
        AmazonDynamoDB dynamoClient) {
        QueryRequest queryRequest = new QueryRequest().withTableName(tableName)
                .withKeyConditionExpression("instance = :pk")
                .addExpressionAttributeValuesEntry(":pk", new AttributeValue().withS(instanceName))
                .withScanIndexForward(false).withLimit(1);
        QueryResult result = dynamoClient.query(queryRequest);
        List<Map<String, AttributeValue>> items = result.getItems();
        if (items == null || items.size() == 0) {
            return 0L;
        } else {
            return Long.valueOf(items.get(0).get("id").getN());
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (appender == null) {
            synchronized (this) {
                if (!initializeAppender()) {
                    System.err.println("The log which supposed to be added to DynamoDB was not added because the dynamo db appender could not be initialized. Ignored the message: " + System.lineSeparator() + eventObject.toString());
                    return;
                }
            }
            this.append(eventObject);
            return;
        }
        eventObject.prepareForDeferredProcessing();
        appender.log(eventObject);
    }

    @Override
    public void stop() {
        try {
            super.stop();
        } finally {
            synchronized (this) {
                if (appender != null) {
                    appender.close();
                }
            }
        }
    }

    private String dynamodbCredentialFilePath;
    private String dynamodbEndpoint;
    private String dynamodbRegion;
    private String outputTableName;
    // An quque name for preventing conflict with other instances.
    private String instanceName;
    // Max queue size of logs which is waiting to be sent (When it reach to the max size, the log will be disappeared).
    private int maxQueueSize;
    private Layout<ILoggingEvent> layout;

    public String getDynamodbCredentialFilePath() {
        return dynamodbCredentialFilePath;
    }

    public void setDynamodbCredentialFilePath(String dynamodbCredentialFilePath) {
        this.dynamodbCredentialFilePath = dynamodbCredentialFilePath;
    }

    public String getDynamodbEndpoint() {
        return dynamodbEndpoint;
    }

    public void setDynamodbEndpoint(String dynamodbEndpoint) {
        this.dynamodbEndpoint = dynamodbEndpoint;
    }

    public String getDynamodbRegion() {
        return dynamodbRegion;
    }

    public void setDynamodbRegion(String dynamodbRegion) {
        this.dynamodbRegion = dynamodbRegion;
    }

    public String getOutputTableName() {
        return outputTableName;
    }

    public void setOutputTableName(String outputTableName) {
        this.outputTableName = outputTableName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public Layout<ILoggingEvent> getLayout() {
        return layout;
    }

    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }
}