// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash.data;

import com.jrefinery.chart.CategoryDataSource;
import com.jrefinery.chart.XYDataSource;
import com.jrefinery.chart.event.DataSourceChangeListener;
import pspdash.PSPProperties;

import java.util.*;

public class ResultSet {

    protected Object[][] data;

    /** Create a result set to store the given number of rows and columns
     * of data. */
    protected ResultSet(int numRows, int numCols) {
        // Add a header row and a header column
        data = new Object[numRows+1][numCols+1];
    }

    /** Return a new ResultSet which is the transposition of this one. */
    public ResultSet transpose() {
        int row = numRows(), col = numCols();
        ResultSet result = new ResultSet(col, row);
        for (;  row >= 0;  row--)
            for (;  col >= 0;  col--)
                result.data[col][row] = data[row][col];
        return result;
    }

    private String asString(Object o) {
        return (o == null) ? null : o.toString();
    }

    /** Return the number of rows of data, not counting the header row. */
    public int numRows() { return data.length - 1; }
    /** Change the header name of a given row.
     * Data row numbering starts at 1 and ends at numRows(). */
    protected void setRowName(int row, String name) { data[row][0] = name; }
    /** Get the header name of a given row.
     * Data row numbering starts at 1 and ends at numRows(). */
    public String getRowName(int row) { return asString(data[row][0]); }

    /** Return the number of columns of data, not counting the header col. */
    public int numCols() { return data[0].length - 1; }
    /** Change the header name of a given column.
     * Data column numbering starts at 1 and ends at numCols(). */
    protected void setColName(int col, String name) { data[0][col] = name; }
    /** Get the header name of a given column.
     * Data column numbering starts at 1 and ends at numCols(). */
    public String getColName(int col) { return asString(data[0][col]); }

    /** Store an object in the result set.
     * Data rows and columns are numbered starting with 1 and ending with
     * numRows() or numCols(), respectively. */
    protected void setData(int row, int col, Object value) {
        if (row == 0 || col == 0) throw new ArrayIndexOutOfBoundsException();
        data[row][col] = value;
    }
    /** Get an object from the result set.
     * Data rows and columns are numbered starting with 1 and ending with
     * numRows() or numCols(), respectively. */
    public SimpleData getData(int row, int col) {
        if (row == 0 || col == 0) throw new ArrayIndexOutOfBoundsException();
        Object o = data[row][col];
        return (o instanceof SimpleData) ? (SimpleData) o : null;
    }

    /** Format an object from the result set for display.
     * Data rows and columns are numbered starting with 1 and ending with
     * numRows() or numCols(), respectively. Row 0 and column 0 contain
     * header information. Null values in the ResultSet will be formatted as
     * the empty string. */
    public String format(int row, int col) {
        Object o = data[row][col];
        if (o == null)
            return "";
        else if (o instanceof SimpleData)
            return ((SimpleData) o).format();
        else
            return o.toString();
    }

    /** Perform a query and return a result set. */
    public static ResultSet get(DataRepository data, String[] conditions,
                                String orderBy, String[] dataNames,
                                String basePrefix, PSPProperties props) {

        // Construct a regular expression for searching the repository.
        StringBuffer re =  new StringBuffer("~.*/");
        if (conditions != null)
            for (int c=0;  c < conditions.length;  c++)
                re.append("{").append(conditions[c]).append("}");
        if (orderBy == null) orderBy = dataNames[0];
        re.append(orderBy);

        // Find data elements that match the regular expression.
        if (basePrefix == null) basePrefix = "";
        SortedList list = SortedList.getInstance
            (data, re.toString(), basePrefix, FAKE_DATA_NAME, props);
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

                } else {
                    dataName = prefix + "/" + dataNames[d];
                    result.setData(p+1, d+1, data.getSimpleValue(dataName));
                }
        }
        return result;
    }
    private static final String FAKE_DATA_NAME =
        DataRepository.anonymousPrefix + "/Data Enumerator";


    /** Perform a query and return a result set.
     *  the queryParameters Map contains the instructions for performing
     * the query */
    public static ResultSet get(DataRepository data, Map queryParameters,
                                String prefix, PSPProperties props) {
        // orderBy dataElement name is stored in the "order" parameter
        String orderBy = (String) queryParameters.get("order");

        // conditions are given via "where" parameter(s)
        String [] conditions = (String[]) queryParameters.get("where_ALL");
        if (conditions == null) {
            String cond = (String) queryParameters.get("where");
            if (cond != null) {
                conditions = new String[1];
                conditions[0] = cond;
            }
        }

        // data names are given with parameters like "d1", "d2", "d3", etc.
        int i = 1;
        while (queryParameters.get("d" + i) != null) i++;
        String [] dataNames = new String[i-1];
        while (--i > 0) dataNames[i-1] = (String) queryParameters.get("d" + i);

        // fetch the results.
        ResultSet result =
            get(data, conditions, orderBy, dataNames, prefix, props);

        // parameters "h0", "h1", etc specify overridden column headers
        String colHeader;
        for (i=dataNames.length;  i >= 0;  i--) {
            colHeader = (String) queryParameters.get("h" + i);
            if (colHeader != null)
                result.setColName(i, colHeader);
        }

        return result;
    }

    /** Make certain that we have at least one row of data, even if it is
     * bogus.
     * This is necessary because the charting library doesn't like drawing
     * charts with no data in them.
     */
    private void ensureOneRow() {
        if (numRows() > 0) return;

        Object[][] newData = new Object[2][numCols()+1];
        for (int i=numCols();  i>=0; i--) {
            newData[0][i] = data[0][i]; // copy column headers.
            newData[1][i] = null; // make all data elements in row 1 null.
        }
        data = newData;
        setRowName(1, "No data to display");
    }

    private static String fixupName(String name) {
        return "anonymousChart" + name.hashCode();
    }



    private class Category {
        int rowNum;
        public Category(int rowNum) { this.rowNum = rowNum; }
        public String toString() { return asString(data[rowNum][0]); }
    }

    ////////////////////////////////////////////////////////////////////////
    // CategoryDataSource
    ////////////////////////////////////////////////////////////////////////

    protected class RSCategoryDataSource implements CategoryDataSource {
        /** Returns the number of series in the data source. */
        public int getSeriesCount() { return numCols(); }
        /** Returns the name of the specified series (zero-based). */
        public String getSeriesName(int seriesIndex) {
            return getColName(seriesIndex+1); }
        /* The following methods are not applicable for us */
        public void addChangeListener(DataSourceChangeListener listener) {}
        public void removeChangeListener(DataSourceChangeListener listener) {}

        /** Returns the value for the specified series (zero-based index) and
         * category. */
        public Number getValue(int seriesIndex, Object category) {
            return getNumber(((Category) category).rowNum, seriesIndex+1);
        }
        /** Returns a list of the categories in the data source */
        public List getCategories() {
            ArrayList result = new ArrayList();
            for (int row=1;  row <= numRows();  row++)
                result.add(new Category(row));
            return result;
        }
        /** Returns the number of categories in the data source. */
        public int getCategoryCount() { return numRows(); }
    }

    public CategoryDataSource catDataSource() {
        ensureOneRow();
        return new RSCategoryDataSource();
    }

    protected class RSXYDataSource implements XYDataSource {
        /** Returns the number of series in the data source. */
        public int getSeriesCount() { return numCols() - 1; }
        /** Returns the name of the specified series (zero-based). */
        public String getSeriesName(int seriesIndex) {
            return getColName(seriesIndex+2); }
        /* The following methods are not applicable for us */
        public void addChangeListener(DataSourceChangeListener listener) {}
        public void removeChangeListener(DataSourceChangeListener listener) {}

        /** Returns the x-value for the specified series and item */
        public Number getXValue(int seriesIndex, int itemIndex) {
            return getNumber(itemIndex+1, 1); }
        /** Returns the y-value for the specified series and item */
        public Number getYValue(int seriesIndex, int itemIndex) {
            if (itemIndex == -1)
                return (numRows() > 0 && numCols() > 0 &&
                        (getData(1,1) instanceof DateData)) ? null : ZERO;
            return getNumber(itemIndex+1, seriesIndex+2); }
        /** Returns the number of items in the specified series */
        public int getItemCount(int seriesIndex) { return numRows(); }
    }
    private static Double ZERO = new Double(0.0);

    public XYDataSource xyDataSource() {
        ensureOneRow();
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
