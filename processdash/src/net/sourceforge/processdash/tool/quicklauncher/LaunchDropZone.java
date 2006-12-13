// Copyright (C) 2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.quicklauncher;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;

import net.sourceforge.processdash.FileBackupManager;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.JOptionPaneClickHandler;
import net.sourceforge.processdash.util.LightweightSet;


public class LaunchDropZone extends TransferHandler {

    QuickLauncher launcher;

    private static final Resources resources = QuickLauncher.resources;

    public LaunchDropZone(QuickLauncher launcher) {
        this.launcher = launcher;
    }

    public boolean canImport(JComponent comp, DataFlavor[] flavors) {
        for (int i = 0; i < flavors.length; i++) {
            if (DataFlavor.javaFileListFlavor.equals(flavors[i]))
                return true;
        }
        return false;
    }

    public boolean importData(JComponent comp, Transferable t) {
        List files = null;
        try {
            files = (List) t.getTransferData(DataFlavor.javaFileListFlavor);
        } catch (Exception ex) {}
        if (files == null || files.isEmpty())
            return false;

        Set targets = new LightweightSet();
        for (Iterator i = files.iterator(); i.hasNext();) {
            File f = (File) i.next();
            InstanceLauncher l = getLauncher(comp, f);
            if (l != null)
                targets.add(l);
        }

        launcher.launchInstances(targets);

        return true;
    }


    private InstanceLauncher getLauncher(JComponent comp, File f) {
        String basename = f.getName().toLowerCase();

        if (f.isDirectory()) {
            if ("backup".equals(basename) || "cms".equals(basename))
                f = f.getParentFile();
            return getDirLauncher(comp, f);
        }

        if (!f.isFile())
            return null;

        if (FileBackupManager.inBackupSet(f.getParentFile(), basename))
            return getDirLauncher(comp, f.getParentFile());

        if (basename.endsWith(".zip"))
            return getZipLauncher(comp, f);

        return null;
    }

    private InstanceLauncher getDirLauncher(JComponent comp, File dir) {
        File testFile = new File(dir, InstanceLauncher.DATA_DIR_FILE_ITEM);
        if (testFile.isFile())
            return new DirectoryInstanceLauncher(dir);

        List dirs;
        try {
            dirs = DirectoryInstanceLauncher.getDataDirectoriesWithinDir(dir);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(comp,
                    resources.formatStrings(
                            "Errors.Dir.Read_Error_FMT",
                            dir.getAbsolutePath(), e.getLocalizedMessage()),
                    resources.getString("Errors.Dialog_Title"),
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        if (dirs == null || dirs.isEmpty()) {
            JOptionPane.showMessageDialog(comp,
                    resources.formatStrings("Errors.Dir.No_Data_Found_FMT",
                            dir.getAbsolutePath()),
                    resources.getString("Errors.No_Data_Found"),
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        if (dirs.size() == 1)
            return new DirectoryInstanceLauncher((File) dirs.get(0));

        JList list = new JList(dirs.toArray());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        new JOptionPaneClickHandler().install(list);
        Object[] message = new Object[] {
                resources.formatStrings("Errors.Dir.Multiple_Data_Found_FMT",
                        dir.getAbsolutePath()),
                new JScrollPane(list) };
        if (JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(comp,
                message, resources.getString("Multiple_Data_Found_Title"),
                JOptionPane.OK_CANCEL_OPTION))
            return null;
        dir = (File) list.getSelectedValue();
        if (dir == null)
            return null;
        else
            return new DirectoryInstanceLauncher(dir);
    }

    private InstanceLauncher getZipLauncher(JComponent comp, File f) {
        List prefixes;
        try {
            prefixes = CompressedInstanceLauncher
                    .getDataDirectoriesWithinZip(f);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(comp,
                    resources.formatStrings("Errors.Zip.Read_Error_FMT",
                            f.getAbsolutePath(), e.getLocalizedMessage()),
                    resources.getString("Errors.Dialog_Title"),
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        if (prefixes == null || prefixes.isEmpty()) {
            JOptionPane.showMessageDialog(comp,
                    resources.formatStrings("Errors.Zip.No_Data_Found_FMT",
                            f.getAbsolutePath()),
                    resources.getString("Errors.No_Data_Found"),
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        if (prefixes.size() == 1)
            return new CompressedInstanceLauncher(f, (String) prefixes.get(0));

        JList list = new JList(prefixes.toArray());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        new JOptionPaneClickHandler().install(list);
        Object[] message = new Object[] {
                resources.formatStrings("Errors.Zip.Multiple_Data_Found_FMT",
                        f.getAbsolutePath()),
                new JScrollPane(list) };
        if (JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(comp,
                message, resources.getString("Multiple_Data_Found_Title"),
                JOptionPane.OK_CANCEL_OPTION))
            return null;
        String prefix = (String) list.getSelectedValue();
        if (prefix == null)
            return null;
        else {
            InstanceLauncher result = new CompressedInstanceLauncher(f, prefix);
            result.setDisplay(resources.format("Launcher.Zip_Display_FMT",
                    prefix, result.getDisplay()));
            return result;
        }
    }
}
