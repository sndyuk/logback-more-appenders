/**
 * Copyright (c) 2012 sndyuk <sanada@sndyuk.com>
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.encoder.Encoder;

/**
 * @deprecated Use Kinesis stream(KinesisStreamLogbackAppender) with stream and DynamoDB stream API.
 */
@Deprecated
public class DynamoDBLogbackAppender<E> extends AwsAppender<E> {

    private Encoder<E> encoder = new EchoEncoder<E>();

    public void setEncoder(Encoder<E> encoder) {
        this.encoder = encoder;
    }

    public void setAwsConfig(AwsConfig config) {
        this.config = config;
    }

    @Override
    public void start() {
        try {
            super.start();
            this.dynamoClient = AmazonDynamoDBClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .withRegion(config.getRegion()).build();
        } catch (Exception e) {
            addError("Could not initialize " + DynamoDBLogbackAppender.class.getCanonicalName(), e);
        }
    }

    @Override
    public void stop() {
        try {
            super.stop();
        } finally {
            try {
                dynamoClient.shutdown();
            } catch (Exception e) {
                // pass
            }
        }
    }

    @Override
    protected void append(E event) {
        if (this.id == -1) {
            this.id = getLastId(outputTableName, instanceName, dynamoClient);
            if (this.id == -1) {
                addError(
                        "Could not initialize " + DynamoDBLogbackAppender.class.getCanonicalName());
                return;
            }
        }
        Map<String, AttributeValue> data = new HashMap<String, AttributeValue>(4);
        data.put("instance", new AttributeValue().withS(instanceName));
        data.put("id", new AttributeValue().withN(String.valueOf(++id)));
        data.put("msg", new AttributeValue().withS(new String(encoder.encode(event))));

        PutItemRequest itemRequest =
                new PutItemRequest().withTableName(outputTableName).withItem(data);
        dynamoClient.putItem(itemRequest);
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

    private String outputTableName;
    private String instanceName;
    private long id = -1;
    private AmazonDynamoDB dynamoClient;

    public void setOutputTableName(String outputTableName) {
        this.outputTableName = outputTableName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
}
