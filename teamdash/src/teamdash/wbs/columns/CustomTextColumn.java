// Copyright (C) 2012-2022 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.util.XMLUtils;

import teamdash.wbs.DataTableModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WrappedValue;

public class CustomTextColumn extends AbstractLabelColumn implements
        CustomColumn, WbsNodeAttributeSource {

    static final String TYPE = "TextColumn";

    private static final String VALUE_SUFFIX = "-CustomText";

    boolean multivalued, inherits, autocomplete;

    Set<String> allowedValues;

    public CustomTextColumn(DataTableModel dataModel, String id, String name,
            Element xml) {

        String safeId = id.replace('_', ' ').replace('@', ' ');
        String attrName = safeId + VALUE_SUFFIX;

        String labelPrefix = xml.getAttribute(CustomColumnManager.SYNC_AS_LABEL);
        if (labelPrefix == null || labelPrefix.length() == 0)
            labelPrefix = null;
        else if ("true".equals(labelPrefix))
            labelPrefix = "";
        else
            labelPrefix = labelPrefix + ":";

        int width = XMLUtils.getXMLInt(xml, CustomColumnManager.COL_WIDTH);
        multivalued = test(xml, "multivalued", false);
        inherits = test(xml, "inherit", true);
        autocomplete = test(xml, "autocomplete", true);

        allowedValues = parseAllowedValues(xml);

        init(dataModel, id, name, width, attrName, multivalued,
            labelPrefix, inherits, autocomplete, isConstrainedValues());
    }

    public CustomTextColumn(DataTableModel dataModel, String id, String name,
            boolean autocomplete, boolean multivalued, boolean inherits,
            Set<String> allowedValues, String labelPrefix) {
        String safeId = id.replace('_', ' ').replace('@', ' ');
        String attrName = safeId + VALUE_SUFFIX;

        if (labelPrefix != null && labelPrefix.length() > 0)
            labelPrefix = labelPrefix + ":";

        this.multivalued = multivalued;
        this.inherits = inherits;
        this.autocomplete = autocomplete;
        this.allowedValues = allowedValues;

        init(dataModel, id, name, -1, attrName, multivalued, labelPrefix,
            inherits, autocomplete, isConstrainedValues());
    }

    private boolean test(Element xml, String attrName, boolean defaultVal) {
        String val = xml.getAttribute(attrName);
        if (val.length() > 0)
            return "true".equals(val);
        else
            return defaultVal;
    }

    private Set<String> parseAllowedValues(Element xml) {
        NodeList valueTags = xml.getElementsByTagName("value");
        if (valueTags == null || valueTags.getLength() == 0)
            return null;

        Set<String> result = new LinkedHashSet<String>();
        for (int i = 0; i < valueTags.getLength(); i++) {
            Element oneTag = (Element) valueTags.item(i);
            result.add(XMLUtils.getTextContents(oneTag));
        }
        return result;
    }

    public void getAsXml(PrintWriter out) {
        out.write("<" + CustomColumnManager.COLUMN_TAG);
        writeAttr(out, CustomColumnManager.COLUMN_ID_ATTR, columnID);
        writeAttr(out, CustomColumnManager.COLUMN_TYPE_ATTR, TYPE);
        writeAttr(out, CustomColumnManager.COLUMN_NAME_ATTR, columnName);

        // write label sync info
        if ("".equals(labelPrefix))
            writeAttr(out, CustomColumnManager.SYNC_AS_LABEL, "true");
        else if (labelPrefix != null)
            writeAttr(out, CustomColumnManager.SYNC_AS_LABEL,
                labelPrefix.substring(0, labelPrefix.length() - 1));

        // write preferred width
        if (preferredWidth > 0)
            writeAttr(out, CustomColumnManager.COL_WIDTH,
                Integer.toString(preferredWidth));

        // write text completion info
        writeAttr(out, "multivalued", Boolean.toString(multivalued));
        writeAttr(out, "inherit", Boolean.toString(inherits));
        writeAttr(out, "autocomplete", Boolean.toString(autocomplete));

        // write allowed values
        if (allowedValues == null) {
            out.write("/>");
        } else {
            out.write(">\n");
            for (String value : allowedValues) {
                out.write("        <value>");
                out.write(XMLUtils.escapeAttribute(value));
                out.write("</value>\n");
            }
            out.write("</" + CustomColumnManager.COLUMN_TAG + ">");
        }
    }

    private void writeAttr(PrintWriter out, String attr, String value) {
        if (XMLUtils.hasValue(value)) {
            out.write(" ");
            out.write(attr);
            out.write("='");
            out.write(XMLUtils.escapeAttribute(value));
            out.write("'");
        }
    }

    @Override
    protected String normalizeValue(String s) {
        if (allowedValues != null) {
            for (String v : allowedValues)
                if (v.equalsIgnoreCase(s))
                    return v;
            return null;
        } else {
            return super.normalizeValue(s);
        }
    }

    @Override
    protected Set<String> getAutocompleteValues() {
        if (allowedValues != null)
            return allowedValues;
        else
            return getValuesInUse();
    }

    public boolean isMultivalued() {
        return multivalued;
    }

    public boolean isConstrainedValues() {
        return allowedValues != null;
    }

    public String getAttributeId() {
        return columnID;
    }

    public String getAttributeName() {
        return columnName;
    }

    public boolean isAttributeAutoInherited() {
        return inherits;
    }

    public List<String> getAllowedValues(boolean asLabels) {
        if (allowedValues == null)
            return null;

        if (asLabels == false)
            return new ArrayList<String>(allowedValues);

        List<String> result = new ArrayList<String>();
        for (String oneValue : allowedValues)
            result.add(convertToLabel(labelPrefix + oneValue));
        return result;
    }

    public List<String> getAttributeValues(WBSNode node) {
        String nodeValue = (String) WrappedValue.unwrap(getValueAt(node));
        List<String> values = getValues(nodeValue, false);
        return values;
    }

}
