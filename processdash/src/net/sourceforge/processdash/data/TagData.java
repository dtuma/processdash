// Copyright (C) 2001-2003 Tuma Solutions, LLC
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

import java.io.ObjectStreamException;

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

    Object readResolve() throws ObjectStreamException { return INSTANCE; }
}
