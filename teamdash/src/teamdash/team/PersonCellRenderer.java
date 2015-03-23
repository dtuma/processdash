// Copyright (C) 2010 Tuma Solutions, LLC
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

package teamdash.team;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class PersonCellRenderer extends DefaultTableCellRenderer {

    private Icon personIcon;

    public PersonCellRenderer() {
        personIcon = new ImageIcon(PersonCellRenderer.class
                .getResource("person.png"));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        TeamMemberList teamList = (TeamMemberList) table.getModel();
        TeamMember m = teamList.get(row);

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
            row, column);

        setIcon(m.getServerIdentityInfo() == null ? null : personIcon);

        return this;
    }



}
