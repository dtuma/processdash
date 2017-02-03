// Copyright (C) 2017 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class WBSPermission {

    private String id;

    private Map<String, String> params;


    public WBSPermission(String id) {
        if (id == null)
            throw new NullPointerException();

        this.id = id;
        this.params = Collections.EMPTY_MAP;
    }


    public WBSPermission(Element xml) {
        this.id = xml.getAttribute("id");

        NodeList nl = xml.getElementsByTagName("param");
        if (nl.getLength() == 0) {
            this.params = Collections.EMPTY_MAP;
        } else {
            Map<String, String> params = new HashMap<String, String>();
            for (int i = 0; i < nl.getLength(); i++) {
                Element p = (Element) nl.item(i);
                String name = p.getAttribute("name");
                String value = p.getAttribute("value");
                params.put(name, value);
            }
            this.params = Collections.unmodifiableMap(params);
        }
    }


    public String getId() {
        return id;
    }


    public Map<String, String> getParams() {
        return params;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof WBSPermission) {
            WBSPermission that = (WBSPermission) obj;
            return this.id.equals(that.id) //
                    && this.params.equals(that.params);
        } else {
            return false;
        }
    }


    @Override
    public int hashCode() {
        return id.hashCode() * 37 ^ params.hashCode();
    }


    @Override
    public String toString() {
        return "WBSPermission[" + id + "]" + (params.isEmpty() ? "" : params);
    }

}
