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

/*
 * Copyright (c) 1995-1997 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies. Please refer to the file "copyright.html"
 * for further important copyright and licensing information.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

package pspdash;

import java.awt.FlowLayout;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import pspdash.data.DataRepository;
import javax.swing.*;

public class PSPDashboard extends JFrame implements WindowListener {

//    GridBagLayout layout = null;
    ConfigureButton configure_button = null;
    PauseButton pause_button = null;
    ScriptButton script_button = null;
    DefectButton defect_button = null;
    HierarchyButton hierarchy = null;
    CompletionButton completion_button = null;
    JMenuBar hierarchy_menubar = null;

    PSPProperties props;
    PSPProperties templates = new PSPProperties(null);
    DataRepository data = null;

    boolean paused = true;
    String timeLogName        = "time.log";
    String defaultPropFile    = "state";
    String starting_dir       = "";
    String property_directory = null;
    String propertiesFile     = defaultPropFile;
    String template_directory = null;
    String templatesFile      = "state";
    PropertyKey currentPhase  = null;


    private void debug(String msg) {
        System.err.print("PSPDashboard: ");
        System.err.println(msg);
    }

    PSPDashboard(String title) {
        super();
        setTitle(title);
        getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 2, 2));
        addWindowListener(this);

        Settings.initialize("");
        data = new DataRepository();

        propertiesFile = Settings.getVal("stateFile");
        template_directory = (Settings.getVal("templates.directory") +
                              System.getProperty("file.separator"));
        data.addDatafileSearchDir(template_directory);

        //open & load the properties for the application
        File prop_file = new File(propertiesFile);
        property_directory = (prop_file.getParent() +
                              System.getProperty("file.separator"));

        // determine if Lost Data Files are present in the pspdata directory
        // and take steps to repair them.
        LostDataFiles lostPSPFiles = new LostDataFiles();
        lostPSPFiles.findLostFiles(property_directory);
        if (lostPSPFiles.repair(this)==false) {

            // if the lost data files could not be repaired, exit the dashboard
            System.err.println("Dashboard was terminated due to user request. " +
                               "The following bad data files were found in the "+
                               "psp data directory:\n" +
                               lostPSPFiles.printOut());
            System.exit(0);
        }

        props = new PSPProperties(property_directory);
        Vector v = null;
        try {
            v = props.load(propertiesFile);
        } catch (Exception e) { /*debug("read failed!");*/
            try {
                v = props.load(getClass().getResourceAsStream(defaultPropFile));
            } catch (Exception e2) { debug("read default props failed!");};
        };

        try {
            if (v != null) {
                String a[];
                for (int i = 0; i < v.size(); i++) {
                    a = (String[])v.elementAt (i);
                    openDataFile (a[0], /* property_directory + */ a[1]);
                }
            }
        }catch (Exception e) { /*debug("open datafiles failed!");*/ };

        try {
            templates.load(template_directory+templatesFile);
        } catch (Exception e) { /*debug("template read failed!");*/};

        configure_button = new ConfigureButton(this);
        pause_button = new PauseButton(this);
        defect_button = new DefectButton(this);
        script_button = new ScriptButton(this);
        getContentPane().add(hierarchy_menubar = new JMenuBar());
        completion_button = new CompletionButton(this);
        hierarchy = new HierarchyButton(this, PropertyKey.ROOT);

        try {
            data.openDatafile ("", property_directory + "global.dat");
        } catch (FileNotFoundException exc) {
            PropertyFrame.createDataFile (property_directory + "global.dat",
                                          "dataFile.txt");
            try {
                data.openDatafile ("", property_directory + "global.dat");
            } catch (Exception exc2) {
                System.err.println("failed to open global data file.. "+exc2);
                exc2.printStackTrace(System.err);
            }
        } catch (Exception exc) {
            System.err.println("when opening datafiles, caught exception "+exc);
            exc.printStackTrace(System.err);
        }
    }

    public void openDataFile (String prefix, String dataFile) {
        try {
//debug("openDatafile " + prefix + ", " + property_directory + dataFile);
            data.openDatafile (prefix, property_directory + dataFile);
        } catch (Exception exc) {
            System.err.println("when opening datafile, caught exception "+exc);
            exc.printStackTrace(System.err);
        }
    }

    public void refreshHierarchy() {
        hierarchy.delete();
        hierarchy = new HierarchyButton(this, PropertyKey.ROOT);
    }

    public void setCurrentPhase(PropertyKey newPhase) {
        currentPhase = newPhase;
        pause_button.setCurrentPhase(newPhase);
        script_button.setPaths(props.getScriptIDs(currentPhase));
        defect_button.setPaths(props.defectLog(currentPhase,
                                               property_directory));
        completion_button.setPath(newPhase.path());
        save();
    }

    public void addToTimeLogEditor (TimeLogEntry tle) {
        configure_button.addToTimeLogEditor (tle);
    }

    public void save() {
        try {
            props.save(propertiesFile,
                       "properties file");
        } catch (Exception e) { debug("prop write failed."); }
        if (configure_button != null)
            configure_button.save();
    }

    public String getTimeLog() {
        return ((property_directory != null) ? property_directory : "") +
            timeLogName;
    }

    public String getDirectory() {
        return (property_directory != null) ? property_directory : "";
    }

    public String getTemplateDirectory() {
        return (template_directory != null) ? template_directory : "";
    }

    public PSPProperties getProperties() {
        return props;
    }

    public PSPProperties getTemplateProperties() {
        return templates;
    }

    public DataRepository getDataRepository() {
        return data;
    }


    public void windowOpened(WindowEvent w) {}
    public void windowClosed(WindowEvent w) {}
    public void windowIconified(WindowEvent w) {}
    public void windowDeiconified(WindowEvent w) {}
    public void windowActivated(WindowEvent w) {}

    public void windowDeactivated(WindowEvent w) {
        script_button.hidePopup();
    }

    public void windowClosing(WindowEvent w) {
        quit();
        System.exit(0);
    }

    protected void quit() {
        if (hierarchy != null) {
            hierarchy.terminate();
            hierarchy = null;
        }

        save();
    }

    protected void finalize() {
        quit();
    }

    public static void main(String args[]) {
        System.runFinalizersOnExit(true);

        PSPDashboard dash = new PSPDashboard("PSP Dashboard");

        dash.pack();
        dash.show();
    }

}
