
package teamdash.wbs;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pspdash.XMLUtils;


public class WBSNode implements Cloneable {

    private WBSModel wbsModel;

    public WBSNode(WBSModel model,
                   String name, String type, int level, boolean expanded) {
        this.wbsModel = model;
        setName(name);
        setType(type);
        setIndentLevel(level);
        setExpanded(expanded);
    }

    public WBSNode(Element e) {
        setName(e.getAttribute(NAME_ATTR));
        setType(e.getAttribute(TYPE_ATTR));
        setIndentLevel(XMLUtils.getXMLInt(e, INDENT_ATTR));
        setExpanded(XMLUtils.hasValue(e.getAttribute(EXPAND_ATTR)));

        NodeList nodeAttributes = e.getElementsByTagName(ATTR_ELEM_NAME);
        int len = nodeAttributes.getLength();
        for (int i = 0;   i < len;   i++)
            setXMLAttribute((Element) nodeAttributes.item(i));
    }


    private String name;

    /** Get the name of this node.
     * @return The name of this node.  */
    public String getName() { return name; }

    /** Set the name of this node.
     * @param newName the new name for this node.  */
    public void setName(String newName) {
        this.name = newName;
        //fireNodeChanged(new WBSNodeEvent(this, WBSNodeEvent.NAME_CHANGE));
    }



    private String type;

    /** Get the type of this node.
     * @return the type of this node.  */
    public String getType() { return type; }

    /** Set the type of this node
     * @param newType the new type for this node.  */
    public void setType(String newType) {
        this.type = newType;
        //fireNodeChanged(new WBSNodeEvent(this, WBSNodeEvent.TYPE_CHANGE));
    }



    private int indentLevel;

    /** Get the indentation level of this node.
     * @return the indentation level of this node.  */
    public int getIndentLevel() { return indentLevel; }

    /** Set the indentation level of this node.
     * @param newLevel the new indentation level for this node.  */
    public void setIndentLevel(int newLevel) { this.indentLevel = newLevel; }



    private boolean expanded;

    /** Returns true if this node is currently expanded.
     * @return true if this node is expanded.  */
    public boolean isExpanded() { return expanded; }

    /** Expand or collapse this node.
     * @param expanded <code>true</code> to expand this node,
     *    <code>false</code> to collapse this node.  */
    public void setExpanded(boolean expanded) { this.expanded = expanded; }


    private Map attributes = new HashMap();

    public Object getAttribute(String attrName) {
        return attributes.get(attrName);
    }
    public void setAttribute(String attrName, Object value) {
        attributes.put(attrName, value);
    }

    public double getNumericAttribute(String attrName) {
        Object value = getAttribute(attrName);
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        else if (value instanceof String) try {
            return Double.parseDouble((String) value);
        } catch (NumberFormatException nfe) {}

        return Double.NaN;
    }
    public void setNumericAttribute(String attrName, double value) {
        setAttribute(attrName, new Double(value));
    }

    public void setXMLAttribute(Element attrElement) {
        String name = attrElement.getAttribute(NAME_ATTR);
        String value = attrElement.getAttribute(VALUE_ATTR);
        setAttribute(name, value);
    }

    public void getAsXML(Writer out) throws IOException {
        // write the opening wbsNode tag.
        out.write("<"+ELEMENT_NAME+" "+NAME_ATTR+"='");
        out.write(XMLUtils.escapeAttribute(getName()));
        if (getType() != null) {
            out.write("' "+TYPE_ATTR+"='");
            out.write(XMLUtils.escapeAttribute(getType()));
        }
        out.write("' "+INDENT_ATTR+"='");
        out.write(Integer.toString(getIndentLevel()));
        if (isExpanded()) out.write("' "+EXPAND_ATTR+"='true");
        out.write("'>");

        // write out a tag for each attribute.
        Iterator i = attributes.entrySet().iterator();
        Map.Entry e;
        Object v;
        String name, value;
        while (i.hasNext()) {
            e = (Map.Entry) i.next();
            name = (String) e.getKey();
            // attributes with an underscore in their name are
            // calculated values that need not be saved.
            if (name.indexOf('_') != -1) continue;

            v = e.getValue();
            if (v == null) continue;
            value = v.toString();

            out.write("<"+ATTR_ELEM_NAME+" "+NAME_ATTR+"='");
            out.write(XMLUtils.escapeAttribute(name));
            out.write("' "+VALUE_ATTR+"='");
            out.write(XMLUtils.escapeAttribute(value));
            out.write("'/>");
        }

        // write the closing wbsNode tag.
        out.write("</"+ELEMENT_NAME+">");
    }

    protected Object clone() {
        try {
            WBSNode result = (WBSNode) super.clone();
            result.attributes = (Map) ((HashMap) result.attributes).clone();
            return result;
        } catch (CloneNotSupportedException cnse) {
            return null;        // can't happen?
        }
    }

    /*
    public void fireNodeChanged(WBSNodeEvent e) {
        wbsModel.fireNodeChanged(e);
        wbsModel.fireTableDataChanged();//
    }
    */

    public static final String ELEMENT_NAME = "wbsNode";
    private static final String NAME_ATTR = "name";
    private static final String TYPE_ATTR = "type";
    private static final String INDENT_ATTR = "indentLevel";
    private static final String EXPAND_ATTR = "expanded";
    private static final String ATTR_ELEM_NAME = "attr";
    private static final String VALUE_ATTR = "value";
}
