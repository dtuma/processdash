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

package net.sourceforge.processdash.net.cms;

import java.io.File;

/** Factory for managing current service implementations.
 */
public class CmsDefaultConfig {

    private static PersistenceService SERVICE = null;

    public static void setPersistenceDirectory(File dir) {
        File cmsDir = new File(dir, "cms");
        FilePersistenceService fs = new FilePersistenceService(cmsDir);

        TemplatePersistenceService ts = new TemplatePersistenceService();

        SERVICE = new DelegatingPersistenceService().add(fs).add(ts);
    }

    public static PersistenceService getPersistence() {
        return SERVICE;
    }


    private static ContentSerializer SERIALIZER = new XmlSnippetContentSerializer();

    public static ContentSerializer getSerializer() {
        return SERIALIZER;
    }
}
