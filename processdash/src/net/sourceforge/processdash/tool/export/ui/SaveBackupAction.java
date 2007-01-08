// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.ui;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.ExampleFileFilter;
import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.util.FileUtils;

public class SaveBackupAction extends AbstractAction {

    private DataContext dataContext;

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard");

    public SaveBackupAction(DataContext dataContext) {
        super(resources.getString("Menu.Save_Backup"));
        this.dataContext = dataContext;
    }

    public void actionPerformed(ActionEvent e) {
        new Worker().start();
    }

    private void showErrorMessage() {
        String[] message = resources.getStrings("Save_Backup.Error.Message");
        String title = resources.getString("Save_Backup.Error.Title");
        JOptionPane.showMessageDialog(null, message, title,
                JOptionPane.ERROR_MESSAGE);
    }

    public String getDefaultFilename() {
        String owner = ProcessDashboard.getOwnerName(dataContext);
        if (owner == null)
            owner = "";
        else
            owner = FileUtils.makeSafe(owner) + "-";

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        return "pdash-" + owner + fmt.format(new Date()) + ".zip";
    }

    private class Worker extends SwingWorker {

        File backupFile;

        JFileChooser fc;

        public Object construct() {
            backupFile = DashController.backupData();

            fc = new JFileChooser();
            fc.setSelectedFile(new File(fc.getCurrentDirectory(),
                    getDefaultFilename()));
            fc.setDialogTitle(resources.getString("Save_Backup.Window_Title"));
            fc.setFileFilter(new ExampleFileFilter("zip", resources
                    .getString("Save_Backup.Zip_File_Description")));

            return null;
        }

        public void finished() {
            if (backupFile == null) {
                showErrorMessage();
                return;
            }

            if (fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
                return;

            File dest = fc.getSelectedFile();
            if (dest == null)
                return;
            if (dest.getName().indexOf('.') == -1)
                dest = new File(dest.getParent(), dest.getName() + ".zip");

            try {
                FileUtils.copyFile(backupFile, dest);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                showErrorMessage();
            }
        }


    }

}
