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

package pspdash;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.ItemSelectable;
import javax.swing.event.EventListenerList;

public class PSPProperties extends Hashtable implements ItemSelectable {

    protected EventListenerList ell = new EventListenerList();
    protected int nextDataFileNumber  = 0;
    protected int nextDefectLogNumber = 0;
    public    String dataPath = null;


    private void debug(String msg) {
        System.out.println(msg);
    }

    public PSPProperties (String baseDataPath) {
        super();
        dataPath = baseDataPath;
    }


    public Object[] getSelectedObjects() {
        return null;
    }

    public void addItemListener(ItemListener l) {
        ell.add(ItemListener.class, l);
    }

    public void removeItemListener(ItemListener l) {
        ell.remove(ItemListener.class, l);
    }

    // Notify all listeners that have registered interest for
    // notification on this event type.  The event instance
    // is created using the parameters passed into the fire method.
    // For the moment, the only events that will generate these events are:
    // setChildKey/move(CHANGE), copyFrom(CREATE), removeChildKey/remove(DELETE)
    protected void fireDataFileChange(PendingDataChange item) {
        // Guaranteed to return a non-null array
        Object[] listeners = ell.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        ItemEvent fooEvent = new ItemEvent(this, 0, item, 0);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ItemListener.class) {
                ((ItemListener)listeners[i+1]).itemStateChanged(fooEvent);
            }
        }
    }

    public void copy (PSPProperties src) {
        PropertyKey key;
        Prop        value;
        if (src == null)
            return;
        clear();
        Enumeration keys = src.keys();
        while (keys.hasMoreElements()) {
            key = (PropertyKey)keys.nextElement();
            value = (Prop)src.get (key);
            put (new PropertyKey (key), new Prop (value));
        }
    }

    public Prop pget (PropertyKey key) {
        Prop val;
        if (key == null)
            val = new Prop ();
        else {
            val = (Prop) get (key);
            if (val == null)
                val = new Prop ();
        }
        return val;
    }

    public Prop premove (PropertyKey key) {
        Prop val = (Prop) super.remove (key);
        if (val == null)
            val = new Prop ();
        return val;
    }

    public int getNumChildren (PropertyKey key) {
        return pget (key).getNumChildren();
    }

    public int getSelectedChild (PropertyKey key) {
        return pget (key).getSelectedChild();
    }

    public void setSelectedChild (PropertyKey key, int childIndex) {
        Prop val = pget (key);
        val.setSelectedChild (childIndex);
        put (key, val);
    }

    public String getChildName (PropertyKey parent, int childIndex) {
        return pget (parent).getChild (childIndex).name();
    }

    public String getName (PropertyKey key) {
        return key.name();
    }

    public PropertyKey getChildKey (PropertyKey parent, int childIndex) {
        return pget (parent).getChild (childIndex);
    }

    // This routine will replace the given child (or add/append it if not there)
    public void setChildKey (PropertyKey parent,
                             String childName,
                             int childIndex) {
        Prop val = pget (parent);
        PropertyKey child = new PropertyKey (parent, childName);
        PropertyKey oldChild = val.getChild (childIndex);
        val.setChild (child, childIndex);
        if (oldChild == null)
            put (child, pget (child));
        else
            move (oldChild, child);
        put (parent, val);
    }

    // This routine will add the given child
    public void addChildKey (PropertyKey parent,
                             String childName,
                             int childIndex) {
        Prop val = pget (parent);
        PropertyKey child = new PropertyKey (parent, childName);
        val.addChild (child, childIndex);
        put (child, pget (child));
        put (parent, val);
    }

    // This routine will remove the given child
    public void removeChildKey (PropertyKey parent,
                                int childIndex) {
        Prop val = pget (parent);
        PropertyKey oldChild = val.getChild (childIndex);
        val.removeChild (childIndex);
        if (oldChild != null)
            remove (oldChild);
        put (parent, val);
    }

    private PropertyKey findExistingKey (PropertyKey key, String s) {
        PropertyKey k;
        if (key.path().equals(s))
            return key;
        for (int i = 0; i < getNumChildren (key); i++)
            if ((k = findExistingKey (getChildKey (key, i), s)) != null)
                return k;
        return null;
    }

    public PropertyKey findExistingKey (String s) {
        return findExistingKey (PropertyKey.ROOT, s);
    }

    /* returns true IIF the property has a datafile. */
    private boolean hasDataFile(Prop p) {
        String datafile = p.getDataFile();
        return (datafile != null && datafile.length() > 0);
    }

    /* returns true IIF this key or any of its parents have a datafile. */
    private boolean inheritsDataFile(PropertyKey k) {
        return ((k != null) &&
                (hasDataFile(pget(k)) || inheritsDataFile(k.getParent())));
    }

    /**
     * responsibleForData
     * This procedure tracks a fairly complicated concept.  When the
     * names of nodes and templates change, we want to rename the data by
     * sending a fireDataFileChange event.  We only want to do this if:
     *   1. the node/template has its own datafile, and needs to rename all
     *      the data that lives there, or
     *   2. there is *no* specific datafile in the node's ancestral hierarchy.
     *      data for such nodes goes into the global datafile, and must be
     *      renamed there.
     */
    private boolean responsibleForData(PropertyKey key) {
        return (hasDataFile(pget(key)) || !inheritsDataFile(key));
    }


    public void move (PropertyKey fromKey,
                      PropertyKey toKey) {
        Prop aProp = pget (fromKey);
        if (aProp != null) {
            for (int ii = 0; ii < aProp.getNumChildren(); ii++) {
                PropertyKey fc = aProp.getChild(ii);
                PropertyKey tc = new PropertyKey (toKey, fc.name());
                aProp.setChild (tc, ii);
                move (fc, tc);
            }
            if (responsibleForData(fromKey))
                fireDataFileChange(new PendingDataChange(toKey.path(),
                                                         fromKey.path()));
            super.remove(fromKey);
            put (toKey, aProp);
        }
    }

    public void remove (PropertyKey key) {
        Prop val = pget(key);
        if (val != null) {
            if (responsibleForData(key))
                fireDataFileChange(new PendingDataChange(key.path()));

            int numChildren = val.getNumChildren ();
            for (int idx = val.getNumChildren() - 1; idx >= 0; idx--) {
                remove (val.getChild (idx));
            }

            super.remove(key);
        }
    }

    public void copy (PropertyKey fromKey,
                      PropertyKey toKey) {
        Prop aProp = pget (fromKey);
        for (int ii = 0; ii < aProp.getNumChildren(); ii++) {
            PropertyKey fc = aProp.getChild(ii);
            PropertyKey tc = new PropertyKey (toKey, fc.name());
                aProp.setChild (tc, ii);
            copy (fc, tc);
        }
        put (toKey, aProp);
    }

    public Vector load (InputStream propStream) throws IOException {

        String line;
        Vector v = null;
        PropertyKey key;
        Prop val;
        int equalsPosition;
        String dataFile;
        BufferedReader in = new BufferedReader(new InputStreamReader(propStream));

        while ((line = in.readLine()) != null) {
            if ((! line.startsWith ("#")) &&
                ((equalsPosition = line.indexOf('=', 0)) != -1)) {
                key = PropertyKey.valueOf (line.substring(0, equalsPosition));
                val = Prop.valueOf (line.substring(equalsPosition+1));
                put (key, val);
                dataFile = val.getDataFile();
                if (dataFile != null && dataFile.length() > 0) {
                    if (v == null)
                        v = new Vector();
                    v.addElement (new String[] {key.path(), dataFile});
                }
            }
        }
        in.close();
        return v;
    }

    public Vector load (String datafilePath) throws IOException {
        return load(new FileInputStream(datafilePath));
    }


    // The save operation writes the property keys to the state file.
    // Data is first written to a temporary file, and later renamed to
    // final output file.
    public void save (String datafilePath,
                      String comment) throws IOException {
        PropertyKey key;
        Prop value;
        File propsFile = new File(datafilePath);
        String fileSep = System.getProperty("file.separator");

        try {
            File parentDir = new File(propsFile.getParent());
            if (!parentDir.isDirectory())
                parentDir.mkdirs();
        } catch (Exception e) {
            throw new IOException();
        }

        // Create temporary files
        File tempFile = new File(propsFile.getParent() + fileSep + "t_state");
        File backupFile = new File(propsFile.getParent() + fileSep + "tstate");

        BufferedWriter out = new BufferedWriter (new FileWriter (tempFile));
        BufferedWriter backup = new BufferedWriter (new FileWriter (backupFile));

        out.write ("# PSPProperties file:" + comment);
        out.newLine();
        out.write ("# format: PropertyKey<parentKey><name>=Prop[defectLog] \\");
        out.newLine();
        out.write ("#         [timeLog][scriptFile][dataFile][status] \\");
        out.newLine();
        out.write ("#         [selected][numChildren(N)][child0]...[childN-1]");
        out.newLine();
        out.write ("#");
        out.newLine();
        backup.write ("# PSPProperties file:" + comment);
        backup.newLine();
        backup.write ("# format: PropertyKey<parentKey><name>=Prop[defectLog] \\");
        backup.newLine();
        backup.write ("#         [timeLog][scriptFile][dataFile][status] \\");
        backup.newLine();
        backup.write ("#         [selected][numChildren(N)][child0]...[childN-1]");
        backup.newLine();
        backup.write ("#");
        backup.newLine();
        Enumeration keys = keys();

        // write the keys to the temporary state file
        while (keys.hasMoreElements()) {
            key = (PropertyKey) keys.nextElement();
            value = (Prop) get (key);
            if (value != null) {
                out.write (key.toString() + "=" + value.toString());
                out.newLine();
                backup.write (key.toString() + "=" + value.toString());
                backup.newLine();
            }
        }
        // close the temporary files
        out.close();
        backup.close();

        // rename to the real state file
        propsFile.delete();
        tempFile.renameTo(propsFile);

        // delete the backup
        backupFile.delete();
    }


    protected String getNextDF () {
        return "" + (nextDataFileNumber++) + ".dat";
    }


    public String getNextDatafilename() {
        File dir;
        try {
            dir = new File (dataPath);
        } catch (NullPointerException e) { return ""; }
        try {
            if ( !dir.exists() )
                return getNextDF();
        } catch (SecurityException e) { return ""; }
        String [] list;
        try {
            list = dir.list();
        } catch (SecurityException e) { return ""; }

        boolean found;
        String aFile;
        do {
            aFile = getNextDF();
            found = false;
            for (int i = 0; (i < list.length) && !found; i++)
                if (list[i].equals (aFile))
                    found = true;
        } while ( found );
        return aFile;
    }


    protected String getNextDL () {
        return "" + (nextDefectLogNumber++) + ".def";
    }


    public String getNextDefectLogname() {
        File dir;
        try {
            dir = new File (dataPath);
        } catch (NullPointerException e) { return ""; }
        try {
            if ( !dir.exists() )
                return getNextDL();
        } catch (SecurityException e) { return ""; }
        String [] list;
        try {
            list = dir.list();
        } catch (SecurityException e) { return ""; }

        boolean found;
        String aFile;
        do {
            aFile = getNextDL();
            found = false;
            for (int i = 0; (i < list.length) && !found; i++)
                if (list[i].equals (aFile))
                    found = true;
        } while ( found );
        return aFile;
    }


    public void copyFrom (PSPProperties fromProps,
                          PropertyKey   fromKey,
                          PropertyKey   toKey) {
        Prop aProp = new Prop (fromProps.pget (fromKey));
        for (int ii = 0; ii < aProp.getNumChildren(); ii++) {
            PropertyKey fc = aProp.getChild(ii);
            PropertyKey tc = new PropertyKey (toKey, fc.name());
                aProp.setChild (tc, ii);
            copyFrom (fromProps, fc, tc);
        }
        String datafile = aProp.getDataFile();
        if (hasDataFile(aProp)) {
            String newfile = getNextDatafilename();

            aProp.setDataFile (newfile);
            fireDataFileChange
                (new PendingDataChange(datafile, newfile, toKey.path()));
        }
        String defectlog = aProp.getDefectLog();
        if (defectlog != null && defectlog.length() > 0) {
            String newfile = getNextDefectLogname();

            aProp.setDefectLog (newfile);
            // this is kind of icky - we're using the datafile change queue to
            // remember changes to defect logs.  Perhaps later we should rework this.
            fireDataFileChange
                (new PendingDataChange(null, newfile, null));
        }
        put (toKey, aProp);
    }


    public void list (PrintStream out) {
        Enumeration keys = keys();
        Object key;
        Prop value;

        out.println("--- PSPProperties list ---");
        while (keys.hasMoreElements()) {
            key   = keys.nextElement();
            value = (Prop) get (key);
            out.println(key + "=" + value);
        }
    }

    private void listLeaves(PropertyKey key, Vector result) {
        int numChildren = getNumChildren(key);
        if (numChildren == 0)
            result.addElement(key);
        else
            for (int index = 0;   index < numChildren;   index++)
                listLeaves(getChildKey(key, index), result);
    }

    public Enumeration getLeaves(PropertyKey parent) {
        Vector leaves = new Vector();
        listLeaves(parent, leaves);
        return leaves.elements();
    }

    public Enumeration getLeafNames(PropertyKey parent) {
        Vector leaves = new Vector();
        listLeaves(parent, leaves);

        for (int index = leaves.size(); index-- > 0; )
            leaves.setElementAt(((PropertyKey)leaves.elementAt(index)).path(),
                                index);
        return leaves.elements();
    }

    public String datapath(PropertyKey key) {

        while (key != null)			// walk up the tree until you find
            if (hasDataFile(pget(key)))	// the first node with a datafile,
                return key.path();		// and return that node's path.
            else
                key = key.getParent();

        return null;		// if no datafile was found in this key's
                                    // ancestry, return null.
    }

    public DefectLogID defectLog(PropertyKey key, String property_directory) {
                                  // walk up the tree until you find the
        String defectLogFilename;	// first node with a defect log.
        while (key != null) {
            defectLogFilename = pget(key).getDefectLog();
            if (defectLogFilename != null && defectLogFilename.length() != 0)
                return new DefectLogID(property_directory + defectLogFilename, key);
            else
                key = key.getParent();
        }

                                    // if no defect log was found in this key's
        return null;		// ancestry, return null.
    }

    // gets all the applicable script IDs for the script button
    // based on the current phase.
    public Vector getScriptIDs(PropertyKey key) {    Vector v = new Vector();    Prop val;    String scriptFile;
        PropertyKey tempKey = key;
        PropertyKey parentKey = key.getParent();

        // First add the key and the key's ancestors (parents)
        while (tempKey != null) {
            val = pget(tempKey);
            scriptFile = val.getScriptFile();
            if (scriptFile != null && scriptFile.length() != 0)
                v.addElement(new ScriptID(scriptFile, datapath(tempKey), tempKey.path()));
            tempKey = tempKey.getParent();
        }

        // Then add all the parent's children, except the key.
        int numChildren = getNumChildren(parentKey);
        for (int index = 0; index < numChildren; index++) {
            tempKey = getChildKey(parentKey, index);
            if (tempKey.equals(key))
                continue;
            val = pget(tempKey);
            scriptFile = val.getScriptFile();
            if (scriptFile != null && scriptFile.length() != 0)
                v.addElement(new ScriptID(scriptFile, datapath(tempKey), tempKey.path()));
        }
        return v;
    }

    public void orderedDump (PrintWriter out, PropertyKey key, Vector filt) {
        PropertyKey child;
        String name;

        for (int ii = 0; ii < getNumChildren (key); ii++) {
            child = getChildKey (key, ii);
            name = child.path();
            if (Filter.matchesFilter(filt, name)) {
                out.println (name);
                orderedDump (out, child, filt);
            }
        }
    }

    public void orderedDump (PrintWriter out, Vector filt) {
        orderedDump (out, PropertyKey.ROOT, filt);
    }

}
