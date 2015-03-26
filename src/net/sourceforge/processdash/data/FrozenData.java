// Copyright (C) 2001-2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data;

import net.sourceforge.processdash.data.repository.DataRepository;


public class FrozenData implements SaveableData {

    public static final SaveableData DEFAULT = new DoubleData(0);

    SimpleData value;
    FrozenUtil util;
    String name, prefix;
    DataRepository data;

    SaveableData o = null;

    /** This constructor is called by ValueFactory when opening a datafile
     * that contains a frozen element.
     */
    public FrozenData(String name, String s, DataRepository data,
                      String prefix) throws MalformedValueException
    {
        this.name = name;
        this.data = data;
        this.prefix = prefix;
        this.util = new FrozenUtil(s);
        try {
            value = (SimpleData) ValueFactory.createQuickly
                (name, util.currentSaveString, data, prefix);
        } catch (MalformedValueException mve) {
            value = new MalformedData(util.currentSaveString);
        }
        if (value != null) value.setEditable(false);
    }

    /** This constructor is called by the DataFreezer to freeze a live
     * data element.
     */
    public FrozenData(String name, SaveableData thawedVal,
                      DataRepository data, String prefix, boolean isDefault) {
        this.name = name;
        this.data = data;
        this.prefix = prefix;
        this.util = new FrozenUtil();

        if (thawedVal == null) {
            value = null;
            util.formerEditable = true;
            util.setFormer("null", isDefault);
        } else {
            value = thawedVal.getSimpleValue();
            util.formerEditable = thawedVal.isEditable();
            util.setFormer(thawedVal.saveString(), isDefault);
        }
        if (value == null)
            util.currentSaveString = "null";
        else {
            util.currentSaveString = value.saveString();
            value = (SimpleData) value.getEditable(false);
        }
    }

    /** This method is called by the DataFreezer to thaw a frozen data
     * element.
     */
    public SaveableData thaw() {
        if (o == null) {
            String value = util.getFormer();
            if (value == null)
                o = DEFAULT;
            else try {
                o = ValueFactory.createQuickly(name, value, data, prefix);
                if (o != null) o.setEditable(util.formerEditable);
            } catch (MalformedValueException mve) {
                o = new MalformedData(value);
            }
        }

        return o;
    }

    public String getPrefix() { return prefix; }

    public boolean isEditable() { return false; }
    public void setEditable(boolean e) {}
    public SaveableData getEditable(boolean editable) { return this; }
    public boolean isDefined() {
        return (value == null || value.isDefined());
    }
    public void setDefined(boolean d) {}
    public String saveString()  { return util.buildSaveString(); }
    public SimpleData getSimpleValue() { return value; }

    public void dispose() {
        value = null;
        name = prefix = null;
        util = null;
        data = null;
        o = null;
    }
}
