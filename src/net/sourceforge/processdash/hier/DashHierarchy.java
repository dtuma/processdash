// Copyright (C) 1999-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier;

import java.awt.ItemSelectable;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.event.EventListenerList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.defects.DefectLogID;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;


public class DashHierarchy extends Hashtable<PropertyKey, Prop> implements
        ItemSelectable, Comparator, PropertyKeyHierarchy {

    protected EventListenerList ell = new EventListenerList();
    protected int nextDataFileNumber  = 0;
    protected int nextDefectLogNumber = 0;
    public    String dataPath = null;


    public DashHierarchy (String baseDataPath) {
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

    public void copy (DashHierarchy src) {
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
        //fireHierarchyChanged();
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
        //else
        //fireHierarchyChanged();
        return val;
    }

    public String getID (PropertyKey key) {
        return pget (key).getID();
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
        //fireHierarchyChanged();
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
        //fireHierarchyChanged();
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
        //fireHierarchyChanged();
    }

    public PropertyKey findExistingKey (String s) {
        return findKey(s, true);
    }

    public PropertyKey findClosestKey(String path) {
        return findKey(path, false);
    }

    private PropertyKey findKey(String path, boolean exactMatch) {
        if (path == null) return null;
        if (path.startsWith("/"))
            path = path.substring(1);
        PropertyKey result = PropertyKey.ROOT;

        SEGMENTS: while (path.length() > 0) {
            Prop p = (Prop) get(result);
            if (p == null || p.getNumChildren() == 0)
                return exactMatch ? null : result;

            String nextSegment;
            int slashPos = path.indexOf('/');
            if (slashPos == -1) {
                nextSegment = path;
                path = "";
            } else {
                nextSegment = path.substring(0, slashPos);
                path = path.substring(slashPos+1);
            }

            for (int i = p.getNumChildren();  i-- > 0; ) {
                PropertyKey child = p.getChild(i);
                if (nextSegment.equals(child.name())) {
                    result = child;
                    continue SEGMENTS;
                }
            }

            return exactMatch ? null : result;
        }

        return result;
    }

    public PropertyKey findKeyByNodeID(String nodeID) {
        if (StringUtils.hasValue(nodeID)) {
            for (Map.Entry<PropertyKey, Prop> e : entrySet()) {
                if (nodeID.equals(e.getValue().getNodeID()))
                    return e.getKey();
            }
        }
        return null;
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
        //fireHierarchyChanged();
    }

    public void remove (PropertyKey key) {
        Prop val = pget(key);
        if (val != null) {
            if (responsibleForData(key))
                fireDataFileChange(new PendingDataChange(key.path()));

            for (int idx = val.getNumChildren() - 1; idx >= 0; idx--) {
                remove (val.getChild (idx));
            }

            super.remove(key);
        }

        //fireHierarchyChanged();
    }

    public Vector load (InputStream propStream) throws IOException {
        return load(propStream, true);
    }
    public Vector load (InputStream propStream, boolean close)
        throws IOException {
        return load(new InputStreamReader(propStream), close);
    }
    public Vector load (Reader reader, boolean close)
        throws IOException {

        String line;
        Vector v = null;
        PropertyKey key;
        Prop val;
        int equalsPosition;
        BufferedReader in = new BufferedReader(reader);

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

        //fireHierarchyChanged();

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

    public static final String DATA_REPOSITORY_NAME = "///Hierarchy";

    public static final String NAME_ATTR = "name";
    public static final String NODE_ID_ATTR = "nodeID";
    public static final String TEMPLATE_ATTR = "templateID";
    public static final String DATAFILE_ATTR = "dataFile";
    public static final String DEFECTLOG_ATTR = "defectLog";
    public static final String SELECTED_ATTR = "selected";
    public static final String HTML_HREF_ATTR = "href";
    public static final String CONSTRAINTS_ATTR = "constraints";
    public static final String IMAGINARY_NODE_ATTR = "imaginary";
    public static final String IMAGINARY_UNLESS_ATTR = "imaginaryUnless";

    public static final String TEMPLATE_NODE_NAME = "template";
    public static final String PHASE_NODE_NAME = "phase";
    public static final String NODE_NODE_NAME = "node";
    public static final String HTML_NODE_NAME = "html";
    public static final String ID_ATTR = "ID";
    public static final String NO_DATAFILE = "none";
    public static final String NO_HREF = "none";

    private static final String DATA_ELEM_TAG = "extraData";

    private static final String[] TEMPLATE_NODE_NAMES = {
        TEMPLATE_NODE_NAME, PHASE_NODE_NAME, NODE_NODE_NAME };
    static { Arrays.sort(TEMPLATE_NODE_NAMES); }

    public static final String XML_HEADER =
        "<?xml version='1.0' encoding='UTF-8'?>";
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

    public static final String EXISTING_DATAFILE = "[[alreadyExistingPhysicalDatafile]]";

    private PropertyKey loadXMLNode (Element e, DashHierarchy templates,
                                     PropertyKey parentKey,
                                     PropertyKey parentTemplate)
        throws SAXException
    {
        String nodeType = e.getTagName();
        if (HTML_NODE_NAME.equals(nodeType)) return null;
        if (e.hasAttribute(IMAGINARY_NODE_ATTR)) return null;
        if (shouldPrune(e.getAttribute(IMAGINARY_UNLESS_ATTR))) return null;

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
            val.setNodeID(e.getAttribute(NODE_ID_ATTR));

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
            String href = e.getAttribute(HTML_HREF_ATTR);
            if (!NO_HREF.equals(href))
                val.setScriptFile(href);

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
        int len = children.getLength();
        for (int i=0;   i < len;   i++) {
            Node n = children.item(i);
            if (n instanceof Element) {
                Element elem = (Element) n;
                String elemName = elem.getTagName().toLowerCase();
                if (Arrays.binarySearch(TEMPLATE_NODE_NAMES, elemName) >= 0) {
                    val.addChild(loadXMLNode(elem, templates, key, templateKey),-1);
                    if ("true".equals(elem.getAttribute(SELECTED_ATTR)))
                        val.setSelectedChild(val.getNumChildren() - 1);
                } else if (DATA_ELEM_TAG.equalsIgnoreCase(elemName)) {
                    val.setExtraData(XMLUtils.getTextContents(elem));
                }
            }
        }

        // Save this node into the hashtable.
        put(key, val);
        return key;
    }

    private boolean shouldPrune(String requiresTemplate) {
        return (requiresTemplate != null &&
                requiresTemplate.length() != 0 &&
                TemplateLoader.getPackageVersion(requiresTemplate) == null);
    }

    private String getChildIDFromTemplate(String nodeName,
                                          DashHierarchy templates,
                                          PropertyKey parentTemplate) {
        if (parentTemplate == null || parentTemplate == INVALID_TEMPLATE)
            return null;
        PropertyKey templateKey = new PropertyKey(parentTemplate, nodeName);
        Prop template = (Prop) templates.get(templateKey);
        if (template != null && Prop.hasValue(template.getID()))
            return template.getID();
        else
            return null;
    }



    public Vector loadXML (String filename, DashHierarchy templates)
        throws IOException, SAXException
    {
        return loadXML(new FileInputStream(filename), templates);
    }

    public Vector loadXML(InputStream in, DashHierarchy templates)
        throws SAXException, IOException
    {
        loadXMLNode(XMLUtils.parse(in).getDocumentElement(),
                    templates, null, null);

        //fireHierarchyChanged();

        Vector v = new Vector();
        scanForDataFiles(v, PropertyKey.ROOT);
        if (v.isEmpty()) v = null;
        return v;
    }

    public void loadXMLTemplate (Element e) throws SAXException {
        e.setAttribute(NAME_ATTR, "top");
        loadXMLNode(e, null, null, null);
        //fireHierarchyChanged();
    }

    private void scanForDataFiles(Vector v, PropertyKey key) {
        Prop val = pget(key);
        String dataFile = val.getDataFile();
        if (dataFile != null && dataFile.length() > 0)
            v.addElement (new String[] {key.path(), dataFile});
        for (int i = 0; i < getNumChildren (key); i++)
            scanForDataFiles(v, getChildKey (key, i));
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
        maybePrintAttribute(out, NODE_ID_ATTR, prop.getNodeID());
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
        if (Settings.isReadOnly())
            return;

        BufferedWriter out = new BufferedWriter
            (new RobustFileWriter(filename, "UTF-8"));
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
        if (Settings.isReadOnly())
            return;

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
                if (list[i].equalsIgnoreCase (aFile))
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
                if (list[i].equalsIgnoreCase (aFile))
                    found = true;
        } while ( found );
        return aFile;
    }

    public boolean assignMissingNodeIDs() {
        // first, find the largest node ID currently in use
        int maxIdNum = 0;
        boolean sawMissingID = false;
        for (Prop p : values()) {
            String oneID = p.getNodeID();
            if (!StringUtils.hasValue(oneID)) {
                sawMissingID = true;
            } else if (oneID.startsWith("0:")) {
                try {
                    int oneIdNum = Integer.parseInt(oneID.substring(2));
                    maxIdNum = Math.max(maxIdNum, oneIdNum);
                } catch (Exception e) {}
            }
        }

        // next, assign node IDs to any nodes that don't have one.
        if (sawMissingID) {
            for (Prop p : values()) {
                String thisID = p.getNodeID();
                if (!StringUtils.hasValue(thisID))
                    p.setNodeID("0:" + (++maxIdNum));
            }
            pget(PropertyKey.ROOT).setNodeID("0:root");
        }

        // return true if we made any assignments.
        return sawMissingID;
    }


    public void copyFrom (DashHierarchy fromProps,
                          PropertyKey   fromKey,
                          PropertyKey   toKey) {
        Prop fromProp = fromProps.pget (fromKey);
        Prop aProp = new Prop (fromProp);
        aProp.setNodeID(null);
        for (int ii = 0; ii < aProp.getNumChildren(); ii++) {
            PropertyKey fc = aProp.getChild(ii);
            PropertyKey tc = new PropertyKey (toKey, fc.name());
                aProp.setChild (tc, ii);
            copyFrom (fromProps, fc, tc);
        }
        String datafile = aProp.getDataFile();
        if (hasDataFile(aProp)) {
            String newfile = getNextDatafilename();
            String extraData = fromProp.getExtraData();

            aProp.setDataFile (newfile);
            fireDataFileChange
                (new PendingDataChange(datafile, newfile, toKey.path(), extraData));
        }
        String defectlog = aProp.getDefectLog();
        if (defectlog != null && defectlog.length() > 0) {
            String newfile = getNextDefectLogname();

            aProp.setDefectLog (newfile);
            // this is kind of icky - we're using the datafile change queue to
            // remember changes to defect logs.  Perhaps later we should rework this.
            fireDataFileChange
                (new PendingDataChange(null, newfile, null, null));
        }
        put (toKey, aProp);
        //fireHierarchyChanged();
    }


    public void mergeChangesFrom(DashHierarchy src) {
        mergeChangesFrom(src, PropertyKey.ROOT);
    }

    private void mergeChangesFrom(DashHierarchy src, PropertyKey node) {
        DashHierarchy dest = this;
        Prop srcProp = src.pget(node);
        Prop destProp = dest.pget(node);
        PropertyKey destSelected = destProp.getChild(destProp
                .getSelectedChild());

        // First, delete any nodes from the dest that do not exist in the src
        for (int i = destProp.getNumChildren();  i-- > 0; ) {
            PropertyKey child = destProp.getChild(i);
            Prop srcChildProp = src.pget(child);
            Prop destChildProp = dest.pget(child);
            if (!src.containsKey(child)
                    || !dataFilesEqual(srcChildProp, destChildProp))
                dest.removeChildKey(node, i);
        }

        // Next, iterate over the src children and create/merge them in the dest
        for (int i = 0;  i < srcProp.getNumChildren();  i++) {
            PropertyKey child = srcProp.getChild(i);
            Prop srcChildProp = src.pget(child);

            if (!dest.containsKey(child) && hasDataFile(srcChildProp)) {
                // if the child doesn't already exist in the dest and it has a
                // datafile, fire an associated data event.
                fireDataFileChange(new PendingDataChange(EXISTING_DATAFILE,
                    srcChildProp.getDataFile(), child.path(), null));
            }

            // now recursively perform a merge on the child.
            mergeChangesFrom(src, child);
        }

        // Finally, copy the prop over.  This will create the target node if
        // it didn't already exist; otherwise, it will ensure that the children
        // appear in the right order.  Try to retain the originally selected
        // child from the dest hierarchy if possible.
        Prop newDestProp = new Prop(srcProp);
        if (destSelected != null)
            for (int i = newDestProp.getNumChildren();  i-- > 0; )
                if (newDestProp.getChild(i).equals(destSelected))
                    newDestProp.setSelectedChild(i);
        dest.put(node, newDestProp);
    }

    private static boolean dataFilesEqual(Prop a, Prop b) {
        if (a == null || b == null) return false;
        return cmpDataFile(a).equalsIgnoreCase(cmpDataFile(b));
    }
    private static String cmpDataFile(Prop p) {
        return (p.dataFile == null ? "" : p.dataFile);
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

    private void listLeaves(PropertyKey key, Vector<PropertyKey> result) {
        int numChildren = getNumChildren(key);
        if (numChildren == 0)
            result.addElement(key);
        else
            for (int index = 0;   index < numChildren;   index++)
                listLeaves(getChildKey(key, index), result);
    }

    public Enumeration<PropertyKey> getLeaves(PropertyKey parent) {
        Vector<PropertyKey> leaves = new Vector();
        listLeaves(parent, leaves);
        return leaves.elements();
    }

    public Enumeration<String> getLeafNames(PropertyKey parent) {
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
    public Vector<ScriptID> getScriptIDs(PropertyKey key) {
        Vector<ScriptID> v = new Vector<ScriptID>();
        Prop val;
        String scriptFile, templateID;
        PropertyKey tempKey = key;
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
                Vector<ScriptID> scriptIDs = TemplateLoader.getScriptIDs
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

    public static void displayCorruptStateFileWarning(Resources resources,
                                               String filename, Exception e) {
          int lineNum = -1;
          if (e instanceof SAXParseException)
              lineNum = ((SAXParseException) e).getLineNumber();

          JOptionPane.showMessageDialog
              (null,
               resources.formatStrings("Errors.Corrupt_Statefile_Warning_FMT",
                                       e.getLocalizedMessage(), filename,
                                       new Integer(lineNum)),
               resources.getString("Errors.Corrupt_Statefile_Title"),
               JOptionPane.ERROR_MESSAGE);
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


    // Listener support

    public class Event extends EventObject {
        boolean adjusting;
        Event(Object o, boolean adjusting) {
            super(o);
            this.adjusting = adjusting;
        }
        public boolean isAdjusting() {
            return adjusting;
        }
    }
    public interface Listener {
        public void hierarchyChanged(Event e);
    }
    public interface PrePostListener extends Listener {
        public void hierarchyWillChange(Event e);
    }

    Set listeners = null;
    public synchronized void addHierarchyListener(Listener l) {
        if (listeners == null)
            listeners = Collections.synchronizedSet(new HashSet());
        listeners.add(l);
    }
    public void removeHierarchyListener(Listener l) {
        if (listeners != null) listeners.remove(l);
    }
    public void fireHierarchyWillChange() {
        Event e = new Event(this, false);
        Iterator i;
        synchronized (listeners) {
            i = new ArrayList(listeners).iterator();
        }
        while (i.hasNext()) {
            Object listener = i.next();
            if (listener instanceof PrePostListener)
                ((PrePostListener) listener).hierarchyWillChange(e);
        }
    }
    public void fireHierarchyChanged() {
        fireHierarchyChanged(false);
    }
    protected void fireHierarchyChanged(boolean isAdjusting) {
        if (listeners != null  && !listeners.isEmpty()) {
            Event e = new Event(this, isAdjusting);
            Iterator i;
            synchronized (listeners) {
                i = new ArrayList(listeners).iterator();
            }
            while (i.hasNext())
                ((Listener) i.next()).hierarchyChanged(e);
        }
    }

    public String toString() { return getClass().getName() + "@" + hashCode(); }
    public boolean equals(Object o) { return (this == o); }
    public int hashCode() { return System.identityHashCode(this); }

    /**
     * @param name the potential name of a single hierarchy node
     * @return true if the name is a valid name for a hierarchy node
     * @since 1.15.8
     */
    public static boolean isValidNodeName(String name) {
        return (name != null //
                && (name = name.trim()).length() > 0 //
                && name.indexOf('/') == -1 //
                && !".".equals(name) //
                && !"..".equals(name));
    }

    /**
     * Clean up a potential hierarchy path by discarding illegal path segments
     * and unnecessary redundant slashes, trimming leading and trailing whitespace
     * on individual path segments, etc.
     * 
     * @param a
     *            potential hierarchy path
     * @return a version of that path which would be valid for use within the
     *         hierarchy.  If the given hierarchy path was relative, the result
     *         will be relative as well.
     * @since 1.15.8
     */
    public static String scrubPath(String path) {
        if (path == null)
            return null;
        else if (path.trim().equals("/"))
            return "/";

        StringBuilder result = new StringBuilder();
        for (String part : path.split("/")) {
            part = part.trim();
            if (isValidNodeName(part))
                result.append("/").append(part);
        }
        if (path.trim().startsWith("/") || result.length() == 0)
            return result.toString();
        else
            return result.substring(1);
    }

}
