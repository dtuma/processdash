// Copyright (C) 2006 Tuma Solutions, LLC
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

import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;

public class HttpQueryParser {

    public void parse(Map parameters, String query) throws IOException {
        if (query == null || query.length() == 0) return;

        String delim = "&";
        boolean urlDecode = true;
        if (query.indexOf('\n') != -1) {
            delim = "\r\n";
            urlDecode = false;
        }
        StringTokenizer params = new StringTokenizer(query, delim);
        String param, name, val;
        int equalsPos;
        while (params.hasMoreTokens()) {
            param = params.nextToken();
            equalsPos = param.indexOf('=');
            if (equalsPos == 0 || param.length() == 0)
                continue;
            else if (equalsPos == -1)
                parameters.put(HTMLUtils.urlDecode(param), Boolean.TRUE);
            else try {
                name = HTMLUtils.urlDecode(param.substring(0, equalsPos));
                val = param.substring(equalsPos+1);
                // skip URL decoding if the value begins with "=".  This
                // saves us from having to URL-encode complex expressions
                // in query files.
                if (val.startsWith("=")) val = val.substring(1);
                else if (urlDecode) val = HTMLUtils.urlDecode(val);

                if (requiresSpecialHandling(parameters, name, val) == false) {
                    val = frobValue(val);
                    putParam(parameters, name, val);
                }
            } catch (Exception e) {
                System.err.println("Malformed query parameter: " + param);
            }
        }
    }
    protected void putParam(Map parameters, String name, String val) {
        parameters.put(name, val);
        name = name + "_ALL";
        parameters.put
            (name, append((String[]) parameters.get(name), val));
    }

    protected String[] append(String [] array, String element) {
        String [] result;
        result = new String[array == null ? 1 : array.length + 1];
        if (array != null)
            System.arraycopy(array, 0, result, 0, array.length);
        result[result.length-1] = element;
        return result;
    }

    protected String frobValue(String value) {
        return value;
    }
    protected boolean requiresSpecialHandling(Map dest, String name, String val) {
        return false;
    }
}
