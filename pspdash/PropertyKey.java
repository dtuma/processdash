// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package pspdash;

public class PropertyKey implements Comparable {

    private static final String ROOT_NAME = "top";

    public static final PropertyKey ROOT =
        new PropertyKey ("", ROOT_NAME);

    private static String SEPARATOR = "/";
    private String myParent = null;
    private String myName   = null;
    private Integer myHashCode = null;

    public boolean equals (Object obj) {
        if (this == obj)
            // simple optimization - check if the objects are the same object
            return true;

        else if (obj instanceof PropertyKey) {
            PropertyKey that = (PropertyKey) obj;

            // quick optimization using cached hash codes
            if (this.myHashCode != null &&
                that.myHashCode != null &&
                !this.myHashCode.equals(that.myHashCode))
                return false;

            return this.myParent.equals(that.myParent) &&
                this.myName.equals(that.myName);

        } else
            return false;
    }

    public int hashCode() {
        if (myHashCode == null)     // cache value to avoid recomputation
            myHashCode = new Integer(key().hashCode());
        return myHashCode.intValue();
    }

    public PropertyKey (PropertyKey parent, String name) {
        myParent = (parent == null) ? "" : parent.key();
        myName   = (name == null) ? "" : name;
    }

                                // essentially a clone() method
    public PropertyKey (PropertyKey key) {
        myParent = key.myParent;
        myName   = key.myName;
    }

    private PropertyKey (String parent, String name) {
        myParent = (parent == null) ? "" : parent;
        myName   = (name == null) ? "" : name;
    }

    public PropertyKey getParent () {
        if (myParent.length() == 0)
            return null;
        int sep = myParent.lastIndexOf (SEPARATOR);
        String p, n;
        if (sep < 0) {
            p = "";
            n = myParent;
        } else {
            p = myParent.substring (0, sep);
            n = myParent.substring (sep + 1);
        }
        return new PropertyKey (p, n);
    }

    public String key () {
        return ((myParent.equals("")) ? "" : myParent + SEPARATOR) + myName;
    }

    public boolean isChildOf (PropertyKey k2) {
        boolean rv = false;
        String s = k2.key();
        if (myParent.equals (s) || myParent.startsWith (s + SEPARATOR))
            rv = true;
        return rv;
    }

    public static PropertyKey fromKey (String s) {
        int sep       = s.lastIndexOf (SEPARATOR);
        String parent = ((sep < 0) ? "" : s.substring (0, sep));
        String name   = s.substring (sep + 1, s.length());
        return new PropertyKey(parent, name);
    }

    public static PropertyKey fromPath (String s) {
        return fromKey (ROOT_NAME + s);
    }

    public String path () {
        try {                       // start 1 char past ROOT (start w/SEPARATOR)
            return key().substring(ROOT_NAME.length());
        } catch (Exception e) {
            return SEPARATOR;
        }
    }

    public String name () { return myName; }

    public String toString () {
        return "PropertyKey<"+myParent+"><"+myName+">";
    }

    static public PropertyKey valueOf(String s) {
        int startPosition;
        int endPosition = 0;
        String parent = null;
        String name;
                                    // error checking
        if (s == null || !s.startsWith("PropertyKey<")) return null;

        startPosition = s.indexOf ('<', endPosition);
        endPosition = s.indexOf ('>', startPosition);
        if (startPosition == -1 || endPosition == -1) return null;
        if (startPosition < endPosition)
            parent = s.substring (startPosition + 1, endPosition);

        startPosition = s.indexOf ('<', endPosition);
        endPosition = s.indexOf ('>', startPosition);
        if (startPosition == -1 || endPosition == -1) return null;
        name = s.substring (startPosition + 1, endPosition);

        return new PropertyKey (parent, name);
    }

    public int compareTo(Object o) {
        PropertyKey that = (PropertyKey) o;
        return this.key().compareTo(that.key());
    }


}
