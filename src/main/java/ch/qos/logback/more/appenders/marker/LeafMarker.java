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
