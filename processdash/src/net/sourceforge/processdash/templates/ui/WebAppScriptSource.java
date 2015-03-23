// Copyright (C) 2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.templates.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.process.ScriptSource;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.util.XMLUtils;

public class WebAppScriptSource implements ScriptSource {

    private DataRepository data;

    public void setDashboardContext(DashboardContext ctx) {
        this.data = ctx.getData();

        for (Element xml : ExtensionManager
                .getXmlConfigurationElements("script-item"))
            SCRIPT_ITEMS.add(new ItemDefinition(xml));
    }

    public String getUniqueID() {
        return WebAppScriptSource.class.getName();
    }

    public List<ScriptID> getScripts(String path) {
        List<ScriptID> result = new ArrayList<ScriptID>();

        while (path != null) {
            DataContext ctx = data.getSubcontext(path);
            for (ItemDefinition item : SCRIPT_ITEMS)
                if (item.test(ctx))
                    result.add(item.getScriptID(path));

            path = DataRepository.chopPath(path);
        }

        return result;
    }

    private static final Set<ItemDefinition> SCRIPT_ITEMS = Collections
            .synchronizedSet(new HashSet<ItemDefinition>());

    private class ItemDefinition {

        String displayName;

        String uri;

        Set<String> contexts;

        public ItemDefinition(Element xml) {
            contexts = new HashSet();
            for (Element e : XMLUtils.getChildElements(xml)) {
                String tagName = e.getTagName();
                if ("display-name".equals(tagName))
                    displayName = XMLUtils.getTextContents(e).trim();
                else if ("uri".equals(tagName))
                    uri = ExtensionManager.getConfigUri(e);
                else if ("context".equals(tagName))
                    contexts.add(XMLUtils.getTextContents(e).trim());
            }

            if (!XMLUtils.hasValue(uri))
                contexts.clear();
        }

        boolean test(DataContext data) {
            for (String c : contexts) {
                SimpleData value = data.getSimpleValue(c);
                if (value != null && value.test())
                    return true;
            }
            return false;
        }

        ScriptID getScriptID(String path) {
            return new ScriptID(uri, path, displayName);
        }
    }

}
