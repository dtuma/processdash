// Copyright (C) 2007-2013 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib.binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.util.SqlResultData;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public abstract class AbstractBoundQuery<T> implements ErrorTokens {

    protected BoundMap map;

    private String destPropName;

    private String connPropName;

    private String emptyResultSetMessage;

    private List<String> parameterNames;


    public AbstractBoundQuery(BoundMap map, Element xml,
            String defaultConnectionId) {
        this.map = map;
        this.destPropName = xml.getAttribute("id");
        this.connPropName = XMLUtils.getAttribute(xml, "connection",
            defaultConnectionId);
        this.emptyResultSetMessage = map.getAttrOrResource(xml, destPropName,
            "Empty_Message", NO_VALUES_FOUND);
        this.parameterNames = new ArrayList<String>();

        map.addPropertyChangeListener(connPropName, this, "recalc");
    }


    protected void addParameter(String parameterName) {
        parameterNames.add(parameterName);
        map.addPropertyChangeListener(parameterName, this, "recalc");
    }


    /** 
     * Perform the query and return a result.
     * 
     * @param connection the connection to use for the query
     * @param parameterValues the parameter values to pass to the query
     * @return the result from executing the query, or an ErrorData object
     * @throws ErrorDataValueException if an error is encountered when
     *     executing the query
     */
    protected abstract Object executeQuery(T connection,
            Object[] parameterValues) throws ErrorDataValueException;



    public void recalc() {
        boolean isNullPresent = false;
        Object[] parameterValues = new Object[parameterNames.size()];
        for (int i = 0; i < parameterValues.length; i++) {
            String parameterName = parameterNames.get(i);
            Object value = map.get(parameterName);
            ErrorData errorData = null;

            if (ErrorValue.isRealError(value))
                errorData = (ErrorData) value;

            else if (value instanceof SqlResultData) {
                SqlResultData data = (SqlResultData) value;
                value = data.getSingleValue();
            }

            if (value == null) {
                isNullPresent = true;
                String missing = map.getErrorForMissingAttr(parameterName);
                if (missing != null)
                    errorData = new ErrorValue(missing, MISSING_DATA_SEVERITY);
            }

            if (errorData != null) {
                map.put(destPropName, errorData);
                return;
            }

            parameterValues[i] = value;
        }

        if (isNullPresent) {
            map.put(destPropName, MISSING_DATA_ERROR_VALUE);
            return;
        }

        map.put(destPropName, LOADING_ERROR_VALUE);
        currentLoader = new AsyncLoader(parameterValues);
        currentLoader.start();
    }


    private volatile AsyncLoader currentLoader = null;

    private class AsyncLoader extends SwingWorker {

        private Object[] parameterValues;

        public AsyncLoader(Object[] parameterValues) {
            this.parameterValues = parameterValues;
        }

        public Object construct() {
            // multiple change notifications could be sent to us in rapid
            // succession. To avoid churn, wait a moment and ensure that
            // no additional values have arrived before we do our work.
            try {
                Thread.sleep(200);
            } catch (InterruptedException ie) {}
            if (currentLoader != this)
                return null;

            ConnectionSource<T> connectionSource =
                    (ConnectionSource<T>) map.get(connPropName);
            if (connectionSource == null)
                return NO_CONNECTION_ERROR_VALUE;

            T connection = connectionSource.getConnection();
            if (connection == null) {
                if (ErrorValue.isRealError(connectionSource))
                    return (ErrorData) connectionSource;
                else
                    return NO_CONNECTION_ERROR_VALUE;
            }

            synchronized (connection) {
                try {
                    Object resultData = executeQuery(connection,
                        parameterValues);

                    if (isEmpty(resultData))
                        return new EmptyResultSet(emptyResultSetMessage);
                    else
                        return resultData;

                } catch (ErrorDataValueException e) {
                    return e;
                }
            }
        }

        public void finished() {
            if (currentLoader == this)
                map.put(destPropName, get());
        }

    }


    protected DynamicAttributeValue getDynamicValue(Element xml,
            String attrName, String errorToken) {
        return new DynamicAttributeValue(map, xml, this, "recalc",
                destPropName, attrName, errorToken);
    }


    protected static boolean isEmpty(Object result) {
        if (result == null)
            return true;

        if (result instanceof Iterable) {
            for (Object o : (Iterable) result) {
                if (!isEmpty(o))
                    return false;
            }
            return true;
        }

        if (result instanceof Map)
            return ((Map) result).isEmpty();

        if (result instanceof String)
            return ((String) result).length() == 0;

        return false;
    }


    protected static String getXmlAttr(Element xml, String attr, String defVal) {
        String result = xml.getAttribute(attr);
        if (StringUtils.hasValue(result))
            return result;
        else
            return defVal;
    }


    private static final ErrorValue MISSING_DATA_ERROR_VALUE = new ErrorValue(
            DATA_MISSING, MISSING_DATA_SEVERITY);

    private static final ErrorValue LOADING_ERROR_VALUE = new ErrorValue(
            LOADING, ErrorData.INFORMATION);

    private static final ErrorValue NO_CONNECTION_ERROR_VALUE = new ErrorValue(
            NO_CONNECTION, ErrorData.SEVERE);


    private static class EmptyResultSet implements List, ErrorData {
        private String error;

        public EmptyResultSet(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public int getSeverity() {
            return MISSING_DATA_SEVERITY;
        }

        public void add(int index, Object element) {}

        public boolean add(Object o) {
            return false;
        }

        public boolean addAll(Collection c) {
            return false;
        }

        public boolean addAll(int index, Collection c) {
            return false;
        }

        public void clear() {}

        public boolean contains(Object o) {
            return false;
        }

        public boolean containsAll(Collection c) {
            return false;
        }

        public Object get(int index) {
            return null;
        }

        public int indexOf(Object o) {
            return -1;
        }

        public boolean isEmpty() {
            return true;
        }

        public int lastIndexOf(Object o) {
            return -1;
        }

        public Iterator iterator() {
            return Collections.EMPTY_LIST.iterator();
        }

        public ListIterator listIterator() {
            return Collections.EMPTY_LIST.listIterator();
        }

        public ListIterator listIterator(int index) {
            return listIterator();
        }

        public Object remove(int index) {
            return null;
        }

        public boolean remove(Object o) {
            return false;
        }

        public boolean removeAll(Collection c) {
            return false;
        }

        public boolean retainAll(Collection c) {
            return false;
        }

        public Object set(int index, Object element) {
            return null;
        }

        public int size() {
            return 0;
        }

        public List subList(int fromIndex, int toIndex) {
            return this;
        }

        public Object[] toArray() {
            return new Object[0];
        }

        public Object[] toArray(Object[] a) {
            return Collections.EMPTY_LIST.toArray(a);
        }
    }
}
