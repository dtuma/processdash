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
                      DataRepository data, String prefix, String defaultVal) {
        this.name = name;
        this.data = data;
        this.prefix = prefix;
        this.util = new FrozenUtil();

        if (thawedVal == null) {
            value = null;
            util.formerEditable = true;
            util.setFormer("null", defaultVal);
        } else {
            value = thawedVal.getEditable(false).getSimpleValue();
            util.formerEditable = thawedVal.isEditable();
            util.setFormer(thawedVal.saveString(), defaultVal);
        }
        if (value == null)
            util.currentSaveString = "null";
        else {
            util.currentSaveString = value.saveString();
            value.setEditable(false);
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
