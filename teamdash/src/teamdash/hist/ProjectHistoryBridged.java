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
import java.util.Date;
import java.util.List;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.bridge.ResourceBridgeConstants;
import net.sourceforge.processdash.util.ClientHttpRequest;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.FastDateFormat;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.TempFileFactory;

import teamdash.wbs.WBSFilenameConstants;

public class ProjectHistoryBridged extends ProjectHistoryBridgedAbstract {

    private URL baseUrl;

    private String cookies;

    private long startTimestamp = NEVER;

    private long endTimestamp = 0;

    public ProjectHistoryBridged(URL baseUrl, boolean precacheAll)
            throws IOException {
        this.baseUrl = baseUrl;
        initChanges();
        if (precacheAll)
            cacheFileRevisions(null, null);
    }

    @Override
    public void refresh() throws IOException {
        initChanges();
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
    protected void maybeCacheTimePeriod(long ts) throws IOException {
        // if the requested timestamp is within our cached safe period, return.
        if (startTimestamp < ts && ts < endTimestamp)
            return;

        // calculate a new time period around the requested timestamp.
        long newStart = ts - 9 * DateUtils.DAYS;
        long newEnd = ts + 3 * DateUtils.DAYS;

        // this class assumes that our cache is contiguous, with no time gaps.
        // Adjust the new target time range to ensure that it overlaps with our
        // existing cache.
        if (endTimestamp > 0)
            newStart = Math.min(newStart, endTimestamp);
        if (startTimestamp < NEVER)
            newEnd = Math.max(newEnd, startTimestamp);

        // retrieve and cache files for the given date range.
        cacheFileRevisions(new Date(newStart), new Date(newEnd));
    }

    private void cacheFileRevisions(Date startDate, Date endDate)
            throws IOException {
        // download a ZIP file containing historical file versions
        // for the given resource collection
        URL dataHistUrl = new URL(baseUrl,
                "../../app/Datasets/GetDataHistory.do");
        ClientHttpRequest req = new ClientHttpRequest(dataHistUrl);
        if (cookies != null)
            req.getConnection().setRequestProperty("Cookie", cookies);
        req.setParameter("rc", getResourceCollectionID(baseUrl));
        for (String filename : FILES_OF_INTEREST)
            req.setParameter("include", filename);
        if (startDate != null)
            req.setParameter("after", PARAM_DATE_FMT.format(startDate));
        if (endDate != null)
            req.setParameter("before", PARAM_DATE_FMT.format(endDate));
        req.setParameter("save", "save");
        InputStream in = req.post();

        // save this ZIP data to a temporary file
        File f = TempFileFactory.get().createTempFile("wbsHist", ".zip");
        if (debugCacheFiles)
            System.out.println("Caching project history file: " + f);
        else
            f.deleteOnExit();
        FileUtils.copyFile(in, f);
        in.close();

        // load the file revisions data from that temporary file
        if (loadFileRevisionsZip(f)) {
            // if the load was successful, update our timestamps to keep track
            // of the region of time that is reliably cached. We adjust the
            // dates inward by 2 days to be safe in the presence of time zone
            // differences between the client and the server.
            startTimestamp = Math.min(startTimestamp, (startDate == null ? 0
                    : startDate.getTime()) + 2 * DateUtils.DAYS);
            endTimestamp = Math.max(endTimestamp, (endDate == null ? NEVER
                    : endDate.getTime()) - 2 * DateUtils.DAYS);
        } else {
            // the load will fail if data security is not enabled on the
            // server. In that case, change the timestamps to avoid any future
            // attempts to cache data.
            startTimestamp = 0;
            endTimestamp = NEVER;
        }
    }

    private boolean debugCacheFiles = Settings.getBool(
        "wbsChangeHistory.debugCacheFiles", false);

    private String getResourceCollectionID(URL baseUrl) {
        String u = baseUrl.toString();
        if (u.endsWith("/"))
            u = u.substring(0, u.length() - 1);
        int slashPos = u.lastIndexOf('/');
        return u.substring(slashPos + 1);
    }

    @Override
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

    @Override
    public ProjectHistoryException wrapException(Throwable e) {
        return new ProjectHistoryException(e, "Server.Cannot_Read_HTML_FMT",
                baseUrl.toString());
    }

    private static final String[] FILES_OF_INTEREST = {
            WBSFilenameConstants.WBS_FILENAME,
            WBSFilenameConstants.TEAM_LIST_FILENAME,
            WBSFilenameConstants.CHANGE_HISTORY_FILE };

    private static final FastDateFormat PARAM_DATE_FMT = FastDateFormat
            .getInstance("yyyy-MM-dd");

    private static final long NEVER = Long.MAX_VALUE / 2;

}
