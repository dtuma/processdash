// Copyright (C) 2002-2018 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import net.sourceforge.processdash.ui.lib.HTMLMarkup;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.WBSNode;

public class NotesColumn extends AbstractNotesColumn implements LinkSource {

    /** The ID we use for this column in the data model */
    public static final String COLUMN_ID = "Notes";

    /** The attribute this column uses to store task notes for a WBS node */
    public static final String VALUE_ATTR = "Notes";

    public NotesColumn(String authorName) {
        this(VALUE_ATTR, authorName);
    }

    protected NotesColumn(String valueAttr, String authorName) {
        super(valueAttr, authorName);
        this.columnID = COLUMN_ID;
        this.columnName = resources.getString("Notes.Name");
    }

    @Override
    protected String getEditDialogTitle() {
        return resources.getString("Notes.Edit_Dialog_Title");
    }

    @Override
    public List<String> getLinks(WBSNode node) {
        String text = getTextAt(node);
        if (!StringUtils.hasValue(text))
            return Collections.EMPTY_LIST;

        LinkedHashMap<String, String> links = HTMLMarkup.getHyperlinks(text);
        if (links.isEmpty())
            return Collections.EMPTY_LIST;

        List<String> result = new ArrayList<String>();
        for (Entry<String, String> e : links.entrySet()) {
            result.add(e.getValue() + " " + e.getKey());
        }
        return result;
    }


    public static String getTextAt(WBSNode node) {
        return getTextAt(node, VALUE_ATTR);
    }

    public static String getAuthorAt(WBSNode node) {
        return getAuthorAt(node, VALUE_ATTR);
    }

    public static Date getTimestampAt(WBSNode node) {
        return getTimestampAt(node, VALUE_ATTR);
    }

    public static String getTooltipAt(WBSNode node, boolean includeByline) {
        return getTooltipAt(node, includeByline, VALUE_ATTR);
    }

    public static void appendNote(WBSNode node, String extraNote) {
        if (extraNote == null || (extraNote = extraNote.trim()).length() == 0)
            return;

        String currentNote = getTextAt(node);
        String fullNote;
        if (currentNote == null || currentNote.trim().length() == 0)
            fullNote = extraNote;
        else if (currentNote.contains(extraNote))
            fullNote = currentNote;
        else
            fullNote = currentNote + "\n\n" + extraNote;
        node.setAttribute(VALUE_ATTR, fullNote);
        node.setAttribute(VALUE_ATTR + TIMESTAMP_SUFFIX,
            System.currentTimeMillis());
    }

    public static void saveSyncData(WBSNode node, String text, String author,
            Date timestamp) {
        node.setAttribute(VALUE_ATTR, text);
        node.setAttribute(VALUE_ATTR +  AUTHOR_SUFFIX, author);
        Long ts = (timestamp == null ? null : new Long(timestamp.getTime()));
        node.setAttribute(VALUE_ATTR + TIMESTAMP_SUFFIX, ts);
    }

}
