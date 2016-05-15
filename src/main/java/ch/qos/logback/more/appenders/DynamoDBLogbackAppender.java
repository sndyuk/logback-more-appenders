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

import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;

public class DynamoDBLogbackAppender<E> extends UnsynchronizedAppenderBase<E> {

    private static final int MSG_SIZE_LIMIT = 65535;

    private static final class DynamoDBDaemonAppender<E> extends
            DaemonAppender<E> {

        private final String tableName;
        private final String instanceName;
        private long id;
        private final AmazonDynamoDBClient dynamoClient;
        private final Layout<E> layout;

        DynamoDBDaemonAppender(String tableName, String instanceName,
                long lastId, AmazonDynamoDBClient dynamoClient,
                Layout<E> layout, int maxQueueSize) {
            super(maxQueueSize);
            this.tableName = tableName;
            this.instanceName = instanceName;
            this.id = lastId;
            this.dynamoClient = dynamoClient;
            this.layout = layout;
        }

        @Override
        protected void append(E rawData) {
            String msg = null;
            if (layout != null) {
                msg = layout.doLayout(rawData);
            } else {
                msg = rawData.toString();
            }
            if (msg != null && msg.length() > MSG_SIZE_LIMIT) {
                msg = msg.substring(0, MSG_SIZE_LIMIT);
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

    private DaemonAppender<E> appender;

    private boolean initializeAppender() {
        try {
            PropertiesCredentials credentials = new PropertiesCredentials(
                    getClass().getClassLoader().getResourceAsStream(dynamodbCredentialFilePath));
            AmazonDynamoDBClient dynamoClient = new AmazonDynamoDBClient(credentials);
            dynamoClient.setEndpoint(dynamodbEndpoint);
            appender = new DynamoDBDaemonAppender<E>(outputTableName, instanceName,
                    getLastId(outputTableName, instanceName, dynamoClient),
                    dynamoClient, layout, maxQueueSize);
            return true;
        } catch (Exception e) {
            System.err.println("Could not initialize " + DynamoDBLogbackAppender.class.getCanonicalName() + " ( will try to initialize again later ): " + e);
            return false;
        }
    }

    private static long getLastId(String tableName, String instanceName,
            AmazonDynamoDBClient dynamoClient) {
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
    protected void append(E eventObject) {
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
    private String outputTableName;
    // An quque name for preventing conflict with other instances.
    private String instanceName;
    // Max queue size of logs which is waiting to be sent (When it reach to the max size, the log will be disappeared).
    private int maxQueueSize;
    private Layout<E> layout;

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

    public Layout<E> getLayout() {
        return layout;
    }

    public void setLayout(Layout<E> layout) {
        this.layout = layout;
    }
}