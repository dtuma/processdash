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

import javax.swing.*;
import javax.swing.event.*;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;
import javax.help.*;
import java.io.File;
import java.net.URL;

class ConfigureButton extends JMenuBar implements ActionListener {
    PSPDashboard   parent       = null;
    PropertyFrame  prop_frame   = null;
    TaskTemplate   task_frame   = null;
    TimeLogEditor  time_frame   = null;
    DefectEditor   defect_frame = null;
    ImportExport   impexp_frame = null;
    ProbeDialog    probe_dialog = null;
    HelpSet        hs           = null;
    DashHelpBroker hb           = null;

    static String FILE_SEP = null;
    static final String ANALYSIS_URL = "/reports/index.htm";
// static final String HELP_URL    = "/help/PSPDash.htm";
    static final String ABOUT_URL    = "/help/Topics/Overview/about.htm";

                                  // indices into menu labels
    static final int HIERARCHY_FRAME    = 0;
    static final int TIME_LOG_FRAME     = 1;
    static final int DEFECT_LOG_FRAME   = 2;
    static final int PROBE_DIALOG       = 3;
    //static final int TASK_DIALOG      = 4; // disabled
    static final int DATA_ANALYSIS      = 4;
    static final int IMPORT_EXPORT      = 5;
    static final int HELP_FRAME         = 6;
    static final int ABOUT_DIALOG       = 7;
    static final int EXIT_PROGRAM       = 8;
    static final int FIRST_HISTORY_ITEM = 8;
    static final int MAX_HISTORY_SIZE   = 0;

                                  // menu labels & cmd text (see above)
    static final String [] menuLabels =
        {new String ("Hierarchy"),
         new String ("Time Log"),
         new String ("Defect Log"),
         new String ("PROBE"),
         //     new String ("Task & Schedule"),
         new String ("Data Analysis"),
         new String ("Export"),
         new String ("Help"),
         new String ("About"),
         new String ("Exit")};

    ConfigureButton(PSPDashboard dash) {
        super();
        parent = dash;

        String    s;
        JMenu     menu = new JMenu("C");
        JMenuItem menuItem;
        JMenuItem helpItem = null;
        add (menu);

        for (int ii = 0; ii < menuLabels.length; ii++) {
            if (ii != HELP_FRAME) {
                menuItem = menu.add(new JMenuItem(menuLabels[ii]));
                menuItem.setActionCommand(menuLabels[ii]);
                menuItem.addActionListener(this);
            } else {
                helpItem = new JMenuItem(menuLabels[HELP_FRAME]);
                menu.add(helpItem);
            }
        }

        //popup.addSeparator();
        //add history elements here? (up to MAX_HISTORY_SIZE)

        dash.getContentPane().add(this);

                                    // get needed system properties
        Properties prop = System.getProperties ();
        FILE_SEP = prop.getProperty ("file.separator");

        // add help listener
        try {
            URL hsURL = TemplateLoader.resolveURL(HELPSET_PATH);

            hs = new HelpSet(null,hsURL);
            //System.out.println("Found help set at " + hsURL);

            hb = new DashHelpBroker(hs);

            // set the size for the display
            hb.setSize(new Dimension(600,450));

            CSH.setHelpIDString(helpItem ,"QuickOverview");

            ActionListener helper = new CSH.DisplayHelpFromSource(hb);
            helpItem.addActionListener(helper);

        } catch (Exception e) {
            System.out.println("Error on help");
        }
    }

    private static final String HELPSET_PATH = "/help/PSPDash.hs";

    public void quit () {
        if (time_frame != null) {
            time_frame.confirmClose(false);
            time_frame = null;
        }
        if (prop_frame != null) {
            prop_frame.confirmClose(false);
            prop_frame = null;
        }
    }

//  public void setHistoryItem(String label, String dest) {
//    update history portion of popup menu here (save where?????)
//  }

    protected void startPropertyFrame () {
        if (parent.getProperties() != null) {
            if (prop_frame != null)
                prop_frame.show();
            else
                prop_frame = new PropertyFrame(parent,
                                               this,
                                               parent.getProperties(),
                                               parent.getTemplateProperties());
        }
    }

    protected void removePropertyFrame () {
        prop_frame = null;
    }

    protected void startProbeDialog () {
        if (probe_dialog != null)
            probe_dialog.show();
        else
            probe_dialog = new ProbeDialog(parent);
    }

    public void startTimeLog() {
        if (parent.getProperties() != null) {
            if (time_frame != null)
                time_frame.show();
            else
                time_frame = new TimeLogEditor(parent,
                                               this,
                                               parent.getProperties());
        }
    }

    public void startDefectLog() {
        if (parent.getProperties() != null) {
            if (defect_frame != null)
                defect_frame.showIt();
            else
                defect_frame = new DefectEditor(parent,
                                                this,
                                                parent.getProperties());
        }
    }

    public void startImportExport() {
        if (parent.getProperties() != null) {
            if (impexp_frame != null)
                impexp_frame.show();
            else
                impexp_frame = new ImportExport(parent);
        }
    }

    public void addToTimeLogEditor (TimeLogEntry tle) {
        if (time_frame != null)
            time_frame.addRow (tle);
    }

    public void startTaskDialog() {
        if (parent.getProperties() != null) {
            if (task_frame != null)
                task_frame.show();
            else
                task_frame = new TaskTemplate(parent,
                                              this,
                                              parent.getProperties());
        }
    }

    public void startDataAnalysis() { Browser.launch(ANALYSIS_URL); }

// public void startHelp() { Browser.launch(HELP_URL); }

    public void startAboutDialog() { new AboutDialog(parent, ABOUT_URL); }

    public void exitProgram() { parent.exitProgram(); }

    public void save() {
        if (task_frame != null)
            task_frame.save();
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals(menuLabels[HIERARCHY_FRAME])) {
            startPropertyFrame ();
        } else if (cmd.equals(menuLabels[TIME_LOG_FRAME])) {
            startTimeLog();
        } else if (cmd.equals(menuLabels[DEFECT_LOG_FRAME])) {
            startDefectLog();
        } else if (cmd.equals(menuLabels[PROBE_DIALOG])) {
            startProbeDialog ();
//  } else if (cmd.equals(menuLabels[TASK_DIALOG])) {
//    startTaskDialog ();
        } else if (cmd.equals(menuLabels[DATA_ANALYSIS])) {
            startDataAnalysis ();
        } else if (cmd.equals(menuLabels[IMPORT_EXPORT])) {
            startImportExport ();
//  } else if (cmd.equals(menuLabels[HELP_FRAME])) {
//    startHelp ();
        } else if (cmd.equals(menuLabels[ABOUT_DIALOG])) {
            startAboutDialog ();
        } else if (cmd.equals(menuLabels[EXIT_PROGRAM])) {
            exitProgram ();
        }
    }

}
