// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.process;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class DataNameScriptSource implements ScriptSource {

    private String id;

    private String dataName;

    private DataContext data;

    public void setConfigElement(Element xml, String attrName) {
        dataName = xml.getAttribute("dataName");
        if (!StringUtils.hasValue(dataName))
            throw new IllegalArgumentException("dataName must be specified");

        id = xml.getAttribute("id");
        if (!StringUtils.hasValue(id))
            id = "DataNameScriptSource." + dataName;
    }

    public void setDashboardContext(DashboardContext ctx) {
        this.data = ctx.getData();
    }

    public String getUniqueID() {
        return id;
    }

    public List<ScriptID> getScripts(String path) {
        List<ScriptID> result = null;

        while (path != null) {
            String fullDataName = DataRepository.createDataName(path, dataName);
            ListData l = ListData.asListData(data.getSimpleValue(fullDataName));
            if (l != null && l.test()) {
                if (result == null)
                    result = new ArrayList<ScriptID>();

                for (int i = 0; i < l.size(); i++)
                    addScriptItem(result, l.get(i), path);
            }

            path = DataRepository.chopPath(path);
        }

        return result;
    }

    private void addScriptItem(List<ScriptID> dest, Object item, String path) {
        if (item instanceof SimpleData)
            addScriptItem(dest, ((SimpleData) item).format(), path);
        else if (item != null)
            addScriptItem(dest, item.toString(), path);
    }

    private void addScriptItem(List<ScriptID> dest, String item, String path) {
        if (item.indexOf('\n') == -1) {
            addScriptUri(dest, item, path);
        } else {
            for (String uri : item.split("\n"))
                addScriptUri(dest, uri, path);
        }
    }

    private void addScriptUri(List<ScriptID> dest, String uri, String path) {
        // Check for URIs that are only whitespace, and do nothing.
        uri = uri.replace('\t', ' ').trim();
        if (!StringUtils.hasValue(uri))
            return;

        // Next: check to see if the URI is of the form "URL <space> NAME".
        // If so, divide on the space to obtain a URL and a display name.
        String displayName = null;
        int spacePos = uri.indexOf(' ');
        if (spacePos != -1) {
            displayName = uri.substring(spacePos + 1).trim();
            uri = uri.substring(0, spacePos).trim();
        }

        // Finally, add the URI to the list.
        dest.add(new ScriptID(uri, path, displayName));
    }

}
