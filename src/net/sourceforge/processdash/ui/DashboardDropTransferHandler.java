// Copyright (C) 2019 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.TransferHandler;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.export.DataImporter;
import net.sourceforge.processdash.ui.lib.TransferHandlerUtils;
import net.sourceforge.processdash.util.FileUtils;

public class DashboardDropTransferHandler extends TransferHandler {

    private File baseDir;


    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.Drag_and_Drop");


    public DashboardDropTransferHandler(File baseDir) {
        this.baseDir = baseDir;
    }


    @Override
    public boolean canImport(TransferSupport support) {
        return TransferHandlerUtils.hasFileListFlavor(support.getDataFlavors());
    }


    @Override
    public boolean importData(TransferSupport support) {
        boolean importedFile = false;

        // retrieve a list of any files that were dropped
        List<File> files = TransferHandlerUtils
                .getTransferredFileList(support.getTransferable());

        // if any PDASH files were dropped, process them
        List<File> pdashFiles = getFileList(files, PDASH_SUFFIX);
        if (pdashFiles != null && !pdashFiles.isEmpty() && getUserApproval(
            support.getComponent(), pdashFiles, "PDASH_File")) {
            for (File f : pdashFiles) {
                if (importPdashFile(f))
                    importedFile = true;
            }
        }

        // tell the DataImporter to check for newly imported files
        if (importedFile)
            DataImporter.refreshPrefix("/");

        return importedFile;
    }


    private boolean getUserApproval(Component dropTarget, List items,
            String resKey) {
        // make a list of the string representations of the items
        String[] itemList = new String[items.size()];
        for (int i = itemList.length; i-- > 0;)
            itemList[i] = "        " + items.get(i).toString();

        // build the title and contents for a dialog box
        String title = resources.getString(resKey + ".Window_Title");
        Object message = new Object[] { resources.getString(resKey + ".Header"),
                itemList, " ", resources.getString(resKey + ".Footer") };

        // prompt the user and return whether they clicked "Yes"
        int userChoice = JOptionPane.showConfirmDialog(dropTarget, message,
            title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return userChoice == JOptionPane.YES_OPTION;
    }


    private boolean importPdashFile(File f) {
        if (!isImportableFile(f, PDASH_SUFFIX))
            return false;

        try {
            File importDir = new File(baseDir, "import");
            if (!importDir.isDirectory())
                importDir.mkdir();
            File dest = new File(importDir, f.getName());
            FileUtils.copyFile(f, dest);
            return true;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
    }


    private List<File> getFileList(List<File> files, String suffix) {
        if (files == null)
            return null;

        List<File> result = new ArrayList();
        for (File f : files) {
            if (isImportableFile(f, suffix))
                result.add(f);
        }

        return result;
    }


    private boolean isImportableFile(File f, String suffix) {
        return f != null && f.isFile() && f.canRead()
                && f.getName().toLowerCase().endsWith(suffix);
    }


    private static final String PDASH_SUFFIX = ".pdash";

}
