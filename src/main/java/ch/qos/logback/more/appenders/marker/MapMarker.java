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
