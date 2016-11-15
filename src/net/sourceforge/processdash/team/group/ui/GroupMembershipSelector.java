// Copyright (C) 2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.group.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;

import net.sourceforge.processdash.team.group.UserGroup;
import net.sourceforge.processdash.team.group.UserGroupMember;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.lib.CheckboxList;
import net.sourceforge.processdash.ui.lib.ToolTipTimingCustomizer;

public class GroupMembershipSelector extends CheckboxList {

    private String readOnlyCode;

    private boolean dirty;


    public GroupMembershipSelector(String readOnlyCode) {
        super(new Object[0]);

        // store the read-only flag
        this.readOnlyCode = readOnlyCode;
        if (readOnlyCode != null)
            new ToolTipTimingCustomizer(1500, 60000).install(this);

        // install a cell renderer that will display a "person" icon
        Icon icon = DashboardIconFactory.getIndividualIcon();
        DefaultTableCellRenderer rend = new DefaultTableCellRenderer();
        rend.setIcon(icon);
        getColumnModel().getColumn(1).setCellRenderer(rend);
        setRowHeight(icon.getIconHeight() + getRowMargin() + 2);

        // add a listener that can watch for changes and set the dirty flag
        getModel().addTableModelListener(new CheckListener());
    }


    /**
     * Update this object to begin a new editing session.
     * 
     * @param allPeople
     *            a list of all known individuals
     * @param selected
     *            a list of the individuals who should initially be selected
     */
    public void setData(Set<UserGroupMember> allPeople, UserGroup selected) {
        // configure the checkboxes to be read-only, if needed
        setReadOnly(readOnlyCode != null && !selected.isCustom());

        // make a list of the people to display, and store it in the data model
        Set<UserGroupMember> fullSet = new HashSet();
        fullSet.addAll(allPeople);
        fullSet.addAll(selected.getMembers());
        List<UserGroupMember> fullList = new ArrayList<UserGroupMember>(fullSet);
        Collections.sort(fullList);
        setItems(fullList.toArray());

        // initialize the checkboxes to reflect the selected individuals
        for (int i = fullList.size(); i-- > 0;) {
            UserGroupMember m = fullList.get(i);
            setChecked(i, selected.getMembers().contains(m));
        }
        clearDirty();
    }


    @Override
    public void setReadOnly(boolean readOnly) {
        if (readOnly) {
            setToolTipText("<html><div style='width:250px'>"
                    + UserGroupEditor.resources.getHTML("Edit_Error."
                            + readOnlyCode) + "</div></html>");
        } else {
            setToolTipText(null);
        }

        super.setReadOnly(readOnly);
    }


    /**
     * Empty the list, so it contains no items.
     */
    public void clearList() {
        setItems(new Object[0]);
        clearDirty();
    }


    /**
     * @return true if any changes have been made to the selected individuals,
     *         since the last call to {@link #setData(UserGroup, UserGroup)} or
     *         {@link #clearDirty()}
     */
    public boolean isDirty() {
        return dirty;
    }


    /**
     * Clear the dirty flag on this list.
     */
    public void clearDirty() {
        dirty = false;
    }


    /**
     * @return the people whose names are selected in the UI
     */
    public Set<UserGroupMember> getSelectedMembers() {
        return new HashSet(Arrays.asList(getCheckedItems()));
    }


    private class CheckListener implements TableModelListener {

        @Override
        public void tableChanged(TableModelEvent e) {
            if (e.getColumn() == 0)
                dirty = true;
        }

    }

}
