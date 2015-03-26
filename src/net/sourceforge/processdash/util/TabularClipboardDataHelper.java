// Copyright (C) 2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabularClipboardDataHelper {

    public static List<List<String>> getTabularDataFromClipboard() {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        DataFlavor flavors[] = cb.getAvailableDataFlavors();
        if (flavors == null)
            return null;

        DataFlavor html = searchForFlavor(flavors, "text/html",
            "java.lang.String");
        if (html != null) {
            try {
                String data = (String) cb.getData(html);
                return HtmlTableParser.parseTable(data, true);
            } catch (Exception e) {
                return null;
            }
        }

        DataFlavor text = searchForFlavor(flavors, "text/plain",
            "java.lang.String");
        if (text != null) {
            try {
                String data = (String) cb.getData(text);
                if (HtmlTableParser.containsHtmlTableData(data))
                    return HtmlTableParser.parseTable(data, true);
                else
                    return parseTabDelimitedData(data);
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    private static DataFlavor searchForFlavor(DataFlavor[] flavors,
            String... patterns) {
        for (DataFlavor f : flavors)
            if (containsPatterns(f.getMimeType(), patterns))
                return f;
        return null;
    }

    private static boolean containsPatterns(String mimeType, String[] patterns) {
        for (String p : patterns)
            if (!mimeType.contains(p))
                return false;

        return true;
    }

    private static List<List<String>> parseTabDelimitedData(String data) {
        if (data == null || data.length() == 0)
            return null;

        List<List<String>> result = new ArrayList<List<String>>();
        String[] lines = data.split("[\r\n]+");
        for (String line : lines) {
            if (line != null && line.length() > 0) {
                List<String> cells = new ArrayList<String>(Arrays.asList(line
                        .split("\t")));
                result.add(cells);
            }
        }
        return result;
    }

}
