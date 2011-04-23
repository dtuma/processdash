// Copyright (C) 2009-2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.probe;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataNameFilter;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.util.StringUtils;

public class SizePerItemTable {

    public enum RelativeSize {
        VS, S, M, L, VL
    };

    public static class ParseException extends Exception {
        private String resKey;
        private Object[] args;

        private ParseException(String resKey, Object... args) {
            this.resKey = resKey;
            this.args = args;
        }
        public String getResKey() {
            return resKey;
        }
        public Object[] getArgs() {
            return args;
        }
    }

    public static final String LEGACY_DEFAULT_TYPE_NAME =
        "Default C++ LOC/Method";

    private String tableName;

    private String sizeUnits;

    private List<String> categoryNames;

    private double[][] sizeData;


    public String getTableName() {
        return tableName;
    }

    public String getSizeUnits() {
        return sizeUnits;
    }

    public List<String> getCategoryNames() {
        return categoryNames;
    }

    public Double getSize(String categoryName, RelativeSize relSize) {
        int catPos = categoryNames.indexOf(categoryName);
        if (catPos == -1)
            return null;
        else
            return sizeData[catPos][relSize.ordinal()];
    }

    public SizePerItemTable(String tableName, String spec)
            throws ParseException {
        this(tableName, null, spec);
    }

    public SizePerItemTable(String tableName, String sizeUnits, String spec)
            throws ParseException {
        this.tableName = tableName;
        if (!StringUtils.hasValue(tableName))
            throw new ParseException("Table_Name_Missing");

        this.sizeUnits = (sizeUnits == null ? null : sizeUnits.trim());
        if (!StringUtils.hasValue(this.sizeUnits))
            this.sizeUnits = ProcessUtil.DEFAULT_SIZE_UNITS;

        this.categoryNames = new ArrayList<String>();
        String[] catSpecs = spec.replace('|', '\n').trim().split("[\r\n]+");
        sizeData = new double[catSpecs.length][5];
        for (String oneCatSpec : catSpecs) {
            oneCatSpec = oneCatSpec.trim();
            if (oneCatSpec.length() == 0)
                continue;  // skip empty lines

            int parenPos = oneCatSpec.lastIndexOf('(');
            if (parenPos == -1)
                throw new ParseException("Size_List_Missing_FMT", oneCatSpec);
            String oneCatName = oneCatSpec.substring(0, parenPos).trim();

            String numSpec = oneCatSpec.substring(parenPos + 1);
            numSpec = numSpec.replace(')', ' ').trim();
            String[] numbers = numSpec.split("[, \t]+");
            if (numbers.length != 5)
                throw new ParseException("Size_List_Wrong_Length_FMT",
                        oneCatName);

            int row = categoryNames.size();
            categoryNames.add(oneCatName);
            for (int i = 0; i < 5; i++) {
                try {
                    sizeData[row][i] = Double.parseDouble(numbers[i]);
                } catch (Exception e) {
                    throw new ParseException("Bad_Number_FMT", oneCatName,
                            numbers[i]);
                }
            }
        }
    }

    public String formatForDisplay() {
        return format("\n");
    }

    public void save(DataContext data) {
        String dataName = DATA_PREFIX + tableName;
        String unitsName = dataName + UNITS_SUFFIX;
        if (categoryNames.isEmpty()) {
            data.putValue(dataName, null);
            data.putValue(unitsName, null);
            CACHE.remove(tableName);
        } else {
            data.putValue(dataName, StringData.create(format("|")));
            data.putValue(unitsName, StringData.create(sizeUnits));
            CACHE.put(tableName, this);
        }
    }

    private String format(String delim) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < categoryNames.size(); i++) {
            result.append(categoryNames.get(i)).append(" (");
            for (int j = 0; j < 5; j++) {
                result.append(Double.toString(sizeData[i][j]));
                if (j < 4)
                    result.append(", ");
            }
            result.append(")").append(delim);
        }
        return result.toString();
    }

    public static SizePerItemTable getByName(String tableName, DataContext data) {
        if (tableName == null)
            return null;

        SizePerItemTable result = CACHE.get(tableName);
        if (result == null) {
            try {
                String spec = getStrVal(data, DATA_PREFIX + tableName);
                String units = getStrVal(data, DATA_PREFIX + tableName
                        + UNITS_SUFFIX);
                result = new SizePerItemTable(tableName, units, spec);
                CACHE.put(tableName, result);
            } catch (Exception e) {
            }
        }
        return result;
    }

    private static String getStrVal(DataContext data, String name) {
        SimpleData sd = data.getSimpleValue(name);
        return (sd == null ? null : sd.format());
    }

    public static SortedMap<String, SizePerItemTable> getDefinedTables(
            DataRepository data) {
        SortedMap<String, SizePerItemTable> result = new TreeMap<String, SizePerItemTable>();

        Iterator k = data.getKeys(null, NAME_FILTER);
        while (k.hasNext()) {
            String name = (String) k.next();
            if (name.startsWith(DATA_PREFIX) && !name.endsWith(UNITS_SUFFIX)) {
                String tableName = name.substring(DATA_PREFIX.length());
                SizePerItemTable table = getByName(tableName, data);
                if (table != null)
                    result.put(tableName, table);
            }
        }

        return result;
    }

    public static SortedMap<String, SizePerItemTable> getDefinedTables(
        DataRepository data, String sizeUnits) {
        if (sizeUnits != null)
            sizeUnits = sizeUnits.trim();
        if (!StringUtils.hasValue(sizeUnits))
            return new TreeMap();

        SortedMap<String, SizePerItemTable> result = getDefinedTables(data);
        for (Iterator i = result.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            SizePerItemTable oneTable = (SizePerItemTable) e.getValue();
            if (!oneTable.getSizeUnits().equalsIgnoreCase(sizeUnits))
                i.remove();
        }
        return result;
    }


    private static class NameFilter implements DataNameFilter.PrefixLocal {
        public boolean acceptPrefixLocalName(String prefix, String localName) {
            return (prefix.length() == 0 && localName.regionMatches(0,
                DATA_PREFIX, 1, DATA_PREFIX.length() - 1));
        }
    }

    private static final NameFilter NAME_FILTER = new NameFilter();


    /** Cache of previously created tables. */
    private static Map<String, SizePerItemTable> CACHE = new Hashtable();


    private static final String DATA_PREFIX = "/Size Per Item Table/";

    private static final String UNITS_SUFFIX = "/Size Units";

}
