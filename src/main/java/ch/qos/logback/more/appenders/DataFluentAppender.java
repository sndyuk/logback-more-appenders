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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import ch.qos.logback.classic.pattern.CallerDataConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.fluentd.logger.FluentLogger;

public class DataFluentAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

	private static final class FluentDaemonAppender extends DaemonAppender<ILoggingEvent> {

		private FluentLogger fluentLogger;
		private final String tag;
		private final String label;
		private final String remoteHost;
		private final int port;

		FluentDaemonAppender(String tag, String label, String remoteHost, int port, int maxQueueSize) {
			super(maxQueueSize);
			this.tag = tag;
			this.label = label;
			this.remoteHost = remoteHost;
			this.port = port;
		}

		@Override
		public void execute() {
			this.fluentLogger = FluentLogger.getLogger(tag, remoteHost, port);
			super.execute();
		}

		@Override
		protected void close() {
			try {
				super.close();
			} finally {
				if (fluentLogger != null) {
					fluentLogger.close();
				}
			}
		}

		@Override
		protected void append(ILoggingEvent rawData) {
			final Map<String, Object> data = new HashMap<String, Object>();
			data.put("message", rawData.getFormattedMessage());
			data.put("logger", rawData.getLoggerName());
			data.put("thread", rawData.getThreadName());
			data.put("level", rawData.getLevel());
			if (rawData.getMarker() != null) {
				data.put("marker", rawData.getMarker());
			}
			if (rawData.hasCallerData()) {
				data.put("caller", new CallerDataConverter().convert(rawData));
			}
			if (rawData.getThrowableProxy() != null) {
				data.put("throwable", ThrowableProxyUtil.asString(rawData.getThrowableProxy()));
			}
			for (Entry<String, String> entry : rawData.getMDCPropertyMap().entrySet()) {
				data.put(entry.getKey(), entry.getValue());
			}
			fluentLogger.log(label, data, rawData.getTimeStamp() / 1000);
		}
	}

	private DaemonAppender<ILoggingEvent> appender;

	// リモートホストに接続できないときに何件までログを保持するか（件数制限に達している時にきたログは破棄する）
	private int maxQueueSize;

	@Override
	public void start() {
		super.start();
		appender = new FluentDaemonAppender(tag, label, remoteHost, port, maxQueueSize);
	}

	@Override
	protected void append(ILoggingEvent eventObject) {
		if (!isStarted()) {
			return;
		}
		// store the whole LogEvent, b/c the MDC will be only available here. (MDC is a ThreadLocal and cannot be
		// retrieved from a daemon appender)
		appender.log(LoggingEventVO.build(eventObject));
	}

	@Override
	public void stop() {
		try {
			super.stop();
		} finally {
			appender.close();
		}
	}

	private String tag;
	private String label;
	private String remoteHost;
	private int port;

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int getMaxQueueSize() {
		return maxQueueSize;
	}

	public void setMaxQueueSize(int maxQueueSize) {
		this.maxQueueSize = maxQueueSize;
	}

	public String getRemoteHost() {
		return remoteHost;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}