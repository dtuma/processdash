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
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.util.XMLUtils;

import teamdash.wbs.ChangeHistory;
import teamdash.wbs.ChangeHistory.Entry;

public abstract class ProjectHistoryBridgedAbstract implements
        ProjectHistory<Entry> {

    private List<Entry> changes;

    private ZipFile revisionZip;

    private List<ManifestEntry> revisions;

    protected long lastMod;

    private long timeDelta;


    protected void initChanges() throws IOException {
        // parse the change history file for this WBS
        changes = new ChangeHistory(parseXml(getChangeHistory())).getEntries();
    }

    protected abstract InputStream getChangeHistory() throws IOException;


    protected void initFileRevisionsZip() throws IOException {
        // retrieve a ZIP file containing all of the historical file versions
        // for this project
        File f = getFileRevisionsZip();
        try {
            revisionZip = new ZipFile(f);
        } catch (Exception e) {
            // if password security is not enabled for the DataBridge servlet,
            // the session cookies we obtained above won't confer a login, so
            // the request for a data history ZIP file will print an HTML login
            // form instead. In this case, abort and let later code retrieve
            // historical versions one at a time.
            return;
        }

        // parse the manifest from the ZIP file
        revisions = new ArrayList<ProjectHistoryBridgedAbstract.ManifestEntry>();
        lastMod = 0;
        Element revisionsXml = parseXml(revisionZip.getInputStream(revisionZip
                .getEntry("manifest.xml")));
        NodeList revisionTags = revisionsXml.getElementsByTagName("file");
        for (int i = 0; i < revisionTags.getLength(); i++) {
            Element tag = (Element) revisionTags.item(i);
            ManifestEntry rev = new ManifestEntry(tag);
            lastMod = Math.max(lastMod, rev.lastMod);
            revisions.add(rev);
        }
    }

    protected abstract File getFileRevisionsZip() throws IOException;


    protected void initTimeDelta() {
        // manifest entry timestamps are written by the PDES process without
        // timezone info. In this class we then parse them in the timezone of
        // the local process. Calculate the offset that is needed to convert
        // these back to UTC timestamps.
        if (lastMod > 0) {
            Entry lastChange = changes.get(changes.size() - 1);
            long lastTimestamp = lastChange.getTimestamp().getTime();
            lastTimestamp -= (lastTimestamp % 1000);
            timeDelta = lastMod - lastTimestamp;
        }
    }


    public List<Entry> getVersions() {
        return changes;
    }

    public Date getVersionDate(Entry version) {
        return version.getTimestamp();
    }

    public String getVersionAuthor(Entry version) {
        return version.getUser();
    }

    public InputStream getVersionFile(Entry version, String filename)
            throws IOException {
        long ts = version.getTimestamp().getTime() + 200 + timeDelta;
        return getVersionFile(filename, ts);
    }

    protected InputStream getVersionFile(String filename, long ts)
            throws IOException {
        if (revisions != null) {
            for (int i = revisions.size(); i-- > 0;) {
                ManifestEntry e = revisions.get(i);
                if (e.matches(ts, filename, false)) {
                    ZipEntry entry = revisionZip.getEntry(e.href);
                    return revisionZip.getInputStream(entry);
                }
            }
        }

        return null;
    }

    private static Element parseXml(InputStream in) throws IOException {
        try {
            return XMLUtils.parse(in).getDocumentElement();
        } catch (SAXException se) {
            throw new IOException(se);
        }
    }

    private static class ManifestEntry {

        String filename;

        String href;

        long startTime, endTime, lastMod;

        public ManifestEntry(Element xml) {
            filename = xml.getAttribute("name");
            href = xml.getAttribute("href");
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

        public boolean matches(long ts, String name, boolean checkEnd) {
            return filename.equalsIgnoreCase(name) && startTime <= ts //
                    && (checkEnd == false || ts <= endTime);
        }

    }

    private static final DateFormat TIMESTAMP_FMT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");

}
