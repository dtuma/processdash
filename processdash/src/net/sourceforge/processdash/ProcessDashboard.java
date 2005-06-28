// Process Dashboard - Data Automation Tool for high-maturity processes
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash;

import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.URL;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;

import net.sourceforge.processdash.data.repository.DataImporter;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.ui.*;
import net.sourceforge.processdash.hier.*;
import net.sourceforge.processdash.hier.DashHierarchy.Event;
import net.sourceforge.processdash.hier.ui.*;
import net.sourceforge.processdash.i18n.*;
import net.sourceforge.processdash.log.*;
import net.sourceforge.processdash.log.ui.*;
import net.sourceforge.processdash.net.cache.*;
import net.sourceforge.processdash.net.http.*;
import net.sourceforge.processdash.process.*;
import net.sourceforge.processdash.process.ui.*;
import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.security.DashboardSecurity;
import net.sourceforge.processdash.templates.*;
import net.sourceforge.processdash.tool.export.*;
import net.sourceforge.processdash.tool.export.mgr.ImportManager;
import net.sourceforge.processdash.ui.*;
import net.sourceforge.processdash.ui.help.*;
import net.sourceforge.processdash.ui.lib.*;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.StringUtils;


public class ProcessDashboard extends JFrame implements WindowListener, DashboardContext {

    ConfigureButton configure_button = null;
    PauseButton pause_button = null;
    ScriptButton script_button = null;
    DefectButton defect_button = null;
    HierarchyMenu hierarchy = null;
    CompletionButton completion_button = null;
    JMenuBar hierarchy_menubar = null;

    DashHierarchy props;
    DashHierarchy templates = null;
    DataRepository data = null;
    WebServer webServer = null;
    ConcurrencyLock concurrencyLock = null;
    AutoUpdateManager aum = null;
    ConsoleWindow consoleWindow = new ConsoleWindow();
    ObjectCache objectCache;
    private ErrorReporter brokenData;
    Resources resources;

    boolean paused = true;
    String timeLogName        = "time.log";
    static final String DEFAULT_PROP_FILE = "state";
    String starting_dir       = "";
    String property_directory = null;
    static String default_directory = null;
    String propertiesFile     = DEFAULT_PROP_FILE;
    static final String TEMPLATES_FILE = "state";
    private PropertyKey currentPhase  = null;
    int httpServerPort = DEFAULT_WEB_PORT;
    private static String versionNumber;

    private static final String TEMPLATES_CLASSPATH = "Templates/";
    public static final int DEFAULT_WEB_PORT = 2468;

    private void debug(String msg) {
        System.err.print("ProcessDashboard: ");
        System.err.println(msg);
    }

    public ProcessDashboard(String title) {
        super();
        setIconImage(DashboardIconFactory.getWindowIconImage());
        getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 2, 2));
        addWindowListener(this);

        // load app defaults and user settings.
        InternalSettings.initialize("");

        // run the backup process as soon as possible
        propertiesFile = Settings.getFile("stateFile");
        File prop_file = new File(propertiesFile);
        property_directory = prop_file.getParent() + Settings.sep;
        FileBackupManager.maybeRun
            (property_directory, FileBackupManager.STARTUP);

        // create the data repository.
        data = new DataRepository();
        data.setRealizationPolicy(Settings.getVal("autoRealization"));
        if ("true".equalsIgnoreCase(Settings.getVal("dataFreezing.disabled")))
            data.disableFreezing();
        templates = TemplateLoader.loadTemplates(data);
        aum = new AutoUpdateManager(TemplateLoader.getPackages());
        resources = Resources.getDashBundle("ProcessDashboard");
        InternalSettings.loadLocaleSpecificDefaults(resources);
        FormatUtil.setDateFormats(Settings.getVal("dateFormat"),
                Settings.getVal("dateTimeFormat"));
        Translator.init();
        LookAndFeelSettings.loadLocalizedSettings();
        data.setDatafileSearchURLs(TemplateLoader.getTemplateURLs());
        versionNumber = TemplateLoader.getPackageVersion("pspdash"); // legacy
        System.out.println("Process Dashboard version " + versionNumber);
        setTitle(title != null ? title : resources.getString("Window_Title"));

        // start the http server.
        try {
            String portSetting = Settings.getVal("http.port");
            if (portSetting != null) try {
                httpServerPort = Integer.parseInt(portSetting);
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid value for 'http.port' setting: "
                                   + nfe);
            }
            webServer = new WebServer
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
        try {
            default_directory = prop_file.getParentFile().getCanonicalPath();
        } catch (IOException ioe) {
            default_directory = prop_file.getParentFile().getAbsolutePath();
        }
        DefectAnalyzer.setDataDirectory(property_directory);
        TimeLog.setDefaultFilename(getTimeLog());

        BetaVersionSetup.runSetup(property_directory);

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
        props = new DashHierarchy(property_directory);
        Vector v = null;
        Exception saxException = null;
        if (prop_file.exists()) {
            try {
                // try to load the user's existing properties file.
                try {
                    v = props.loadXML(propertiesFile, templates);
                } catch (Exception se1) {
                    if (!safeInstanceof(se1, "org.xml.sax.SAXException"))
                        throw se1;
                    saxException = se1;
                    props.load(propertiesFile);
                    LegacySupport.fixupV13ScriptIDs(props);
                    props.saveXML(propertiesFile, null);
                    props.clear();
                    v = props.loadXML(propertiesFile, templates);
                }
            } catch (Exception e) {
                // this is a serious problem, indicating a corrupt
                // state file.  Display a warning to the user, then exit.
                if (saxException != null) e = saxException;
                propertiesFile = prop_file.getAbsolutePath();
                try {
                    propertiesFile = prop_file.getCanonicalPath();
                } catch (Exception e2) {}
                DashHierarchy.displayCorruptStateFileWarning
                    (resources, propertiesFile, e);
                System.exit(0);
            }
        } else {
            try {
                // apparently, the user doesn't already have a properties
                // file.  read the default properties file, which simply
                // contains nodes for "Project" and "Non Project".
                String state = new String
                    (WebServer.slurpContents(getClass().getResourceAsStream
                                                 (DEFAULT_PROP_FILE), true),
                     "ISO-8859-1");
                // localize the strings "Project" and "Non Project"
                state = StringUtils.findAndReplace
                    (state, "<Project>",
                     "<"+resources.getString("Project")+">");
                state = StringUtils.findAndReplace
                    (state, "<Non Project>",
                     "<"+resources.getString("Non_Project")+">");

                v = props.load(new StringReader(state), true);
                displayFirstTimeUserHelp();
            } catch (Exception e) {
                System.err.println("Couldn't read default state file: " + e);
                e.printStackTrace();
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
        brokenData = new ErrorReporter
            (resources.getString("Errors.Broken_Data_Title"),
             resources.getStrings("Errors.Broken_Data_Header"),
             resources.getStrings("Errors.Broken_Data_Footer"));
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
            HierarchyEditor.createDataFile (property_directory + "global.dat",
                                          "dataFile.txt");
            openDatafile("", "global.dat");

        } catch (Exception exc) {
            System.err.println
                ("when generating default datafile, caught exception "+exc);
            exc.printStackTrace(System.err);
        }
        ImportManager.init(data);
        data.finishInconsistency();
        try {
            data.maybeSaveDefinitions(serializedDefinitions);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ImportExport.startAutoExporter(this, property_directory);
        LegacySupport.configureRemoteListeningCapability(data);

        try {
            objectCache =
                new FileObjectCache(new File(property_directory), ".obj");
        } catch (IOException ioe) {
            // not possible?
            ioe.printStackTrace();
        }

        webServer.setData(data);
        webServer.setProps(props);
        webServer.setCache(objectCache);

        hierarchy = new HierarchyMenu
            (this, hierarchy_menubar, PropertyKey.ROOT);
        if (Settings.getVal(COMPLETION_FLAG_SETTING) == null) {
            hierarchy.cleanupCompletionFlags();
            InternalSettings.set(COMPLETION_FLAG_SETTING, "true");
        }
        props.addHierarchyListener(new DashHierarchy.Listener() {
                public void hierarchyChanged(Event e) {
                    refreshHierarchy();
                }});

        brokenData.done();
        TemplateLoader.showTemplateErrors();
    }

    private static final String FIRST_TIME_HELP_URL = "/help/first-use.htm";
    private void displayFirstTimeUserHelp() {
        new AboutDialog(null, resources.getString("Welcome_Dialog_Title"),
                        FIRST_TIME_HELP_URL);
    }
    private boolean safeInstanceof(Object c, String className) {
        Class clz = c.getClass();
        while (clz != null) {
            if (clz.getName().equals(className)) return true;
            clz = clz.getSuperclass();
        }
        return false;
    }


    private static final String BULLET = "\u2022 ";
    private static final String COMPLETION_FLAG_SETTING =
        "internal.ranCompletionFlagCleanup";

    public void openDatafile (String prefix, String dataFile) {
        try {
            data.openDatafile (prefix, property_directory + dataFile);
        } catch (FileNotFoundException fnfe) {
            brokenData.logError(prefix);
        } catch (Exception exc) {
            System.err.println("when opening datafile, '" + dataFile +
                               "' for path '" + prefix +
                               "', caught exception " + exc);
            exc.printStackTrace(System.err);
        }
    }

    private static void ensureJRE13() {
        String versionNum = System.getProperty("java.version");
        String req = "1.4";
        if (DashPackage.compareVersions(versionNum, req) < 0) {
            Resources res = Resources.getDashBundle("ProcessDashboard.Errors");
            String vendorURL = System.getProperty("java.vendor.url");
            JOptionPane.showMessageDialog
                (null,
                 res.formatStrings("JRE_Requirement_Message_FMT",
                                   versionNum, vendorURL, req),
                 res.format("JRE_Requirement_Title_FMT", req),
                 JOptionPane.ERROR_MESSAGE);
            Browser.launch(vendorURL);
            System.exit(0);
        }
    }

    boolean addTemplateJar(String jarfileName) {
        if (!TemplateLoader.addTemplateJar(data, templates, jarfileName))
            return false;

        URL [] templateRoots = TemplateLoader.getTemplateURLs();
        data.setDatafileSearchURLs(templateRoots);
        webServer.setRoots(templateRoots);
        return true;
    }

    void changeHttpPort(int newPort) {
        try {
            webServer.addExtraPort(newPort);
            Browser.setDefaults("localhost", newPort);
            data.startSecondServer(webServer.getDataSocket());
        } catch (IOException ioe) {}
    }

    private void refreshHierarchy() {
        if (SwingUtilities.isEventDispatchThread())
            refreshHierarchyImpl();
        else try {
            SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() { refreshHierarchyImpl(); }});
        } catch (Exception e) { }
    }
    private void refreshHierarchyImpl() {
        hierarchy.delete();
        hierarchy = new HierarchyMenu
            (this, hierarchy_menubar, PropertyKey.ROOT);
    }

    public HierarchyMenu getHierarchyMenu() {
        return hierarchy;
    }


    public void pauseTimer() {
        pause_button.pause();
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

    public boolean isHierarchyEditorOpen() {
        return configure_button.isHierarchyEditorOpen();
    }

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

    public DashHierarchy getProperties() {
        return props;
    }

    public DashHierarchy getTemplateProperties() {
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

    public void exitProgram() {
        new DashboardPermission("exitProgram").checkPermission();

        try {
            quit();
        } catch (Throwable t) {
            // if the shutdown sequence encounters an uncaught exception,
            // display an error message, but still exit.
            System.err.println
                ("When shutting down, encountered the exception " + t);
            t.printStackTrace();
        }
        FileBackupManager.maybeRun
            (property_directory, FileBackupManager.SHUTDOWN);

        System.exit(0);
    }

    protected void quit() {
        TaskScheduleChooser.closeAll();
        if (data != null)
            data.saveAllDatafiles();
        ImportExport.exportAll(this);
        if (hierarchy != null) {
            hierarchy.terminate();
            hierarchy = null;
        }
        if (pause_button != null) {
            pause_button.quit();
            pause_button = null;
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

    public static String getVersionNumber() { return versionNumber; }

    static SplashScreen ss = null;

    public static void dropSplashScreen() {
        if (ss != null) ss.okayToDispose();
        ss = null;
    }

    private static final String[] MENU_FONT_KEYS = {
        "MenuBar.font", "Menu.font", "MenuItem.font", "PopupMenu.font",
        "CheckBoxMenuItem.font", "RadioButtonMenuItem.font",
        "ToolBar.font", "MenuItem.acceleratorFont" };

    private static void changeMenuFont() {
        try {
            Font f = (FontUIResource) UIManager.get(MENU_FONT_KEYS[0]);
            f = f.deriveFont(Font.PLAIN);
            f = new FontUIResource(f);
            for (int i = 0;   i < MENU_FONT_KEYS.length;   i++)
                UIManager.put(MENU_FONT_KEYS[i], f);
        } catch (Exception e) {}
    }

    public static void main(String args[]) {
        DashboardSecurity.setupSecurityManager();

        ss = new SplashScreen(ProcessDashboard.class.getResource("ui/splash.png"));
        ss.displayFor(3000);      // show for at least 3 seconds.

        ensureJRE13();

        ProcessDashboard dash = new ProcessDashboard
            (args.length > 0 ? args[0] : null);
        DashController.setDashboard(dash);

        dash.pack();
        dash.show();

        dropSplashScreen();
        dash.aum.maybePerformCheck(dash);
    }

    public DashHierarchy getHierarchy() { return props; }
    public DataRepository getData() { return data; }
    public ObjectCache getCache() { return objectCache; }
    public WebServer getWebServer() { return webServer; }

}
