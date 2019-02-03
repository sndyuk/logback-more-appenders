package ch.qos.logback.more.appenders.marker;

import java.util.Map;

public class AppendersMarkerMap extends AppendersMarker {

	public static final String MARKER_NAME = AppendersMarker.MARKER_NAME_PREFIX + "MAP_FIELDS";

	private final Map<String, Object> map;

	public AppendersMarkerMap(Map<String, Object> map) {
		super(MARKER_NAME);
		this.map = map;
	}

	public Map<String, Object> getMap() {
		return map;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		final AppendersMarkerMap that = (AppendersMarkerMap) o;

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
		return "AppendersMarkerMap{" +
				"map=" + map +
				'}';
	}
}
