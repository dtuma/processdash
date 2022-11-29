// Copyright (C) 2001-2022 Tuma Solutions, LLC
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.util.StringUtils;


public class BetaVersionSetup {

    private static final boolean enable = false;

    /** Build a submenu containing beta-related options, and add it to
     * <code>menu</code> */
    public static final void addSubmenu(JMenu menu) {
        if (enable) {
            JMenu betaMenu = new JMenu(getVersion() + "-beta");
            menu.add(betaMenu);

            // workaround jre 1.3 bug...reference http://developer.java.sun.com/developer/bugParade/bugs/4280243.html
            betaMenu.enableInputMethods(false);

            // add a "submit bug report" menu option
            JMenuItem menuItem = new JMenuItem("Submit bug report");
            betaMenu.add(menuItem);
            menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Browser.launch(ConfigureButton.BUG_URL); } });

            // add a "debugging output" menu option
            betaMenu.add(menuItem = new JMenuItem("View debugging output"));
            menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ConsoleWindow c = ConsoleWindow.getInstalledConsole();
                        if (c != null) c.setVisible(true);
                    } } );
        }
    }


    public static final void runSetup(String property_directory) {
        if (enable && Settings.isReadWrite()) {
            // Try to backup all the files in the user's data
            // directory.  This will only happen once, the first time
            // this beta version is run.
            File backupDirectory =
                new File(property_directory, "backup_" + getVersion());
            if (!backupDirectory.exists() && backupDirectory.mkdir())
                copyDir(new File(property_directory), backupDirectory);

            Object message[] = new Object[] { StringUtils.split
                (StringUtils.findAndReplace(BETA_WARNING_MESSAGE, "VERSION",
                                            getVersion()),
                 "\n"), new JOptionPaneTweaker.ToFront() };

            // display a beta warning message to the user.
            JOptionPane.showMessageDialog(null, message, "Beta software",
                                          JOptionPane.WARNING_MESSAGE);
        }
    }
    private static final String BULLET = "\u2022  ";
    private static String BETA_WARNING_MESSAGE =
        "This is a beta release of the Process Dashboard.\n" +
        BULLET+"Please be watchful for unusual behavior; if you encounter\n"+
        "   a bug, please submit a bug report.  (The 'VERSION-beta' menu on\n"+
        "   the 'C' menu contains a shortcut to the bug report form.)\n"+
        BULLET+"If you use this software with real-world project data, do\n"+
        "   so with caution and doublecheck the calculations.\n"+
        BULLET+"Please check the website http://www.processdash.com\n"+
        "   and download the final release of version VERSION when it\n"+
        "   becomes available.\n"+
        "Thank you for your willingness to evaluate this beta release! The\n"+
        "Process Dashboard development team appreciates your support.";


    /** Copy all the files in a directory.
     * @param srcDir the source directory
     * @param destDir the destintation directory
     */
    private static final void copyDir(File srcDir, File destDir) {
        File [] files = srcDir.listFiles();
        byte [] buffer = new byte[4096];
        for (int i = files.length;   i-- > 0;  )
            if (files[i].isFile())
                copyFile(files[i], destDir, buffer);
    }


    /** Copy a file.
     * @param srcFile the source file to copy
     * @param destDir the directory to copy the file to
     * @param buffer a buffer to use for copying
     */
    private static final void copyFile(File srcFile, File destDir,
                                       byte[] buffer) {
        try {
            File destFile = new File(destDir, srcFile.getName());
            InputStream in = new FileInputStream(srcFile);
            OutputStream out = new FileOutputStream(destFile);

            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1)
                out.write(buffer, 0, bytesRead);
            in.close();
            out.close();
        } catch (IOException ioe) {
            System.err.println("Couldn't copy file '" + srcFile +
                               "' to directory '" + destDir + "'");
        }
    }



    private static String getVersion() {
        return ProcessDashboard.getVersionNumber();
    }

}
