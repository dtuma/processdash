// Copyright (C) 2010-2020 Tuma Solutions, LLC
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

import static teamdash.wbs.IconFactory.ERROR_ICON;

import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.team.group.UserGroupManagerWBS;
import net.sourceforge.processdash.team.ui.PersonLookupDialog;
import net.sourceforge.processdash.ui.lib.ScalableImageIcon;

import teamdash.wbs.IconFactory;
import teamdash.wbs.WBSZoom;

public class PersonCellRenderer extends DefaultTableCellRenderer {

    private Icon personIcon, errorIcon, addIcon;

    private boolean lookupEnabled, lookupRequired;

    public PersonCellRenderer() {
        personIcon = WBSZoom.icon(new ScalableImageIcon(14, //
                PersonCellRenderer.class, "person.png"));
        errorIcon = IconFactory.getModifiedIcon(personIcon, ERROR_ICON);
        addIcon = WBSZoom.icon(IconFactory.getAddTabIcon());
        lookupEnabled = PersonLookupDialog.isLookupServerConfigured();
        lookupRequired = PersonLookupDialog.isTeamMemberLookupRequired();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        TeamMemberList teamList = (TeamMemberList) table.getModel();
        TeamMember m = teamList.get(row);

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
            row, column);

        if (m.getServerIdentityInfo() != null) {
            // if server identity info has been entered for this user, display
            // a regular icon with the username as a tooltip
            setIconAndTip(personIcon,
                (String) m.getServerIdentityInfoMap().get("username"));

        } else if (!table.isCellEditable(row, column)) {
            // if the user doesn't have permission to edit this row, don't call
            // their attention to any problems
            setIconAndTip(null, null);

        } else if (lookupEnabled && m.isEmpty()) {
            // if this is an empty row for creating a new user, and the PDES
            // user lookup dialog is enabled, display an "add" icon.
            setIconAndTip(addIcon, resources.getString("Columns.Username.Add"));

        } else if (lookupRequired) {
            // if the PDES is requiring usernames, but this row has none, show
            // an error icon telling the user how they can correct the problem.
            setIconAndTip(errorIcon, resources.getString(getErrorKey(m)));

        } else {
            // if this is a non-PDES project, or if this PDES does not require
            // usernames for team members, or if the current user doesn't have
            // permission to edit this row, display no icon.
            setIconAndTip(null, null);
        }

        return this;
    }

    private void setIconAndTip(Icon icon, String tooltip) {
        setIcon(icon);
        setToolTipText(tooltip);
    }

    private String getErrorKey(TeamMember m) {
        try {
            boolean teamMemberHasJoined = UserGroupManagerWBS.getInstance()
                    .getDatasetIDMap().containsKey(m.getId());
            if (!teamMemberHasJoined)
                return "Columns.Username.Missing_Unjoined";
        } catch (Exception e) {
        }
        return "Columns.Username.Missing";
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (event.getX() > personIcon.getIconWidth())
            return null;
        else
            return super.getToolTipText(event);
    }

    private static final Resources resources = TeamMember.resources;

}
