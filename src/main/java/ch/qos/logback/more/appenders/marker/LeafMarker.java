package ch.qos.logback.more.appenders.marker;

import java.util.Iterator;
import org.slf4j.Marker;

public class LeafMarker implements Marker {

    private static final long serialVersionUID = 1L;

    private final String name;

    public LeafMarker(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void add(Marker reference) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Marker reference) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasChildren() {
        return false;
    }

    @Override
    public boolean hasReferences() {
        return false;
    }

    @Override
    public Iterator<Marker> iterator() {
        return null;
    }

    public boolean contains(Marker other) {
        if (other == null) {
            throw new IllegalArgumentException("Other cannot be null");
        }
        return this.equals(other);
    }

    @Override
    public boolean contains(String name) {
        return this.name != null && this.name.equals(name);
    }
}
