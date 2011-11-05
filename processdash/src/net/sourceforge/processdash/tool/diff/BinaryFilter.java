// Copyright (C) 2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class BinaryFilter implements LanguageFilter,
        AbstractLanguageFilter.NamedFilter {

    public String getFilterName() {
        return AbstractLanguageFilter.resources.getString("Report.Binary");
    }

    public String[][] getOptions() {
        return null;
    }

    public void highlightSyntax(StringBuffer file) {}

    public boolean isSignificant(String line) {
        return false;
    }

    public int languageMatches(String f, String c, String o) {
        return 0;
    }

    public void service(InputStream in, OutputStream out, Map env)
            throws IOException {}

    public void setCharset(String charset) {}

    public static final BinaryFilter INSTANCE = new BinaryFilter();

}
