// Copyright (C) 2008-2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.tool.export.DataExtractionScaffold;
import net.sourceforge.processdash.util.DataStreamCache;
import net.sourceforge.processdash.util.FileUtils;

public class PngCache extends TinyCGIBase {

    private static final boolean USE_DISK_CACHE = !Settings.getBool(
        DataExtractionScaffold.SCAFFOLD_MODE_SETTING, false);

    private static final DataStreamCache PNG_CACHE = new DataStreamCache(
            USE_DISK_CACHE);

    static OutputStream getOutputStream() {
        return PNG_CACHE.getOutputStream();
    }

    @Override
    protected void doGet() throws IOException {
        int streamID = Integer.parseInt(getParameter("id"));
        InputStream pngData = PngCache.PNG_CACHE.getInputStream(streamID);
        if (pngData == null)
            throw new TinyCGIException(HttpURLConnection.HTTP_NOT_FOUND,
                    "Not Found", "Not Found");

        out.print("Content-type: image/png\r\n\r\n");
        out.flush();

        FileUtils.copyFile(pngData, outStream);
        outStream.flush();
        outStream.close();
    }

}
