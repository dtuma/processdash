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

package net.sourceforge.processdash.log.ui;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.ui.AbbreviatingPathLabel;

public class TaskPathTableCellRenderer extends DefaultTableCellRenderer {

    private String impliedPrefix;

    private AbbreviatingPathLabel abbr;

    public TaskPathTableCellRenderer() {
        this.impliedPrefix = null;
        this.abbr = new AbbreviatingPathLabel();
        this.abbr.setOpaque(true);
    }

    public void setImpliedPrefix(String prefix) {
        impliedPrefix = prefix;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        // allow our superclass to compute the correct visual appearance,
        // then copy those visual attributes to the abbreviating label
        super.getTableCellRendererComponent(table, null, isSelected, hasFocus,
            row, column);
        abbr.setFont(getFont());
        abbr.setForeground(getForeground());
        abbr.setBackground(getBackground());
        abbr.setBorder(getBorder());

        // the value we are displaying should be a task path string
        String path = (value == null ? "" : value.toString());

        // compute a relative path to display
        String relativePath = computeRelativePath(path);

        // update the abbreviating label to display the relative path
        abbr.setBounds(table.getCellRect(row, column, false));
        abbr.setPath(relativePath);
        return abbr;
    }

    private String computeRelativePath(String path) {
        // if we don't have a working prefix, just return the full path
        if (impliedPrefix == null)
            return path;

        // if this path doesn't start with our prefix, return the full path
        if (!Filter.pathMatches(path, impliedPrefix, true))
            return path;

        // we need to display *something*, so if the path and the prefix are
        // equal, show the final path segment
        if (path.equals(impliedPrefix)) {
            int lastSlashPos = path.lastIndexOf('/');
            return path.substring(lastSlashPos + 1);
        }

        // return the portion of the path that follows the prefix
        return path.substring(impliedPrefix.length() + 1);
    }

}
