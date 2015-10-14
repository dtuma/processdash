// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import static teamdash.wbs.columns.CustomColumnManager.COLUMN_ID_ATTR;
import static teamdash.wbs.columns.CustomColumnManager.COLUMN_TAG;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;

import org.w3c.dom.Element;

import net.sourceforge.processdash.util.XMLUtils;

public class CustomColumnSpecs extends LinkedHashMap<String, Element> {

    public CustomColumnSpecs() {}

    public CustomColumnSpecs load(Element xml, boolean replaceExisting) {
        for (Element tag : XMLUtils.getChildElements(xml)) {
            if (COLUMN_TAG.equals(tag.getTagName())) {
                String id = tag.getAttribute(COLUMN_ID_ATTR);
                if (XMLUtils.hasValue(id))
                    if (replaceExisting || !containsKey(id))
                        put(id, tag);
            }
        }

        return this;
    }

    public void getAsXML(Writer out) throws IOException {
        out.write("<?xml version='1.1' encoding='UTF-8' standalone='yes' ?>");
        out.write("\n\n<" + COLUMN_SPECS_TAG + ">");
        for (Element columnTag : values()) {

            // Retrieve the XML text of the column tag. (The XMLUtils method
            // returns a full XML document including a prologue, which must
            // be discarded.)
            String xml = XMLUtils.getAsText(columnTag);
            int pos = xml.indexOf("<" + COLUMN_TAG);
            if (pos == -1)
                continue;
            xml = xml.substring(pos);

            // write the XML to the file
            out.write("\n\n    ");
            out.write(xml);
        }
        out.write("\n\n</" + COLUMN_SPECS_TAG + ">\n");
    }

    private static final String COLUMN_SPECS_TAG = "custom-wbs-columns";

}
