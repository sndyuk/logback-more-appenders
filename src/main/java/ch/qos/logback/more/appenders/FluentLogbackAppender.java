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

import org.fluentd.logger.FluentLogger;

import ch.qos.logback.core.UnsynchronizedAppenderBase;

public class FluentLogbackAppender<E> extends UnsynchronizedAppenderBase<E> {

	private static final class FluentDaemonAppender<E> extends
			DaemonAppender<E> {

		private final FluentLogger fluentLogger;
		private final String label;

		FluentDaemonAppender(String tag, String label, String remoteHost,
				int port, int maxQueueSize) {
			super(maxQueueSize);
			this.fluentLogger = FluentLogger.getLogger(tag, remoteHost, port);
			this.label = label;
		}

		@Override
		protected void close() {
			try {
				super.close();
			} finally {
				FluentLogger.close();
			}
		}

		@Override
		protected void append(Object rawData) {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("msg", rawData);
			fluentLogger.log(label, data);
		}
	}

	private DaemonAppender<E> appender;

	// リモートホストに接続できないときに何件までログを保持するか（件数制限に達している時にきたログは破棄する）
	private int maxQueueSize;

	@Override
	public void start() {
		super.start();
		appender = new FluentDaemonAppender<E>(tag, label, remoteHost, port,maxQueueSize);
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