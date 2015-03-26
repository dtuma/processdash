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

package net.sourceforge.processdash.tool.redact;

import net.sourceforge.processdash.util.StringMapper;

public class LocationMapper {

    public static final String FAKE_TEAM_DIR = "teamdir";

    public static String mapFilename(String filename) {
        if (filename == null || filename.trim().length() == 0)
            return filename;
        filename = filename.replace('\\', '/');
        if ("./import".equals(filename))
            return filename;
        int dataPos = filename.indexOf("/data/");
        if (dataPos == -1)
            return FAKE_TEAM_DIR;
        else
            return FAKE_TEAM_DIR + filename.substring(dataPos);
    }

    public static final StringMapper FILE_MAPPER = new StringMapper() {
        public String getString(String str) {
            return mapFilename(str);
        }
    };

    public static String mapURL(String url) {
        if (url == null || url.trim().length() == 0)
            return url;
        int pos = url.indexOf("/DataBridge/");
        if (pos == -1)
            return null;
        else
            return "http://redacted.invalid" + url.substring(pos);
    }

    public static final StringMapper URL_MAPPER = new StringMapper() {
        public String getString(String str) {
            return mapURL(str);
        }
    };

}
