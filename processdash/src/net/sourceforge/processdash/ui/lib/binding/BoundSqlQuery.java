// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib.binding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.ui.lib.binding.BoundSqlConnection.ConnectionSource;
import net.sourceforge.processdash.util.SqlResultData;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class BoundSqlQuery implements ErrorTokens {

    protected BoundMap map;

    protected String destPropName;

    protected String connPropName;

    protected String query;

    protected String emptyResultSetMessage;

    protected String[] parameterNames;


    protected static final Logger logger = Logger.getLogger(BoundSqlQuery.class
            .getName());


    public BoundSqlQuery(BoundMap map, Element xml) {
        String destPropName = xml.getAttribute("id");
        String connPropName = XMLUtils.getAttribute(xml, "connection",
                BoundSqlConnection.DEFAULT_ID);
        String query = xml.getAttribute("query");
        if (!XMLUtils.hasValue(query))
            query = XMLUtils.getTextContents(xml);
        String emptyMessage = XMLUtils.getAttribute(xml, "emptyMessage",
                NO_VALUES_FOUND);

        init(map, destPropName, connPropName, query, emptyMessage);
    }

    public BoundSqlQuery(BoundMap map, String destPropName,
            String connectionPropName, String query, String emptyMessage) {
        init(map, destPropName, connectionPropName, query, emptyMessage);
    }

    protected void init(BoundMap map, String destPropName,
            String connectionPropName, String query, String emptyMessage) {
        this.map = map;
        this.destPropName = destPropName;
        this.connPropName = connectionPropName;
        this.emptyResultSetMessage = emptyMessage;

        parseQuery(query);

        map.addPropertyChangeListener(parameterNames, this, "recalc");
        map.addPropertyChangeListener(connectionPropName, this, "recalc");

        recalc();
    }

    protected void parseQuery(String query) {
        List names = new ArrayList();
        Matcher m;
        while ((m = PARAM_PATTERN.matcher(query)).find()) {
            String paramName = m.group(1);
            names.add(paramName);
            query = query.substring(0, m.start(1)) + query.substring(m.end(1));
        }
        this.query = query;
        this.parameterNames = (String[]) names
                .toArray(new String[names.size()]);
    }

    protected static final Pattern PARAM_PATTERN = Pattern
            .compile("\\?([a-zA-Z_]+)");


    public void recalc() {
        boolean isNullPresent = false;
        Object[] parameterValues = new Object[parameterNames.length];
        for (int i = 0; i < parameterValues.length; i++) {
            Object value = map.get(parameterNames[i]);
            ErrorData errorData = null;

            if (ErrorValue.isRealError(value))
                errorData = (ErrorData) value;

            else if (value instanceof SqlResultData) {
                SqlResultData data = (SqlResultData) parameterValues[i];
                value = data.getSingleValue();
            }

            if (value == null) {
                isNullPresent = true;
                String missing = map.getErrorForMissingAttr(parameterNames[i]);
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

            ConnectionSource connectionSource = (ConnectionSource) map
                    .get(connPropName);
            if (connectionSource == null)
                return NO_CONNECTION_ERROR_VALUE;

            Connection connection = connectionSource.getConnection();
            if (connection == null) {
                if (ErrorValue.isRealError(connectionSource))
                    return (ErrorData) connectionSource;
                else
                    return NO_CONNECTION_ERROR_VALUE;
            }

            int sqlStep = PREPARING;
            try {
                PreparedStatement statement = connection
                        .prepareStatement(query);

                sqlStep = SETTING_PARAMS;
                statement.clearParameters();
                for (int i = 0; i < parameterValues.length; i++)
                    statement.setObject(i + 1, parameterValues[i]);

                sqlStep = EXECUTING;
                ResultSet results = statement.executeQuery();

                sqlStep = READING;
                SqlResultData resultData = new SqlResultData(results);

                if (resultData.isEmpty())
                    return new EmptyResultSet(emptyResultSetMessage);
                else
                    return resultData;

            } catch (SQLException sqle) {
                logger.log(Level.SEVERE, LOG_MESSAGES[sqlStep], sqle);
                return SQL_ERROR_VALUE;
            }
        }

        public void finished() {
            if (currentLoader == this)
                map.put(destPropName, get());
        }

    }

    private static final int PREPARING = 0;

    private static final int SETTING_PARAMS = 1;

    private static final int EXECUTING = 2;

    private static final int READING = 3;

    private static final String[] LOG_MESSAGES = {
            "Encountered error when preparing SQL statement",
            "Encountered error when setting parameter values",
            "Encountered error while executing statement",
            "Encountered error while reading result data", };

    private static final ErrorValue MISSING_DATA_ERROR_VALUE = new ErrorValue(
            DATA_MISSING, MISSING_DATA_SEVERITY);

    private static final ErrorValue LOADING_ERROR_VALUE = new ErrorValue(
            LOADING, ErrorData.INFORMATION);

    private static final ErrorValue NO_CONNECTION_ERROR_VALUE = new ErrorValue(
            NO_CONNECTION, ErrorData.SEVERE);

    private static final ErrorValue SQL_ERROR_VALUE = new ErrorValue(SQL_ERROR,
            ErrorData.SEVERE);


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

        public boolean equals(Object o) {
            return (o == this);
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
