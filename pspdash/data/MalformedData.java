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


public class MalformedData extends DoubleData {

    private static StringData SIMPLE_VALUE = null;
    static {
        try {
            SIMPLE_VALUE = new StringData("\"ERROR");
            SIMPLE_VALUE.setEditable(false);
        } catch (MalformedValueException mve) {}
    }

    String saveString;

    public MalformedData(String value) {
        super(Double.NaN, false);
        saveString = value;
    }
    public boolean isEditable()           { return false;        }
    public void setEditable(boolean e)    {                      }
    public boolean isDefined()            { return true;         }
    public void setDefined(boolean d)     {                      }
    public String saveString()            { return saveString;   }
    public SimpleData getSimpleValue()    { return SIMPLE_VALUE; }
    public void dispose()                 { saveString = null;   }
}
