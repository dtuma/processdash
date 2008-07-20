// Copyright (C) 2005 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.mgr;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class AbstractInstruction implements Cloneable {

    protected static final Resources resource = Resources
            .getDashBundle("ImportExport");

    private boolean enabled = true;

    private Map attributes = new TreeMap();

    public AbstractInstruction() {
    }

    public AbstractInstruction(Element e) {
        readAttrsFromXML(e);
    }

    public void mergeXML(Element e) {
        readAttrsFromXML(e);
    }

    private void readAttrsFromXML(Element e) {
        NamedNodeMap attrs = e.getAttributes();
        if (attrs != null) {
            int len = attrs.getLength();
            for (int i = 0; i < len; i++) {
                Node n = attrs.item(i);
                if (n instanceof Attr) {
                    Attr a = (Attr) n;
                    if ("enabled".equals(a.getName()))
                        enabled = "true".equalsIgnoreCase(a.getValue());
                    else
                        attributes.put(a.getName(), a.getValue());
                }
            }
        }
    }

    public abstract String getDescription();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAttribute(String name) {
        return (String) attributes.get(name);
    }

    public void setAttribute(String name, String value) {
        if (value == null)
            attributes.remove(name);
        else
            attributes.put(name, value);
    }

    public abstract String getXmlTagName();

    public void getAsXML(StringBuffer out) {
        out.append("<").append(getXmlTagName());
        if (!enabled)
            out.append(" enabled='false'");
        for (Iterator iter = attributes.entrySet().iterator(); iter.hasNext();) {
            Map.Entry e = (Map.Entry) iter.next();
            out.append(" ").append(e.getKey()).append("='").append(
                    XMLUtils.escapeAttribute(e.getValue().toString())).append(
                    "'");
        }
        if (hasChildXMLContent()) {
            out.append(">");
            getChildXMLContent(out);
            out.append("</").append(getXmlTagName()).append(">");
        } else {
            out.append("/>");
        }
    }

    protected boolean hasChildXMLContent() {
        return false;
    }

    protected void getChildXMLContent(StringBuffer out) {
    }

    protected void loadListFromXML(Element e, List list, String tagName) {
        NodeList elements = e.getElementsByTagName(tagName);
        for (int i=0;  i < elements.getLength();  i++) {
            Element child = (Element) elements.item(i);
            list.add(XMLUtils.getTextContents(child));
        }
    }

    protected void getListAsXML(StringBuffer out, List list, String listTag,
            String listItemTag) {
        out.append("<").append(listTag).append(">");
        getListItemsAsXML(out, list, listItemTag);
        out.append("</").append(listTag).append(">");
    }

    protected void getListItemsAsXML(StringBuffer out, List list, String listItemTag) {
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            String listItem = (String) iter.next();
            out.append("<").append(listItemTag).append(">")
                    .append(XMLUtils.escapeAttribute(listItem))
                    .append("</").append(listItemTag).append(">");
        }
    }

    public Object dispatch(ImportInstructionDispatcher dispatcher) {
        throw new UnsupportedOperationException();
    }

    public Object dispatch(ExportInstructionDispatcher dispatcher) {
        throw new UnsupportedOperationException();
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null)
            return false;
        if (this.getClass() != obj.getClass())
            return false;

        AbstractInstruction that = (AbstractInstruction) obj;
        return this.attributes.equals(that.attributes);
    }

    public int hashCode() {
        return attributes.hashCode() << 1;
    }

    public String toString() {
        return getXmlTagName() + " " + attributes;
    }

    public Object clone() {
        try {

            AbstractInstruction result = (AbstractInstruction) super.clone();
            result.attributes = new TreeMap(attributes);
            return result;
        } catch (CloneNotSupportedException e) {
            // can't happen
            return null;
        }
    }
}
