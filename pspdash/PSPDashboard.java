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

import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.URL;
import javax.swing.*;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import pspdash.data.DataRepository;
import pspdash.data.DataImporter;

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
    ConcurrencyLock concurrencyLock = null;
    AutoUpdateManager aum = null;

    boolean paused = true;
    String timeLogName        = "time.log";
    static final String DEFAULT_PROP_FILE = "state";
    String starting_dir       = "";
    String property_directory = null;
    static String default_directory = null;
    String propertiesFile     = DEFAULT_PROP_FILE;
    static final String TEMPLATES_FILE = "state";
    PropertyKey currentPhase  = null;
    int httpServerPort = DEFAULT_WEB_PORT;

    private static final String TEMPLATES_CLASSPATH = "Templates/";
    public static final int DEFAULT_WEB_PORT = 2468;

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
        data.setRealizationPolicy(Settings.getVal("autoRealization"));
        if ("true".equalsIgnoreCase(Settings.getVal("dataFreezing.disabled")))
            data.disableFreezing();
        aum = new AutoUpdateManager();
        templates = TemplateLoader.loadTemplates(data, aum);
        data.setDatafileSearchURLs(TemplateLoader.getTemplateURLs());

        // start the http server.
        try {
            String portSetting = Settings.getVal("http.port");
            if (portSetting != null) try {
                httpServerPort = Integer.parseInt(portSetting);
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid value for 'http.port' setting: "
                                   + nfe);
            }
            webServer = new TinyWebServer
                (httpServerPort, TemplateLoader.getTemplateURLs());
            webServer.start();
            webServer.allowRemoteConnections
                (Settings.getVal("http.allowRemote"));
            Browser.setDefaults("localhost", webServer.getPort());
            ScriptID.setNameResolver(new ScriptNameResolver(webServer));
        } catch (IOException ioe) {
            System.err.println("Couldn't start web server: " + ioe);
        }

        //open & load the User's hierarchical work breakdown structure,
        //henceforth referred to as "properties"
        //
        propertiesFile = Settings.getFile("stateFile");
        File prop_file = new File(propertiesFile);
        property_directory = prop_file.getParent() + Settings.sep;
        try {
            default_directory = prop_file.getParentFile().getCanonicalPath();
        } catch (IOException ioe) {
            default_directory = prop_file.getParentFile().getAbsolutePath();
        }
        DefectAnalyzer.setDataDirectory(property_directory);
        TimeLog.setDefaultFilename(getTimeLog());

        // ensure that we have exclusive control of the data in the
        // property_directory
        //
        concurrencyLock = new ConcurrencyLock(property_directory,
                                              webServer.getPort(),
                                              webServer.getTimestamp());

        // Start up the data repository server.
        data.startServer(webServer.getDataSocket());

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
        SAXException se = null;
        if (prop_file.exists()) {
            try {
                // try to load the user's existing properties file.
                try {
                    v = props.loadXML(propertiesFile, templates);
                } catch (SAXException se1) {
                    se = se1;
                    props.load(propertiesFile);
                    props.runV1_4Hack();
                    props.saveXML(propertiesFile, null);
                    props.clear();
                    v = props.loadXML(propertiesFile, templates);
                }
            } catch (Exception e) {
                // this is a serious problem, indicating a corrupt
                // state file.  Display a warning to the user, then exit.
                if (se != null) e = se;
                propertiesFile = prop_file.getAbsolutePath();
                try {
                    propertiesFile = prop_file.getCanonicalPath();
                } catch (Exception e2) {}
                displayCorruptStateFileWarning(propertiesFile, e);
                System.exit(0);
            }
        } else {
            try {
                // apparently, the user doesn't already have a properties
                // file.  read the default properties file, which simply
                // contains nodes for "Project" and "Non Project".
                v = props.load
                    (getClass().getResourceAsStream(DEFAULT_PROP_FILE));
                displayFirstTimeUserHelp();
            } catch (Exception e) {
                System.err.println("Couldn't read default state file: " + e);
            }
        }
        data.setNodeComparator(props);

        // possibly reload cached data definitions.
        File serializedDefinitions = new File(property_directory, "defns.ser");
        if (serializedDefinitions.exists() &&
            (serializedDefinitions.lastModified() >
             TemplateLoader.getTemplateTimestamp()))
            try {
                data.loadDefinitions(new FileInputStream
                    (serializedDefinitions));
            } catch (Exception e) {}

        // open all the datafiles that were specified in the properties file.
        data.startInconsistency();
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
        PCSH.enableHelpKey(this, "QuickOverview");
        pause_button = new PauseButton(this);
        defect_button = new DefectButton(this);
        script_button = new ScriptButton(this);
        getContentPane().add(hierarchy_menubar = new JMenuBar());
        completion_button = new CompletionButton(this);

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
        DataImporter.init(data, Settings.getFile("import.directories"));
        data.finishInconsistency();
        try {
            data.saveDefinitions(new FileOutputStream(serializedDefinitions));
        } catch (Exception e) {
            e.printStackTrace();
        }
        ImportExport.registerUserSetting(this, Settings.getVal("export.data"));

        webServer.setData(data);
        webServer.setProps(props);

        hierarchy = new HierarchyButton(this, PropertyKey.ROOT);

        if (!brokenData.isEmpty())
            displayBrokenDataWarning();
    }

    private static final String FIRST_TIME_HELP_URL = "/help/first-use.htm";
    private void displayFirstTimeUserHelp() {
        new AboutDialog(null, "Welcome", FIRST_TIME_HELP_URL);
    }

    private Vector brokenData = new Vector();
    private void displayBrokenDataWarning() {
        JList brokenList = new JList(brokenData);
        BROKEN_DATA_WARNING[2] = new JScrollPane(brokenList);
        JOptionPane.showMessageDialog(null, BROKEN_DATA_WARNING,
                                      "Missing data files",
                                      JOptionPane.ERROR_MESSAGE);
    }
    private static final String BULLET = "\u2022 ";
    private static final Object[] BROKEN_DATA_WARNING = {
        "Missing files are preventing the dashboard from opening the",
        "data for the following projects/tasks:",
        "",
        "The most likely cause of this problem is that you created",
        "the above projects/tasks based on an add-on process set,",
        "then deleted the file containing that add-on process set.",
        "The recommended course of action is to shut down the",
        "dashboard, reinstall the add-on process set, then restart the",
        "dashboard.  Until you do this, the data for these",
        "projects/tasks will be inaccessible and/or incomplete."};

    private void displayCorruptStateFileWarning(String filename,
                                                Exception e) {
        CORRUPT_STATEFILE_WARNING[6] = "    " + filename;
        CORRUPT_STATEFILE_WARNING[8] = "    " + e.getMessage();
        int lineNum = -1;
        if (e instanceof SAXParseException)
            lineNum = ((SAXParseException) e).getLineNumber();
        if (lineNum != -1)
            CORRUPT_STATEFILE_WARNING[7] =
                CORRUPT_STATEFILE_WARNING[7] + " on line " + lineNum + ":";
        else
            CORRUPT_STATEFILE_WARNING[7] =
                CORRUPT_STATEFILE_WARNING[7] + ":";

        JOptionPane.showMessageDialog(null, CORRUPT_STATEFILE_WARNING,
                                      "Cannot read hierarchy file",
                                      JOptionPane.ERROR_MESSAGE);
    }
    private static final Object[] CORRUPT_STATEFILE_WARNING = {
        "The dashboard was unable to open and read the file that",
        "contains your hierarchy.  This problem could be caused by",
        "insufficient file permissions, or by corrupt data in the",
        "file itself.  This is a very serious problem; the dashboard",
        "will not continue until the problem is corrected.  Please",
        "examine the file:",
        "",
        "and correct the following error",
        "",
        "If you cannot correct this error, the only other course of",
        "action is to rename or delete the file.  This effectively",
        "will cause all your data to be lost." };

    public void openDatafile (String prefix, String dataFile) {
        try {
            data.openDatafile (prefix, property_directory + dataFile);
        } catch (FileNotFoundException fnfe) {
            brokenData.add(prefix);
        } catch (Exception exc) {
            System.err.println("when opening datafile, '" + dataFile +
                               "' for path '" + prefix +
                               "', caught exception " + exc);
            exc.printStackTrace(System.err);
        }
    }

    private static void ensureJRE13() {
        String versionNum = System.getProperty("java.version");
        if (versionNum.startsWith("1.2")) {
            //Class.forName("javax.sound.sampled.Clip") == null) {
            String vendorURL = System.getProperty("java.vendor.url");
            JRE_REQ_MESSAGE[3] += versionNum + ".";
            JRE_REQ_MESSAGE[5] += vendorURL;
            JOptionPane.showMessageDialog(null, JRE_REQ_MESSAGE,
                                          "JRE 1.3 Required",
                                          JOptionPane.ERROR_MESSAGE);
            Browser.launch(vendorURL);
            System.exit(0);
        }
    }
    private static final String[] JRE_REQ_MESSAGE = {
        "You need to upgrade your Java Runtime Environment!  This",
        "version of the Process Dashboard requires version 1.3 or",
        "higher of the Java Runtime Environment.  You are currently",
        "running Java Runtime Environment version ",
        "    To download an updated version of the Java Runtime",
        "Environment, visit   " };


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

    public PropertyKey getCurrentPhase() { return currentPhase; }

    public void addToTimeLogEditor (TimeLogEntry tle) {
        configure_button.addToTimeLogEditor (tle);
    }

    public void releaseTimeLogEntry (TimeLogEntry tle) {
        pause_button.maybeReleaseEntry(tle);
    }

    public void save() {
        try {
            props.save(propertiesFile, "hierarchical work breakdown structure");
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

    public static String getDefaultDirectory() { return default_directory; }

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
    public void windowDeactivated(WindowEvent w) {}

    public void windowClosing(WindowEvent w) {
        exitProgram();
    }

    void exitProgram() {
        quit();
        System.exit(0);
    }

    protected void quit() {
        TaskScheduleChooser.closeAll();
        if (data != null)
            data.saveAllDatafiles();
        ImportExport.exportAll(this, Settings.getVal("export.data"));
        if (hierarchy != null) {
            hierarchy.terminate();
            hierarchy = null;
        }
        if (configure_button != null) {
            configure_button.quit();
            configure_button = null;
        }
        if (webServer != null) {
            webServer.quit();
            webServer = null;
        }
        if (data != null) {
            data.finalize();
            data = null;
        }
        if (concurrencyLock != null) {
            concurrencyLock.unlock();
            concurrencyLock = null;
        }

        save();
    }

    static SplashScreen ss = null;

    public static void dropSplashScreen() {
        if (ss != null) ss.okayToDispose();
        ss = null;
    }

    public static void main(String args[]) {
        ss = new SplashScreen(PSPDashboard.class.getResource("splash.gif"));
        ss.displayFor(3000);      // show for at least 3 seconds.

        ensureJRE13();

        PSPDashboard dash = new PSPDashboard("Process Dashboard");
        DashController.setDashboard(dash);

        dash.pack();
        dash.show();

        dropSplashScreen();
        dash.aum.maybePerformCheck(dash);
    }

}
