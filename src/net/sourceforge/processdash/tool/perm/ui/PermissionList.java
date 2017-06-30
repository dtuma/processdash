// Copyright (C) 2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.perm.ui;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import net.sourceforge.processdash.tool.perm.Permission;
import net.sourceforge.processdash.ui.lib.WrappedTextTableCellRenderer;

public class PermissionList extends JTable {

    private boolean dirty;

    public PermissionList() {
        super(new DefaultTableModel(1, 1));
        getTableModel().setValueAt(SELECT_ROLE, 0, 0);
        setTableHeader(null);
        getColumnModel().getColumn(0)
                .setCellRenderer(new PermissionRenderer(this));
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    public boolean isEmpty() {
        return getRowCount() == 1 && getValueAt(0, 0) instanceof String;
    }

    public List<Permission> getContents() {
        List<Permission> result = new ArrayList<Permission>(getRowCount());
        if (!isEmpty()) {
            for (int i = 0; i < getRowCount(); i++)
                result.add(getPermission(i));
        }
        return result;
    }

    public void setContents(List<Permission> permissions) {
        clearSelection();
        DefaultTableModel m = getTableModel();
        if (permissions.isEmpty()) {
            m.setRowCount(1);
            m.setValueAt(NO_PERMISSION, 0, 0);
        } else {
            m.setRowCount(permissions.size());
            for (int i = 0; i < permissions.size(); i++)
                m.setValueAt(permissions.get(i), i, 0);
        }
        this.dirty = false;
    }

    public void clearList() {
        DefaultTableModel m = getTableModel();
        m.setRowCount(1);
        m.setValueAt(SELECT_ROLE, 0, 0);
        this.dirty = false;
    }

    public int addPermission(Permission p) {
        DefaultTableModel m = getTableModel();
        if (isEmpty()) {
            m.setValueAt(p, 0, 0);
            dirty = true;
            return 0;
        } else {
            for (int i = 0; i < getRowCount(); i++) {
                if (p.equals(getPermission(i)))
                    return i;
            }
            m.addRow(new Object[] { p });
            dirty = true;
            return getRowCount() - 1;
        }
    }

    public Permission getPermission(int row) {
        if (row == 0 && isEmpty())
            return null;
        else
            return (Permission) getValueAt(row, 0);
    }

    public void alterPermission(int row, Permission p) {
        getTableModel().setValueAt(p, row, 0);
        dirty = true;
    }

    public void deletePermission(int row) {
        if (isEmpty())
            return;

        if (row == 0 && getRowCount() == 1) {
            getTableModel().setValueAt(NO_PERMISSION, 0, 0);
        } else {
            getTableModel().removeRow(row);
        }
        dirty = true;
    }

    private DefaultTableModel getTableModel() {
        return (DefaultTableModel) getModel();
    }


    /**
     * Renderer for permissions. Draws items as a wrapped, bulleted list; and
     * uses an italic font for special items.
     */
    private class PermissionRenderer extends WrappedTextTableCellRenderer {

        private Border plainBorder, bulletBorder;

        private Font regularFont, italicFont;

        public PermissionRenderer(JTable table) {
            super(table);
            this.regularFont = table.getFont();
            this.italicFont = regularFont.deriveFont(Font.ITALIC);
            this.plainBorder = BorderFactory.createEmptyBorder(12, 9, 12, 9);
            this.bulletBorder = new BulletBorder(regularFont);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object text, boolean isSelected, boolean hasFocus, int row,
                int column) {

            super.getTableCellRendererComponent(table, text, isSelected,
                hasFocus, row, column);

            // configure the font and border based on the item type
            if (text instanceof String) {
                setFont(italicFont);
                setBorder(plainBorder);
            } else {
                setFont(regularFont);
                setBorder(bulletBorder);
            }

            return this;
        }

    }


    /**
     * Class to draw a bullet next to a wrapped text item.
     */
    private class BulletBorder extends EmptyBorder {

        public BulletBorder(Font f) {
            super(3, 3, 3, 3);
            this.left = 6 + getFontMetrics(f).stringWidth(BULLET);
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y,
                int width, int height) {
            g.setFont(PermissionList.this.getFont());
            g.setColor(c.getForeground());

            FontMetrics m = g.getFontMetrics();
            int drop = m.getHeight() - m.getDescent();
            g.drawString("\u2022", x + 3, y + 3 + drop);
        }

        private static final String BULLET = "\u2022";

    }

    private static final String NO_PERMISSION = RolesEditor.resources
            .getString("No_Permission");

    private static final String SELECT_ROLE = RolesEditor.resources
            .getString("Select_Role");
}
