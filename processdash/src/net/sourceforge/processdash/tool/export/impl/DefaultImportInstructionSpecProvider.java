// Copyright (C) 2013 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.SimplePrefixLocalDataNameFilter;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.tool.export.mgr.ImportInstructionSpecProvider;
import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.XMLUtils;

public class DefaultImportInstructionSpecProvider implements
        ImportInstructionSpecProvider {

    private DataRepository data;

    private Map<String, Element> cache;

    public DefaultImportInstructionSpecProvider(DataRepository data) {
        this.data = data;
        this.cache = Collections.synchronizedMap(new HashMap());
    }

    public Element getImportInstructionSpec(String id) {
        Element result = cache.get(id);
        if (result == null && id != null && !cache.containsKey(id)) {
            scanForInstructions();
            result = cache.get(id);
            if (result == null)
                cache.put(id, null);
        }
        return result;
    }

    private void scanForInstructions() {
        Iterator keys = data.getKeys(null, new SimplePrefixLocalDataNameFilter(
                new PatternList(INSTRUCTIONS_DATANAME)));
        while (keys.hasNext()) {
            String dataName = (String) keys.next();
            if (!dataName.endsWith("/" + INSTRUCTIONS_DATANAME))
                continue;

            ListData l = ListData.asListData(data.getSimpleValue(dataName));
            if (l == null || !l.test())
                continue;

            for (int i = 1; i < l.size(); i += 2) {
                String id = l.get(i - 1).toString();
                if (cache.containsKey(id))
                    continue;

                String spec = l.get(i).toString();
                Element specXml;
                try {
                    specXml = parseSpec(spec).getDocumentElement();
                } catch (Exception e) {
                    specXml = null;
                }

                cache.put(id, specXml);
            }
        }
    }

    private Document parseSpec(String spec) throws IOException, SAXException {
        if (spec.startsWith("file:")) {
            String uri = spec.substring(5);
            String url;
            if (uri.startsWith("/"))
                url = WebServer.DASHBOARD_PROTOCOL + ":" + uri;
            else
                url = WebServer.DASHBOARD_PROTOCOL + ":/" + uri;
            return XMLUtils.parse(new URL(url).openStream());
        } else {
            return XMLUtils.parse(spec);
        }
    }

    private static final String INSTRUCTIONS_DATANAME = "IMPORT_FILE/Instructions";

}
