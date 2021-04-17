// Copyright (C) 2012-2021 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.XMLUtils;

public class ChangeHistory {

    public interface Entry {

        public String getUid();

        public Date getTimestamp();

        public String getUser();
    }

    /*
     * Underlying data storage for this change history object.
     * 
     * XML is used as the in-memory storage strategy to allow better backwards
     * and forwards compatibility when the structure of the data changes. If new
     * tags and/or attributes are added to the XML structure in the future, but
     * someone opens and resaves this file with an older version of the WBS
     * Editor, retaining the original XML document will prevent those new tags
     * from being lost.
     */
    private Element xml;

    public ChangeHistory() {
        this(getEmptyDocument());
    }

    public ChangeHistory(File f) {
        this(readChangeHistoryFromFile(f));
    }

    public ChangeHistory(InputStream in) {
        this(readChangeHistoryFromStream(in));
    }

    public ChangeHistory(Element xml) {
        if (!CHANGE_HISTORY_TAG.equals(xml.getTagName()))
            throw new IllegalArgumentException(
                    "Invalid XML change history document.");

        this.xml = xml;
    }

    public List<Entry> getEntries() {
        NodeList entryTags = xml.getElementsByTagName(CHANGE_ENTRY_TAG);
        List<Entry> result = new ArrayList(entryTags.getLength());
        for (int i = 0; i < entryTags.getLength(); i++)
            result.add(new ChangeEntry((Element) entryTags.item(i)));
        return result;
    }

    public Entry getLastEntry() {
        List<Entry> entries = getEntries();
        if (entries.isEmpty())
            return null;
        else
            return entries.get(entries.size() - 1);
    }

    public Entry addEntry(String userName) {
        return addEntry(createUid(), userName);
    }

    public Entry addEntry(String uid, String userName) {
        Element newTag = xml.getOwnerDocument().createElement(CHANGE_ENTRY_TAG);
        newTag.setAttribute(UID_ATTR, uid);
        newTag.setAttribute(USER_ATTR, userName);
        xml.appendChild(newTag);

        return new ChangeEntry(newTag);
    }

    private String createUid() {
        long time = System.currentTimeMillis();
        int rand = new Random().nextInt(1500000);
        return Long.toString(time, Character.MAX_RADIX) + "-"
                + Long.toString(rand, Character.MAX_RADIX);
    }

    public void write(File f) throws IOException {
        // if we were given a directory, write to the change history file there
        if (f.isDirectory())
            f = new File(f, WBSFilenameConstants.CHANGE_HISTORY_FILE);

        // write the XML to the file
        write(new RobustFileOutputStream(f));
    }

    public void write(OutputStream outStream) throws IOException {
        BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(outStream, "UTF-8"));
        getAsXML(out);
        out.flush();
        out.close();
    }

    public void getAsXML(Writer out) throws IOException {
        out.write(XMLUtils.getAsText(xml));
    }

    private static Element readChangeHistoryFromFile(File f) {
        // if we were given a directory, examine the change history file there
        if (f.isDirectory())
            f = new File(f, WBSFilenameConstants.CHANGE_HISTORY_FILE);

        // try reading the file if it exists
        if (f.isFile()) {
            try {
                BufferedInputStream in = new BufferedInputStream(
                        new FileInputStream(f));
                return XMLUtils.parse(in).getDocumentElement();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // if the file did not exist or could not be read, create a new/empty
        // change history document.
        return getEmptyDocument();
    }

    private static Element readChangeHistoryFromStream(InputStream in) {
        try {
            return XMLUtils.parse(new BufferedInputStream(in))
                    .getDocumentElement();
        } catch (Exception e) {
        }

        // if the file did not exist or could not be read, create a new/empty
        // change history document.
        return getEmptyDocument();
    }

    private static Element getEmptyDocument() {
        try {
            return XMLUtils.parse(NULL_DOCUMENT).getDocumentElement();
        } catch (Exception e) {
            // "can't happen"
            throw new RuntimeException(e);
        }
    }

    private class ChangeEntry implements Entry {
        private Element tag;

        public ChangeEntry(Element tag) {
            this.tag = tag;
        }

        public String getUser() {
            return tag.getAttribute(USER_ATTR);
        }

        public String getUid() {
            return tag.getAttribute(UID_ATTR);
        }

        public Date getTimestamp() {
            String uid = getUid();
            int dashpos = uid.indexOf('-');
            if (dashpos == -1)
                return null;
            String timeStr = uid.substring(0, dashpos);
            long when = Long.parseLong(timeStr, Character.MAX_RADIX);
            return new Date(when);
        }

    }

    private static final String CHANGE_HISTORY_TAG = "changeHistory";

    private static final String CHANGE_ENTRY_TAG = "change";

    private static final String UID_ATTR = "uid";

    private static final String USER_ATTR = "user";

    private static final String NULL_DOCUMENT = "<" + CHANGE_HISTORY_TAG + "/>";

}
