// Copyright (C) 2006-2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cms;

import java.io.File;
import java.util.Map;

/** Factory for managing current service implementations.
 */
public class CmsDefaultConfig {

    private static PersistenceService SERVICE = null;

    public static void setPersistenceDirectories(Map<String, File> dirs) {
        DelegatingPersistenceService service = new DelegatingPersistenceService();

        File defaultDir = dirs.remove(null);
        for (Map.Entry<String, File> e : dirs.entrySet()) {
            String qualifier = e.getKey();
            File dir = e.getValue();
            service.add(new FilePersistenceService(qualifier, dir));
        }
        service.add(new FilePersistenceService(null, defaultDir));

        TemplatePersistenceService ts = new TemplatePersistenceService();
        service.add(ts);

        SERVICE = service;
    }

    public static PersistenceService getPersistence() {
        return SERVICE;
    }


    private static ContentSerializer SERIALIZER = new XmlSnippetContentSerializer();

    public static ContentSerializer getSerializer() {
        return SERIALIZER;
    }
}
