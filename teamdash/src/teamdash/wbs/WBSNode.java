// Copyright (C) 2002-2015 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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


package teamdash.wbs;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.util.NullSafeObjectUtils;
import net.sourceforge.processdash.util.PatternList;

import teamdash.XMLUtils;

/** This class represents a node in the work breakdown structure hierarchy.
 */
public class WBSNode implements Cloneable {

    /** The WBSMode to which this node belongs. */
    private WBSModel wbsModel;
    /** The name of this node */
    private String name;
    /** A number uniquely identifying this node */
    private int uniqueID;
    /** The type of this node */
    private String type;
    /** The indentation depth of this node */
    private int indentLevel;
    /** True if this node is expanded, false if it is collapsed */
    private boolean expanded;
    /** True if this node is read only */
    private boolean readOnly;
    /** A collection of attributes containing the data for this node */
    private Map attributes = new HashMap();



    /** Create a new WBSNode with given characteristics. */
    public WBSNode(WBSModel model, String name, String type,
                   int level, boolean expanded) {
        this.wbsModel = model;
        setUniqueID(-1); // not very unique - but let WBSModel fix it.
        setName(name);
        setType(type);
        setIndentLevel(level);
        setExpanded(expanded);
        setReadOnly(false);
    }



    /** Create a new WBSNode with information from an XML Element. */
    public WBSNode(WBSModel model, Element e) {
        this.wbsModel = model;
        setName(e.getAttribute(NAME_ATTR));
        setUniqueID(XMLUtils.getXMLInt(e, ID_ATTR));
        setType(e.getAttribute(TYPE_ATTR));
        setIndentLevel(XMLUtils.getXMLInt(e, INDENT_ATTR));
        setExpanded(XMLUtils.hasValue(e.getAttribute(EXPAND_ATTR)));
        setReadOnly(XMLUtils.hasValue(e.getAttribute(READ_ONLY_ATTR)));

        NodeList nodeAttributes = e.getElementsByTagName(ATTR_ELEM_NAME);
        int len = nodeAttributes.getLength();
        for (int i = 0;   i < len;   i++)
            setXMLAttribute((Element) nodeAttributes.item(i));
    }

    // Getter/setter methods

    /** Returns the WBSModel that this node belongs to.
     * @return Returns the WBSModel that this node belongs to.  */
    public WBSModel getWbsModel() {
        return wbsModel;
    }

    /** Get the name of this node.
     * @return The name of this node.  */
    public String getName() { return name; }

    /** Set the name of this node.
     * @param newName the new name for this node.  */
    public void setName(String newName) { this.name = newName; }

    /** Get the unique ID of this node.
     * @return the unique ID of this node.  */
    public int getUniqueID() {
        return uniqueID;
    }

    /** Set the unique ID of this node.
     * @param uniqueID the new ID for this node. */
    public void setUniqueID(int uniqueID) {
        this.uniqueID = uniqueID;
    }

    /** Get the type of this node.
     * @return the type of this node.  */
    public String getType() { return type; }

    /** Set the type of this node
     * @param newType the new type for this node.  */
    public void setType(String newType) { this.type = newType; }

    /** Test to see if another node is of the same type */
    public boolean typeMatches(WBSNode that) {
        return that != null && this.type.equals(that.type);
    }


    /** Get the indentation level of this node.
     * @return the indentation level of this node.  */
    public int getIndentLevel() { return indentLevel; }

    /** Set the indentation level of this node.
     * @param newLevel the new indentation level for this node.  */
    public void setIndentLevel(int newLevel) { this.indentLevel = newLevel; }


    /** Returns true if this node is currently expanded.
     * @return true if this node is expanded.  */
    public boolean isExpanded() { return expanded; }

    /** Expand or collapse this node.
     * @param expanded <code>true</code> to expand this node,
     *    <code>false</code> to collapse this node.  */
    public void setExpanded(boolean expanded) { this.expanded = expanded; }


    /** Returns true if this node is read only.
     * @return true if this node is read only.  */
    public boolean isReadOnly() {  return readOnly; }

    /** Set the read only status of this node.
     * @param readOnly the new read only status for this node.  */
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }



    // Methods to get/set data attributes of the node


    /** Get an attribute of type <code>Object</code> */
    public Object getAttribute(String attrName) {
        return attributes.get(attrName);
    }
    /** Set an attribute of type <code>Object</code> */
    public void setAttribute(String attrName, Object value) {
        if (value == null)
            attributes.remove(attrName);
        else
            attributes.put(attrName, value);
    }
    /** Store a number of attributes on this node */
    public void setAttributes(Map<String, Object> attrs,
            boolean overwriteExisting) {
        if (attrs != null && !attrs.isEmpty()) {
            for (Map.Entry<String, Object> e : attrs.entrySet()) {
                String attrName = e.getKey();
                Object attrValue = e.getValue();
                if (overwriteExisting || attributes.get(attrName) == null)
                    setAttribute(attrName, attrValue);
            }
        }
    }
    /** Remove an attribute and return its previous value */
    public Object removeAttribute(String attrName) {
        return attributes.remove(attrName);
    }
    /** Remove a number of attributes that match a given pattern */
    public void removeAttributes(PatternList matchingPattern) {
        Iterator<String> attrNames = attributes.keySet().iterator();
        while (attrNames.hasNext())
            if (matchingPattern.matches(attrNames.next()))
                attrNames.remove();
    }
    /** Get a list of the attributes on this node */
    public Set<String> listAttributeNames() {
        return Collections.unmodifiableSet(attributes.keySet());
    }


    /** Get a numeric attribute.
     * @return <code>Double.NaN</code> if the named attribute is not set,
     *   or is not a numeric attribute. */
    public double getNumericAttribute(String attrName) {
        Object value = getAttribute(attrName);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();

        } else if (value instanceof String) {
            try {
                double doubleValue = Double.parseDouble((String) value);
                setAttribute(attrName, doubleValue);
                return doubleValue;
            } catch (NumberFormatException nfe) {
            }
        }

        return Double.NaN;
    }
    /** Set a numeric attribute */
    public void setNumericAttribute(String attrName, double value) {
        setAttribute(attrName, new Double(value));
    }


    /** Return an attribute that is known to be an integer */
    public Integer getIntegerAttribute(String attrName) {
        Object attrValue = getAttribute(attrName);
        if (attrValue instanceof Integer)
            return (Integer) attrValue;

        if (attrValue instanceof String) {
            Integer i = null;
            try {
                i = Integer.valueOf((String) attrValue);
            } catch (Exception e) {
            }
            setAttribute(attrName, i);
            return i;
        }

        return null;
    }



    // Methods for conversion to/from XML


    /** Extract an attribute name/value pair from the given XML Element,
     * and store it in the attribute map.
     *
     * Note: this stores values in the attribute map as <code>String</code>s,
     * so all getXXXAttribute methods must be capable of automatically
     * interpreting <code>String</code> values. */
    protected void setXMLAttribute(Element attrElement) {
        String name = attrElement.getAttribute(NAME_ATTR).intern();
        String value = attrElement.getAttribute(VALUE_ATTR);
        setAttribute(name, value);
    }


    /** Write an XML representation of this node to the given
     * <code>Writer</code> object. */
    public void getAsXML(Writer out) throws IOException {
        getAsXML(out, false);
    }
    /** Write an XML representation of this node to the given
     * <code>Writer</code> object.
     * @param out the Writer to write the XML to
     * @param full <code>true</code> to dump all attributes, including
     * transient attributes;  <code>false</code> to dump only "authoritative"
     * attributes. */
    public void getAsXML(Writer out, boolean full) throws IOException {
        // write the opening wbsNode tag.
        String indentation = SPACES.substring
            (0, Math.min(2 * (indentLevel + 1), SPACES.length()));
        out.write(indentation);
        out.write("<"+ELEMENT_NAME+" "+NAME_ATTR+"='");
        out.write(XMLUtils.escapeAttribute(getName()));
        out.write("' "+ID_ATTR+"='");
        out.write(Integer.toString(uniqueID));
        if (getType() != null) {
            out.write("' "+TYPE_ATTR+"='");
            out.write(XMLUtils.escapeAttribute(getType()));
        }
        out.write("' "+INDENT_ATTR+"='");
        out.write(Integer.toString(getIndentLevel()));
        if (isExpanded()) out.write("' "+EXPAND_ATTR+"='true");
        if (isReadOnly()) out.write("' "+READ_ONLY_ATTR+"='true");
        out.write("'");         // don't close tag yet

        // write out a tag for each attribute.
        Iterator i = new TreeMap(attributes).entrySet().iterator();
        Map.Entry e;
        Object v;
        String name, value;
        boolean wroteAttribute = false;
        while (i.hasNext()) {
            e = (Map.Entry) i.next();
            name = (String) e.getKey();
            // attributes with an underscore in their name are
            // calculated values that need not be saved unless the
            // "full" parameter is true.
            if (!full && name.indexOf('_') != -1) continue;

            v = e.getValue();
            if (v == null) continue;
            value = v.toString();
            if (value == null || value.length() == 0) continue;

            out.write(">\n");   // close previous tag
            out.write(indentation);
            out.write("  <"+ATTR_ELEM_NAME+" "+NAME_ATTR+"='");
            out.write(XMLUtils.escapeAttribute(name));
            out.write("' "+VALUE_ATTR+"='");
            out.write(XMLUtils.escapeAttribute(value));
            out.write("'/");
            wroteAttribute = true;
        }


        if (!wroteAttribute)
            // if this node had no attributes, we can get away with simply
            // closing the original <wbsNode> tag.
            out.write("/>\n");
        else {
            out.write(">\n");        // close final <attr> tag
            out.write(indentation);  // write the closing wbsNode tag.
            out.write("</"+ELEMENT_NAME+">\n");
        }
    }



    /** Make a copy of this WBSNode. */
    protected Object clone() {
        try {
            WBSNode result = (WBSNode) super.clone();

            // clone the attributes Map
            Map newAttributes = new HashMap(attributes.size());
            for (Iterator i = attributes.entrySet().iterator(); i.hasNext();) {
                Map.Entry e = (Map.Entry) i.next();
                String name = (String) e.getKey();
                if (getTransientAttrType(name) != TRANSIENT_ATTR)
                    newAttributes.put(name, e.getValue());
            }
            result.attributes = newAttributes;

            return result;
        } catch (CloneNotSupportedException cnse) {
            return null;        // can't happen?
        }
    }



    /**
     * Remove extraneous data attributes from this node.
     * 
     * @param attrsToKeep
     *            one or more objects describing attribute names that should be
     *            retained. Each object can be either (a) a String, (b) a
     *            Collection of Strings, or (c) a PatternList. If an attribute
     *            matches one of the provided tests, it will be retained; other
     *            attributes will be discarded.
     */
    public void discardAttributesExcept(Object... attrsToKeep) {
        Iterator i = attributes.keySet().iterator();
        while (i.hasNext()) {
            String attrName = (String) i.next();
            if (!attrNameMatchesTests(attrName, attrsToKeep))
                i.remove();
        }
    }
    private boolean attrNameMatchesTests(String attrName, Object[] tests) {
        for (Object t : tests)
            if (attrNameMatchesTest(attrName, t))
                return true;
        return false;
    }
    private boolean attrNameMatchesTest(String attrName, Object test) {
        if (test instanceof Collection) {
            return ((Collection) test).contains(attrName);
        } else if (test instanceof PatternList) {
            return ((PatternList) test).matches(attrName);
        } else if (test instanceof String) {
            return ((String) test).equals(attrName);
        } else if (test instanceof Object[]) {
            return attrNameMatchesTests(attrName, (Object[]) test);
        } else {
            return false;
        }
    }



    /** Remove calculated/derived/actual data attributes from this node.
     * 
     * @param discardActualData if true, attributes representing actual data
     *     will be discarded along with other transient attributes.  (If false,
     *     they will be retained.)
     */
    public void discardTransientAttributes(boolean discardActualData) {
        Iterator i = attributes.keySet().iterator();
        while (i.hasNext()) {
            String attrName = (String) i.next();
            switch (getTransientAttrType(attrName)) {

            case ACTUAL_DATA_ATTR:
                if (discardActualData == false)
                    break;
                // otherwise, fall through to the statement below, removing
                // the attribute

            case TRANSIENT_ATTR:
                i.remove();

            }
        }
    }

    /**
     * Return a copy of the map of attributes for this node.
     * 
     * @param discardTransientData
     *            true if transient data should not be included in the result
     * @param discardActualData
     *            true if actual data should not be included in the result
     * @return a copy of the node attributes, possibly excluding the attributes
     *         in question
     */
    public Map getAttributeMap(boolean discardTransientData,
            boolean discardActualData) {
        Map result = new HashMap(attributes.size());
        for (Iterator i = attributes.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String attrName = (String) e.getKey();
            int transientAttrType = getTransientAttrType(attrName);
            if (transientAttrType == ACTUAL_DATA_ATTR && discardActualData)
                continue;
            if (transientAttrType == TRANSIENT_ATTR && discardTransientData)
                continue;
            if (e.getValue() == null)
                continue;

            result.put(attrName, e.getValue());
        }
        return result;
    }

    private static final int NON_TRANSIENT_ATTR = 0;
    private static final int TRANSIENT_ATTR = 1;
    private static final int ACTUAL_DATA_ATTR = 2;
    private static final int getTransientAttrType(String attrName) {
        Integer result = TRANSIENT_ATTR_NAME_CACHE.get(attrName);
        if (result == null) {
            if (isActualDataAttribute(attrName))
                result = ACTUAL_DATA_ATTR;
            else if (isTransientAttribute(attrName))
                result = TRANSIENT_ATTR;
            else
                result = NON_TRANSIENT_ATTR;
            TRANSIENT_ATTR_NAME_CACHE.put(attrName, result);
        }
        return result;
    }
    private static final Map<String, Integer> TRANSIENT_ATTR_NAME_CACHE =
        new ConcurrentHashMap<String, Integer>(1000);

    private static boolean isTransientAttribute(String attrName) {
        return (attrName.indexOf('_') != -1);
    }
    private static boolean isActualDataAttribute(String attrName) {
        return (attrName.indexOf('@') != -1);
    }


    public boolean isEqualTo(WBSNode that) {
        return this.uniqueID == that.uniqueID
            && this.indentLevel == that.indentLevel
            && this.readOnly == that.readOnly
            && NullSafeObjectUtils.EQ(this.name, that.name)
            && NullSafeObjectUtils.EQ(this.type, that.type)
            && this.getAttributeMap(true, true).equals(
                    that.getAttributeMap(true, true));
    }

    /** Make a deep copy of a list of WBSNodes */
    public static List cloneNodeList(List nodesToCopy) {
        return cloneNodeList(nodesToCopy, null);
    }

    /** Make a deep copy of a list of WBSNodes, possibly changing the model */
    public static List cloneNodeList(List nodesToCopy, WBSModel newModel) {
        List result = new ArrayList();
        Iterator i = nodesToCopy.iterator();
        while (i.hasNext()) {
            WBSNode node = (WBSNode) ((WBSNode) i.next()).clone();
            if (newModel != null) node.wbsModel = newModel;
            result.add(node);
        }
        return result;
    }


    // constants used in creating/parsing XML
    public static final String ELEMENT_NAME = "wbsNode";
    private static final String NAME_ATTR = "name";
    private static final String ID_ATTR = "id";
    private static final String TYPE_ATTR = "type";
    private static final String READ_ONLY_ATTR = "readOnly";
    private static final String INDENT_ATTR = "indentLevel";
    private static final String EXPAND_ATTR = "expanded";
    private static final String ATTR_ELEM_NAME = "attr";
    private static final String VALUE_ATTR = "value";
    private static final String SPACES =
        "                                                            ";

    public static final String UNKNOWN_TYPE = "Unknown Task";
}
