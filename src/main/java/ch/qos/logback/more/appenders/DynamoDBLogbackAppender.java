/**
 * Copyright (c) 2012 sndyuk <sanada@sndyuk.com>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ch.qos.logback.more.appenders;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.qos.logback.core.UnsynchronizedAppenderBase;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.PutItemRequest;
import com.amazonaws.services.dynamodb.model.QueryRequest;
import com.amazonaws.services.dynamodb.model.QueryResult;

public class DynamoDBLogbackAppender<E> extends UnsynchronizedAppenderBase<E> {

	private static final class DynamoDBDaemonAppender<E> extends
			DaemonAppender<E> {

		private final String tableName;
		private final String instanceName;
		private long id;
		private final AmazonDynamoDBClient dynamoClient;

		DynamoDBDaemonAppender(String tableName, String instanceName,
				long lastId, AmazonDynamoDBClient dynamoClient, int maxQueueSize) {
			super(maxQueueSize);
			this.tableName = tableName;
			this.instanceName = instanceName;
			this.id = lastId;
			this.dynamoClient = dynamoClient;
		}

		@Override
		protected void append(E rawData) {

			Map<String, AttributeValue> data = new HashMap<String, AttributeValue>();
			data.put("instance", new AttributeValue().withS(instanceName));
			data.put("id", new AttributeValue().withN(String.valueOf(++id)));
			data.put("msg", new AttributeValue().withS(rawData.toString()));

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

	@Override
	public void start() {
		super.start();
		try {
			PropertiesCredentials credentials = new PropertiesCredentials(this
					.getClass().getResourceAsStream(dynamodbCredentialFilePath));
			AmazonDynamoDBClient dynamoClient = new AmazonDynamoDBClient(
					credentials);
			dynamoClient.setEndpoint(dynamodbEndpoint);
			appender = new DynamoDBDaemonAppender<E>(outputTableName,
					instanceName, getLastId(outputTableName, instanceName,
							dynamoClient), dynamoClient, maxQueueSize);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static long getLastId(String tableName, String instanceName,
			AmazonDynamoDBClient dynamoClient) {
		QueryRequest queryRequest = new QueryRequest().withTableName(tableName)
				.withHashKeyValue(new AttributeValue().withS(instanceName))
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
		appender.log(eventObject);
	}

	@Override
	public void stop() {
		try {
			super.stop();
		} finally {
			appender.close();
		}
	}

	private String dynamodbCredentialFilePath;
	private String dynamodbEndpoint;
	private String outputTableName;
	// 他InstanceとIdの競合を避けるために使用するinstanceを表す一意な名前
	private String instanceName;
	// リモートホストに接続できないときに何件までログを保持するか（件数制限に達している時にきたログは破棄する）
	private int maxQueueSize;
	
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
}