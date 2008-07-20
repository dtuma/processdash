// Copyright (C) 2007 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.ui;

import java.awt.event.ActionEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicFileChooserUI;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.quicklauncher.CompressedInstanceLauncher;
import net.sourceforge.processdash.ui.lib.ExampleFileFilter;
import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.XorOutputStream;

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

            fc = new MyJFileChooser();
            fc.setAcceptAllFileFilterUsed(false);
            fc.setSelectedFile(new File(fc.getCurrentDirectory(),
                    getDefaultFilename()));
            fc.setDialogTitle(resources.getString("Save_Backup.Window_Title"));
            fc.addChoosableFileFilter(makeFilter(PDBK));
            fc.addChoosableFileFilter(makeFilter("zip"));

            return null;
        }

        private ExampleFileFilter makeFilter(String ext) {
            String descr = resources.getString("Save_Backup." + ext
                    + ".File_Description");
            return new ExampleFileFilter(ext, descr);
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

            ExampleFileFilter ff = (ExampleFileFilter) fc.getFileFilter();
            dest = ff.maybeAppendExtension(dest);

            try {
                OutputStream out = new BufferedOutputStream(
                        new FileOutputStream(dest));
                if (dest.getName().toLowerCase().endsWith(PDBK))
                    out = new XorOutputStream(out,
                            CompressedInstanceLauncher.PDASH_BACKUP_XOR_BITS);

                FileUtils.copyFile(backupFile, out);
                out.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                showErrorMessage();
            }
        }


    }

    private class MyJFileChooser extends JFileChooser {

        public void setFileFilter(FileFilter filter) {
            super.setFileFilter(filter);

            if (!(getUI() instanceof BasicFileChooserUI))
                return;

            final BasicFileChooserUI ui = (BasicFileChooserUI) getUI();
            String name = ui.getFileName().trim();

            if ((name == null) || (name.length() == 0))
                return;

            int dotPos = name.lastIndexOf('.');
            if (dotPos != -1)
                name = name.substring(0, dotPos);

            if (filter instanceof ExampleFileFilter) {
                ExampleFileFilter eff = (ExampleFileFilter) filter;
                name = eff.maybeAppendExtension(name);
            }

            final String nameToUse = name;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    String currentName = ui.getFileName();
                    if ((currentName == null) || (currentName.length() == 0)) {
                        ui.setFileName(nameToUse);
                    }
                }
            });
        }

    }

    private static final String PDBK = CompressedInstanceLauncher.PDASH_BACKUP_EXTENSION;

}
