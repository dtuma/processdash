// Copyright (C) 2020 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;


/**
 * Manages the working title of the various task-driven windows that are
 * displayed by the WBS Editor (e.g. Workflows, Milestones, etc).
 * 
 * Keeps track of the project-specific name that should appear in the title bar
 * of these windows, and provides support to update all windows when that title
 * changes.
 */
public class WBSWindowTitle {

    private static final String SEPARATOR = " - ";

    private String title;

    private List<JFrame> windows;


    public WBSWindowTitle(String title) {
        this.title = title;
        this.windows = new ArrayList<JFrame>();
    }

    public JFrame register(JFrame f) {
        windows.add(f);
        updateTitle(f);
        return f;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String newTitle) {
        if (newTitle != null) {
            this.title = newTitle;
            for (JFrame frame : windows)
                updateTitle(frame);
        }
    }

    private void updateTitle(JFrame frame) {
        String oldWindowTitle = frame.getTitle(), newWindowTitle;
        int sepPos = oldWindowTitle.indexOf(SEPARATOR);
        if (sepPos == -1)
            newWindowTitle = getTitle() + SEPARATOR + oldWindowTitle;
        else
            newWindowTitle = getTitle() + oldWindowTitle.substring(sepPos);
        frame.setTitle(newWindowTitle);
    }

    public static String extractTitle(JFrame f) {
        String title = f.getTitle();
        int sepPos = title.indexOf(SEPARATOR);
        if (sepPos == -1)
            return title;
        else
            return title.substring(0, sepPos);
    }

}
