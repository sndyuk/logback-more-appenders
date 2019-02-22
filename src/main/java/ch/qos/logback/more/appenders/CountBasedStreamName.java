package ch.qos.logback.more.appenders;

import java.util.List;
import java.util.UUID;
import com.amazonaws.services.logs.model.InputLogEvent;
import ch.qos.logback.more.appenders.CloudWatchLogbackAppender.StreamName;

public class CountBasedStreamName implements StreamName {
    private long count = 0;
    private long limit = 1000;
    private String baseName = "";
    private String currentName;

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public void setLimit(long limit) {
        this.limit = limit;
        this.count = limit + 1;
    }

    @Override
    public String get(List<InputLogEvent> events) {
        count += events.size();
        if (count > limit) {
            synchronized (this) {
                if (count > limit) {
                    currentName = baseName + UUID.randomUUID();
                    count = events.size();
                }
            }
        }
        return currentName;
    }
}
