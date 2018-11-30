// Copyright (C) 2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.prefs.editor;

import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.log.defects.DefectLog;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;

import org.w3c.dom.Element;

public class PreferencesDatasetEncodingconverter extends JPanel {

    /**
     * Before converting the files, we back them up in case something goes wrong
     * during the conversion process. If the conversion process is successful,
     * we delete this folder.
     */
    private static final String BACKUP_FOLDER = "encoding_conversion_backup";

    private BoundMap map;

    private String id;

    private JButton button;

    private static final Logger logger = Logger
            .getLogger(PreferencesDatasetEncodingconverter.class.getName());

    public PreferencesDatasetEncodingconverter(BoundMap map, Element xml) {
        this.map = map;
        this.id = xml.getAttribute("id");

        // We only want to show a control if the current dataset is not already
        // in UTF8 and if the user language is not Engligh
        if (!Settings.getBool(DataRepository.USE_UTF8_SETTING, false)
                && !"en".equalsIgnoreCase(System.getProperty("user.language"))) {
            button = new JButton(this.map.getResource(this.id
                    + ".Button_Label"));
            button.setToolTipText("<html><div width='300'>"
                    + HTMLUtils.escapeEntities(this.map.getResource(
                            this.id + ".Tooltip"))
                    + "</div></html>");
            button.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "convert"));
            this.add(button);
        }
    }

    public void convert() {
        if (InternalSettings.getBool(DataRepository.USE_UTF8_SETTING, false))
            return;

        int choice = JOptionPane.showConfirmDialog(this, MessageFormat.format(
            map.getResource(id + ".Confirm_Dialog_FMT"),
            new Object[] { DataRepository.UTF8_SUPPORT_VERSION }), map
                .getResource(id + ".Dialog_Title"), JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            File[] files = new File(DashController.getSettingsFileName())
                      .getParentFile().listFiles(new ConversionFileFilter());

            if (files == null)
                return;

            String backupFolderPath = files[0].getParent() + File.separator + BACKUP_FOLDER;
            File backupFolder = new File(backupFolderPath);

            try {
                createTempBackup(files, backupFolderPath, backupFolder);

                try {
                    convertFiles(files);

                    // No exception was thrown, which means that the files were correctly
                    // converted. We can set the UTF8 flag, delete the backed up files and
                    // hide the converter button.
                    DataRepository.enableUtf8Encoding();
                    DefectLog.enableXmlStorageFormat();

                    try {
                        FileUtils.deleteDirectory(backupFolder, true);
                    } catch (IOException ioe) {}
                    button.setEnabled(false);

                    JOptionPane.showMessageDialog(this, map.getResource(id
                            + ".Success_Dialog"), map.getResource(id
                            + ".Dialog_Title"), JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    // An error has occurred while converting the files.
                    logger.log(Level.WARNING, e.getMessage());
                    JOptionPane.showMessageDialog(this, MessageFormat.format(map
                        .getResource(id + ".Error_Dialog_FMT"), new Object[] { backupFolder }),
                        map.getResource(id + ".Dialog_Title"), JOptionPane.ERROR_MESSAGE);
                }


            } catch (IOException e) {
                // An error has occurred while backing up the datafiles
                logger.log(Level.WARNING, e.getMessage());
                JOptionPane.showMessageDialog(this, map.getResource(id
                        + ".Backup_Error_Dialog"), map.getResource(id
                        + ".Dialog_Title"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void convertFiles(File[] files) throws IOException {
        for (File f : files) {
            if (f.getName().toLowerCase().endsWith(".dat"))
                convertDataFile(f);
            else
                DefectLog.convertFileToXml(f);
        }
    }

    private void convertDataFile(File f) throws IOException {
        File convertedFile = new File(f.getAbsolutePath() + ".converted");

        FileUtils.reencodeFile(f, System.getProperty("file.encoding"),
            convertedFile, "UTF-8");

        FileUtils.renameFile(convertedFile, f);
    }

    private void createTempBackup(File[] files, String backupFolderPath,
            File backupFolder) throws IOException {
        backupFolder.mkdir();

        for (File f : files)
            FileUtils.copyFile(f,
                new File(backupFolderPath + File.separator + f.getName()));
    }

    private class ConversionFileFilter implements FilenameFilter {

        public boolean accept(File directory, String filename) {
            String filenameLC = filename.toLowerCase();
            return filenameLC.endsWith(".dat") || filenameLC.endsWith(".def");
        }
    }
}
