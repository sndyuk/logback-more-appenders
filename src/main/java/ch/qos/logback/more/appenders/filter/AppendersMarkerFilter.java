/**
 * Copyright (c) 2018 sndyuk <sanada@sndyuk.com>
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
package ch.qos.logback.more.appenders.filter;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Marker;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.AbstractMatcherFilter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Simple marker filter for Appender.
 * 
 * <pre>
 * {@code
 * <filter class="ch.qos.logback.more.appenders.filter.AppendersMarkerFilter">
 *    <marker>NOTIFY_ADMIN</marker>
 *    <!-- Accept multiple markers -->
 *    <marker>TRANSACTION_FAILURE</marker>
 *    <!--[Optional] OnMismatch/OnMatch-->
 *    <OnMismatch>DENY</OnMismatch>
 *    <OnMatch>NEUTRAL</OnMatch>
 *  </filter>
 * }
 * </pre>
 * 
 * @author sndyuk
 */
public class AppendersMarkerFilter extends AbstractMatcherFilter<ILoggingEvent> {

    private List<String> markers = new ArrayList<String>();

    public AppendersMarkerFilter() {
        setOnMatch(FilterReply.NEUTRAL);
        setOnMismatch(FilterReply.DENY);
    }

    public void addMarker(String markerStr) {
        markers.add(markerStr);
    }

    @Override
    public void start() {
        if (!markers.isEmpty()) {
            super.start();
        } else {
            String name = this.getName();
            addError("No marker set for filter " + (name != null ? name : this.getClass()));
        }
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }
        Marker eventsMarker = event.getMarker();
        if (eventsMarker == null) {
            return onMismatch;
        }

        for (String markerStr : markers) {
            if (eventsMarker.contains(markerStr)) {
                return onMatch;
            }
        }
        return onMismatch;
    }
}
