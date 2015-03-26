// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.HierarchyNote;
import net.sourceforge.processdash.hier.HierarchyNoteEvent;
import net.sourceforge.processdash.hier.HierarchyNoteListener;
import net.sourceforge.processdash.hier.HierarchyNoteManager;
import net.sourceforge.processdash.process.ScriptEnumerator;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.process.ScriptSource;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class HierarchyNoteScriptSource implements ScriptSource,
        HierarchyNoteListener {

    private DashboardContext context;

    public void setDashboardContext(DashboardContext ctx) {
        this.context = ctx;
        HierarchyNoteManager.addHierarchyNoteListener(this);
    }

    public String getUniqueID() {
        return "HierarchyNoteScriptSource";
    }

    public List<ScriptID> getScripts(String path) {
        List<ScriptID> result = new ArrayList<ScriptID>();

        for (; path != null; path = DataRepository.chopPath(path)) {
            Map<String, HierarchyNote> notes = HierarchyNoteManager
                    .getNotesForPath(context.getData(), path);
            if (notes != null) {
                HierarchyNote note = notes.get(HierarchyNoteManager.NOTE_KEY);
                if (note != null)
                    extractScriptsFromNote(result, path, note);
            }
        }

        return (result.isEmpty() ? null : result);
    }

    private void extractScriptsFromNote(List<ScriptID> result, String path,
            HierarchyNote note) {
        String html = note.getAsHTML();
        if (html == null || html.length() == 0)
            return;

        Matcher m = LINK_PATTERN.matcher(html);
        while (m.find()) {
            Map attrs = HTMLUtils.parseAttributes(m.group(1));
            String href = (String) attrs.get("href");
            if (!StringUtils.hasValue(href))
                href = (String) attrs.get("HREF");
            if (!StringUtils.hasValue(href))
                return;

            String text = HTMLUtils.unescapeEntities(m.group(2));
            if (text == null || text.trim().length() == 0
                    || text.startsWith("http"))
                text = null;

            ScriptID s = new ScriptID(href, path, text);
            result.add(s);
        }
    }
    private static final Pattern LINK_PATTERN = Pattern.compile(
        "<a([^>]+)>([^<]+)</a>", Pattern.CASE_INSENSITIVE);

    public void notesChanged(HierarchyNoteEvent e) {
        ScriptEnumerator.scriptSourceChanged(this, e.getPath());
    }

}
