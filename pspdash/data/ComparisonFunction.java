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


abstract class ComparisonFunction extends DoubleData implements NumberFunction
{

    String name = null;
    String saveValue = null;
    DataRepository data;

    private static final StringData nullString = StringData.create("");
    SimpleData[] val;
    SimpleList[] list;


    public ComparisonFunction(String name, String params, String saveAs,
                              DataRepository r, String prefix) {
        super(0.0);
        setEditable(false);
        data = r;
        saveValue = saveAs;

        ArgumentList args = new ArgumentList(params);

        val = new SimpleData[args.size()];
        list = new SimpleList[args.size()];
        int pos = 0;

        while (args.hasMoreElements())
            parseParam(name, args.nextElement(), prefix, pos++);

        this.name = name;
        recalc();
    }

    private void parseParam(String name, String parameter,
                            String prefix, int which) {
        try {
            val[which] = ValueFactory.createSimple(parameter);
        } catch (MalformedValueException e) {
            list[which] = new SimpleList(data, parameter, prefix, this, name);
        }
    }

    abstract boolean compare(SimpleData v1, SimpleData v2);

    public void recalc() {
        if (name == null) return;

        double oldValue = value;
        value = 1.0;

        int i;

        for (i=val.length; i-- > 0; ) {

                                // if any of the parameters is a SimpleList,
                                // fetch its value.
            if (list[i] != null) val[i] = list[i].value;

                                  // since empty strings are saved into the
                                  // repository as null values, treat null
                                  // values as empty strings.
            if (val[i] == null) val[i] = nullString;
        }

                                // step through the list and compare each
                                // element to the following element.
        for (i=val.length; --i > 0; )
            if (! compare(val[i-1], val[i])) {
                value = 0.0;
                break;
            }


        // only update our value in the repository if our boolean
        // value really has changed.
        if (value != oldValue) data.putValue(name, this);
    }

    public String saveString() { return saveValue; }

    public void dispose() {
        name = saveValue = null;
        for (int i = val.length; i-- > 0; ) {
            val[i] = null;
            if (list[i] != null) list[i].dispose();
            list[i] = null;
        }
        data = null;
    }

    public String name() { return name; }
}

