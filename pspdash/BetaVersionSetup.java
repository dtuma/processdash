// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package pspdash;

import java.awt.event.*;
import java.io.*;
import javax.swing.*;


class BetaVersionSetup {

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
                        Browser.launch("http://sourceforge.net/tracker/?group_id=9858&atid=109858"); } });

            // add a "debugging output" menu option
            betaMenu.add(menuItem = new JMenuItem("View debugging output"));
            menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ConsoleWindow c = ConsoleWindow.getInstalledConsole();
                        if (c != null) c.show();
                    } } );
        }
    }


    public static final void runSetup(String property_directory) {
        if (enable) {
            // Try to backup all the files in the user's data
            // directory.  This will only happen once, the first time
            // this beta version is run.
            File backupDirectory =
                new File(property_directory, "backup_" + getVersion());
            if (!backupDirectory.exists() && backupDirectory.mkdir())
                copyDir(new File(property_directory), backupDirectory);

            String message[] = StringUtils.split
                (StringUtils.findAndReplace(BETA_WARNING_MESSAGE, "VERSION",
                                            getVersion()),
                 "\n");

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
        BULLET+"Please check the website http://processdash.sourceforge.net\n"+
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
        return PSPDashboard.getVersionNumber();
    }

}
