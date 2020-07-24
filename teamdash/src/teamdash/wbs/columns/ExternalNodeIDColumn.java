// Copyright (C) 2018-2020 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

import teamdash.sync.ExtSyncUtil;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.WBSNode;


/**
 * Column which displays the ID of a node from an external system
 */
public class ExternalNodeIDColumn extends AbstractDataColumn
        implements ExternalSystemPrimaryColumn, CustomEditedColumn, LinkSource {

    private String idAttr, keyAttr, urlAttr;

    private String lastCellEditUrl;

    public ExternalNodeIDColumn(String systemID, String systemName) {
        this.columnID = systemID + " ID";
        this.columnName = resources.format("External_ID.Name_FMT", systemName);
        this.idAttr = ExtSyncUtil.getExtIDAttr(systemID);
        this.keyAttr = ExtSyncUtil.getExtKeyAttr(systemID);
        this.urlAttr = ExtSyncUtil.getExtUrlAttr(systemID);
    }

    @Override
    public boolean isCellEditable(WBSNode node) {
        // in reality, no cells in this column are editable. However, we would
        // like to open a web browser when a user double-clicks on a given cell.
        // To accomplish this, we claim that cells are editable if they have
        // associated URLs. This causes the JTable to retrieve our cell editor,
        // and ask it for a second opinion about whether the cell is truly
        // editable. Our cell editor always answers no, but opens the web
        // browser first.
        lastCellEditUrl = (String) node.getAttribute(urlAttr);
        return lastCellEditUrl != null;
    }

    @Override
    public Object getValueAt(WBSNode node) {
        if (ExtSyncUtil.isExtNode(node) == false)
            return null;

        String id = (String) node.getAttribute(keyAttr);
        if (id == null)
            id = (String) node.getAttribute(idAttr);
        if (ExtSyncUtil.INCOMING_PARENT_ID.equals(id))
            id = null;
        return (id == null ? null : new ReadOnlyValue(id));
    }

    @Override
    public void setValueAt(Object aValue, WBSNode node) {
        // do nothing
    }

    @Override
    public List<String> getLinks(WBSNode node) {
        String url = (String) node.getAttribute(urlAttr);
        if (url == null)
            return Collections.EMPTY_LIST;
        else
            return Arrays.asList(url.split("\n"));
    }

    @Override
    public TableCellEditor getCellEditor() {
        return CELL_EDITOR;
    }


    private class CellEditor extends DefaultCellEditor {

        private CellEditor() {
            super(new JTextField());
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            if (shouldOpenUrl(e))
                openUrl();

            return false;
        }

        private boolean shouldOpenUrl(EventObject e) {
            // open the URL if the user presses a key on this cell
            if (e == null || e instanceof ActionEvent || e instanceof KeyEvent)
                return true;

            // open the URL if the user double-clicks on this cell
            if (e instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) e;
                return me.getClickCount() >= 2;
            }

            // ignore other events
            return false;
        }

        private void openUrl() {
            try {
                // retrieve the URL of the node the user clicked on
                String url = lastCellEditUrl;
                if (url == null)
                    return;

                // discard any display text from the end of the node
                int pos = url.indexOf(' ');
                if (pos != -1)
                    url = url.substring(0, pos);

                // open the URL in a web browser
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                ex.printStackTrace();
                Toolkit.getDefaultToolkit().beep();
            }
        }

    }

    private final TableCellEditor CELL_EDITOR = new CellEditor();

}
