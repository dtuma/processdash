// Copyright (C) 2007 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.impl;

import java.io.InputStream;

import net.sourceforge.processdash.log.time.ImportedTimeLogManager;

public class TimeLogImporterXMLv1 implements
        ArchiveMetricsFileImporter.Handler, ArchiveMetricsXmlConstants {

    public boolean canHandle(String type, String version) {
        return FILE_TYPE_TIME_LOG.equals(type) && "1".equals(version);
    }

    public void handle(ArchiveMetricsFileImporter caller, InputStream in,
            String type, String version) throws Exception {
        String prefix = caller.getPrefix();
        ImportedTimeLogManager.getInstance().importTimeLog(prefix, in);
    }

}
