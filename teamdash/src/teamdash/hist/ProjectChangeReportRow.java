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

import java.util.List;
import java.util.Stack;

import net.sourceforge.processdash.util.HTMLUtils;

public class ProjectChangeReportRow {

    private int indent;

    private boolean expanded;

    private String icon;

    private String iconTooltip;

    private String html;

    boolean visible;

    boolean expandable;

    String expansionId;

    protected ProjectChangeReportRow(int indent, boolean expanded, String icon,
            String iconTooltip, String text, boolean escapeText) {
        this.indent = indent;
        this.expanded = expanded;
        this.icon = icon;
        this.iconTooltip = iconTooltip;
        this.html = (escapeText ? HTMLUtils.escapeEntities(text) : text);
    }

    public int getIndent() {
        return indent;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isExpandable() {
        return expandable;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public String getExpansionId() {
        return expansionId;
    }

    public String getIcon() {
        return icon;
    }

    public String getIconTooltip() {
        return iconTooltip;
    }

    public String getHtml() {
        return html;
    }

    public static List<ProjectChangeReportRow> initExpansion(
            List<ProjectChangeReportRow> rows) {
        Stack<ProjectChangeReportRow> parents = new Stack();
        for (int i = 0; i < rows.size(); i++) {
            ProjectChangeReportRow row = rows.get(i);
            while (!parents.isEmpty() && row.indent <= parents.peek().indent) {
                parents.pop();
            }
            if (parents.isEmpty()) {
                row.visible = true;
                row.expansionId = "_" + i;
            } else {
                ProjectChangeReportRow parent = parents.peek();
                parent.expandable = true;
                row.visible = parent.visible && parent.expanded;
                row.expansionId = parent.expansionId + "-" + i;
            }
            parents.push(row);
        }
        return rows;
    }

}
