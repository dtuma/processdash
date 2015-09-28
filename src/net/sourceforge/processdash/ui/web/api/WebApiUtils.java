// Copyright (C) 2013-2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.api;

import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.Settings;

public class WebApiUtils {

    public static void checkRequestOrigin(Map env) {
        if (Settings.getBool("api.allowRemote", false))
            return;

        String requestUri = (String) env.get("SCRIPT_NAME");
        int pos = requestUri.lastIndexOf('/');
        String apiName = requestUri.substring(pos + 1);
        if (Settings.getBool("api." + apiName + ".allowRemote", false))
            return;

        try {
            DashController.checkIP(env.get("REMOTE_ADDR"));
        } catch (IOException e) {
            throw new WebApiException("unauthorized", 404, e.getMessage());
        }
    }

    public static void sendError(Writer out, WebApiException ex)
            throws IOException {
        out.write("Status: " + ex.getHttpCode() + " " + ex.getErrorCode() + "\r\n");
        out.write("Content-Type: application/json\r\n\r\n");
        out.write(ex.asJSON().toString());
        out.flush();
    }

    static final DateFormat DATE_FMT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");

}
