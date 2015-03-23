// Copyright (C) 2000-2013 Tuma Solutions, LLC
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

/*
 * This class provides a property object with the the attributes necessary
 * for the ProcessDashboard.  The child array uses zero-based indexing.
 */

package net.sourceforge.processdash.hier;

import net.sourceforge.processdash.util.*;

public class Prop
{
    // attributes
    protected String defectLog  = null;
    protected String myID       = null;
    protected String scriptFile = null;
    protected String dataFile   = null;
    protected String extraData  = null;
    protected String status     = null;
    protected int selectedChild = 0;
    protected PropertyKey children[] = null;

    protected static final char ESC_CHAR = '\\';
    public static final String TEMPLATE_QUALIFIER = ":!*!:";

    //constructors
    public Prop () {
        super();
    }

    public Prop (Prop p) {
        defectLog  = p.defectLog;
        myID       = p.myID;
        scriptFile = p.scriptFile;
        dataFile   = p.dataFile;
        status     = p.status;
        selectedChild = p.selectedChild;
        if (p.children == null)
            children = null;
        else {
            children = new PropertyKey [p.children.length];
            for (int i = 0; i < p.children.length; i++)
                children [i] = new PropertyKey (p.children [i]);
        }
    }

    // methods
    public void setDefectLog  (String log) { defectLog     = log; }
    public void setID         (String id)  { myID          = id;  }
    public void setScriptFile (String log) { scriptFile    = log; }
    public void setDataFile   (String log) { dataFile      = log; }
    public void setExtraData  (String d)   { extraData     = d;   }
    public void setStatus     (String log) { status        = log; }
    public void setSelectedChild (int num) { selectedChild = num; }

    public void setChild (PropertyKey childKey, int which) {
        if ((which < children.length) && (which >= 0))
            children [which] = childKey;
        else
            addChild (childKey, which);
    }

    public void addChild (PropertyKey childKey, int where) {
                                  // error handling
        if (childKey == null) return;
        int newLength = (children == null) ? 1 : children.length + 1;
        if ((where < 0) || (where >= newLength))
            where = newLength - 1;
                                      // add the child
        PropertyKey temp[] = new PropertyKey [newLength];
        for (int ii = 0; ii < where; ii++)
            temp [ii] = children [ii];
        temp [where] = childKey;
        for (int ii = where + 1; ii < temp.length; ii++)
            temp [ii] = children [ii - 1];
        children = temp;
        if (selectedChild >= where)
            selectedChild++;
        if (selectedChild >= temp.length)
            selectedChild = temp.length - 1;
    }

    public void removeChild (int which) {
                                  // error handling
        if (children == null)
            return;
        if ((which < 0) || (which >= children.length))
            return;
        int newLength = children.length - 1;
                                    // add the child
        PropertyKey temp[] = new PropertyKey [newLength];
        for (int ii = 0; ii < which; ii++)
            temp [ii] = children [ii];
        for (int ii = which; ii < temp.length; ii++)
            temp [ii] = children [ii + 1];
        children = temp;
        if (selectedChild > which)
            selectedChild--;
        else if (selectedChild == which)
            selectedChild = 0;
    }

    public void moveChildUp(int which) {
                                  // error handling
        if (children == null)
            return;
        if ((which < 1) || (which >= children.length))
            return;

        PropertyKey childA = children[which-1];
        PropertyKey childB = children[which];
        children[which-1] = childB;
        children[which]   = childA;

        if (selectedChild == which)
            selectedChild--;
        else if (selectedChild == (which-1))
            selectedChild++;
    }


    public String getDefectLog ()  { return defectLog; }
    public String getID ()         { return myID; }
    public String getScriptFile () { return scriptFile; }
    public String getDataFile ()   { return dataFile; }
    public String getExtraData()   { return extraData; }
    public String getStatus ()     { return status; }
    public int getSelectedChild () { return selectedChild; }

    public int getNumChildren () {
        return (children == null) ? 0 : children.length;
    }

    public PropertyKey getChild (int which) {
        return (children == null) ?
            null : ((which >= children.length) ? null : children [which]);
    }

    public int getChildPos(PropertyKey child) {
        if (child != null && children != null) {
            for (int i = children.length; i-- > 0;)
                if (child.equals(children[i]))
                    return i;
        }
        return -1;
    }

    private String stringRep (String s) {
        return ((s == null) ? "" : EscapeString.applyEscape (s, ESC_CHAR, "]"));
    }

    private String stringRep (PropertyKey c[]) {
        String s = "";
        for (int ii = 0; ii < c.length; ii++)
            s = s + "[" + stringRep (c [ii].toString()) + "]";
        return s;
    }

    public boolean isUniqueChildName (String s) {
        if (children == null)
            return true;
        for (int ii = 0; ii < children.length; ii++)
            if (children [ii].name().equals (s))
                return false;
        return true;
    }

    public String uniqueChildName (String baseName) {
        baseName = unqualifiedName(baseName);
        String aName = baseName;
        int index = 1;
        while ( !isUniqueChildName (aName))
            aName = baseName + (index++);
        return aName;
    }

    public static String unqualifiedName(String name) {
        if (name == null) return null;
        int pos = name.indexOf(TEMPLATE_QUALIFIER);
        if (pos == -1) return name;
        return name.substring(pos + TEMPLATE_QUALIFIER.length());
    }

    public String toString () {
        return
            ("Prop[" +
             stringRep (defectLog)     + "][" + stringRep (myID)  + "][" +
             stringRep (scriptFile)    + "][" + stringRep (dataFile) + "][" +
             stringRep (status) + "][" +
             stringRep (String.valueOf (selectedChild)) + "][" +
             ((children == null) ? 0 + "]" :
                children.length + "]" + stringRep (children)));
    }

    static public Prop valueOf (String s) {
        Prop val = new Prop ();
        int startPosition;
        int endPosition = 0;

        try {
            startPosition = s.indexOf ('[', endPosition);
            endPosition = EscapeString.indexOf (s, ']', startPosition, ESC_CHAR);
            val.setDefectLog (s.substring (startPosition + 1, endPosition));

            startPosition = s.indexOf ('[', endPosition);
            endPosition = EscapeString.indexOf (s, ']', startPosition, ESC_CHAR);
            val.setID (s.substring (startPosition + 1, endPosition));

            startPosition = s.indexOf ('[', endPosition);
            endPosition = EscapeString.indexOf (s, ']', startPosition, ESC_CHAR);
            val.setScriptFile (s.substring (startPosition + 1, endPosition));

            startPosition = s.indexOf ('[', endPosition);
            endPosition = EscapeString.indexOf (s, ']', startPosition, ESC_CHAR);
            val.setDataFile (s.substring (startPosition + 1, endPosition));

            startPosition = s.indexOf ('[', endPosition);
            endPosition = EscapeString.indexOf (s, ']', startPosition, ESC_CHAR);
            val.setStatus (s.substring (startPosition + 1, endPosition));

            startPosition = s.indexOf ('[', endPosition);
            endPosition = EscapeString.indexOf (s, ']', startPosition, ESC_CHAR);
            int selKid = (new Integer (s.substring (startPosition + 1,
                                                    endPosition))).intValue();

            startPosition = s.indexOf ('[', endPosition);
            endPosition = EscapeString.indexOf (s, ']', startPosition, ESC_CHAR);
            int numKids = (new Integer (s.substring (startPosition + 1,
                                                     endPosition))).intValue();

            for (int ii = 0; ii < numKids; ii++) {
                startPosition = s.indexOf ('[', endPosition);
                endPosition = EscapeString.indexOf (s, ']', startPosition, ESC_CHAR);
                val.addChild (PropertyKey.valueOf (s.substring (startPosition + 1,
                                                                endPosition)),
                              ii);
            }

            val.setSelectedChild (selKid);
        } catch (Exception e) {
            System.out.println("Prop.valueOf error:"+s);
        }
        return val;
    }

    public static boolean hasValue(String val) {
        return (val != null && val.length() != 0);
    }
}
