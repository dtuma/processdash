// Copyright (C) 2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.api.PDashData;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.util.MockMap;

public class PDashDataImpl extends MockMap<String, Object> implements PDashData {

    private DataRepository data;

    private String prefix;


    public PDashDataImpl(DataRepository data, String prefix) {
        this.data = data;
        this.prefix = prefix;
    }

    public Double getNumber(String dataName) {
        SimpleData sd = getSimpleValue(dataName);
        if (sd == null) {
            return null;
        } else if (sd instanceof NumberData) {
            return ((NumberData) sd).getDouble();
        } else {
            return Double.NaN;
        }
    }

    public String getString(String dataName) {
        SimpleData sd = getSimpleValue(dataName);
        return (sd == null ? null : sd.format());
    }

    public List<String> getList(String dataName) {
        SimpleData sd = getSimpleValue(dataName);
        ListData ld = ListData.asListData(sd);
        if (ld == null)
            return null;

        List result = new ArrayList(ld.asList());
        for (int i = result.size(); i-- > 0;) {
            Object item = result.get(i);
            if (item instanceof SimpleData)
                result.set(i, convert((SimpleData) item));
        }
        return result;
    }

    public Date getDate(String dataName) {
        SimpleData sd = getSimpleValue(dataName);
        if (sd instanceof DateData) {
            return ((DateData) sd).getValue();
        } else {
            return null;
        }
    }

    public boolean getTest(String dataName) {
        SimpleData sd = getSimpleValue(dataName);
        return (sd != null && sd.test());
    }

    public PDashData getChild(String childName) {
        String childPrefix = DataRepository.createDataName(prefix, childName);
        return new PDashDataImpl(data, childPrefix);
    }

    /**
     * Implement the {@link Map#get(Object)} method. This will allow EL
     * expressions such as <tt>data["foo"]</tt>
     */
    public Object get(Object key) {
        // see if the user has requested one of the pseudo keys
        if ("number".equals(key))
            return new GetNumber();
        else if ("string".equals(key))
            return new GetString();
        else if ("list".equals(key))
            return new GetList();
        else if ("date".equals(key))
            return new GetDate();
        else if ("test".equals(key))
            return new GetTest();
        else if ("child".equals(key))
            return new GetChild();

        // Look up a data value, and cast it to a plain Java type
        SimpleData sd = getSimpleValue(key);
        return convert(sd);
    }

    private Object convert(SimpleData sd) {
        if (sd instanceof NumberData) {
            return ((NumberData) sd).getDouble();
        } else if (sd instanceof DateData) {
            return ((DateData) sd).getValue();
        } else if (sd instanceof TagData) {
            return Boolean.TRUE;
        } else if (sd != null) {
            return sd.format();
        } else {
            return null;
        }
    }

    private SimpleData getSimpleValue(Object key) {
        if (key == null) {
            return null;
        } else {
            String dataName = DataRepository.createDataName(prefix,
                key.toString());
            return data.getSimpleValue(dataName);
        }
    }

    public boolean containsKey(Object key) {
        if (key == null)
            return false;
        else if (PSEUDO_KEYS.contains(key))
            return true;

        String dataName = DataRepository.createDataName(prefix, key.toString());
        return data.getValue(dataName) != null;
    }

    private String stringKey(Object key) {
        return (key == null ? null : key.toString());
    }

    private class GetNumber extends MockMap<String, Object> {
        public Object get(Object key) {
            return getNumber(stringKey(key));
        }
    }

    private class GetString extends MockMap<String, Object> {
        public Object get(Object key) {
            return getString(stringKey(key));
        }
    }

    private class GetList extends MockMap<String, Object> {
        public Object get(Object key) {
            return getList(stringKey(key));
        }
    }

    private class GetDate extends MockMap<String, Object> {
        public Object get(Object key) {
            return getDate(stringKey(key));
        }
    }

    private class GetTest extends MockMap<String, Object> {
        public Object get(Object key) {
            return getTest(stringKey(key));
        }
    }

    private class GetChild extends MockMap<String, Object> {
        public Object get(Object key) {
            return getChild(stringKey(key));
        }
    }

    private static final Set<String> PSEUDO_KEYS = Collections
            .unmodifiableSet(new HashSet(Arrays.asList("number", "string",
                "list", "date", "test", "child")));

}
