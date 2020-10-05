package ch.qos.logback.more.appenders.encoder;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class FastJsonEncoderTest {
    private static ObjectMapper objectMapper = new ObjectMapper();

    @BeforeClass
    public static void before() {
        objectMapper.setConfig(objectMapper.getDeserializationConfig()
            .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    private FastJsonEncoder createEncoder() {
        FastJsonEncoder encoder = new FastJsonEncoder();
        encoder.setCompressSpace(true);
        encoder.setPattern("{\"message\": \"%msg\", \"decoy\": false}");
        encoder.setContext(new LoggerContext());
        encoder.start();
        return encoder;
    }

    public static class Message {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    private String encode(String message) {
        FastJsonEncoder encoder = createEncoder();
        LoggingEvent event = new LoggingEvent();
        event.setMessage(message);
        return new String(encoder.encode(event));
    }

    String[] pattern1 = new String[]{
        "a",
        "abc",
        "test message",
        "\n",
        " ",
        "\t",
        "\r\n",
        "\\",
        "\\\"\\\"",
        "2020-10-03T23:12:43+00:00       debug.logback   {\"msg\":\"23:12:43.615 [HikariPool-1 housekeeper] WARN  c.z.h.p.HikariPool#779 HikariPool-1 - Thread starvation or clock leap detected (housekeeper delta=2h24s477ms).\",\"caller\":\"Caller+0\\t at com.zaxxer.hikari@3.4.2/com.zaxxer.hikari.pool.HikariPool$HouseKeeper.run(HikariPool.java:779)\\nCaller+1\\t at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:515)\\nCaller+2\\t at java.base/java.util.concurrent.FutureTask.runAndReset$$$capture(FutureTask.java:305)\\nCaller+3\\t at java.base/java.util.concurrent.FutureTask.runAndReset(FutureTask.java)\\nCaller+4\\t at java.base/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:305)\\n\",\"level\":\"WARN\",\"logger\":\"com.zaxxer.hikari.pool.HikariPool\",\"foo\":\"bar\",\"thread\":\"HikariPool-1 housekeeper\",\"foo2\":\"bar2\"}",
    };

    @Test
    public void test() throws IOException {
        for (int i = 0; i < pattern1.length; i++) {
            String messageJson = encode(pattern1[i]);
            Message message = objectMapper.readValue(messageJson, Message.class);
            assertEquals(pattern1[i], message.getMessage());
        }
    }
}
