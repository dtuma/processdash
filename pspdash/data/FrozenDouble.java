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


public class FrozenDouble extends DoubleData implements FrozenData {

    FrozenUtil util;
    String name, prefix;
    DataRepository data;

    SaveableData o = null;

    public FrozenDouble(String name, String s, DataRepository data,
                        String prefix) throws MalformedValueException
    {
        this(name, new FrozenUtil(s), data, prefix);
    }

    private FrozenDouble(String name, FrozenUtil util, DataRepository data,
                         String prefix) throws MalformedValueException {
        super(util.currentSaveString);
        setEditable(false);

        this.name = name;
        this.util = util;
        this.data = data;
        this.prefix = prefix;
    }


    public FrozenDouble(String name, DoubleData value,
                        DataRepository data, String prefix) {
        super(value.value, false);
        defined = value.defined;

        this.name = name;
        this.util = new FrozenUtil();
        this.data = data;
        this.prefix = prefix;

        if (value instanceof FrozenDouble) {
            // Don't freeze things twice!
            util.formerEditable  = ((FrozenDouble)value).util.formerEditable;
            util.formerSaveString= ((FrozenDouble)value).util.formerSaveString;
        } else {
            util.formerEditable   = value.editable;
            util.formerSaveString = value.saveString();
        }
        util.currentSaveString = super.saveString();
    }

    public SaveableData thaw() throws MalformedValueException {
        if (o == null) {
            System.out.println("thawing " + name);
            o = ValueFactory.create(name, util.formerSaveString, data, prefix);
            o.setEditable(util.formerEditable);
        }

        return o;
    }

    public boolean isEditable() { return false; }
    public String saveString()  { return util.buildSaveString(); }

    public void dispose() {
        name = prefix = null;
        util = null;
        data = null;
        o = null;
    }
}
