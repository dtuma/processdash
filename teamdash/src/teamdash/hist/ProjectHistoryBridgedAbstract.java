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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import teamdash.team.TeamMember;
import teamdash.team.TeamMemberList;
import teamdash.wbs.ChangeHistory;
import teamdash.wbs.ChangeHistory.Entry;
import teamdash.wbs.WBSFilenameConstants;

public abstract class ProjectHistoryBridgedAbstract implements
        ProjectHistory<Entry> {

    private List<Entry> changes;

    private List<ManifestEntry> revisions;

    private long timeDelta;

    private Map<String, String> identities;


    protected void initChanges() throws IOException {
        // parse the change history file for this WBS
        changes = new ChangeHistory(parseXml(getChangeHistory())).getEntries();
    }

    protected abstract InputStream getChangeHistory() throws IOException;


    protected boolean loadFileRevisionsZip(File f) throws IOException {
        // retrieve a ZIP file containing all of the historical file versions
        // for this project
        ZipFile revisionZip;
        try {
            revisionZip = new ZipFile(f);
        } catch (Exception e) {
            // if password security is not enabled for the DataBridge servlet,
            // the session cookies we obtained above won't confer a login, so
            // the request for a data history ZIP file will print an HTML login
            // form instead. In this case, abort and let later code retrieve
            // historical versions one at a time.
            return false;
        }
        ZipEntry entry = revisionZip.getEntry("manifest.xml");
        if (entry == null)
            return false;

        // parse the manifest from the ZIP file
        if (revisions == null)
            revisions = new ArrayList<ManifestEntry>();
        ManifestEntry changeHist = null, teamList = null;
        Element revisionsXml = parseXml(revisionZip.getInputStream(entry));
        NodeList revisionTags = revisionsXml.getElementsByTagName("file");
        for (int i = 0; i < revisionTags.getLength(); i++) {
            Element tag = (Element) revisionTags.item(i);
            ManifestEntry rev = new ManifestEntry(revisionZip, tag);
            if (rev.matches(WBSFilenameConstants.CHANGE_HISTORY_FILE))
                changeHist = rev;
            else if (rev.matches(WBSFilenameConstants.TEAM_LIST_FILENAME))
                teamList = rev;
            revisions.add(rev);
        }
        Collections.sort(revisions);

        // load data from the last changeHist.xml and team.xml files we saw
        initTimeDelta(changeHist);
        loadIdentityInfo(teamList);
        return true;
    }

    protected void initTimeDelta(ManifestEntry changeHist) {
        // manifest entry timestamps are written by the PDES process without
        // timezone info. In this method we then parse them in the timezone of
        // the local process. Calculate the offset that is needed to convert
        // these back to UTC timestamps.
        if (timeDelta == 0 && changeHist != null) {
            try {
                Element xml = parseXml(changeHist.getStream());
                ChangeHistory changes = new ChangeHistory(xml);
                Entry lastChange = changes.getLastEntry();
                long lastTimestamp = lastChange.getTimestamp().getTime();
                timeDelta = changeHist.lastMod - lastTimestamp;
                // the file modification time can normally differ from the
                // change timestamp by a second or two; but we are only
                // interested in the delta caused by time zone differences.
                // round the delta to an even half-hour interval.
                double fraction = timeDelta / (30.0 * DateUtils.MINUTES);
                timeDelta = (int) Math.round(fraction) * 30 * DateUtils.MINUTES;
            } catch (Exception e) {
            }
        }
    }

    protected void loadIdentityInfo(ManifestEntry teamList) {
        if (teamList != null) {
            try {
                if (identities == null)
                    identities = new HashMap<String, String>();
                Element xml = parseXml(teamList.getStream());
                TeamMemberList team = new TeamMemberList(xml);
                for (TeamMember indiv : team.getTeamMembers()) {
                    String infoStr = indiv.getServerIdentityInfo();
                    Map info = HTMLUtils.parseQuery(infoStr);
                    String username = (String) info.get("username");
                    if (StringUtils.hasValue(username))
                        identities.put(username, indiv.getName());
                }
            } catch (Exception e) {
            }
        }
    }


    public List<Entry> getVersions() {
        return changes;
    }

    public Date getVersionDate(Entry version) {
        return version.getTimestamp();
    }

    public String getVersionAuthor(Entry version) {
        String result = null;
        if (identities != null) {
            try {
                long ts = version.getTimestamp().getTime() + TIME_PAD;
                ManifestEntry e = getCachedManifestEntry(
                    WBSFilenameConstants.WBS_FILENAME, ts);
                if (e != null)
                    result = identities.get(e.username);
            } catch (IOException ioe) {
            }
        }
        return (result != null ? result : version.getUser());
    }

    public InputStream getVersionFile(Entry version, String filename)
            throws IOException {
        long ts = version.getTimestamp().getTime() + TIME_PAD;
        return getVersionFile(filename, ts);
    }

    protected InputStream getVersionFile(String filename, long ts)
            throws IOException {
        ManifestEntry e = getCachedManifestEntry(filename, ts);
        return (e == null ? null : e.getStream());
    }

    private ManifestEntry getCachedManifestEntry(String filename, long ts)
            throws IOException {
        maybeCacheTimePeriod(ts);

        if (revisions != null) {
            ts = ts + timeDelta;
            for (int i = revisions.size(); i-- > 0;) {
                ManifestEntry e = revisions.get(i);
                if (e.matches(ts, filename, false))
                    return e;
            }
        }

        return null;
    }

    protected void maybeCacheTimePeriod(long ts) throws IOException {}

    private static Element parseXml(InputStream in) throws IOException {
        try {
            return XMLUtils.parse(in).getDocumentElement();
        } catch (SAXException se) {
            throw new IOException(se);
        }
    }

    private static class ManifestEntry implements Comparable<ManifestEntry> {

        ZipFile revisionZip;

        String filename;

        String href;

        String username;

        long startTime, endTime, lastMod;

        public ManifestEntry(ZipFile zip, Element xml) {
            revisionZip = zip;
            filename = xml.getAttribute("name");
            href = xml.getAttribute("href");
            username = xml.getAttribute("createdBy");
            startTime = parseDate(xml, "effStartDate", Long.MAX_VALUE);
            endTime = parseDate(xml, "effEndDate", Long.MAX_VALUE);
            lastMod = parseDate(xml, "lastModified", 0);
        }

        private long parseDate(Element xml, String attr, long defaultVal) {
            String val = xml.getAttribute(attr);
            try {
                if (XMLUtils.hasValue(val))
                    return TIMESTAMP_FMT.parse(val).getTime();
            } catch (ParseException pe) {
            }
            return defaultVal;
        }

        public boolean matches(String name) {
            return filename.equalsIgnoreCase(name);
        }

        public boolean matches(long ts, String name, boolean checkEnd) {
            return matches(name) && startTime <= ts //
                    && (checkEnd == false || ts <= endTime);
        }

        public InputStream getStream() throws IOException {
            ZipEntry entry = revisionZip.getEntry(href);
            return revisionZip.getInputStream(entry);
        }

        public int compareTo(ManifestEntry that) {
            if (this.startTime > that.startTime)
                return 1;
            else if (this.startTime < that.startTime)
                return -1;
            else
                return 0;
        }

    }

    // some filesystems only track modification dates to within 2 seconds, and
    // some databases round off timestamps to the nearest second. To account
    // for these sources of error, we add a few seconds to target timestamps
    // when we are performing a search.
    private static final int TIME_PAD = 3000;

    private static final DateFormat TIMESTAMP_FMT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");

}
