// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact.filter;

import java.util.List;

import org.w3c.dom.Element;

import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.tool.redact.RedactFilterData;
import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.XMLUtils;

public class FilterCensoredData implements DataFileEntryFilter {

    private RedactFilterData data;

    private PatternList censoredDataElements;

    public void afterPropertiesSet() {
        censoredDataElements = new PatternList();

        List<Element> cfgXml = ExtensionManager
                .getXmlConfigurationElements("redact-filter-censored-data");
        for (Element xml : cfgXml)
            if (isEnabled(xml))
                parseConfig(xml);
    }

    private boolean isEnabled(Element xml) {
        String enabledFor = xml.getAttribute("forFilterSets");
        if (!XMLUtils.hasValue(enabledFor))
            return true;

        for (String filterId : enabledFor.split(","))
            if (data.isFiltering(filterId.trim()))
                return true;

        return false;
    }

    private void parseConfig(Element xml) {
        String content = XMLUtils.getTextContents(xml);
        if (content == null)
            return;

        for (String item : content.trim().split("[\r\n]+")) {
            item = item.trim();
            if (item.length() == 0)
                continue;

            if (item.startsWith("\""))
                item = item.substring(1);
            if (item.endsWith("\""))
                item = item.substring(0, item.length()-1);

            censoredDataElements.addRegexp(item);
        }
    }

    public void filter(DataFileEntry e) {
        if (censoredDataElements.matches(e.getKey()))
            e.setKey(null);
    }

}
