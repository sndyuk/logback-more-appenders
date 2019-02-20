package ch.qos.logback.more.appenders.marker;

import java.util.Map;

public class MapMarker extends LeafMarker {

    private static final long serialVersionUID = 1L;

    private final Map<String, ?> map;

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
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        final MapMarker that = (MapMarker) o;
        return map != null ? map.equals(that.map) : that.map == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (map != null ? map.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AppendersMarkerMap{" + "map=" + map + '}';
    }
}
