// Copyright (C) 2026 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package teamdash.sync.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.InterpolatingProperties;

import teamdash.sync.ExtChange;
import teamdash.sync.ExtNode;
import teamdash.sync.ExtNodeSet;
import teamdash.sync.ExtSyncUtil;
import teamdash.sync.SyncDataFile;
import teamdash.sync.SyncMetadata;


public class SyncDatabaseNodeSet implements ExtNodeSet, ExtNodeSet.WithConfig {

    private SyncDatabaseConnection sync;

    private InterpolatingProperties properties;

    private SyncDataFile syncData;

    private Logger log;

    private NamedParamQuery baseQuery, itemQuery;

    private String itemIdColumnType;

    private MessageFormat itemKeyFmt, itemNameFmt, itemSimpleNameFmt,
            itemTypeFmt, itemTypeIdFmt, itemOwnerFmt, itemUrlFmt,
            itemUrlTextFmt;

    private int itemIdIdx, estHoursIdx, remHoursIdx, actualHoursIdx;

    private String wbsNodeType;


    public SyncDatabaseNodeSet(SyncDatabaseConnection sync, Map configDefaults,
            Element configXml, SyncDataFile syncData) {
        this.sync = sync;
        this.properties = ExtSyncUtil.getTargetProperties(
            SyncDatabaseNodeSet.class.getResourceAsStream(
                "db-defaults.properties"),
            configDefaults, configXml);
        this.syncData = syncData;
        this.syncData.setLogLevel(properties.getProperty("logLevel"));
        this.log = syncData.getLogger();
        initializeMetadata();
    }

    private void initializeMetadata() {
        // retrieve the SQL statements that will be used to drive the query
        this.baseQuery = new NamedParamQuery("query");
        this.itemQuery = new NamedParamQuery("itemQuery");
        if (itemQuery.keyParamPos == -1)
            throw new RuntimeException("Expected itemQuery to contain a "
                    + "'idColumnName in (:keys)' clause.");
        this.itemIdColumnType = getProperty("itemIdType");

        // get the templates that should be used for various node attributes
        this.itemIdIdx = getIndex("itemIdIdx");
        this.itemKeyFmt = getFormat("itemKeyFmt");
        this.itemNameFmt = getFormat("itemNameFmt");
        this.itemSimpleNameFmt = getFormat("itemSimpleNameFmt");
        this.itemTypeFmt = getFormat("itemTypeFmt");
        this.itemTypeIdFmt = getFormat("itemTypeIdFmt");
        this.itemOwnerFmt = getFormat("itemOwnerFmt");
        this.itemUrlFmt = getFormat("itemUrlFmt");
        this.itemUrlTextFmt = getFormat("itemUrlTextFmt");

        // get the optional positions of various hour columns
        this.estHoursIdx = getIndex("estHoursIdx");
        this.remHoursIdx = getIndex("remHoursIdx");
        this.actualHoursIdx = getIndex("actualHoursIdx");

        // get the type of WBS node that should be created for incoming items
        this.wbsNodeType = getProperty("wbsNodeType");
    }

    private String getProperty(String attr) {
        return properties.getProperty(attr);
    }

    private MessageFormat getFormat(String attr) {
        try {
            String fmt = getProperty(attr);
            if (fmt == null)
                return null;
            return new MessageFormat(fmt);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Invalid message format for '" + attr + "' property.");
        }
    }

    private int getIndex(String attr) {
        try {
            String idx = getProperty(attr);
            if (idx == null)
                return -1;
            return Integer.parseInt(idx);
        } catch (NumberFormatException nfe) {
            throw new RuntimeException(
                    "Invalid number provided for the '" + attr + "' property");
        }
    }


    @Override
    public Properties getEffectiveConfig() {
        return properties;
    }


    @Override
    public List<ExtNode> getExtNodes(Set<String> includingIDs)
            throws IOException {
        Connection conn;
        try {
            conn = sync.getConnection();
        } catch (Exception e) {
            throw new IOException("Unable to open database connection", e);
        }

        List<ExtNode> result = new ArrayList<ExtNode>();
        Set<String> itemIdsNeeded = new HashSet(includingIDs);
        try {
            getExtNodes(result, itemIdsNeeded, conn, false);
            getExtNodes(result, itemIdsNeeded, conn, true);
            return result;

        } catch (Exception e) {
            throw new IOException("Unable to perform database query", e);
        } finally {
            sync.releaseConnection(conn);
        }
    }

    private void getExtNodes(List<ExtNode> result, Set<String> itemsNeeded,
            Connection conn, boolean itemPass) throws SQLException {
        // abort if we are doing the item pass but no items are needed
        if (itemPass && itemsNeeded.isEmpty())
            return;

        // prepare the SQL statement and bind parameters
        NamedParamQuery q = (itemPass ? itemQuery : baseQuery);
        PreparedStatement s = q.prepareAndBind(conn, itemsNeeded);

        // execute the query and retrieve a result set
        ResultSet rs = s.executeQuery();
        int numCols = rs.getMetaData().getColumnCount();

        // retrieve the Java type of the identifier column
        if (itemIdColumnType == null) {
            itemIdColumnType = rs.getMetaData().getColumnClassName(itemIdIdx);
        }

        // iterate over the result set and build nodes for each row
        while (rs.next()) {
            // put null in position 0 so the formatters can use 1-based indexes
            Object[] rowData = new Object[numCols + 1];
            for (int i = 1; i <= numCols; i++)
                rowData[i] = rs.getObject(i);
            DatabaseNode node = new DatabaseNode(rowData);
            itemsNeeded.remove(node.getID());
            result.add(node);
        }
    }


    @Override
    public void applyWbsChanges(List<ExtChange> changes, SyncMetadata metadata)
            throws IOException {
        // bidirectional sync is not supported; do nothing
    }



    private class NamedParamQuery {

        private List<String> parameters;

        private int keyParamPos;

        private Set<String> badKeys;

        private String sql;

        public NamedParamQuery(String sqlProperty) {
            this.parameters = new ArrayList<String>();
            this.keyParamPos = -1;
            this.badKeys = new HashSet<String>();

            String baseSql = getProperty(sqlProperty);
            if (baseSql == null)
                throw new RuntimeException("An SQL statement must be "
                        + "specified via the '" + sqlProperty + "' property");

            StringBuffer sb = new StringBuffer();
            Matcher m = PARAM_PAT.matcher(baseSql);
            while (m.find()) {
                String param = m.group(1);
                if (param != null) {
                    // when we find a named parameter, add it to the param list
                    parameters.add(param);
                    m.appendReplacement(sb, "?");
                    if (KEYS.equals(param))
                        keyParamPos = sb.length() - 1;
                } else {
                    // if we find a string literal, add it to the query without
                    // making any changes
                    m.appendReplacement(sb, m.group(0));
                }
            }
            this.sql = m.appendTail(sb).toString();
        }

        public PreparedStatement prepareAndBind(Connection conn,
                Set<String> keys) throws SQLException {
            // build the query we will execute
            String query;
            if (keyParamPos == -1 || keys.size() == 1) {
                // if this SQL statement doesn't make use of the keyset, it can
                // be used verbatim. If it uses the keyset but we only need to
                // find a single key, it's also good verbatim.
                query = sql;
            } else {
                // if we are searching for multiple items, add extra "?"
                // placeholders so we have one for each item key
                StringBuilder sb = new StringBuilder();
                sb.append(sql.substring(0, keyParamPos));
                for (int i = keys.size(); i-- > 1;)
                    sb.append("?, ");
                sb.append(sql.substring(keyParamPos));
                query = sb.toString();
            }

            // create a PreparedStatement for that query
            PreparedStatement s = conn.prepareStatement(query);

            // bind all parameter values to the statement
            int i = 0;
            for (String paramName : parameters) {
                if (KEYS.equals(paramName)) {
                    ParamType keyType = ParamType.forType(itemIdColumnType);
                    for (String oneKey : keys) {
                        try {
                            keyType.setParam(s, ++i, oneKey);
                        } catch (NumberFormatException nfe) {
                            if (badKeys.add(oneKey))
                                log.warning("Ignoring invalid numeric key '"
                                        + oneKey + "'");
                            keyType.setParam(s, i, "-1");
                        }
                    }
                } else {
                    String paramValue = getProperty(paramName);
                    s.setString(++i, paramValue);
                }
            }

            // return the prepared and bound statement
            return s;
        }

        private static final String KEYS = "keys";
    }

    private static final Pattern PARAM_PAT = Pattern
            .compile("'[^']*'|:([\\w]+)");



    private enum ParamType {

        STRING {
            void setParam(PreparedStatement s, int pos, String val)
                    throws SQLException {
                s.setString(pos, val);
            }
        },

        LONG {
            void setParam(PreparedStatement s, int pos, String val)
                    throws SQLException {
                s.setLong(pos, Long.parseLong(val));
            }
        },

        INTEGER {
            void setParam(PreparedStatement s, int pos, String val)
                    throws SQLException {
                s.setInt(pos, Integer.parseInt(val));
            }
        },

        SHORT {
            void setParam(PreparedStatement s, int pos, String val)
                    throws SQLException {
                s.setShort(pos, Short.parseShort(val));
            }
        };

        abstract void setParam(PreparedStatement s, int pos, String val)
                throws SQLException;

        static ParamType forType(String typeName) {
            if (typeName != null) {
                typeName = typeName.toUpperCase();
                for (ParamType t : values()) {
                    if (typeName.endsWith(t.name()))
                        return t;
                }
            }
            return INTEGER;
        }
    }



    private class DatabaseNode implements ExtNode {

        private Object[] rowData;

        public DatabaseNode(Object[] rowData) {
            this.rowData = rowData;
        }

        @Override
        public String getID() {
            return rowData[itemIdIdx].toString();
        }

        @Override
        public String getKey() {
            return format(itemKeyFmt);
        }

        @Override
        public String getName() {
            return format(itemNameFmt);
        }

        @Override
        public String getSimpleName() {
            return format(itemSimpleNameFmt);
        }

        @Override
        public String getType() {
            return format(itemTypeFmt);
        }

        @Override
        public String getTypeID() {
            return format(itemTypeIdFmt);
        }

        @Override
        public String getWbsType() {
            String extType = getType();
            String wbsType = properties.getProperty("wbsNodeType." + extType,
                wbsNodeType);
            return wbsType;
        }

        @Override
        public List<ExtNode> getChildren() {
            return Collections.EMPTY_LIST;
        }

        @Override
        public String getOwner() {
            return format(itemOwnerFmt);
        }

        @Override
        public String getUrl() {
            // if no URL template was provided, return null
            if (itemUrlFmt == null)
                return null;

            // create URL-encoded versions of the row data
            String[] args = new String[rowData.length];
            for (int i = args.length; i-- > 0;) {
                if (rowData[i] == null)
                    args[i] = "";
                else
                    args[i] = HTMLUtils.urlEncode(rowData[i].toString());
            }
            String url = itemUrlFmt.format(args);
            String text = format(itemUrlTextFmt);
            return url + " " + text;
        }

        private String format(MessageFormat fmt) {
            return (fmt == null ? null : fmt.format(rowData));
        }

        @Override
        public Double getEstimatedHours() {
            return getNumber(estHoursIdx, null);
        }

        @Override
        public Double getRemainingHours() {
            return getNumber(remHoursIdx, null);
        }

        @Override
        public Double getActualHours() {
            return getNumber(actualHoursIdx, ZERO);
        }

        private Double getNumber(int idx, Double defVal) {
            if (idx < 0 || idx >= rowData.length)
                return defVal;

            Object n = rowData[idx];
            return (n instanceof Number ? ((Number) n).doubleValue() : defVal);
        }

    }

    private static final Double ZERO = Double.valueOf(0.0);

}
