// Copyright (C) 2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.mgr;

import org.w3c.dom.Element;

public class ExportFileEntry {


    private static final String NAME_ATTR = "name";

    private static final String TYPE_ATTR = "type";

    private static final String VERSION_ATTR = "version";

    private static final String HREF_ATTR = "href";

    private String filename;

    private String type;

    private String version;

    private String href;

    public ExportFileEntry(Element e) {
        this(e.getAttribute(NAME_ATTR), e.getAttribute(TYPE_ATTR), //
                e.getAttribute(VERSION_ATTR), e.getAttribute(HREF_ATTR));
    }

    public ExportFileEntry(String filename, String type, String version,
            String href) {
        this.filename = filename;
        this.type = type;
        this.version = version;
        this.href = href;
    }

    public String getFilename() {
        return filename;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getHref() {
        return href;
    }


}
