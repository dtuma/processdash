
package teamdash.wbs;

import java.util.HashMap;
import java.util.Map;

public class WBSNode {

    private WBSModel wbsModel;

    public WBSNode(WBSModel model,
                   String name, String type, int level, boolean expanded) {
        this.wbsModel = model;
        setName(name);
        setType(type);
        setIndentLevel(level);
        setExpanded(expanded);
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
    /*
    public void fireNodeChanged(WBSNodeEvent e) {
        wbsModel.fireNodeChanged(e);
        wbsModel.fireTableDataChanged();//
    }
    */
}
