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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package teamdash.hist;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import net.sourceforge.processdash.tool.bridge.ResourceBridgeConstants;
import net.sourceforge.processdash.util.ClientHttpRequest;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.TempFileFactory;

import teamdash.wbs.WBSFilenameConstants;

public class ProjectHistoryBridged extends ProjectHistoryBridgedAbstract {

    private URL baseUrl;

    private String cookies;

    public ProjectHistoryBridged(URL baseUrl, boolean cache) throws IOException {
        this.baseUrl = baseUrl;
        initChanges();
        if (cache) {
            initFileRevisionsZip();
            initTimeDelta();
        }
    }

    protected InputStream getChangeHistory() throws IOException {
        // connect to the server and download the change history for this WBS
        URL changeHistUrl = new URL(baseUrl,
                WBSFilenameConstants.CHANGE_HISTORY_FILE);
        URLConnection conn = changeHistUrl.openConnection();
        conn.connect();
        cookies = extractCookies(conn);
        return conn.getInputStream();
    }

    private String extractCookies(URLConnection conn) {
        List<String> cookies = conn.getHeaderFields().get("Set-Cookie");
        if (cookies == null || cookies.isEmpty())
            return null;

        StringBuilder result = new StringBuilder();
        for (String oneCookie : cookies) {
            int semicolonPos = oneCookie.indexOf(';');
            if (semicolonPos != -1)
                oneCookie = oneCookie.substring(0, semicolonPos);
            result.append("; ").append(oneCookie);
        }
        return result.substring(2);
    }

    @Override
    protected File getFileRevisionsZip() throws IOException {
        // download a ZIP file containing all the historical file versions
        // for the given resource collection
        URL dataHistUrl = new URL(baseUrl,
                "../../app/Datasets/GetDataHistory.do");
        ClientHttpRequest req = new ClientHttpRequest(dataHistUrl);
        if (cookies != null)
            req.getConnection().setRequestProperty("Cookie", cookies);
        req.setParameter("rc", getResourceCollectionID(baseUrl));
        for (String filename : FILES_OF_INTEREST)
            req.setParameter("include", filename);
        req.setParameter("save", "save");
        InputStream in = req.post();

        // save this ZIP data to a temporary file
        File f = TempFileFactory.get().createTempFile("wbsHist", ".zip");
        f.deleteOnExit();
        FileUtils.copyFile(in, f);
        in.close();

        return f;
    }

    private String getResourceCollectionID(URL baseUrl) {
        String u = baseUrl.toString();
        if (u.endsWith("/"))
            u = u.substring(0, u.length() - 1);
        int slashPos = u.lastIndexOf('/');
        return u.substring(slashPos + 1);
    }

    protected InputStream getVersionFile(String filename, long ts)
            throws IOException {
        InputStream result = super.getVersionFile(filename, ts);
        if (result == null) {
            String uri = filename + "?"
                    + ResourceBridgeConstants.EFFECTIVE_DATE_PARAM + "=" + ts;
            URL url = new URL(baseUrl, uri);
            URLConnection conn = url.openConnection();
            if (cookies != null)
                conn.setRequestProperty("Cookie", cookies);
            result = conn.getInputStream();
        }
        return result;
    }

    private static final String[] FILES_OF_INTEREST = { "wbs.xml", "team.xml" };

}
