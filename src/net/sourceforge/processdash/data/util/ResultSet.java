// Copyright (C) 2001-2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.util;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import net.sourceforge.processdash.data.DataComparator;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.MalformedValueException;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.ValueFactory;
import net.sourceforge.processdash.data.compiler.Compiler;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.SearchFunction;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.util.LocalizedString;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;



public class ResultSet {

    protected String prefix[];
    protected String suffix[];
    protected double multiplier[];
    protected Object[][] data;
    boolean useRowFormats = false;
    boolean translateRowHeaders = false;
    boolean translateColHeaders = true;

    /** Create a result set to store the given number of rows and columns
     * of data. */
    public ResultSet(int numRows, int numCols) {
        this(numRows, numCols, false);
    }

    protected ResultSet(int numRows, int numCols, boolean useRowFormats) {
        // Add a header row and a header column
        data = new Object[numRows+1][numCols+1];
        this.useRowFormats = useRowFormats;
        int numFormats = useRowFormats ? numRows : numCols;
        multiplier = new double[numFormats+1];
        prefix = new String[numFormats+1];
        suffix = new String[numFormats+1];
        for (int i=numFormats;  i >= 0;  i--) setFormat(i, null);
    }

    /** Return a new ResultSet which is the transposition of this one. */
    public ResultSet transpose() {
        int row = numRows(), col = numCols();
        ResultSet result = new ResultSet(col, row, !useRowFormats);
        for (;  row >= 0;  row--)
            for (col=numCols();  col >= 0;  col--)
                result.data[col][row] = data[row][col];
        for (int f=prefix.length;  f-- > 0; ) {
            result.prefix[f]     = prefix[f];
            result.suffix[f]     = suffix[f];
            result.multiplier[f] = multiplier[f];
        }
        // transpose header translation flags, if necessary.
        if (translateRowHeaders != translateColHeaders) {
            translateRowHeaders = !translateRowHeaders;
            translateColHeaders = !translateColHeaders;
        }

        return result;
    }

    public ResultSet pluckColumns(int...columns) {
        int row = this.numRows();
        ResultSet result = new ResultSet(row, columns.length);
        for (;  row >= 0;  row--) {
            result.setRowName(row, this.getRowName(row));
            for (int i = columns.length;  i-- > 0; ) {
                result.data[row][i+1] = this.data[row][columns[i]];
            }
        }
        for (int i = columns.length;  i-- > 0; ) {
            result.prefix[i+1] = this.prefix[columns[i]];
            result.suffix[i+1] = this.suffix[columns[i]];
            result.multiplier[i+1] = this.multiplier[columns[i]];
        }
        return result;
    }

    private static String asString(Object o) {
        return (o == null) ? null : o.toString();
    }

    /** Return the number of rows of data, not counting the header row. */
    public int numRows() { return data.length - 1; }
    /** Change the header name of a given row.
     * Data row numbering starts at 1 and ends at numRows(). */
    public void setRowName(int row, String name) { data[row][0] = name; }
    /** Get the header name of a given row.
     * Data row numbering starts at 1 and ends at numRows(). */
    public String getRowName(int row) {
        return getRowName(row, translateRowHeaders);
    }
    private String getRowName(int row, boolean translateRowHeaders) {
        String result = asString(data[row][0]);
        if (translateRowHeaders) result = Translator.translate(result);
        return result;
    }

    /** Return the number of columns of data, not counting the header col. */
    public int numCols() { return data[0].length - 1; }
    /** Change the header name of a given column.
     * Data column numbering starts at 1 and ends at numCols(). */
    public void setColName(int col, String name) { data[0][col] = name; }
    /** Get the header name of a given column.
     * Data column numbering starts at 1 and ends at numCols(). */
    public String getColName(int col) {
        return getColName(col, translateColHeaders);
    }
    public String getColName(int col, boolean translateColHeaders) {
        String result = asString(data[0][col]);
        if (translateColHeaders) result = Translator.translate(result);
        return result;
    }

    /** Store an object in the result set.
     * Data rows and columns are numbered starting with 1 and ending with
     * numRows() or numCols(), respectively. */
    public void setData(int row, int col, Object value) {
        if (row == 0 || col == 0) throw new ArrayIndexOutOfBoundsException();
        data[row][col] = value;
    }
    /** Get an object from the result set.
     * Data rows and columns are numbered starting with 1 and ending with
     * numRows() or numCols(), respectively. */
    public SimpleData getData(int row, int col) {
        if (row == 0 || col == 0) throw new ArrayIndexOutOfBoundsException();
        Object o = data[row][col];
        if (o instanceof NumberData && ((NumberData)o).isDefined())
            return new DoubleData
                (((NumberData) o).getDouble() *
                 multiplier[useRowFormats ? row : col], false);
        else
            return (o instanceof SimpleData) ? (SimpleData) o : null;
    }
    public void setFormat(int col, String format) {
        int len = 0, p, s;
        if (format == null || (len = format.length()) == 0) {
            multiplier[col] = 1.0;
            prefix[col] = suffix[col] = "";
        }

                                // find end of prefix
        for (p=0;  p<len && !Character.isDigit(format.charAt(p));  p++);
                                // find beginning of suffix
        for (s=len;  s>0 && !Character.isDigit(format.charAt(s-1));  s--);

        if (p > 0) prefix[col] = format.substring(0, p);
        if (s > p) {
            suffix[col] = format.substring(s);
            String mult = format.substring(p, s);
            try {
                if (mult.indexOf('.') == -1)
                    multiplier[col] = Integer.parseInt(mult);
                else
                    multiplier[col] = Double.parseDouble(mult);
            } catch (NumberFormatException nfe) {}
            if (multiplier[col] == 0) multiplier[col] = 1.0;
        }
    }

    /** Format an object from the result set for display.
     * Data rows and columns are numbered starting with 1 and ending with
     * numRows() or numCols(), respectively. Row 0 and column 0 contain
     * header information. Null values in the ResultSet will be formatted as
     * the empty string. */
    public String format(int row, int col) {
        // row and column headers are strings
        if (row == 0) return getColName(col);
        if (col == 0) return getRowName(row);

        SimpleData d = getData(row, col);
        if (d == null || !d.isDefined()) return "";

        String result = d.format();
        if (result.startsWith("#") || result.startsWith("ERR")) return result;

        int fmt = useRowFormats ? row : col;
        return prefix[fmt] + result + suffix[fmt];
    }

    /** Resort the result set by the data in the specified column.
     * @throws ArrayIndexOutOfBoundsException unless 1 <= col <= numCols().
     * WARNING: do not use this routine on a transposed result set which
     * uses row formats. The mapping of format to row will not be preserved.
     */
    public void sortBy(int col, boolean descending) {
        if (col < 1 || col > numCols())
            throw new ArrayIndexOutOfBoundsException();
        Arrays.sort(data, 1, numRows()+1, new RowComparator(col, descending));
    }

    /** Remove a row from the data set.
     * @param row The row to remove
     */
    public void removeRow(int row) {
        if (row < 1 || row > numRows())
            throw new ArrayIndexOutOfBoundsException();

        List newData = new ArrayList(Arrays.asList(data));
        newData.remove(row);
        data = (Object[][]) newData.toArray(new Object[numRows()][0]);
    }

    private static class NullDataListener implements DataListener {
        public void dataValueChanged(DataEvent e) {}
        public void dataValuesChanged(Vector v) {}
    }
    private static NullDataListener NULL_LISTENER = new NullDataListener();

    private static boolean forParamIsTagName(String forParam) {
        if (forParam == null || forParam.length() == 0) return false;
        if (forParam.charAt(0) == '/') return false;
        if (forParam.charAt(0) == '[') return false;
        return true;
    }

    /** Based upon the given "for" parameter, return the name of a
     *  data element in the repository that will list all the appropriate
     *  prefixes. */
    private static String getDataListName(DataRepository data,
                                          String forParam,
                                          String prefix) {
        if (forParam == null || forParam.length() == 0) return null;
        switch (forParam.charAt(0)) {
        case '/': return forParam;
        case '[': return DataRepository.createDataName
                      (prefix, Compiler.trimDelim(forParam));
        default:
        }

        String dataName =
            DataRepository.createDataName(FAKE_DATA_NAME, forParam);
        if (data.getValue(dataName) == null) {
            data.putValue(dataName, new SearchFunction(null, dataName, "",
                    forParam, null, data, ""));
            // this keeps the search function from disappearing on us.
            data.addDataListener(dataName, NULL_LISTENER);
        }

        return dataName;
    }

    private static final String esc(String a) {
        return Compiler.escapeLiteral(a);
    }

    private static class ResultSetSearchExpression {
        String forList, orderBy;
        String [] conditions;
        private int hashCode = -1;
        private String expression = null;

        ResultSetSearchExpression(DataRepository data,
                                  String forParam,
                                  String prefix,
                                  String [] conditions,
                                  String orderBy) {
            if (prefix == null) prefix = "";
            this.forList = getDataListName(data, forParam, prefix);
            if (conditions != null) Arrays.sort(conditions);
            this.conditions = conditions;
            this.orderBy = orderBy;
        }
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ResultSetSearchExpression)) return false;
            ResultSetSearchExpression that = (ResultSetSearchExpression) obj;
            return (compareStrings(this.forList,   that.forList) &&
                    compareStrings(this.orderBy,   that.orderBy) &&
                    compareArrays(this.conditions, that.conditions));
        }
        private boolean compareStrings(String a, String b) {
            if (a == b) return true;
            if (a == null || b == null) return false;
            return a.equals(b);
        }
        private boolean compareArrays(String [] a, String [] b) {
            if (a == b) return true;
            if (a == null || b == null) return false;
            if (a.length != b.length) return false;
            for (int i = a.length;   i-- > 0; )
                if (!compareStrings(a[i], b[i])) return false;
            return true;
        }
        @Override
        public int hashCode() {
            if (hashCode == -1) {
                int result = forList.hashCode();
                if (orderBy != null)
                    result = result ^ (orderBy.hashCode() << 1);
                if (conditions != null)
                    for (int i = conditions.length;  i-- > 0; )
                        result = result ^ (conditions[i].hashCode() << 2);
                hashCode = result;
            }
            return hashCode;
        }

        public String buildExpression() {
            if (expression != null) return expression;

            StringBuffer expr = new StringBuffer();
            expr.append("[").append(esc(forList)).append("]");

            if (conditions != null && conditions.length > 0) {
                StringBuffer condExpr = new StringBuffer();
                String cond;
                for (int i = conditions.length;   i-- > 0;  ) {
                    condExpr.append(" && ");
                    cond = conditions[i];
                    if (cond.indexOf('[') == -1)
                        condExpr.append("[").append(esc(cond)).append("]");
                    else
                        condExpr.append("(").append(cond).append(")");
                }

                cond = esc(condExpr.toString().substring(4));
                expr.append(")").insert(0, "\", [_]), ")
                    .insert(0, cond).insert(0, "filter(eval(\"");
            }

            if (orderBy != null && orderBy.length() > 0) {
                expr.append(")").insert(0, "\", ").insert(0, esc(orderBy))
                    .insert(0, "sort(\"");
            }

            expression = expr.toString();
            //System.out.println("expression is " + expression);
            return expression;
        }
    }


    private static Map listNames = new Hashtable();
    private static int listNumber = 0;

    private static ListData getList(DataRepository data, String forParam,
                                    String[] conditions, String orderBy,
                                    String basePrefix) {
        // construct an applicable ResultSetSearchExpression.
        ResultSetSearchExpression rsse = new ResultSetSearchExpression
            (data, forParam, basePrefix, conditions, orderBy);

        // see if someone else has already generated a list name for
        // our ResultSetSearchExpression.  If not, generate one.
        String listName;
        synchronized (listNames) {
            listName = (String) listNames.get(rsse);
            if (listName == null) {
                int num = listNumber++;
                listName = DataRepository.createDataName
                    (FAKE_DATA_NAME, "List" + num);
                listNames.put(rsse, listName);
            }
        }

        // fetch the value of the named list.
        ListData result = lookupList(data, listName);

        // if the named list has no value yet, create the value and
        // save it in the repository.
        if (result == null) synchronized (listName) {
            result = lookupList(data, listName);
            if (result == null) {
                String expression = rsse.buildExpression();

                try {
                    data.putExpression(listName, "", expression);
                } catch (MalformedValueException mve) {
                    System.err.println("malformed value!");
                    data.putValue(listName, new ListData());
                }
                data.addDataListener(listName, NULL_LISTENER);
                result = lookupList(data, listName);
            }
        }

        return result;
    }

    private static ListData lookupList(DataRepository data, String name) {
        SimpleData result = data.getSimpleValue(name);
        if (result instanceof ListData)
            return (ListData) result;
        if (result instanceof StringData)
            return ((StringData) result).asList();
        return null;
    }

    private static ListData getFilteredList(DataRepository data,
                                            String forParam,
                                            String[] conditions,
                                            String orderBy,
                                            String basePrefix) {
        ListData result;
            // if the "for" parameter is missing or is simply ".", then
            // return a list containing only one element - the base prefix.
        if (forParam == null ||
            forParam.length() == 0 ||
            forParam.equals(".")) {
            result = new ListData();
            result.add(basePrefix);

            // if the for parameter specifies a data list rather than
            // a tag name, or if the base prefix is empty, return all the
            // items in the list in question.
        } else if (basePrefix == null || basePrefix.length() == 0 ||
                   !forParamIsTagName(forParam)) {
            result =
                getList(data, forParam, conditions, orderBy, basePrefix);

            // otherwise, get the appropriate prefix list, and create
            // a filtered copy containing only those prefixes that
            // start with the base prefix.
        } else {
            ListData fullPrefixList =
                getList(data, forParam, conditions, orderBy, basePrefix);
            result = new ListData();
            String item;
            for (int i = 0;   i < fullPrefixList.size();   i++) {
                item = (String) fullPrefixList.get(i);
                if (item.equals(basePrefix) ||
                    item.startsWith(basePrefix + "/"))
                    result.add(item);
            }
        }

        return result;
    }


    /** Perform a query and return a result set. */
    public static ResultSet get(DataRepository data, String forParam,
                                String[] conditions, String orderBy,
                                String[] dataNames, String basePrefix) {
        // get the list of prefixes
        ListData prefixList = getFilteredList(data, forParam, conditions,
                                              orderBy, basePrefix);

        // Create a result set to return
        ResultSet result = new ResultSet(prefixList.size(), dataNames.length);

        // write the column headers into the result set.
        result.setColName(0, null);
        for (int i=0;  i < dataNames.length;  i++)
            result.setColName(i+1, dataNames[i]);

        // get the data and fill the result set.
        String prefix, dataName;
        if (basePrefix == null) basePrefix = "";
        int baseLen = basePrefix.length();
        if (baseLen > 0) baseLen++; // remove / as well

        for (int p=0;  p < prefixList.size();  p++) {
            // get the next prefix
            Object pfx = prefixList.get(p);
            if (pfx instanceof String)
                prefix = (String) pfx;
            else if (pfx instanceof SimpleData)
                prefix = ((SimpleData) pfx).format();
            else
                continue;

            // write the row headers into the result set.
            if (prefix.length() > baseLen && prefix.startsWith(basePrefix))
                result.setRowName(p+1, prefix.substring(baseLen));
            else {
                result.setRowName(p+1, prefix);
                if (!prefix.startsWith("/"))
                    prefix = DataRepository.createDataName(basePrefix, prefix);
            }

            // look up the data for this row.
            for (int d=0;  d < dataNames.length;  d++)

                if (dataNames[d].startsWith("\"")) {
                    try {
                        result.setData(p+1, d+1, new StringData(dataNames[d]));
                    } catch (MalformedValueException mve) {}

                } else if (dataNames[d].indexOf('[') != -1) {
                    try {
                        result.setData(p+1, d+1,
                                       data.evaluate(dataNames[d], prefix));
                    } catch (Exception e) {}

                } else {
                    dataName = DataRepository.createDataName(prefix, dataNames[d]);
                    result.setData(p+1, d+1, data.getSimpleValue(dataName));
                }
        }
        return result;
    }



    /** Perform a query and return a result set, using the old-style
        mechanism. */
    public static ResultSet get(DataRepository data, String[] conditions,
                                String orderBy, String[] dataNames,
                                String basePrefix, Comparator nodeComparator) {

        // Construct a regular expression for searching the repository.
        StringBuffer re =  new StringBuffer("~(.*/)?");
        if (conditions != null)
            for (int c=0;  c < conditions.length;  c++)
                re.append("{").append(conditions[c]).append("}");
        if (orderBy == null) orderBy = dataNames[0];
        re.append(orderBy);

        // Find data elements that match the regular expression.
        if (basePrefix == null) basePrefix = "";
        SortedList list = SortedList.getInstance
            (data, re.toString(), basePrefix, FAKE_DATA_NAME, nodeComparator);
        String [] prefixes = list.getNames();

        // Create a result set to return
        ResultSet result = new ResultSet(prefixes.length, dataNames.length);

        // write the column headers into the result set.
        result.setColName(0, null);
        for (int i=0;  i < dataNames.length;  i++)
            result.setColName(i+1, dataNames[i]);

        // get the data and fill the result set.
        String prefix, dataName;
        int baseLen = basePrefix.length(), tailLen = orderBy.length() + 1;
        if (baseLen > 0) baseLen++; // remove / as well

        String [] fixedUpNames = new String[dataNames.length];
        for (int i=dataNames.length;  i>0;  )
            if (dataNames[--i].charAt(0) == '!')
                fixedUpNames[i] = fixupName(dataNames[i]);
        SaveableData value;

        for (int p=0;  p < prefixes.length;  p++) {
            // get the next prefix
            prefix = prefixes[p];
            // remove the name of the orderBy data element, & the preceeding /
            prefix = prefix.substring(0, prefix.length() - tailLen);
            if (baseLen > prefix.length())
                result.setRowName(p+1, "");
            else
                result.setRowName(p+1, prefix.substring(baseLen));

            // look up the data for this row.
            for (int d=0;  d < dataNames.length;  d++)
                if (dataNames[d].startsWith("![(")) {
                    dataName = DataRepository.anonymousPrefix + "/" +
                        prefix + "/" + fixedUpNames[d];
                    value = data.getSimpleValue(dataName);

                    if (value == null) try {
                        value = ValueFactory.create
                            (dataName, dataNames[d], data, prefix);
                        data.putValue(dataName, value);
                    } catch (MalformedValueException mve) { }
                    result.setData(p+1, d+1,
                                   value==null ?null :value.getSimpleValue());

                } else if (dataNames[d].startsWith("\"")) { try {
                    result.setData(p+1, d+1, new StringData(dataNames[d]));
                } catch (MalformedValueException mve) {
                    result.setData(p+1, d+1, null);
                }

                } else {
                    dataName = DataRepository.createDataName
                        (prefix, dataNames[d]);
                    result.setData(p+1, d+1, data.getSimpleValue(dataName));
                }
        }
        return result;
    }
    private static final String FAKE_DATA_NAME =
        DataRepository.anonymousPrefix + "/Data Enumerator";

    private static String getUseParam(Map queryParameters) {
        // useParam is stored in the "useData" parameter
        return (String) queryParameters.get("useData");
    }
    private static String getForParam(Map queryParameters) {
        // forParam is stored in the "for" parameter
        return (String) queryParameters.get("for");
    }
    private static String getOrderBy(Map queryParameters) {
        // orderBy dataElement name is stored in the "order" parameter
        return (String) queryParameters.get("order");
    }
    private static String[] getConditions(Map queryParameters) {
        // conditions are given via "where" parameter(s)
        String [] conditions = (String[]) queryParameters.get("where_ALL");
        if (conditions == null) {
            String cond = (String) queryParameters.get("where");
            if (cond != null) {
                conditions = new String[1];
                conditions[0] = cond;
            }
        }
        return conditions;
    }

    /** Perform a query and return a result set.
     *  the queryParameters Map contains the instructions for performing
     * the query */
    public static ResultSet get(DataRepository data, Map queryParameters,
                                String prefix, Comparator nodeComparator) {

        String useParam = getUseParam(queryParameters);
        if (useParam != null)
            return getResultSetFromRepository(data, prefix, useParam);

        String forParam      = getForParam(queryParameters);
        String orderBy       = getOrderBy(queryParameters);
        String [] conditions = getConditions(queryParameters);

        // data names are given with parameters like "d1", "d2", "d3", etc.
        int i = 1;
        while (queryParameters.get("d" + i) != null) i++;
        String [] dataNames = new String[i-1];
        while (--i > 0) dataNames[i-1] = (String) queryParameters.get("d" + i);

        // fetch the results.
        ResultSet result = (forParam == null
            ? get(data, conditions, orderBy, dataNames, prefix, nodeComparator)
            : get(data, forParam, conditions, orderBy, dataNames, prefix));

        // parameters "h0", "h1", etc specify overridden column headers
        String colHeader;
        for (i=dataNames.length;  i >= 0;  i--) {
            colHeader = (String) queryParameters.get("h" + i);
            if (colHeader != null)
                result.setColName(i, colHeader);
        }

        // set format for each column
        String format;
        for (i=dataNames.length;  i > 0;  i--) {
            format = (String) queryParameters.get("f" + i);
                                // if a format was given, set it.
            if (format != null) result.setFormat(i, format);
            // if no format was given, but the data name contains a "%",
            // assume % format.
            else if (!dataNames[i-1].startsWith("![(") &&
                     dataNames[i-1].indexOf('%') != -1)
                result.setFormat(i, "100%");
        }

        return result;
    }

    private static ResultSet getResultSetFromRepository(DataRepository data,
            String prefix, String useParam) {
        String dataName = DataRepository.createDataName(prefix, useParam);
        ListData l = ListData.asListData(data.getSimpleValue(dataName));
        if (l != null && l.size() == 1)
            return (ResultSet) l.get(0);
        else
            return null;
    }


    /** Return the list of prefixes that would have been used to generate
     *  a result set.
     */
    public static String[] getPrefixList(DataRepository data,
                                         Map queryParameters,
                                         String prefix) {
        ListData list =  getFilteredList(data,
                                         getForParam(queryParameters),
                                         getConditions(queryParameters),
                                         getOrderBy(queryParameters),
                                         prefix);
        int i = list.size();
        String [] result = new String[i];
        while (i-- > 0) result[i] = asString(list.get(i));
        return result;
    }


    private static String fixupName(String name) {
        return "anonymousChart" + name.hashCode();
    }


    private class RowComparator implements Comparator {

        int colNum;
        boolean descending;

        public RowComparator(int colNum, boolean descending) {
            this.colNum = colNum;
            this.descending = descending;
        }

        public int compare(Object o1, Object o2) {
            // The items being passed in will be rows of the ResultSet.
            Object [] row1 = (Object[]) o1;
            Object [] row2 = (Object[]) o2;

            int result =
                DataComparator.getInstance().compare(row1[colNum], row2[colNum]);
            return descending ? 0-result : result;
        }
    }


    private class Category implements Comparable, LocalizedString {
        int ordinal;
        String value;
        String unlocalized;
        public Category(int ord, String val, String unlocalized) {
            this.ordinal = ord;
            this.value = val;
            this.unlocalized = unlocalized;
        }
        @Override
        public String toString() {
            return value;
        }
        public String getUnlocalizedString() {
            return unlocalized;
        }
        public int compareTo(Object o) {
            return ((Category) o).ordinal - ordinal;
        }
        @Override
        public boolean equals(Object o) {
            return ordinal == ((Category) o).ordinal;
        }
        @Override
        public int hashCode() {
            return ordinal;
        }
    }

    ////////////////////////////////////////////////////////////////////////
    // CategoryDataSource
    ////////////////////////////////////////////////////////////////////////

    protected class RSCategoryDataSource extends DefaultCategoryDataset
    {
        RSCategoryDataSource() {
            for (int row = 1;   row <= numRows();   row++) {
                Category rowCat = new Category(row, getRowName(row),
                        getRowName(row, false));
                for (int col = 1;   col <= numCols();   col++) {
                    Category colCat = new Category(col-1000, getColName(col),
                            getColName(col, false));
                    addValue(getNumber(row, col), colCat, rowCat);
                }
            }
        }
    }

    public CategoryDataset catDataSource() {
        return new RSCategoryDataSource();
    }

    protected class RSXYDataSource extends AbstractXYDataset
        implements XYDataset, XYToolTipGenerator
    {
        /** Returns the number of series in the data source. */
        @Override
        public int getSeriesCount() { return numCols() - 1; }
        /** Returns the name of the specified series (zero-based). */
        @Override
        public Comparable getSeriesKey(int seriesIndex) {
            return getColName(seriesIndex+2); }

        /** Returns the x-value for the specified series and item */
        public Number getX(int seriesIndex, int itemIndex) {
            return getNumber(itemIndex+1, 1); }
        /** Returns the y-value for the specified series and item */
        public Number getY(int seriesIndex, int itemIndex) {
            if (itemIndex == -1)
                return (numRows() > 0 && numCols() > 0 &&
                        (getData(1,1) instanceof DateData)) ? null : ZERO;
            return getNumber(itemIndex+1, seriesIndex+2); }
        /** Returns the number of items in the specified series */
        public int getItemCount(int seriesIndex) { return numRows(); }

        public String generateToolTip(XYDataset dataset, int series, int item) {
                return getRowName(item+1) + ": ("
                    + format(item+1, 1) + ", "
                    + format(item+1, series+2) + ")";
        }
    }
    private static Double ZERO = new Double(0.0);

    public XYDataset xyDataSource() {
        sortBy(1, false);       // ensure data is sorted by X, ascending.
        return new RSXYDataSource();
    }

    protected Number asNumber(SimpleData s) {
        double d = 0.0;
        if (s instanceof NumberData) {
            d = ((NumberData) s).getDouble();
            if (Double.isNaN(d) || Double.isInfinite(d)) d = 0.0;
        } else if (s instanceof DateData) {
            d = ((DateData) s).getValue().getTime();
        }
        return new Double(d);
    }
    protected Number getNumber(int row, int col) {
        return asNumber(getData(row, col));
    }
}
