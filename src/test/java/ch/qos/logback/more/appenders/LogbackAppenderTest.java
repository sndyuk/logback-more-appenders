package ch.qos.logback.more.appenders;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogbackAppenderTest {

	private static final Logger LOG = LoggerFactory.getLogger(LogbackAppenderTest.class);
	
	@Test
	public void log1() throws InterruptedException {
		
		LOG.debug("Test the logger 1.");
		LOG.debug("Test the logger 2.");
		
		Thread.sleep(1000); // 非同期にlogをappendしてるのでちょっと待つ...
	}
}
