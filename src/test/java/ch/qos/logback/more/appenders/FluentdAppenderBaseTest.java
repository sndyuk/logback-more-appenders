/**
 * Copyright (c) 2020 sndyuk <sanada@sndyuk.com>
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.more.appenders.marker.MapMarker;

public class FluentdAppenderBaseTest {

    /**
     * Creating a test appender to be a test proxy
     */
    class TestAppender<E> extends FluentdAppenderBase<E> {
        protected List<E> appended = new ArrayList<E>();

        @Override
        protected void append(E event) {
            appended.add(event);
        }

    }

    class TestEvent implements ILoggingEvent {

        Object[] argumentArray = { "arg1", "arg2" };
        StackTraceElement[] callerData;
        Level level = Level.INFO;
        LoggerContextVO loggerContextVO;
        String loggerName = "defaultLoggerName";
        Map<String, String> mdcPropertyMap = new HashMap<>();
        Marker marker = null;
        Map<String, String> mdc = new HashMap<>();
        final String message;
        final long timeStamp;
        final String threadName = "threadName";
        IThrowableProxy throwableProxy;

        public TestEvent(String message) {
            this.message = message;
            this.timeStamp = System.currentTimeMillis();
        }

        // Fluent setters for testing
        public TestEvent setArgumentArray(Object[] argumentArray) {
            this.argumentArray = argumentArray;
            return this;
        }

        public TestEvent setCallerData(StackTraceElement[] callerData) {
            this.callerData = callerData;
            return this;
        }

        public TestEvent setLevel(Level level) {
            this.level = level;
            return this;
        }

        public TestEvent setLoggerContextVO(LoggerContextVO loggerContextVO) {
            this.loggerContextVO = loggerContextVO;
            return this;
        }

        public TestEvent setLoggerName(String loggerName) {
            this.loggerName = loggerName;
            return this;
        }

        public TestEvent setMdcPropertyMap(Map<String, String> mdcPropertyMap) {
            this.mdcPropertyMap = mdcPropertyMap;
            return this;
        }

        // Fluent setters for testing
        public TestEvent setMarker(Marker marker) {
            this.marker = marker;
            return this;
        }

        public TestEvent setMdc(Map<String, String> mdc) {
            this.mdc = mdc;
            return this;
        }

        public TestEvent setThrowableProxy(IThrowableProxy throwableProxy) {
            this.throwableProxy = throwableProxy;
            return this;
        }

        @Override
        public Object[] getArgumentArray() {
            return argumentArray;
        }

        @Override
        public StackTraceElement[] getCallerData() {
            return callerData;
        }

        @Override
        public String getFormattedMessage() {
            return this.message;
        }

        @Override
        public Level getLevel() {
            return level;
        }

        @Override
        public LoggerContextVO getLoggerContextVO() {
            return loggerContextVO;
        }

        @Override
        public String getLoggerName() {
            return loggerName;
        }

        @Override
        public Map<String, String> getMDCPropertyMap() {
            return mdcPropertyMap;
        }

        @Override
        public Marker getMarker() {
            return marker;
        }

        @Override
        public Map<String, String> getMdc() {
            return mdc;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public String getThreadName() {
            return threadName;
        }

        @Override
        public IThrowableProxy getThrowableProxy() {
            return throwableProxy;
        }

        @Override
        public long getTimeStamp() {
            return timeStamp;
        }

        @Override
        public boolean hasCallerData() {
            return ((callerData != null) && (callerData.length > 0));
        }

        @Override
        public void prepareForDeferredProcessing() {
        }

    }

    private MapMarker makeMapMarker(String name) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        return new MapMarker(name, map);

    }

    @Test
    public void emptyStringTestEmpty() {
        String[] empty = { null, "" };
        for (String testString : empty) {
            assertTrue("Expecting '" + testString + "' to be empty", FluentdAppenderBase.emptyString(testString));
        }
    }

    @Test
    public void emptyStringTestNotEmpty() {
        String[] empty = { " ", "string" };
        for (String testString : empty) {
            assertFalse("Expecting '" + testString + "' to be not empty", FluentdAppenderBase.emptyString(testString));
        }
    }

    @Test
    public void mapMarkerNameTest() {
        TestAppender<Object> appender = new TestAppender<>();
        assertEquals("marker", appender.markerName());
        appender.setMarkerPrefix("custom");
        assertEquals("custom", appender.markerName());
        appender.setMarkerPrefix("");
        assertEquals("marker", appender.markerName());
        appender.setMarkerPrefix(null);
        assertEquals("marker", appender.markerName());
    }

    @Test
    public void mapMarkerNameWithMarkerMapTest() {
        TestAppender<Object> appender = new TestAppender<>();
        MapMarker mapMarker = makeMapMarker("MAP_MARKER");

        // default map marker prefix
        assertEquals("marker.MAP_MARKER", appender.mapMarkerName(mapMarker));

        appender.setMarkerPrefix("custom");
        assertEquals("custom.MAP_MARKER", appender.mapMarkerName(mapMarker));

        appender.setMarkerPrefix("");
        assertEquals("MAP_MARKER", appender.mapMarkerName(mapMarker));

        appender.setMarkerPrefix(null);
        assertEquals("marker.MAP_MARKER", appender.mapMarkerName(mapMarker));
    }

    private void assertExpectedData(Map<String, Object> expected, TestAppender<Object> appender, Object testEvent) {

        assertEquals(expected, appender.createData(testEvent));
    }

    /**
     * Test createData with non-ILoggingEvent argument
     */
    @Test
    public void createDataNonILoggingEvent() {
        TestAppender<Object> appender = new TestAppender<>();

        Object testEvent = "TEST";

        Map<String, Object> expected = new HashMap<>();
        expected.put("message", testEvent);

        assertExpectedData(expected, appender, testEvent);
    }

    /**
     * Test createData with ILoggingEvent argument
     */
    @Test
    public void createDataILoggingEvent() {
        TestAppender<Object> appender = new TestAppender<>();

        ILoggingEvent testEvent = new TestEvent("Test Message");

        Map<String, Object> expected = new HashMap<>();
        expected.put("message", testEvent.getFormattedMessage());
        expected.put("level", testEvent.getLevel().toString());
        expected.put("logger", testEvent.getLoggerName());
        expected.put("thread", testEvent.getThreadName());

        assertExpectedData(expected, appender, testEvent);
    }

    /**
     * Test createData with ILoggingEvent argument with Map Marker with default
     * prefix
     */
    @Test
    public void createDataILoggingEventWithMapMarker1() {
        TestAppender<Object> appender = new TestAppender<>();
        MapMarker mapMarker = makeMapMarker("MAP_MARKER");

        TestEvent testEvent = new TestEvent("Test Message");
        testEvent.setMarker(mapMarker).setLevel(Level.ERROR);

        Map<String, Object> expected = new HashMap<>();
        expected.put("message", testEvent.getFormattedMessage());
        expected.put("level", testEvent.getLevel().toString());
        expected.put("logger", testEvent.getLoggerName());
        expected.put("thread", testEvent.getThreadName());
        expected.put("marker.MAP_MARKER", mapMarker.getMap());

        assertExpectedData(expected, appender, testEvent);
    }

    /**
     * Test createData with ILoggingEvent argument with MapMarker without a prefix
     */
    @Test
    public void createDataILoggingEventWithMapMarkerNoPrefix() {
        TestAppender<Object> appender = new TestAppender<>();
        appender.setMarkerPrefix("");
        MapMarker mapMarker = makeMapMarker("MAP_MARKER");

        TestEvent testEvent = new TestEvent("Test Message");
        testEvent.setMarker(mapMarker).setLevel(Level.ERROR);

        Map<String, Object> expected = new HashMap<>();
        expected.put("message", testEvent.getFormattedMessage());
        expected.put("level", testEvent.getLevel().toString());
        expected.put("logger", testEvent.getLoggerName());
        expected.put("thread", testEvent.getThreadName());
        expected.put("MAP_MARKER", mapMarker.getMap());

        assertExpectedData(expected, appender, testEvent);
    }

    /**
     * Test createData with ILoggingEvent argument with Ignored Fields
     */
    @Test
    public void createDataILoggingEventWithIgnoredFields() {
        TestAppender<Object> appender = new TestAppender<>();
        appender.addIgnoredField("thread");
        appender.addIgnoredField("logger");

        TestEvent testEvent = new TestEvent("Test Message");
        testEvent.setLevel(Level.ERROR);

        Map<String, Object> expected = new HashMap<>();
        expected.put("message", testEvent.getFormattedMessage());
        expected.put("level", testEvent.getLevel().toString());

        assertExpectedData(expected, appender, testEvent);
    }

    /**
     * Test createData with ILoggingEvent argument with Ignored Flattened Marker Fields
     */
    @Test
    public void createDataILoggingEventWithIgnoredFlattenMarkerFields() {
        TestAppender<Object> appender = new TestAppender<>();
        appender.setFlattenMapMarker(true);
        appender.addIgnoredField("key1");

        MapMarker mapMarker = makeMapMarker("MAP_MARKER");

        TestEvent testEvent = new TestEvent("Test Message");
        testEvent.setMarker(mapMarker).setLevel(Level.ERROR);

        Map<String, Object> expected = new HashMap<>();
        expected.put("message", testEvent.getFormattedMessage());
        expected.put("level", testEvent.getLevel().toString());
        expected.put("logger", testEvent.getLoggerName());
        expected.put("thread", testEvent.getThreadName());
        expected.put("key2", "value2");

        assertExpectedData(expected, appender, testEvent);
    }
}
