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
import java.awt.Toolkit;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.URL;

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
    PSPProperties templates = null;
    DataRepository data = null;
    TinyWebServer webServer = null;

    boolean paused = true;
    String timeLogName        = "time.log";
    static final String DEFAULT_PROP_FILE = "state";
    String starting_dir       = "";
    String property_directory = null;
    String propertiesFile     = DEFAULT_PROP_FILE;
    static final String TEMPLATES_FILE = "state";
    PropertyKey currentPhase  = null;

    private static final String TEMPLATES_CLASSPATH = "Templates/";

    private void debug(String msg) {
        System.err.print("PSPDashboard: ");
        System.err.println(msg);
    }

    PSPDashboard(String title) {
        super();
        setTitle(title);
        setIconImage(Toolkit.getDefaultToolkit().createImage
                     (getClass().getResource("icon32.gif")));
        getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 2, 2));
        addWindowListener(this);

        // load app defaults and user settings.
        InternalSettings.initialize("");

        // create the data repository.
        data = new DataRepository();
        templates = TemplateLoader.loadTemplates(data);

        // start the http server.
        try {
            String template_directory =
                Settings.getDir("templates.directory", true);
            if (template_directory != null)
                // if the user has specified a Templates directory,
                // serve http requests out of that directory.
                webServer = new TinyWebServer(template_directory, 2468);

            else
                // default behavior: if no Templates directory is
                // specified, serve http requests out of the classpath.
                webServer = new TinyWebServer(2468, TEMPLATES_CLASSPATH);

            webServer.start();
            webServer.allowRemoteConnections
                (Settings.getVal("http.allowRemote"));
        } catch (IOException ioe) {
            System.err.println("Couldn't start web server: " + ioe);
        }

        //open & load the User's hierarchical work breakdown structure,
        //henceforth referred to as "properties"
        //
        propertiesFile = Settings.getFile("stateFile");
        File prop_file = new File(propertiesFile);
        property_directory = prop_file.getParent() + Settings.sep;

        // determine if Lost Data Files are present in the pspdata directory
        // and take steps to repair them.
        LostDataFiles lostPSPFiles = new LostDataFiles();
        lostPSPFiles.findLostFiles(property_directory);
        if (lostPSPFiles.repair(this)==false) {

            // if the lost data files could not be repaired, exit the dashboard
            System.err.println
                ("Dashboard was terminated due to user request. " +
                 "The following bad data files were found in the "+
                 "psp data directory:\n" + lostPSPFiles.printOut());
            System.exit(0);
        }

        // open and load the properties file.
        props = new PSPProperties(property_directory);
        Vector v = null;
        try {
            // try to load the user's existing properties file.
            v = props.load(propertiesFile);
        } catch (Exception e) { try {
            // apparently, the user doesn't already have a properties file.
            // read the default properties file, which simply contains
            // nodes for "Project" and "Non Project".
            v = props.load(getClass().getResourceAsStream(DEFAULT_PROP_FILE));
        } catch (Exception e2) {
            // this is a serious problem that should never happen if the
            // dashboard is installed correctly.
            debug("read default props failed!");
        } }

        // open all the datafiles that were specified in the properties file.
        try {
            if (v != null) {
                String a[];
                for (int i = 0; i < v.size(); i++) {
                    a = (String[])v.elementAt (i);
                    openDatafile(a[0], a[1]);
                }
            }
        }catch (Exception e) { debug("open datafiles failed!"); };

        configure_button = new ConfigureButton(this);
        pause_button = new PauseButton(this);
        defect_button = new DefectButton(this);
        script_button = new ScriptButton(this);
        getContentPane().add(hierarchy_menubar = new JMenuBar());
        completion_button = new CompletionButton(this);
        hierarchy = new HierarchyButton(this, PropertyKey.ROOT);

        // open the global data file.
        try {
            data.openDatafile("", property_directory + "global.dat");
        } catch (FileNotFoundException exc) {
            // if the user doesn't have a global data file, create one
            // for them from the default template.
            PropertyFrame.createDataFile (property_directory + "global.dat",
                                          "dataFile.txt");
            openDatafile("", "global.dat");

        } catch (Exception exc) {
            System.err.println
                ("when generating default datafile, caught exception "+exc);
            exc.printStackTrace(System.err);
        }

        webServer.setData(data);
        webServer.setProps(props);
    }

    public void openDatafile (String prefix, String dataFile) {
        try {
            data.openDatafile (prefix, property_directory + dataFile);
        } catch (Exception exc) {
            System.err.println("when opening datafile, '" + dataFile +
                               "' for path '" + prefix +
                               "', caught exception " + exc);
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
            props.save(propertiesFile, "properties file");
        } catch (Exception e) { debug("prop write failed."); }
        if (configure_button != null)
            configure_button.save();
        // shouldn't there be something here for the time and defect log, too?
    }

    public String getTimeLog() {
        return ((property_directory != null) ? property_directory : "") +
            timeLogName;
    }

    public String getDirectory() {
        return (property_directory != null) ? property_directory : "";
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
        if (webServer != null) {
            webServer.quit();
            webServer = null;
        }
        if (data != null) {
            data.finalize();
            data = null;
        }

        save();
    }

    public static void main(String args[]) {
        SplashScreen ss = new SplashScreen
            (PSPDashboard.class.getResource("splash.gif"));
        ss.displayFor(3000);      // show for at least 3 seconds.

        PSPDashboard dash = new PSPDashboard("Process Dashboard");

        dash.pack();
        dash.show();

        ss.okayToDispose();
        ss = null;
    }

}
