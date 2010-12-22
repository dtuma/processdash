// Copyright (C) 2002-2010 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import teamdash.team.TeamMemberList;
import teamdash.wbs.CsvNodeDataImporter.ParseException;

/** Interact with the user to find and import data from an MS Project CSV file.
 */
public class CsvNodeDataImporterUI {

    public void run(WBSJTable table, TeamMemberList team) {
        File f = selectFile(table);
        if (f == null)
            return;

        List newNodes = null;
        try {
            CsvNodeDataImporter importer = new CsvNodeDataImporter();
            newNodes = importer.getNodesFromCsvFile(f, team);
        } catch (IOException e) {
            showError("Could not open and read from the file '" + f + "'");
        } catch (ParseException e) {
            showError(e.getMessage());
        }

        if (newNodes == null)
            return;

        WBSModel model = (WBSModel) table.getModel();
        int[] newRows = model.insertNodes(newNodes, model.getRowCount());
        if (newRows == null || newRows.length == 0)
            return;

        table.selectRows(newRows);
        table.scrollRectToVisible(table.getCellRect(newRows[0], 0, true));
        UndoList.madeChange(table, "Import from CSV File");
    }

    private File selectFile(JComponent parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select MS Project Export File");
        chooser.addChoosableFileFilter(new CsvFileFilter(
                "Text (Tab-delimited)", ".txt"));
        chooser.addChoosableFileFilter(new CsvFileFilter(
                "CSV (Comma-delimited)", ".csv"));
        chooser.setMultiSelectionEnabled(false);

        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
            return chooser.getSelectedFile();
        else
            return null;
    }

    private void showError(String error) {
        String[] message = error.split("\n");
        JOptionPane.showMessageDialog(null, message, "Unable to Import",
                JOptionPane.ERROR_MESSAGE);
    }


    private static class CsvFileFilter extends FileFilter {

        private String description;

        private String extension;

        public CsvFileFilter(String description, String extension) {
            this.description = description;
            this.extension = extension;
        }

        public boolean accept(File f) {
            return (f.isDirectory() || f.getName().endsWith(extension));
        }

        public String getDescription() {
            return description;
        }

    }
}