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

import java.util.Comparator;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.Writer;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.ItemSelectable;
import javax.swing.event.EventListenerList;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PSPProperties extends Hashtable implements ItemSelectable,
                                                        Comparator {

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
        move(fromKey, toKey, true);
    }

    public void move (PropertyKey fromKey,
                      PropertyKey toKey,
                      boolean explicit) {
        Prop aProp = pget (fromKey);
        if (aProp != null) {
            for (int ii = 0; ii < aProp.getNumChildren(); ii++) {
                PropertyKey fc = aProp.getChild(ii);
                PropertyKey tc = new PropertyKey (toKey, fc.name());
                aProp.setChild (tc, ii);
                move (fc, tc, false);
            }
            if (explicit || responsibleForData(fromKey))
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
        return load(propStream, true);
    }
    public Vector load (InputStream propStream, boolean close)
        throws IOException {

        String line;
        Vector v = null;
        PropertyKey key;
        Prop val;
        int equalsPosition;
        BufferedReader in = new BufferedReader(new InputStreamReader(propStream));

        while ((line = in.readLine()) != null) {
            if ((! line.startsWith ("#")) &&
                ((equalsPosition = line.indexOf('=', 0)) != -1))
                try {
                    key = PropertyKey.valueOf (line.substring(0, equalsPosition));
                    val = Prop.valueOf (line.substring(equalsPosition+1));
                    put (key, val);
                } catch (Exception e) {
                    throw new IOException("Invalid hierarchy file format.");
                }
        }

        if (close) in.close();

        v = new Vector();
        scanForDataFiles(v, PropertyKey.ROOT);
        if (v.isEmpty()) v = null;
        return v;
    }

    public Vector load (String datafilePath) throws IOException {
        return load(new FileInputStream(datafilePath));
    }

    public PropertyKey getByID(String id) {
        Iterator i = entrySet().iterator();
        Map.Entry e;
        Prop val;
        PropertyKey result = null, oneKey;
        while (i.hasNext()) {
            e = (Map.Entry) i.next();
            val = (Prop) e.getValue();
            if (id.equals(val.getID())) {
                oneKey = (PropertyKey) e.getKey();

                if (// If this is the first match we've found,
                    result == null ||

                    // or if this node's name also matches the id (while our
                    // previous result's name did not),
                    id.equals(oneKey.name()) && !id.equals(result.name()) ||

                    // or if this node's path is shorter than the previous result's
                    // path (presumably because it is closer to the ROOT),
                    result.path().length() > oneKey.path().length())

                    // then save this key as our best result found so far.
                    result = oneKey;
            }
        }
        return result;
    }

    public PropertyKey getRootChildByName(String id) {
        // This is NOT the correct way to do this, but this is how PropertyFrame
        // is currently doing it.  Fix it later.
        PropertyKey result = new PropertyKey (PropertyKey.ROOT, id);
        return (containsKey(result) ? result : null);
    }

    public static final String NAME_ATTR = "name";
    public static final String TEMPLATE_ATTR = "templateID";
    public static final String DATAFILE_ATTR = "dataFile";
    public static final String DEFECTLOG_ATTR = "defectLog";
    public static final String SELECTED_ATTR = "selected";
    public static final String HTML_HREF_ATTR = "href";
    public static final String CONSTRAINTS_ATTR = "constraints";
    public static final String IMAGINARY_NODE_ATTR = "imaginary";

    public static final String TEMPLATE_NODE_NAME = "template";
    public static final String HTML_NODE_NAME = "html";
    public static final String ID_ATTR = "ID";
    public static final String NO_DATAFILE = "none";

    public static final String XML_HEADER =
        "<?xml version='1.0'?>";
    public static final String XML_DTD =
        "<!DOCTYPE node [\n" +
        "    <!ELEMENT node (node*)>\n" +
        "    <!ATTLIST node\n" +
        "      "+NAME_ATTR+" CDATA #REQUIRED\n" +
        "      "+TEMPLATE_ATTR+" CDATA #IMPLIED\n" +
        "      "+DATAFILE_ATTR+" CDATA #IMPLIED\n" +
        "      "+DEFECTLOG_ATTR+" CDATA #IMPLIED\n" +
        "      "+SELECTED_ATTR+" (true|false) #IMPLIED>\n" +
        "]>\n\n";

    private static final PropertyKey INVALID_TEMPLATE =
        new PropertyKey((PropertyKey) null, "INVALID");
    private static final String DEFAULT_PHASE_STATUS   = "ME<>";
    private static final String DEFAULT_PROJECT_STATUS = "MED<>";
    private static final String DEFAULT_TEMPLATE_STATUS = "<";

    private PropertyKey loadXMLNode (Element e, PSPProperties templates,
                                     PropertyKey parentKey,
                                     PropertyKey parentTemplate)
        throws SAXException
    {
        String nodeType = e.getTagName();
        if (HTML_NODE_NAME.equals(nodeType)) return null;
        if (e.hasAttribute(IMAGINARY_NODE_ATTR)) return null;

        String nodeName = e.getAttribute(NAME_ATTR);
        if (nodeName == null || nodeName.length() == 0)
            throw new SAXException("Every node MUST have a name.");
        PropertyKey key = new PropertyKey(parentKey, nodeName);

        PropertyKey templateKey = null;
        Prop val = new Prop();
        val.setDefectLog(e.getAttribute(DEFECTLOG_ATTR));
        String datafile = e.getAttribute(DATAFILE_ATTR);
        if (!NO_DATAFILE.equals(datafile)) val.setDataFile(datafile);

        if (templates != null) {
            // We are reading the user's state file.
            // Determine the template node which this node should be modeled after.
            String id = getChildIDFromTemplate(nodeName, templates, parentTemplate);
            if (id == null) id = e.getAttribute(TEMPLATE_ATTR);
            boolean idSet = Prop.hasValue(id);
            if (idSet) {
                templateKey = templates.getByID(id);
                if (templateKey == null) templateKey = INVALID_TEMPLATE;
            } else if (parentTemplate == INVALID_TEMPLATE)
                templateKey = INVALID_TEMPLATE;
            else if (parentTemplate != null)
                templateKey = new PropertyKey(parentTemplate, nodeName);
            Prop template = null;
            if (templateKey != null && templateKey != INVALID_TEMPLATE) {
                template = (Prop) templates.get(templateKey);
                if (template == null)
                    templateKey = idSet ? INVALID_TEMPLATE : null;
            }

            val.setID(e.getAttribute(TEMPLATE_ATTR));

            // Copy script and status information from the template, if there is one.
            if (template == null) {
                val.setScriptFile("");
                if (templateKey != INVALID_TEMPLATE)
                    val.setStatus("");
                else
                    val.setStatus(idSet ? DEFAULT_PROJECT_STATUS : DEFAULT_PHASE_STATUS);
            } else {
                val.setScriptFile(template.getScriptFile());
                val.setStatus(template.getStatus());
            }

        } else {
            // We are loading a process template definition. Store the template ID.
            String theid = e.getAttribute(ID_ATTR);
            if (!Prop.hasValue(theid) && TEMPLATE_NODE_NAME.equals(nodeType))
                theid = nodeName;
            val.setID(theid);

            // Store the defined script href.
            val.setScriptFile(e.getAttribute(HTML_HREF_ATTR));

            // Store the applicable constraint.
            String constraints = e.getAttribute(CONSTRAINTS_ATTR);
            if (Prop.hasValue(constraints))
                constraints = constraints.replace('{', '<').replace('}', '>');
            else
                constraints = (TEMPLATE_NODE_NAME.equals(nodeType) ?
                               DEFAULT_TEMPLATE_STATUS : DEFAULT_PHASE_STATUS);
            val.setStatus(constraints);
        }

        // Recursively add children.
        NodeList children = e.getChildNodes();
        for (int i=0;   i < children.getLength();   i++) {
            Node n = children.item(i);
            if (n instanceof Element) {
                val.addChild(loadXMLNode((Element) n, templates, key, templateKey),-1);
                if ("true".equals(((Element) n).getAttribute(SELECTED_ATTR)))
                    val.setSelectedChild(val.getNumChildren() - 1);
            }
        }

        // Save this node into the hashtable.
        put(key, val);
        return key;
    }

    private String getChildIDFromTemplate(String nodeName,
                                          PSPProperties templates,
                                          PropertyKey parentTemplate) {
        if (parentTemplate == null || parentTemplate == INVALID_TEMPLATE)
            return null;
        PropertyKey templateKey = new PropertyKey(parentTemplate, nodeName);
        Prop template = (Prop) templates.get(templateKey);
        if (template != null && template.hasValue(template.getID()))
            return template.getID();
        else
            return null;
    }



    public Vector loadXML (String filename, PSPProperties templates)
        throws IOException, SAXException
    {
        FileInputStream in = new FileInputStream(filename);
        loadXMLNode(XMLUtils.parse(in).getDocumentElement(),
                    templates, null, null);

        Vector v = new Vector();
        scanForDataFiles(v, PropertyKey.ROOT);
        if (v.isEmpty()) v = null;
        return v;
    }

    public void loadXMLTemplate (Element e) throws SAXException {
        e.setAttribute(NAME_ATTR, "top");
        loadXMLNode(e, null, null, null);
    }

    private void scanForDataFiles(Vector v, PropertyKey key) {
        Prop val = pget(key);
        String dataFile = val.getDataFile();
        if (dataFile != null && dataFile.length() > 0)
            v.addElement (new String[] {key.path(), dataFile});
        for (int i = 0; i < getNumChildren (key); i++)
            scanForDataFiles(v, getChildKey (key, i));
    }

    void runV1_4Hack() {
        Hashtable brokenIDs = new Hashtable();
        brokenIDs.put("pspForEng/2A/script.htm", "PSP0.1-PFE-2A");
        brokenIDs.put("pspForEng/4A/script.htm", "PSP1-PFE-4A");
        brokenIDs.put("pspForEng/5A/script.htm", "PSP1.1-PFE-5A");
        brokenIDs.put("pspForEng/7A/script.htm", "PSP2-PFE-7A");
        brokenIDs.put("pspForEng/8A/script.htm", "PSP2.1-PFE-8A");
        brokenIDs.put("pspForMSE/2A/script.htm", "PSP0.1-MSE-2A");
        brokenIDs.put("pspForMSE/3B/script.htm", "PSP1-MSE-3B");
        brokenIDs.put("pspForMSE/4B/script.htm", "PSP1.0.1-MSE-4B");

        PropertyKey key;
        Prop        value;
        String      s;
        Enumeration keys = keys();
        while (keys.hasMoreElements()) {
            key = (PropertyKey)keys.nextElement();
            value = (Prop)get (key);
            if (! Prop.hasValue(value.getID())) continue;
            if (! Prop.hasValue(s = value.getScriptFile ())) continue;
            s = (String) brokenIDs.get(s);
            if (s != null) value.setID(s);
        }
    }

    private void maybePrintAttribute(Writer out, String attr, String val)
        throws IOException
    {
        if (val == null || val.length() == 0) return;
        out.write(" ");
        out.write(attr);
        out.write("=\"");
        out.write(XMLUtils.escapeAttribute(val));
        out.write("\"");
    }

    private void saveXMLNode(BufferedWriter out, int depth,
                             PropertyKey key, boolean selected)
        throws IOException
    {
        if (key == null) return;
        Prop prop = (Prop) get (key);
        if (prop == null) return;
        int i;

        for (i = depth;   i > 0;   i--)
            out.write("  ");

        out.write("<node " + NAME_ATTR + "=\"" +
                  XMLUtils.escapeAttribute(key.name()) + "\"");
        maybePrintAttribute(out, TEMPLATE_ATTR, prop.getID());
        maybePrintAttribute(out, DATAFILE_ATTR, prop.getDataFile());
        maybePrintAttribute(out, DEFECTLOG_ATTR, prop.getDefectLog());
        if (selected) maybePrintAttribute(out, SELECTED_ATTR, "true");

        int numChildren = prop.getNumChildren();
        if (numChildren == 0) {
            out.write("/>");
            out.newLine();
        } else {
            out.write(">");
            out.newLine();

            int selectedIndex = prop.getSelectedChild();
            for (i = 0;  i < numChildren;  i++)
                saveXMLNode(out, depth+1, prop.getChild(i), (i == selectedIndex));


            for (i = depth;   i > 0;   i--)
                out.write("  ");
            out.write("</node>");
            out.newLine();
        }
    }

    public void saveXML(String filename, String comment) throws IOException {
        BufferedWriter out = new BufferedWriter(new RobustFileWriter(filename));
        out.write(XML_HEADER);
        out.newLine();    out.newLine();

        if (comment != null && comment.length() != 0) {
            out.write("<!-- " + XMLUtils.escapeAttribute(comment) + " -->");
            out.newLine();    out.newLine();
        }

        saveXMLNode(out, 0, PropertyKey.ROOT, false);
        out.close();
    }

    public void save (String datafilePath, String comment) throws IOException {
        saveXML(datafilePath, comment);
    }


    // The save operation writes the property keys to the state file.
    // Data is first written to a temporary file, and later renamed to
    // final output file.
    public void saveOld (String datafilePath,
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
        File tempFile = new File(propsFile.getParent() + fileSep + "tttt_sta");
        File backupFile = new File(propsFile.getParent() + fileSep + "ttttstat");

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

        while (key != null)                 // walk up the tree until you find
            if (hasDataFile(pget(key)))       // the first node with a datafile,
                return key.path();              // and return that node's path.
            else
                key = key.getParent();

        return null;                // if no datafile was found in this key's
                                    // ancestry, return null.
    }

    public DefectLogID defectLog(PropertyKey key, String property_directory) {
                                  // walk up the tree until you find the
        String defectLogFilename;   // first node with a defect log.
        while (key != null) {
            defectLogFilename = pget(key).getDefectLog();
            if (defectLogFilename != null && defectLogFilename.length() != 0)
                return new DefectLogID(property_directory + defectLogFilename, key);
            else
                key = key.getParent();
        }

                                    // if no defect log was found in this key's
        return null;                // ancestry, return null.
    }

    // gets all the applicable script IDs for the script button
    // based on the current phase.
    public Vector getScriptIDs(PropertyKey key) {
        Vector v = new Vector();    Prop val;    String scriptFile, templateID;
        PropertyKey tempKey = key;
        PropertyKey parentKey = null;
        ScriptID defaultScript = null;

        // Find and add all applicable scripts.
        while (tempKey != null) {
            val = pget(tempKey);

            if (defaultScript == null) {
                scriptFile = val.getScriptFile();
                if (scriptFile != null && scriptFile.length() != 0)
                    defaultScript = new ScriptID(scriptFile, datapath(tempKey), null);
            }

            if (Prop.hasValue(templateID = val.getID())) {
                Vector scriptIDs = TemplateLoader.getScriptIDs
                    (templateID, datapath(tempKey));
                if (scriptIDs != null)
                    v.addAll(scriptIDs);
            }

            tempKey = tempKey.getParent();
        }

        if (defaultScript == null && v.size() > 0)
            defaultScript = (ScriptID) v.elementAt(0);
        if (defaultScript != null)
            v.insertElementAt(defaultScript, 0);

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



    /** compare two strings for ordering within the tree.
     * @param p1, p2 the names of two DATA ELEMENTS.
     * most data element names will begin with some hierarchy path.
     * This determines the initial hierarchy path of each data element, then
     * returns
     *    -1 if p1 is associated with an earlier node in the hierarchy than p2.
     *    +1 if p2 is associated with an earlier node in the hierarchy than p1.
     *     0 if both data elements are associated with the same hierarchy node.
     */
    public int comparePaths(String p1, String p2) {
        if (p1.equals(p2)) return 0;
        return comparePaths(p1, p2, PropertyKey.ROOT);
    }
    private int comparePaths(String p1, String p2, PropertyKey key) {
        String path = key.path() + "/";
        boolean match1, match2;
        match1 = p1.startsWith(path);
        match2 = p2.startsWith(path);
        if (!match1 && !match2) return  0;
        if ( match1 && !match2) return  1;
        if (!match1 &&  match2) return -1;

        int result;
        for (int i = getNumChildren (key); i-- > 0; )
            if ((result = comparePaths (p1, p2, getChildKey (key, i))) != 0)
                return result;
        return 0;
    }
    public int compare(Object o1, Object o2) {
        return comparePaths((String) o1, (String) o2);
    }

}
