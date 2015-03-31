// Copyright (C) 2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.http;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Locale;

import net.sourceforge.processdash.team.mcf.MCFManager;
import net.sourceforge.processdash.util.FastDateFormat;

public class MCFURLConnection extends DashboardURLConnection {

    public static final String PROTOCOL = WebServer.DASHBOARD_PROTOCOL + "-mcf";


    protected MCFURLConnection(URL url) {
        super(null, url);
    }

    @Override
    public void connect() throws IOException {
        if (!connected) {
            String url = getURL().getPath();
            if (url.startsWith("/"))
                url = url.substring(1);
            int slashPos = url.indexOf('/');
            if (slashPos == -1)
                throw new FileNotFoundException(getURL().toString());

            String mcfId = url.substring(0, slashPos);
            String path = "/Templates/" + url;

            byte[] data = MCFManager.getInstance().getMcfContent(mcfId, path);
            if (data == null)
                throw new FileNotFoundException(getURL().toString());

            Date modTimestamp = MCFManager.getInstance().getMcfTimestamp(mcfId);

            String[] headers = new String[] { //
                    "Content-Length: " + data.length, //
                    "Last-Modified: " + DATE_FMT.format(modTimestamp), //
            };
            InputStream response = new ByteArrayInputStream(data);
            setConnectionData(headers, response);
        }
    }

    private static final FastDateFormat DATE_FMT = FastDateFormat.getInstance(
        "EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

}
