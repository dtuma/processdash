// Copyright (C) 2012-2013 Tuma Solutions, LLC
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
        WbsNodeAttributeSource {

    private static final String VALUE_SUFFIX = "-CustomText";

    private boolean inherits;

    private Set<String> allowedValues;

    public CustomTextColumn(DataTableModel dataModel, String id, String name,
            Element xml) {

        String safeId = id.replace('_', ' ').replace('@', ' ');
        String attrName = safeId + VALUE_SUFFIX;

        String labelPrefix = xml.getAttribute("syncAsLabel");
        if (labelPrefix == null || labelPrefix.length() == 0)
            labelPrefix = null;
        else if ("true".equals(labelPrefix))
            labelPrefix = "";
        else
            labelPrefix = labelPrefix + ":";

        int width = XMLUtils.getXMLInt(xml, "width");
        boolean multivalued = test(xml, "multivalued", false);
        inherits = test(xml, "inherit", true);
        boolean autocomplete = test(xml, "autocomplete", true);

        allowedValues = parseAllowedValues(xml);

        String separatorRegexp = multivalued ? "," : null;

        init(dataModel, id, name, width, attrName, separatorRegexp,
            labelPrefix, inherits, autocomplete);
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

    public String getAttributeId() {
        return columnID;
    }

    public String getAttributeName() {
        return columnName;
    }

    public boolean isAttributeAutoInherited() {
        return inherits;
    }

    public List<String> getAttributeValues(WBSNode node) {
        String nodeValue = (String) WrappedValue.unwrap(getValueAt(node));
        List<String> values = getValues(nodeValue, false);
        return values;
    }

}
