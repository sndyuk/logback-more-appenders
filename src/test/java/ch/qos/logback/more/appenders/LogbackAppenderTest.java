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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.more.appenders.marker.MapMarker;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LogbackAppenderTest {

    private static final Logger LOG = LoggerFactory.getLogger(LogbackAppenderTest.class);

    @BeforeClass
    public static void before() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        if (!lc.isStarted()) {
            lc.start();
        }
    }

    @AfterClass
    public static void after() throws InterruptedException {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.stop();

        Thread.sleep(1500); // Wait a moment because these log is being appended asynchronous...
    }
    
    @Test
    public void logSimple() {
        LOG.debug("Test the logger 1.");
        LOG.debug("Test the logger 2.");
    }

    @Test
    public void logMdc() {
        MDC.put("req.requestURI", "/hello/world.js");
        LOG.debug("Test the logger 1.");
        LOG.debug("Test the logger 2.");
    }

    @Test
    public void logMarker() {
        Marker sendEmailMarker = MarkerFactory.getMarker("SEND_EMAIL");
        LOG.debug(sendEmailMarker, "Test the marker 1.");
        LOG.debug(sendEmailMarker, "Test the marker 2.");
    }

    @Test
    public void logNestedMarker() {
        Marker notifyMarker = MarkerFactory.getMarker("NOTIFY");
        Marker sendEmailMarker = MarkerFactory.getMarker("SEND_EMAIL");
        sendEmailMarker.add(notifyMarker);
        LOG.debug(sendEmailMarker, "Test the nested marker 1.");
        LOG.debug(sendEmailMarker, "Test the nested marker 2.");
    }

    @Test
    public void logThrowable() {
        LOG.info("Without Exception.");
        LOG.error("\"Test the checked Exception\"", new IOException("Connection something"));
        LOG.warn("Test the unchecked Exception.", new IllegalStateException("Oh your state"));
    }

    @Test
    public void logMapMarker() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        MapMarker mapMarker = new MapMarker("MAP_MARKER", map);

        LOG.debug(mapMarker, "Test the marker map.");
    }

    @Test
    public void logNestedMapMarker() {
        Marker notifyMarker = MarkerFactory.getMarker("NOTIFY");
        Map<String, String> map = new HashMap<String, String>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        MapMarker mapMarker = new MapMarker("MAP_MARKER", map);
        notifyMarker.add(mapMarker);
        LOG.debug(notifyMarker, "Test the nested marker map.");
    }

    @Test
    public void logDecidedByAppendersMarkerFilter() {
        Marker alertMarker = MarkerFactory.getMarker("SECURITY_ALERT");
        LOG.debug(alertMarker, "Test alert filter.");
    }
}
