// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.report;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.util.FileUtils;

public class ResourceBackupStream implements CollectionReport {

    public String getContentType() {
        return "application/zip";
    }

    public void runReport(ResourceCollection collection,
            List<String> resourceNames, OutputStream out) throws IOException {
        InputStream in = collection.getBackupInputStream();
        FileUtils.copyFile(in, out);
        in.close();
    }

    public static final ResourceBackupStream INSTANCE = new ResourceBackupStream();

}
