// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;

import java.beans.EventHandler;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ObservableMap implements Map {

    private Map delegate;

    private PropertyChangeSupport changeSupport;

    public ObservableMap() {
        this(new HashMap());
    }

    public ObservableMap(Map base) {
        delegate = base;
        changeSupport = new PropertyChangeSupport(this);
    }

    public void clear() {
        for (Iterator i = new ArrayList(keySet()).iterator(); i.hasNext();)
            remove(i.next());
    }

    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    public Set entrySet() {
        return Collections.unmodifiableSet(delegate.entrySet());
    }

    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    public Object get(Object key) {
        return delegate.get(key);
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public Set keySet() {
        return Collections.unmodifiableSet(delegate.keySet());
    }

    public Object put(Object key, Object value) {
        Object oldValue = delegate.put(key, value);
        if (key instanceof String)
            changeSupport.firePropertyChange((String) key, oldValue, value);
        return oldValue;
    }

    public void putAll(Map t) {
        for (Iterator i = t.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            put(e.getKey(), e.getValue());
        }
    }

    public Object remove(Object key) {
        Object oldValue = delegate.remove(key);
        if (key instanceof String)
            changeSupport.firePropertyChange((String) key, oldValue, null);
        return oldValue;
    }

    public int size() {
        return delegate.size();
    }

    public Collection values() {
        return Collections.unmodifiableCollection(delegate.values());
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void addPropertyChangeListener(String[] properties, Object target,
            String action) {

        boolean needsListener = false;
        for (int i = 0; i < properties.length; i++) {
            if (StringUtils.hasValue(properties[i]))
                needsListener = true;
        }
        if (needsListener == false)
            return;

        PropertyChangeListener l = (PropertyChangeListener) EventHandler
                .create(PropertyChangeListener.class, target, action);
        for (int i = 0; i < properties.length; i++)
            if (StringUtils.hasValue(properties[i]))
                addPropertyChangeListener(properties[i], l);
    }

    public void addPropertyChangeListener(String propertyName, Object target,
            String action) {
        if (StringUtils.hasValue(propertyName)) {
            PropertyChangeListener l = (PropertyChangeListener) EventHandler
                    .create(PropertyChangeListener.class, target, action);
            addPropertyChangeListener(propertyName, l);
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return changeSupport.getPropertyChangeListeners();
    }

    public PropertyChangeListener[] getPropertyChangeListeners(
            String propertyName) {
        return changeSupport.getPropertyChangeListeners(propertyName);
    }

    public boolean hasListeners(String propertyName) {
        return changeSupport.hasListeners(propertyName);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(propertyName, listener);
    }

}
