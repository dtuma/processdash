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

import java.util.*;
import javax.swing.*;


/** This class provides a generally reusable feature for displaying a
 *  collected list of error messages to the user. To use this class,
 *  <ol>
 *    <li>Create an instance of ErrorReporter.
 *    <li>Possibly log one or more error messages using the
 *        <code>logError</code> method.
 *    <li>Call <code>done</code>.
 *  </ol>
 *  If any error messages have been logged when you call the done() method,
 *  they will be displayed to the user in a dialog box. If no errors were
 *  reported, the done() method will do nothing and return.
 */
public class ErrorReporter {

    String title;
    Object preMessage, postMessage;
    Vector errors;

    /** Create a new ErrorReporter.
     *
     * The parameters control the apppearance of the error dialog that
     * this object will display if needed.
     * @param title the title to display
     * @param preMessage the object(s) to display above the error list. See
     *     JOptionPane for valid choices.
     * @param postMessage the object(s) to display below the error list. See
     *     JOptionPane for valid choices.
     */
    public ErrorReporter(String title, Object preMessage, Object postMessage) {
        this.title = title;
        this.preMessage = (preMessage == null ? "" : preMessage);
        this.postMessage = (postMessage == null ? "" : postMessage);
        this.errors = new Vector();
    }

    /** Add an error to this error reporter.
     *
     * If the error message is a duplicate of one already logged for
     * this ErrorReporter, this method call will have no effect.
     */
    public synchronized void logError(String errorMessage) {
        if (!errors.contains(errorMessage))
            errors.add(errorMessage);
    }

    public void done() {
        if (errors.isEmpty()) return;

        StringBuffer errorList = new StringBuffer();
        errorList.append("<html><body><table border=0>\n");
        Iterator i = errors.iterator();
        while (i.hasNext()) {
            errorList.append("<tr><td valign=top><b>-</b></td><td>");
            errorList.append(HTMLUtils.escapeEntities((String) i.next()));
            errorList.append("</td></tr>\n");
        }
        errorList.append("</table></body></html>");
        JEditorPane jep = new JEditorPane("text/html", errorList.toString());
        jep.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(jep);
        scrollPane.setPreferredSize(new java.awt.Dimension(200, 200));

        Object [] dialogComponents = new Object[3];
        dialogComponents[0] = preMessage;
        dialogComponents[1] = scrollPane;
        dialogComponents[2] = postMessage;

        JOptionPane.showMessageDialog(null, dialogComponents, title,
                                      JOptionPane.ERROR_MESSAGE);
    }


    /** Singleton object for reporting errors in template definition
     *  files.
     */
    public static ErrorReporter templates = new ErrorReporter
        (/* title */
         "Process Template Error",

         /* pre message */
         new String[] {
             "When reading the files which define process templates,",
             "the dashboard encountered the following error(s):" },

         /* post message */
         new String[] {
             "Errors in process templates are generally critical.",
             "It is recommended that you shut down the dashboard",
             "immediately and correct the error(s) listed above",
             "before using the dashboard further." });


    /** Singleton object for reporting projects whose datafiles seem
     *  to be missing.
     */
    public static ErrorReporter brokenData = new ErrorReporter
        (/* title */
         "Missing data files",

         /* pre message */
         new String[] {
             "Missing files are preventing the dashboard from opening the",
             "data for the following projects/tasks:" },

         /* post message */
         new String[] {
             "The most likely cause of this problem is that you created",
             "the above projects/tasks based on an add-on process set,",
             "then deleted the file containing that add-on process set.",
             "The recommended course of action is to shut down the",
             "dashboard, reinstall the add-on process set, then restart the",
             "dashboard.  Until you do this, the data for these",
             "projects/tasks will be inaccessible and/or incomplete."});

}
