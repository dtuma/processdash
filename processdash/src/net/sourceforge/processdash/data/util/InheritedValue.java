// Copyright (C) 2008 Tuma Solutions, LLC
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

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;

public class InheritedValue {

    protected String prefix;

    protected String dataName;

    protected SaveableData value;

    private InheritedValue(String prefix, String dataName, SaveableData value) {
        this.prefix = prefix;
        this.dataName = dataName;
        this.value = value;
    }

    /**
     * Return the prefix where the value was finally found.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Return the data name that we were searching for.
     */
    public String getDataName() {
        return dataName;
    }

    /**
     * Return the full data name of the value that was finally found.
     */
    public String getFullDataName() {
        return DataRepository.createDataName(prefix, dataName);
    }

    /**
     * Return the value that was found. If no inherited value was found, this
     * will return null.
     */
    public SaveableData getValue() {
        return value;
    }

    /**
     * Return the value that was found, as a simple value. If no inherited value
     * was found, this will return null.
     */
    public SimpleData getSimpleValue() {
        return (value == null ? null : value.getSimpleValue());
    }

    /**
     * Look for a hierarchically inherited value in the given DataContext.
     * 
     * This method first constructs a data name by appending the given prefix
     * and name. Then it checks to see if the data context has a non-null value
     * for that name. If so, an InheritableValue object is returned containing
     * the value found. Otherwise, this method chops the final name segment off
     * of the prefix and tries again. It walks up the path hierarchy in this
     * manner and returns the first match found. Even if no match can be found,
     * an InheritableValue object is still returned; its getValue method will
     * return null.
     * 
     * Note that the search algorithm above is looking for non-null
     * {@link SaveableValue} objects. If the value found is a calculation, that
     * calculation might still evaluate to null.
     * 
     * @param data
     *                the data context
     * @param prefix
     *                the prefix to start the search
     * @param name
     *                the name of a data element to look up
     * @return return an {@link InheritedValue} object capturing the inherited
     *         value that was or was not found
     */
    public static InheritedValue get(DataContext data, String prefix,
            String name) {
        String dataName = prefix + "/" + name;
        SaveableData result = data.getValue(dataName);
        int pos;
        while (result == null && prefix.length() > 0) {
            pos = prefix.lastIndexOf('/');
            if (pos == -1)
                prefix = "";
            else
                prefix = prefix.substring(0, pos);
            dataName = prefix + "/" + name;
            result = data.getValue(dataName);
        }
        return new InheritedValue(prefix, name, result);
    }
}
