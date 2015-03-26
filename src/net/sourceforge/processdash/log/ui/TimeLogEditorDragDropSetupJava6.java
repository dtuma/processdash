// Copyright (C) 2013 Tuma Solutions, LLC
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

import java.util.List;

import javax.swing.DropMode;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import net.sourceforge.processdash.log.time.TimeLogEntry;
import net.sourceforge.processdash.log.time.TimeLogSelection;

public class TimeLogEditorDragDropSetupJava6 implements
        TimeLogEditorDragDropSetup {

    public void setupEditorForDragDrop(TimeLogEditor editor) {
        editor.table.setDragEnabled(true);
        editor.tree.setDropMode(DropMode.ON);
        editor.tree.setTransferHandler(new DropSupport(editor));
    }

    private static class DropSupport extends TransferHandler {

        TimeLogEditor editor;

        public DropSupport(TimeLogEditor editor) {
            this.editor = editor;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return getTargetNodePath(support) != null;
        }

        @Override
        public boolean importData(TransferSupport support) {
            String targetPath = getTargetNodePath(support);
            if (targetPath == null)
                return false;

            try {
                List<TimeLogEntry> entries = (List) support.getTransferable()
                        .getTransferData(TimeLogSelection.FLAVOR);
                editor.tableModel.moveTimeLogEntries(entries, targetPath);
                return true;
            } catch (Exception e) {
            }

            return false;
        }

        private String getTargetNodePath(TransferSupport support) {
            if (!support.isDataFlavorSupported(TimeLogSelection.FLAVOR))
                return null;

            TreePath path;
            if (support.isDrop()) {
                // find the path the user dropped the entries on.
                DropLocation loc = support.getDropLocation();
                path = ((JTree.DropLocation) loc).getPath();
            } else {
                // for cut/paste, we paste to the selected path.
                path = editor.tree.getSelectionPath();
            }
            // check whether the target path is a valid time logging node.
            return editor.getPathIfLoggingAllowed(path);
        }

    }

}
