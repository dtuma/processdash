// Copyright (C) 2009-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.dash;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.web.TinyCGIBase;

public class ClipboardHelper extends TinyCGIBase {

    public static final String HEADER_ITEMS = "<link rel=\"stylesheet\" type=\"text/css\" href=\"/lib/clipboard.css\">"
            + "<script src=\"/lib/prototype.js\" type=\"text/javascript\"></script>"
            + "<script src=\"/lib/clipboard.js\" type=\"text/javascript\"></script>";


    private static final Map<Integer, String> CLIP_ITEMS = new Hashtable<Integer, String>();

    private static int CLIP_NUMBER = (int) (System.currentTimeMillis() & 0xfff);

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.CopyToClipboard");


    public synchronized static int storeClip(String clip) {
        int id = CLIP_NUMBER++;
        CLIP_ITEMS.put(id, clip);
        return id;
    }

    public static String getHyperlinkTag(String clip) {
        int id = storeClip(clip);
        return "<a class=\"copyToClipboard\" href=\"#\" title=\""
                + resources.getHTML("Tooltip")
                + "\" onclick=\"copyToClipboard(" + id + "); return false;\">";
    }

    @Override
    protected void writeContents() throws IOException {
        rejectCrossSiteRequests(env);
        String id = getParameter("id");
        Integer i = Integer.parseInt(id);
        String clip = CLIP_ITEMS.get(i);

        if (clip != null) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(clip), null);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }

        DashController.printNullDocument(out);
    }

}
