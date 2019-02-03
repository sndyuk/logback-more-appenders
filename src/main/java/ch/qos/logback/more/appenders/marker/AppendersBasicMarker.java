package ch.qos.logback.more.appenders.marker;

import org.slf4j.Marker;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/* Copy of {@link org.slf4j.helpers.BasicMarker} from slf4j-api v1.7.25,
 * with a minor change to make the constructor public.
 * <p>
 * slf4j-api, {@link org.slf4j.helpers.BasicMarker}, and the portions
 * of this class that have been copied from BasicMarker are provided under
 * the MIT License copied here:
 *
 * Copyright (c) 2004-2017 QOS.ch
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
public class AppendersBasicMarker implements Marker {

	private final String name;
	private List<Marker> markerList = new CopyOnWriteArrayList();
	private static String OPEN = "[ ";
	private static String CLOSE = " ]";
	private static String SEP = ", ";

	public AppendersBasicMarker(String name) {
		if (name == null) {
			throw new IllegalArgumentException("A marker name cannot be null");
		} else {
			this.name = name;
		}
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void add(final Marker marker) {
		if (marker == null) {
			throw new IllegalArgumentException("A null value cannot be added to a Marker as reference.");
		} else if (!this.contains(marker)) {
			if (!marker.contains(this)) {
				this.markerList.add(marker);
			}
		}
	}

	@Override
	public boolean remove(final Marker marker) {
		return this.markerList.remove(marker);
	}

	@Override
	public boolean hasChildren() {
		return this.hasReferences();
	}

	@Override
	public boolean hasReferences() {
		return this.markerList.size() > 0;
	}

	@Override
	public Iterator<Marker> iterator() {
		return this.markerList.iterator();
	}

	@Override
	public boolean contains(final Marker marker) {
		if (marker == null) {
			throw new IllegalArgumentException("Other cannot be null");
		} else if (this.equals(marker)) {
			return true;
		} else {
			if (this.hasReferences()) {
				Iterator i$ = this.markerList.iterator();

				while(i$.hasNext()) {
					Marker ref = (Marker)i$.next();
					if (ref.contains(marker)) {
						return true;
					}
				}
			}

			return false;
		}	}

	@Override
	public boolean contains(final String name) {
		if (name == null) {
			throw new IllegalArgumentException("Other cannot be null");
		} else if (this.name.equals(name)) {
			return true;
		} else {
			if (this.hasReferences()) {
				Iterator i$ = this.markerList.iterator();

				while(i$.hasNext()) {
					Marker ref = (Marker)i$.next();
					if (ref.contains(name)) {
						return true;
					}
				}
			}

			return false;
		}
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (!(obj instanceof Marker)) {
			return false;
		} else {
			Marker other = (Marker)obj;
			return this.name.equals(other.getName());
		}
	}

	public int hashCode() {
		return this.name.hashCode();
	}

	public String toString() {
		if (!this.hasReferences()) {
			return this.getName();
		} else {
			Iterator<Marker> it = this.iterator();
			StringBuilder sb = new StringBuilder(this.getName());
			sb.append(' ').append(OPEN);

			while(it.hasNext()) {
				Marker reference = it.next();
				sb.append(reference.getName());
				if (it.hasNext()) {
					sb.append(SEP);
				}
			}

			sb.append(CLOSE);
			return sb.toString();
		}
	}
}
