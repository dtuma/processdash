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


public class TagData implements SimpleData {

    private TagData() {}

    private static final TagData INSTANCE = new TagData();
    public static TagData getInstance() { return INSTANCE; }

    public boolean isEditable()                { return false;        }
    public void setEditable(boolean e)         {                      }
    public SaveableData getEditable(boolean e) { return this;         }
    public boolean isDefined()                 { return true;         }
    public void setDefined(boolean d)          {                      }
    public String saveString()                 { return "TAG";        }
    public SimpleData getSimpleValue()         { return this;         }
    public void dispose()                      {                      }

    public String format()                     { return "TRUE";       }
    public SimpleData parse(String val)        { return this;         }
    public boolean equals(SimpleData val)      { return val == this;  }
    public boolean lessThan(SimpleData val)    { return false;        }
    public boolean greaterThan(SimpleData val) { return !equals(val); }
    public boolean test()                      { return true;         }

    public String toString()                   { return "TAG";        }
}
