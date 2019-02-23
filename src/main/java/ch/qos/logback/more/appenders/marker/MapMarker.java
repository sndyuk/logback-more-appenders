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
package ch.qos.logback.more.appenders.marker;

import java.util.Map;
import org.slf4j.Marker;

public class MapMarker extends LeafMarker {

    private static final long serialVersionUID = 1L;

    private final Map<String, ?> map;
    private String mapStr;

    public MapMarker(String name, Map<String, ?> map) {
        super(name);
        this.map = map;
    }

    public Map<String, ?> getMap() {
        return map;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;

        if (o instanceof MapMarker) {
            final MapMarker that = (MapMarker) o;
            return map != null ? map.equals(that.map) : that.map == null;
        }
        if (o instanceof Marker) {
            final Marker that = (Marker) o;
            return that.equals(this);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (map != null ? map.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (mapStr == null) {
            mapStr = mapStr.toString();
        }
        return getName() + '{' + mapStr + '}';
    }
}
