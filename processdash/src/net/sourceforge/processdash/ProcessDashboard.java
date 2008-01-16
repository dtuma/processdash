// Copyright (C) 1998-2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.EventListenerList;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTaskDependencyResolver;
import net.sourceforge.processdash.ev.ui.DependencyIndicator;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.DefaultActiveTaskModel;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.DashHierarchy.Event;
import net.sourceforge.processdash.hier.ui.HierarchyEditor;
import net.sourceforge.processdash.hier.ui.TaskCommenterButton;
import net.sourceforge.processdash.i18n.LookAndFeelSettings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.log.defects.DefectAnalyzer;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.TimeLog;
import net.sourceforge.processdash.log.time.TimeLoggingModel;
import net.sourceforge.processdash.log.time.WorkingTimeLog;
import net.sourceforge.processdash.log.ui.DefectButton;
import net.sourceforge.processdash.log.ui.PauseButton;
import net.sourceforge.processdash.net.cache.FileObjectCache;
import net.sourceforge.processdash.net.cache.ObjectCache;
import net.sourceforge.processdash.net.cms.CmsDefaultConfig;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.process.LegacySupport;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.process.ScriptNameResolver;
import net.sourceforge.processdash.process.ui.ScriptButton;
import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.security.DashboardSecurity;
import net.sourceforge.processdash.templates.AutoUpdateManager;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.tool.export.mgr.ExportManager;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.tool.export.mgr.ImportManager;
import net.sourceforge.processdash.ui.AboutDialog;
import net.sourceforge.processdash.ui.BetaVersionSetup;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.CompletionButton;
import net.sourceforge.processdash.ui.ConfigureButton;
import net.sourceforge.processdash.ui.ConsoleWindow;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.DashboardSplashScreen;
import net.sourceforge.processdash.ui.PercentSpentIndicator;
import net.sourceforge.processdash.ui.TaskNavigationSelector;
import net.sourceforge.processdash.ui.UserNotificationManager;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.ErrorReporter;
import net.sourceforge.processdash.ui.systray.SystemTrayManagement;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTTPUtils;
import net.sourceforge.processdash.util.ProfTimer;
import net.sourceforge.processdash.util.StringUtils;


public class ProcessDashboard extends JFrame implements WindowListener,
        DashboardContext, ApplicationEventSource {

    public static final String HTTP_PORT_SETTING = "http.port";
    private static final String DISABLE_AUTO_EXPORT_SETTING =
        "export.disableAutoExport";
    public static final String NOTIFY_ON_OPEN_PORT_PROPERTY =
        ProcessDashboard.class.getName() + ".notifyOnOpen.port";
    public static final String NOTIFY_ON_OPEN_ID_PROPERTY =
        ProcessDashboard.class.getName() + ".notifyOnOpen.id";

    ConfigureButton configure_button = null;
    PauseButton pause_button = null;
    PercentSpentIndicator pct_spent_indicator = null;
    ScriptButton script_button = null;
    DefectButton defect_button = null;
    TaskNavigationSelector taskNav = null;
    CompletionButton completion_button = null;
    TaskCommenterButton taskCommenterButton = null;
    JMenuBar hierarchy_menubar = null;

    DashHierarchy props;
    DashHierarchy templates = null;
    DashboardTimeLog timeLog = null;
    DataRepository data = null;
    WebServer webServer = null;
    ConcurrencyLock concurrencyLock = null;
    AutoUpdateManager aum = null;
    ConsoleWindow consoleWindow = new ConsoleWindow();
    ObjectCache objectCache;
    private ErrorReporter brokenData;
    Resources resources;
    EventListenerList ell;

    boolean paused = true;
    static final String DEFAULT_PROP_FILE = "state";
    String starting_dir       = "";
    String property_directory;
    static String default_directory = null;
    String propertiesFile     = DEFAULT_PROP_FILE;
    static final String TEMPLATES_FILE = "state";

    private ActiveTaskModel activeTaskModel;

    private static final Logger logger = Logger
            .getLogger(ProcessDashboard.class.getName());

    private static String versionNumber;

    public static final int DEFAULT_WEB_PORT = 2468;

    public ProcessDashboard(String title) {
        super();
        getContentPane().setLayout(new GridBagLayout());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(this);
        ProfTimer pt = new ProfTimer(ProcessDashboard.class, "ProcessDashboard");

        // load app defaults and user settings.
        try {
            InternalSettings.initialize("");
        } catch (Exception e) {
            logErr("Unable to read settings file", e);
            displayStartupIOError("Errors.Read_File_Error.Settings_File",
                    e.getMessage());
            System.exit(0);
        }
        propertiesFile = Settings.getFile("stateFile");
        File prop_file = new File(propertiesFile);
        try {
            prop_file = prop_file.getCanonicalFile();
        } catch (IOException ioe) {}
        propertiesFile = prop_file.getPath();
        property_directory = prop_file.getParent() + Settings.sep;
        TemplateLoader.resetTemplateURLs();

        // if this JVM does not explicitly have a logging config set, but one
        // is present in the user's data directory, read it.
        File logConfig = new File(property_directory + "logging.properties");
        if (System.getProperty("java.util.logging.config.file") == null
                  && logConfig.isFile()) {
            try {
                LogManager.getLogManager().readConfiguration(
                    new FileInputStream(logConfig));
            } catch (Exception e) {}
        }
        pt.click("Read settings");

        DefectAnalyzer.setDataDirectory(property_directory);
        CmsDefaultConfig.setPersistenceDirectory(prop_file.getParentFile());
        ExternalResourceManager.getInstance().initializeMappings(
                prop_file.getParentFile());
        try {
            default_directory = prop_file.getParentFile().getCanonicalPath();
        } catch (IOException ioe) {
            default_directory = prop_file.getParentFile().getAbsolutePath();
        }
        pt.click("Set default directory");

        // start the http server.
        try {
            int httpPort = Settings.getInt(HTTP_PORT_SETTING, DEFAULT_WEB_PORT);
            webServer = new WebServer(httpPort);
            webServer.setDashboardContext(this);
            InternalSettings.addPropertyChangeListener
                (HTTP_PORT_SETTING, new HttpPortSettingListener());
            ScriptID.setNameResolver(new ScriptNameResolver(webServer));
        } catch (IOException ioe) {
            logErr("Couldn't start web server", ioe);
        }
        pt.click("Started web server");

        maybeEnableReadOnlyMode();
        pt.click("Checked read only mode");

        // ensure that we have exclusive control of the data in the
        // property_directory
        //
        if (!Settings.isReadOnly()) {
            concurrencyLock = new ConcurrencyLock(property_directory,
                                                  webServer.getPort(),
                                                  webServer.getTimestamp());
            pt.click("Obtained concurrency lock");
        }

        // run the backup process as soon as possible
        FileBackupManager.maybeRun
            (property_directory, FileBackupManager.STARTUP, null);
        pt.click("Ran file backup");


        // create the data repository.
        data = new DataRepository();
        if ("true".equalsIgnoreCase(Settings.getVal("dataFreezing.disabled")))
            data.disableFreezing();
        pt.click("Created Data Repository");
        templates = TemplateLoader.loadTemplates(data);
        pt.click("Loaded templates");
        aum = new AutoUpdateManager(TemplateLoader.getPackages());
        resources = Resources.getDashBundle("ProcessDashboard");
        InternalSettings.loadLocaleSpecificDefaults(resources);
        FormatUtil.setDateFormats(Settings.getVal("dateFormat"),
                Settings.getVal("dateTimeFormat"));
        Translator.init();
        LookAndFeelSettings.loadLocalizedSettings();
        pt.click("Set locale specific defaults");
        data.setDatafileSearchURLs(TemplateLoader.getTemplateURLs());
        pt.click("Set datafile search URLs");
        versionNumber = TemplateLoader.getPackageVersion("pspdash"); // legacy
        logger.info("Process Dashboard version " + versionNumber);

        if (title == null)
            title = Settings.getVal("window.title");
        if (title == null)
            title = resources.getString("Window_Title");
        if (Settings.isReadOnly())
            title = resources.format("ReadOnly.Title_FMT", title);
        setTitle(title);

        // initialize the content roots for the http server.
        webServer.setRoots(TemplateLoader.getTemplateURLs());
        WebServer.setOutputCharset(getWebCharset());

        BetaVersionSetup.runSetup(property_directory);
        pt.click("Ran beta version setup");

        // determine if Lost Data Files are present in the pspdata directory
        // and take steps to repair them.
        LostDataFiles lostPSPFiles = new LostDataFiles();
        lostPSPFiles.findLostFiles(property_directory);
        pt.click("Checked for lost data files");
        if (lostPSPFiles.repair(this)==false) {

            // if the lost data files could not be repaired, exit the dashboard
            logger.severe
            ("Dashboard was terminated due to user request. " +
                    "The following bad data files were found in the "+
                    "psp data directory:\n" + lostPSPFiles.printOut());
            System.exit(0);
        }

        // open and load the the user's work breakdown structure
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
                    (FileUtils.slurpContents(getClass().getResourceAsStream
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
                logErr("Couldn't read default state file", e);
            }
        }
        data.setNodeComparator(props);
        registerHierarchyDataElement();
        data.pinElement(DashHierarchy.DATA_REPOSITORY_NAME);
        activeTaskModel = new DefaultActiveTaskModel(props);
        pt.click("Loaded dashboard hierarchy");

        // create the time log
        try {
            this.timeLog = new DashboardTimeLog(new File(property_directory), data, props);
            this.timeLog.getTimeLoggingModel().setActiveTaskModel(activeTaskModel);
            DashboardTimeLog.setDefault(this.timeLog);
        } catch (IOException e1) {
            logErr("Unable to read time log", e1);
            displayStartupIOError("Errors.Read_File_Error.Time_Log",
                    property_directory + WorkingTimeLog.TIME_LOG_FILENAME);
            System.exit(0);
        }
        pt.click("Initialized time log");

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
        }catch (Exception e) { logErr("open datafiles failed!", e); };

        // open the global data file.
        try {
            data.openDatafile("", property_directory + "global.dat");
        } catch (FileNotFoundException exc) {
            // if the user doesn't have a global data file, create one
            // for them from the default template.
            HierarchyEditor.createDataFile (property_directory + "global.dat",
                                          "dataFile.txt", null);
            openDatafile("", "global.dat");

        } catch (Exception exc) {
            logErr("when generating default datafile, caught exception", exc);
        }
        pt.click("Opened data files");

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
        pt.click("Set web server context objects");

        configure_button = new ConfigureButton(this);
        addToMainWindow(configure_button, 0, 2, 2);
        PCSH.enableHelpKey(this, "QuickOverview");
        pt.click("Created configure button");
        pause_button = new PauseButton(timeLog.getTimeLoggingModel());
        if (Settings.isReadWrite())
            addToMainWindow(pause_button, 0);
        pt.click("Created play/pause button");
        pct_spent_indicator = new PercentSpentIndicator(this, activeTaskModel);
        addToMainWindow(pct_spent_indicator, 0);
        pt.click("Created percent spent indicator");
        defect_button = new DefectButton(this);
        if (Settings.isReadWrite())
            addToMainWindow(defect_button, 0);
        pt.click("Created defect button");
        script_button = new ScriptButton(this);
        addToMainWindow(script_button, 0);
        pt.click("Created script button");
        hierarchy_menubar = new JMenuBar();
        addToMainWindow(hierarchy_menubar, 1.0);
        DependencyIndicator dependencyIndicator = new DependencyIndicator(this,
                activeTaskModel);
        addToMainWindow(dependencyIndicator, 0, 0, 0);
        pt.click("Created dependency indicator");
        completion_button = new CompletionButton(this, activeTaskModel);
        addToMainWindow(completion_button, 0);
        pt.click("Created completion button");
        taskCommenterButton = new TaskCommenterButton(this, activeTaskModel);
        addToMainWindow(taskCommenterButton, 0, 0,
            Settings.getInt("mainWindow.paddingRight", 2));
        pt.click("Created task commenter button");

        ImportManager.init(data);
        pt.click("Initialized import manager");
        data.finishInconsistency();
        ExportManager.init(data, this);
        pt.click("Initialized export manager");
        try {
            data.maybeSaveDefinitions(serializedDefinitions);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LegacySupport.configureRemoteListeningCapability(data);
        timeLog.refreshMetrics();
        pt.click("Refreshed time log metrics");

        EVTaskDependencyResolver.init(this);

        taskNav = new TaskNavigationSelector
            (this, hierarchy_menubar, activeTaskModel);
        completion_button.setNavSelector(taskNav);
        pt.click("Created task navigation selector");
        dependencyIndicator.update();
        pt.click("Updated dependency indicator");
        props.addHierarchyListener(new DashHierarchy.PrePostListener() {
                public void hierarchyWillChange(Event e) {
                    fireApplicationEvent(
                        ApplicationEventListener.APP_EVENT_SAVE_ALL_DATA);
                }
                public void hierarchyChanged(Event e) {
                    saveHierarchy();
                    registerHierarchyDataElement();
                    if (!e.isAdjusting())
                        timeLog.refreshMetrics();
                    refreshHierarchy(e.isAdjusting());
                }});

        brokenData.done();
        TemplateLoader.showTemplateErrors();
        addComponentListener(new ResizeWatcher());
        ExternalResourceManager.getInstance().cleanupBogusExtResDirectory(
                prop_file.getParentFile());
        DashController.setDashboard(this);
        BackgroundTaskManager.initialize(this);
        initializeSystemTray();
        pt.click("Finished initializing Process Dashboard object");
    }

    public void addApplicationEventListener(ApplicationEventListener l) {
        synchronized (this) {
            if (ell == null)
                ell = new EventListenerList();
            ell.add(ApplicationEventListener.class, l);
        }
    }

    public void removeApplicationEventListener(ApplicationEventListener l) {
        if (ell != null)
            ell.remove(ApplicationEventListener.class, l);
    }

    protected void fireApplicationEvent(String message) {
        if (ell != null) {
            ActionEvent e = new ActionEvent(this, 0, message);
            for (ApplicationEventListener l : ell
                    .getListeners(ApplicationEventListener.class)) {
                try {
                    l.handleApplicationEvent(e);
                } catch (Throwable t) {
                    // application events are delivered to clients as a
                    // courtesy during sensitive parts of the application
                    // lifecycle.  We cannot allow those clients to thwart
                    // the sensitive lifecycle events by throwing an
                    // unchecked exception or error.
                    logger.log(Level.SEVERE,
                        "When sending application event, caught exception", t);
                }
            }
        }
    }

    /**
     * Enable system tray icon based on a property value.
     */
    private void initializeSystemTray() {
        if (isSystemTrayEnabled()){
            //install icon
            SystemTrayManagement.getIcon().initialize(this);
        } else {
            //remove the icon from the system tray
            SystemTrayManagement.getIcon().dispose();
        }
    }

    /**
     * @return true if preferences allow system tray icon
     */
    private static boolean isSystemTrayEnabled() {
        return !Settings.getBool(SystemTrayManagement.DISABLED_SETTING, false);
    }

    private int hierChangeCount = 0;
    private void registerHierarchyDataElement() {
        ListData propItem = new ListData();
        propItem.add(props);
        propItem.add(String.valueOf(hierChangeCount++));
        data.putValue(DashHierarchy.DATA_REPOSITORY_NAME, propItem);
    }
    private Component addToMainWindow(Component component, double weight) {
        return addToMainWindow(component, weight, 0, 2);
    }
    private Component addToMainWindow(Component component, double weight,
            int padLeft, int padRight) {
        if (component instanceof ApplicationEventListener)
            addApplicationEventListener((ApplicationEventListener) component);

        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.gridx = getContentPane().getComponentCount();
        g.insets = new Insets(2, padLeft, 2, padRight);
        g.weightx = weight;
        if (weight > 0)
            g.fill = GridBagConstraints.HORIZONTAL;

        GridBagLayout layout = (GridBagLayout) getContentPane().getLayout();
        layout.setConstraints(component, g);
        return getContentPane().add(component);
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

    private class HttpPortSettingListener implements PropertyChangeListener {

        public HttpPortSettingListener() {
            setBrowserDefaults();
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if (HTTP_PORT_SETTING.equalsIgnoreCase(evt.getPropertyName()))
                try {
                    int newPort = Integer.parseInt((String) evt.getNewValue());
                    webServer.addPort(newPort);
                    setBrowserDefaults();
                } catch (Exception e) {}
        }

        private void setBrowserDefaults() {
            Browser.setDefaults(
                    webServer.getHostName(false),
                    webServer.getPort());
        }
    }

    private String getWebCharset() {
        String charsetName = Settings.getVal("http.charset");

        if (charsetName == null)
            charsetName = HTTPUtils.DEFAULT_CHARSET;
        else if ("auto".equals(charsetName))
            charsetName = (Translator.isTranslating() ? "UTF-8"
                    : HTTPUtils.DEFAULT_CHARSET);

        return charsetName;
    }

    private void maybeEnableReadOnlyMode() {
        if (Settings.isReadOnly())
            return;

        if (Settings.getBool("readOnly", false)) {
            // if the user has a "readOnly=true" setting, turn on the
            // read only flag.
            InternalSettings.setReadOnly(true);
            return;
        }

        if (someDashboardFilesAreReadOnly()) {
            // the user does not have write access to all of the files in
            // the data directory.  They either need to open the dashboard
            // in read only mode, or exit.
            ResourceBundle res = ResourceBundle
                    .getBundle("Templates.resources.ProcessDashboard");
            String title = res.getString("ReadOnly.File_Permissions.Title");
            Object message = MessageFormat.format(
                    res.getString("ReadOnly.File_Permissions.Message_FMT"),
                    new Object[] { property_directory }).split("\n");
            int userResponse = JOptionPane.showConfirmDialog(null, message,
                    title, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.ERROR_MESSAGE);
            if (userResponse == JOptionPane.OK_OPTION) {
                InternalSettings.setReadOnly(true);
                return;
            } else {
                System.exit(1);
            }
        }

        String setting = Settings.getVal("readOnly");
        if (setting == null) return;
        if (setting.toLowerCase().startsWith("recommend")) {
            // Must manually use ResourceBundles until after the
            // TemplateLoader is initialized.
            ResourceBundle res = ResourceBundle.getBundle(
                    "Templates.resources.ProcessDashboard");
            JRadioButton readOnlyOption = new JRadioButton(res
                    .getString("ReadOnly.Recommended.Read_Only_Option"));
            readOnlyOption.setSelected(true);
            JRadioButton readWriteOption = new JRadioButton(res
                    .getString("ReadOnly.Recommended.Read_Write_Option"));
            ButtonGroup group = new ButtonGroup();
            group.add(readOnlyOption);
            group.add(readWriteOption);
            String title = res.getString("ReadOnly.Recommended.Title");
            Object[] message = new Object[] {
                    res.getString("ReadOnly.Recommended.Message").split("\n"),
                    readOnlyOption, readWriteOption };
            JOptionPane.showMessageDialog(null, message, title,
                    JOptionPane.QUESTION_MESSAGE);
            if (readOnlyOption.isSelected())
                InternalSettings.setReadOnly(true);
        }
    }
    private boolean someDashboardFilesAreReadOnly() {
        File dataDir = new File(property_directory);
        File[] files = dataDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (FileBackupManager.inBackupSet(dataDir, files[i].getName())) {
                if (!files[i].canWrite())
                    return true;
            }
        }
        return false;
    }

    private void displayStartupIOError(String resourceKey, String filename) {
        if (resources == null)
            resources = Resources.getDashBundle("ProcessDashboard");

        try {
            File f = new File(filename);
            filename = f.getAbsolutePath();
            filename = f.getCanonicalPath();
        } catch (Exception e) {}

        JOptionPane.showMessageDialog
            (null,
             resources.formatStrings("Errors.Read_File_Error.Message_FMT",
                                     resources.getString(resourceKey),
                                     filename),
             resources.getString("Errors.Read_File_Error.Title"),
             JOptionPane.ERROR_MESSAGE);
    }

    private static final String BULLET = "\u2022 ";

    public void openDatafile (String prefix, String dataFile) {
        try {
            data.openDatafile (prefix, property_directory + dataFile);
        } catch (FileNotFoundException fnfe) {
            brokenData.logError(prefix);
        } catch (Exception exc) {
            logErr("when opening datafile, '" + dataFile + "' for path '"
                    + prefix + "', caught exception:", exc);
        }
    }

    public static String getOwnerName(DataRepository data) {
        return getOwnerName((DataContext) data);
    }
    public static String getOwnerName(DataContext data) {
        SimpleData val = data.getSimpleValue("/Owner");
        if (val == null) return null;
        String result = val.format();

        String defaultVal = Resources.getDashBundle("ProcessDashboard")
            .getString("Enter_your_name");
        if (result.equals(defaultVal))
            return null;
        else
            return result;
    }

    boolean addTemplateJar(String jarfileName) {
        if (!TemplateLoader.addTemplateJar(data, templates, jarfileName))
            return false;

        URL [] templateRoots = TemplateLoader.getTemplateURLs();
        data.setDatafileSearchURLs(templateRoots);
        webServer.setRoots(templateRoots);
        return true;
    }

    private void refreshHierarchy(final boolean isAdjusting) {
        if (SwingUtilities.isEventDispatchThread())
            refreshHierarchyImpl(isAdjusting);
        else try {
            SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() { refreshHierarchyImpl(isAdjusting); }});
        } catch (Exception e) { }
    }
    private void refreshHierarchyImpl(boolean isAdjusting) {
        logger.finer("ProcessDashboard.refreshHierarchyImpl starting");
        taskNav.hierarchyChanged();
        logger.finer("ProcessDashboard.refreshHierarchyImpl finished");
    }

    public ActiveTaskModel getActiveTaskModel() {
        return activeTaskModel;
    }

    public TimeLoggingModel getTimeLoggingModel() {
        return timeLog.getTimeLoggingModel();
    }


    public void pauseTimer() {
        timeLog.getTimeLoggingModel().stopTiming();
    }

    public PropertyKey getCurrentPhase() { return activeTaskModel.getNode(); }

    public boolean isHierarchyEditorOpen() {
        return configure_button.isHierarchyEditorOpen();
    }


    public TimeLog getTimeLog() {
        return timeLog;
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

    public JMenu getConfigurationMenu() {
        return configure_button.getMainMenu();
    }

    public Action getChangeTaskAction() {
        return taskNav.getChangeTaskAction();
    }

    public Dimension getPreferredSize() {
        Dimension minSize = super.getMinimumSize();
        Dimension result = super.getPreferredSize();
        int maxWidth = Settings.getInt("window.maxWidth", -1);
        if (maxWidth != -1)
            result.width = Math.max(
                    Math.min(result.width, maxWidth),
                    minSize.width);
        return result;
    }

    private class ResizeWatcher extends ComponentAdapter implements ActionListener {
        Timer t;
        public ResizeWatcher() {
            t = new Timer(1000, this);
            t.setRepeats(false);
        }

        public void componentResized(ComponentEvent e) {
            t.restart();
        }

        public void actionPerformed(ActionEvent e) {
            Dimension minSize = getMinimumSize();
            Dimension preferredSize = getPreferredSize();
            Dimension currentSize = getSize();

            if (currentSize.height != preferredSize.height
                    || currentSize.width < minSize.width) {
                currentSize.height = preferredSize.height;
                currentSize.width = Math.max(currentSize.width, minSize.width);
                setSize(currentSize);

            } else if (currentSize.width != preferredSize.width) {
                // this is (generally) an indication that the user has resized
                // the window manually.  Remember their decision and don't
                // grow past this width in the future.
                InternalSettings.set("window.maxWidth",
                        Integer.toString(currentSize.width));
            }
        }
    }


    public void windowOpened(WindowEvent w) {}
    public void windowClosed(WindowEvent w) {}
    public void windowIconified(WindowEvent w) {}
    public void windowDeiconified(WindowEvent w) {}
    public void windowDeactivated(WindowEvent w) {}

    public void windowActivated(WindowEvent w) {
        UserNotificationManager.getInstance().maybeShowNotifications(this);
    }

    public void windowClosing(WindowEvent w) {
        exitProgram();
    }

    public void exitProgram() {
        new DashboardPermission("exitProgram").checkPermission();

        String owner = getOwnerName(data);
        try {
            if (quit() == false)
                return;
        } catch (Throwable t) {
            // if the shutdown sequence encounters an uncaught exception,
            // display an error message, but still exit.
            logErr("When shutting down, encountered the exception:", t);
        }

        SystemTrayManagement.getIcon().dispose();
        setVisible(false);
        logger.fine("Backing up data directory");
        FileBackupManager.maybeRun
            (property_directory, FileBackupManager.SHUTDOWN, owner);

        logger.fine("Shutdown complete");
        System.exit(0);
    }

    protected boolean quit() {
        List unsavedData = saveAllData();
        if (unsavedData.isEmpty() == false
                && warnUserAboutUnsavedData(unsavedData) == false) {
            InternalSettings.setDisableChanges(false);
            return false;
        }

        if (!Settings.getBool(DISABLE_AUTO_EXPORT_SETTING, false)) {
            logger.fine("Performing auto exports");
            ExportManager.getInstance().exportAll(this, this);
        }
        if (webServer != null) {
            logger.fine("Shutting down web server");
            webServer.quit();
            webServer = null;
        }
        if (data != null) {
            logger.fine("Finalizing data repository");
            data.finalize();
            data = null;
        }

        if (concurrencyLock != null) {
            logger.fine("Removing concurrency lock");
            concurrencyLock.unlock();
            concurrencyLock = null;
        }

        return true;
    }

    private boolean warnUserAboutUnsavedData(List unsavedData) {
        if (unsavedData.isEmpty())
            return true;

        StringBuffer dataItems = new StringBuffer();
        for (Iterator i = unsavedData.iterator(); i.hasNext();) {
            String dataDescr = (String) i.next();
            dataItems.append(BULLET).append(dataDescr).append("\n");
        }

        String title = resources.getString("Errors.Save_Error.Title");
        String[] message = resources.formatStrings(
                "Errors.Save_Error.Message_FMT", dataItems.toString());
        int userChoice = JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.YES_NO_OPTION);
        return userChoice == JOptionPane.YES_OPTION;
    }

    public List saveAllData() {
        List unsavedData = new LinkedList();
        if (Settings.isReadOnly())
            return unsavedData;

        // First, ask all GUIs to save their dirty data
        fireApplicationEvent(ApplicationEventListener.APP_EVENT_SAVE_ALL_DATA);

        // Next, save and close the Hierarchy Editor if it is open/dirty
        if (configure_button != null)
            configure_button.saveAndCloseHierarchyEditor();

        // Now, flush all in-memory data to disk, recording whether any steps
        // were unsuccessful.

        if (saveHierarchy() == false)
            recordUnsavedItem(unsavedData, "Hierarchy");

        if (saveTimeLogData() == false)
            recordUnsavedItem(unsavedData, "Time_Log");

        if (saveMetricsData() == false)
            recordUnsavedItem(unsavedData, "Metrics_Data");

        if (saveSettingsData() == false)
            recordUnsavedItem(unsavedData, "Settings_Data");

        return unsavedData;
    }

    private boolean saveMetricsData() {
        if (data == null)
            return true;

        data.saveAllDatafiles();

        return ! data.areDatafilesDirty();
    }

    private void recordUnsavedItem(List unsavedData, String dataType) {
        unsavedData.add(resources.getString("Errors.Save_Error." + dataType));
    }

    protected boolean saveTimeLogData() {
        timeLog.getTimeLoggingModel().setPaused(true);
        timeLog.getTimeLoggingModel().saveData();
        timeLog.saveData();
        return ! timeLog.isDirty();
    }

    public boolean saveHierarchy() {
        try {
            props.save(propertiesFile, "hierarchical work breakdown structure");
            return true;
        } catch (Exception e) {
            logErr("prop write failed.", e);
            return false;
        }
    }

    public boolean saveSettingsData() {
        InternalSettings.setDisableChanges(true);
        if (InternalSettings.isDirty())
            InternalSettings.saveSettings();
        return InternalSettings.isDirty() == false;
    }


    public static String getVersionNumber() { return versionNumber; }

    static DashboardSplashScreen ss = null;

    public static void dropSplashScreen() {
        if (ss != null) ss.okayToDispose();
        ss = null;
    }

    private void maybeNotifyOpened() {
        Integer portToNotify = Integer.getInteger(NOTIFY_ON_OPEN_PORT_PROPERTY);
        if (portToNotify != null) {
            try {
                Socket s = new Socket(InetAddress.getLocalHost(), portToNotify
                        .intValue());
                Writer out = new OutputStreamWriter(s.getOutputStream(),
                        "UTF-8");
                out.write("<?xml version='1.0' encoding='UTF-8'?>");
                out.write("<pdashNotification");
                out.write(" instanceId='"
                        + System.getProperty(NOTIFY_ON_OPEN_ID_PROPERTY));
                out.write("' httpPort='"
                        + Integer.toString(webServer.getPort()));
                out.write("'>");
                out.write("<event type='opened'/>");
                out.write("</pdashNotification>");
                out.close();
                s.close();
            } catch (Exception e) {}
        }
    }

    private static void logErr(String msg, Throwable t) {
        logger.log(Level.SEVERE, msg, t);
    }

    public void windowSizeRequirementsChanged() {
        if (this.isVisible())
            this.pack();
    }

    public static void main(String args[]) {
        DashboardSecurity.setupSecurityManager();

        ss = new DashboardSplashScreen();
        ss.displayFor(3000);      // show for at least 3 seconds.

        if (Boolean.getBoolean("readOnly"))
            InternalSettings.setReadOnly(true);

        int pos = 0;
        if (args.length > 0 && "readOnly".equalsIgnoreCase(args[0])) {
            InternalSettings.setReadOnly(true);
            pos++;
        }

        ProcessDashboard dash = new ProcessDashboard
            (args.length > pos ? args[pos] : null);

        DashboardIconFactory.setWindowIcon(dash);
        dash.setVisible(true);
        dash.windowSizeRequirementsChanged();

        dropSplashScreen();
        dash.maybeNotifyOpened();
        dash.aum.maybePerformCheck(dash);
    }
    public DashHierarchy getHierarchy() { return props; }
    public DataRepository getData() { return data; }
    public ObjectCache getCache() { return objectCache; }
    public WebServer getWebServer() { return webServer; }

}
