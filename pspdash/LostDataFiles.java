// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Vector;
import java.lang.reflect.Array;
import javax.swing.JOptionPane;

// class LostDataFiles contains information and methods that are used to
// deal with lost or damaged data files.

public class LostDataFiles implements FilenameFilter {

    private static String acceptFilter = "t"; // used to determine if a file
                                               // is lost
    private static String rejectFilter[] = {"tasks", "time.log"};
                                     // an exception to acceptFilter
    private static String warnMsg =
                             "\nThe above files are corrupt.  Running the\n" +
                             "dashboard may cause data to become\n" +
                             "UNRETRIEVABLE!  Do you wish to run the\n" +
                             "dashboard and RISK LOSING DATA?\n";

    private String lostFiles[]; // contains the list of lost data files

    // the constructor
    public LostDataFiles() {
        lostFiles = null;
    }

    // implements the accept method for the FilenameFilter used by
    // File.list().  Currently a file is accepted if it is not a directory
    // starts with the letters in acceptFilter, and is not one of the
    // strings in rejectFilter.

    public boolean accept(File location, String filename) {

        if (filename.startsWith(acceptFilter)) {
            for(int j = 0; j < rejectFilter.length; j++) {
                if (filename.equals(rejectFilter[j])) {
                    return false;
                }  // end if
            } // end for
        } // end if
        else {
            return false;
        }
    return true;
    }

    // findLostFiles gets a list of files in the searchDir that match the
    // acceptFilter, removes any files that match the rejectFilter, and
    // places the result in lostFiles.

    public void findLostFiles (String searchDir) {
        File searchFile = new File(searchDir);

        // First make sure we have a directory, then get a directory list
        // that matches the accept method
        if (searchFile.isDirectory()) {
            lostFiles = searchFile.list(this);
        }
    }

    // For now, resolve just pops up an information dialog to indicate to
    // the user that lost data must be resolved manually.  A future change
    // may actually allow the user to resolve the problem on the fly.
    // true is returned if the repair was "successful".
    public boolean repair(PSPDashboard dash) {
        int response;

        PSPDashboard parent = dash;

        int lostCount = 0;
            if (lostFiles != null) {
                    lostCount = Array.getLength(lostFiles);
            }

        // If there are lost files, resolve them
        if (lostCount > 0) {
            parent.dropSplashScreen();

            // Create an instance of the InfoDialog
            response = JOptionPane.showConfirmDialog(parent ,
                                                     this.printOut() + warnMsg,
                                                     "CORRUPT DATA FOUND!",
                                                     JOptionPane.YES_NO_OPTION,
                                                     JOptionPane.ERROR_MESSAGE);
            if ((response == JOptionPane.NO_OPTION)||
                (response == JOptionPane.CLOSED_OPTION)) {
                return (false);
            }
        }
        return (true);
    }

    // printOut converts the data in lostFiles into a single printable string
    public String printOut() {
        String result = "";

        int lostCount = Array.getLength(lostFiles);

        // If there are lost files, add them to the result
        for (int i = 0; i < lostCount; i++) {
            result = result + lostFiles[i] + "\n";
        }
    return (result);
    }
}

