// Copyright (C) 1998-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.event.EventListenerList;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.ImmutableStringData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.InvalidDatafileFormat;
import net.sourceforge.processdash.ev.EVCalculator;
import net.sourceforge.processdash.ev.EVTaskDependencyResolver;
import net.sourceforge.processdash.ev.WBSTaskOrderComparator;
import net.sourceforge.processdash.ev.ui.DependencyIndicator;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.DashHierarchy.Event;
import net.sourceforge.processdash.hier.DefaultActiveTaskModel;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.AddTaskButton;
import net.sourceforge.processdash.hier.ui.HierarchyEditor;
import net.sourceforge.processdash.hier.ui.TaskCommenterButton;
import net.sourceforge.processdash.i18n.LocaleResetSupport;
import net.sourceforge.processdash.i18n.LookAndFeelSettings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.log.defects.DefectAnalyzer;
import net.sourceforge.processdash.log.defects.DefectLog;
import net.sourceforge.processdash.log.defects.RepairDefectCounts;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.ImportedTimeLogManager;
import net.sourceforge.processdash.log.time.TimeLog;
import net.sourceforge.processdash.log.time.TimeLoggingModel;
import net.sourceforge.processdash.log.time.WorkingTimeLog;
import net.sourceforge.processdash.log.ui.DefectButton;
import net.sourceforge.processdash.log.ui.PauseButton;
import net.sourceforge.processdash.log.ui.TaskTimeLoggingErrorWatcher;
import net.sourceforge.processdash.msg.MessageDispatcher;
import net.sourceforge.processdash.net.cache.FileObjectCache;
import net.sourceforge.processdash.net.cache.ObjectCache;
import net.sourceforge.processdash.net.cms.CmsDefaultConfig;
import net.sourceforge.processdash.net.http.DashboardURLStreamHandlerFactory;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.process.DefectTypeStandard;
import net.sourceforge.processdash.process.LegacySupport;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.process.ScriptNameResolver;
import net.sourceforge.processdash.process.ui.ScriptButton;
import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.security.DashboardSecurity;
import net.sourceforge.processdash.team.group.UserGroupManager;
import net.sourceforge.processdash.templates.AutoUpdateManager;
import net.sourceforge.processdash.templates.DataVersionChecker;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectoryFactory;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectoryFactory;
import net.sourceforge.processdash.tool.bridge.impl.DatasetAutoMigrator;
import net.sourceforge.processdash.tool.bridge.impl.HttpAuthenticator;
import net.sourceforge.processdash.tool.bridge.impl.JnlpRelauncher;
import net.sourceforge.processdash.tool.db.DatabasePlugin;
import net.sourceforge.processdash.tool.export.DataImporter;
import net.sourceforge.processdash.tool.export.mgr.ExportManager;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.tool.export.mgr.ImportManager;
import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.tool.quicklauncher.QuickLauncher;
import net.sourceforge.processdash.ui.AlwaysOnTopManager;
import net.sourceforge.processdash.ui.BetaVersionSetup;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.CompletionButton;
import net.sourceforge.processdash.ui.ConfigureButton;
import net.sourceforge.processdash.ui.ConsoleWindow;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.DashboardSplashScreen;
import net.sourceforge.processdash.ui.DashboardWelcomePane;
import net.sourceforge.processdash.ui.PercentSpentIndicator;
import net.sourceforge.processdash.ui.TaskNavigationSelector;
import net.sourceforge.processdash.ui.TeamProjectBrowser;
import net.sourceforge.processdash.ui.UserNotificationManager;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.ExceptionDialog;
import net.sourceforge.processdash.ui.lib.JLinkLabel;
import net.sourceforge.processdash.ui.lib.LargeFontsHelper;
import net.sourceforge.processdash.ui.lib.PleaseWaitDialog;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.ui.systray.SystemTrayManagement;
import net.sourceforge.processdash.ui.web.psp.SizeEstimatingTemplate;
import net.sourceforge.processdash.ui.web.reports.DashboardChartDefaults;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.FallbackObjectFactory;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HttpException;
import net.sourceforge.processdash.util.Initializable;
import net.sourceforge.processdash.util.ProfTimer;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.UsageLogger;
import net.sourceforge.processdash.util.lock.AlreadyLockedException;
import net.sourceforge.processdash.util.lock.CannotCreateLockException;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.LockMessage;
import net.sourceforge.processdash.util.lock.LockMessageHandler;
import net.sourceforge.processdash.util.lock.LockUncertainException;
import net.sourceforge.processdash.util.lock.OfflineLockLostException;
import net.sourceforge.processdash.util.lock.ReadOnlyLockFailureException;
import net.sourceforge.processdash.util.lock.SentLockMessageException;


public class ProcessDashboard extends JFrame implements WindowListener,
        DashboardContext, ApplicationEventSource {

    public static final String HTTP_PORT_SETTING = "http.port";
    private static final String LOCATION_ARG_PREFIX = "-location=";
    private static final String DATA_DIR_LINK_FILE_SETTING =
        "net.sourceforge.processdash.dataDirLinkFile";
    private static final String DISABLE_AUTO_EXPORT_SETTING =
        "export.disableAutoExport";
    public static final String NOTIFY_ON_OPEN_PORT_PROPERTY =
        ProcessDashboard.class.getName() + ".notifyOnOpen.port";
    public static final String NOTIFY_ON_OPEN_ID_PROPERTY =
        ProcessDashboard.class.getName() + ".notifyOnOpen.id";
    public static final String WINDOW_TITLE_SETTING = "window.title";

    WorkingDirectory workingDirectory;
    FileBackupManager fileBackupManager;
    LockMessageHandler lockMessageHandler;
    ConfigureButton configure_button = null;
    PauseButton pause_button = null;
    TaskNavigationSelector taskNav = null;
    JMenuBar hierarchy_menubar = null;
    Initializable osHelper = null;

    DashHierarchy props;
    DashHierarchy templates = null;
    DashboardTimeLog timeLog = null;
    DataRepository data = null;
    WebServer webServer = null;
    DatabasePlugin databasePlugin = null;
    AutoUpdateManager aum = null;
    ConsoleWindow consoleWindow = new ConsoleWindow();
    ObjectCache objectCache;
    private BrokenDataFileHandler brokenData;
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

    public ProcessDashboard(String location, String title) {
        super();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(this);
        ProfTimer pt = new ProfTimer(ProcessDashboard.class, "ProcessDashboard");

        // adjust the working directory if necessary.
        if (location == null)
            location = maybeFollowDataDirLinkFile();
        configureWorkingDirectory(location);

        // load app defaults and user settings.
        try {
            InternalSettings.initialize("");
        } catch (Exception e) {
            logErr("Unable to read settings file", e);
            displayStartupIOError("Errors.Read_File_Error.Settings_File",
                    e.getMessage(), e);
            System.exit(0);
        }
        propertiesFile = Settings.getFile("stateFile");
        File prop_file = new File(propertiesFile);
        try {
            prop_file = prop_file.getCanonicalFile();
        } catch (IOException ioe) {}
        propertiesFile = prop_file.getPath();
        property_directory = prop_file.getParent() + Settings.sep;
        DashboardURLStreamHandlerFactory.initialize(null);
        TemplateLoader.resetTemplateURLs();
        configureSystemPropertiesFromSettings();
        pt.click("Read settings");

        DefectAnalyzer.setDataDirectory(property_directory);
        CmsDefaultConfig.setPersistenceDirectories(
            getCmsPersistenceDirs(prop_file.getParentFile()));
        ExternalResourceManager.getInstance().setDashboardContext(this);
        ExternalResourceManager.getInstance().initializeMappings(
                prop_file.getParentFile());
        try {
            default_directory = prop_file.getParentFile().getCanonicalPath();
        } catch (IOException ioe) {
            default_directory = prop_file.getParentFile().getAbsolutePath();
        }
        ImportDirectoryFactory.getInstance().setBaseDirectory(workingDirectory);
        pt.click("Set default directory");

        // create the data repository
        data = new DataRepository();
        if ("true".equalsIgnoreCase(Settings.getVal("dataFreezing.disabled")))
            data.disableFreezing();
        data.addGlobalDefineDeclarations("#define AUTO_INDIV_ROOT_TAG t");
        pt.click("Created Data Repository");

        // load process templates, extension points, and other materials
        templates = TemplateLoader.loadTemplates(data);
        pt.click("Loaded templates");
        DataVersionChecker.ensureVersionsOrExit();

        // read and initialize users, roles, and permissions
        try {
            PermissionsManager.getInstance().init(this);
            pt.click("Initialized permissions manager");

            // check the permissions that are granted to the current user. If
            // they do not have the "active dashboard user" permission, display
            // a "forbidden" message and exit.
            if (!PermissionsManager.getInstance().hasPermission(
                PermissionsManager.ACTIVE_USER_PERMISSION_ID)) {
                displayStartupPermissionError("Forbidden");
                System.exit(1);
            }
        } catch (HttpException.Unauthorized u) {
            // if we are connecting to a PDES that has data security disabled,
            // the PermissionManager.init() logic will trigger a login prompt.
            // If the user fails to login successfully, display an
            // "unauthorized" message and exit.
            displayStartupPermissionError("Unauthorized");
            System.exit(1);
        } catch (IOException ioe) {
            // if I/O problems prevented reading users/roles, exit with error
            logErr("Unable to read permission files", ioe);
            displayStartupIOError("Errors.Read_File_Error.Permissions_Files",
                    ioe.getMessage(), ioe);
            System.exit(1);
        }

        // configure the writability of the data and lock if applicable
        maybeSetupHistoricalMode();
        maybeEnableReadOnlyMode();
        pt.click("Checked read only mode");
        if (!Settings.isReadOnly()) {
            tryToLockDataForWriting();
            pt.click("Tried to acquire write lock");
        }
        data.configureEditability();

        // check for a dataset migration import request
        DatasetAutoMigrator.maybeRun(workingDirectory,
            new DatasetAutoMigrator.DialogParentSource() {
                public Component getDialogParent() { return hideSS(); }});

        // run the backup process as soon as possible
        fileBackupManager = new FileBackupManager(workingDirectory);
        fileBackupManager.maybeRun(FileBackupManager.STARTUP, null);
        pt.click("Ran file backup");


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


        aum = new AutoUpdateManager(TemplateLoader.getPackages());
        resources = Resources.getDashBundle("ProcessDashboard");
        InternalSettings.loadLocaleSpecificDefaults(resources);
        FormatUtil.setDateFormats(Settings.getVal("dateFormat"),
                Settings.getVal("dateTimeFormat"));
        Translator.init();
        LookAndFeelSettings.loadLocalizedSettings();
        pt.click("Set locale specific defaults");
        DefectTypeStandard.registerStandardsFromTemplates(data);
        data.setDatafileSearchURLs(TemplateLoader.getTemplateURLs());
        pt.click("Set datafile search URLs");
        versionNumber = TemplateLoader.getPackageVersion("pspdash"); // legacy
        System.out.println("Process Dashboard version " + versionNumber);
        System.out.println("Running as user "
                + PermissionsManager.getInstance().getCurrentUsername());

        setupWindowTitle(title);

        // initialize the content roots for the http server.
        webServer.setRoots(TemplateLoader.getTemplateURLs());
        WebServer.setOutputCharset(getWebCharset());
        pt.click("Set web server roots");

        BetaVersionSetup.runSetup(property_directory);
        pt.click("Ran beta version setup");

        // determine if corrupt Data Files are present in the pspdata directory
        // and take steps to repair them.
        brokenData = new BrokenDataFileHandler();
        brokenData.findCorruptFiles(property_directory, workingDirectory);
        pt.click("Checked for lost data files");
        if (brokenData.repairCorruptFiles(this) == false) {

            // if the lost data files could not be repaired, exit the dashboard
            logger.severe
            ("Dashboard was terminated due to user request. " +
                    "The following bad data files were found in the "+
                    "psp data directory:\n" + brokenData.getCorruptFileStr());
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

                // Starting with version UTF8_SUPPORT_VERSION, all new datasets
                // use UTF-8 for datafiles and XML for defects.
                DataRepository.enableUtf8Encoding();
                DefectLog.enableXmlStorageFormat();

                if (Settings.isTeamMode() == false)
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

        // Make certain we know whether this dashboard is operating as a
        // team dashboard or a personal dashboard.
        configureTeamOrPersonalDatasetMode();

        // create the database plugin
        if (createDatabasePlugin())
            pt.click("Created and started the database plugin");

        // create the time log
        try {
            this.timeLog = new DashboardTimeLog(new File(property_directory), data, props);
            this.timeLog.getTimeLoggingModel().setActiveTaskModel(activeTaskModel);
            DashboardTimeLog.setDefault(this.timeLog);
        } catch (IOException e1) {
            logErr("Unable to read time log", e1);
            displayStartupIOError("Errors.Read_File_Error.Time_Log",
                    property_directory + WorkingTimeLog.TIME_LOG_FILENAME, e1);
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
        registerEnvironmentalData();
        pt.click("Opened data files");
        data.fixMisparentedData();
        pt.click("Fixed misparented data");
        timeLog.setTimingForbiddenPaths(getBrokenDataPaths());
        SizeEstimatingTemplate.migrateLegacyData(props, data);

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

        MessageDispatcher.init(this);
        ImportManager.init(data);
        InternalSettings.addPropertyChangeListener(ImportManager.SETTING_NAME,
            EventHandler.create(PropertyChangeListener.class, this,
                "flushWorkingData"));
        pt.click("Initialized import manager");
        data.finishInconsistency();
        ExportManager.init(data, this);
        pt.click("Initialized export manager");
        try {
            data.maybeSaveDefinitions(serializedDefinitions);
        } catch (Exception e) {
            e.printStackTrace();
        }
        RepairDefectCounts.maybeRun(this, property_directory);
        timeLog.refreshMetrics();
        pt.click("Refreshed time log metrics");

        EVTaskDependencyResolver.init(this);
        WBSTaskOrderComparator.init(this);

        configure_button = new ConfigureButton(this);
        PCSH.enableHelpKey(this, "QuickOverview");
        pt.click("Created configure button");
        configureStandardIconSize();
        pause_button = new PauseButton(timeLog.getTimeLoggingModel());
        pt.click("Created play/pause button");

        if (Settings.isPersonalMode())
            buildPersonalUI(pt);
        else
            buildTeamUI(pt);

        props.addHierarchyListener(new DashHierarchy.PrePostListener() {
                public void hierarchyWillChange(Event e) {
                    if (!Settings.isFollowMode())
                        fireApplicationEvent(
                            ApplicationEventListener.APP_EVENT_SAVE_ALL_DATA);
                }
                public void hierarchyChanged(Event e) {
                    saveHierarchy();
                    registerHierarchyDataElement();
                    if (!e.isAdjusting() && !Settings.isFollowMode())
                        timeLog.refreshMetrics();
                }});

        brokenData.showMissingDataFileWarnings();
        TemplateLoader.showTemplateErrors();
        ExternalResourceManager.getInstance().cleanupBogusExtResDirectory(
                prop_file.getParentFile());
        DashController.setDashboard(this);
        Settings.setDatasetID(DashController.getDatasetID(false));
        UserGroupManager.getInstance().init(this);
        BackgroundTaskManager.initialize(this);
        SystemTrayManagement.getIcon().initialize(this);
        AlwaysOnTopManager.initialize(this);
        DashboardChartDefaults.initialize();
        initializeOsHelper();
        maybeRecordRecentDataset();
        RuntimeUtils.addPropagatedSystemProperty(UsageLogger.FILE_SETTING, null);
        RuntimeUtils.autoregisterPropagatedSystemProperties();
        if (Settings.isFollowMode())
            new FollowModeManager(workingDirectory, props, prop_file,
                    templates, data, timeLog);
        fireApplicationEvent(ApplicationEventListener.APP_EVENT_STARTED);
        pt.click("Finished initializing Process Dashboard object");
    }

    private Map<String, File> getCmsPersistenceDirs(File dataDir) {
        Map<String, File> result = new LinkedHashMap<String, File>();

        // read the user setting and add the entries it contains.
        String setting = Settings.getVal("cms.persistenceLocations", "");
        for (String spec : setting.split(";")) {
            int sepPos = spec.indexOf("=>");
            if (sepPos < 1 || sepPos > spec.length()-3)
                continue;

            String qualifier = spec.substring(0, sepPos).trim();
            String path = spec.substring(sepPos + 2).replace('/',
                File.separatorChar).trim();
            File dir;
            if (path.startsWith("~")) {
                dir = new File(System.getProperty("user.home"),
                    path.substring(2));
            } else if (path.startsWith("[APPDIR]")) {
                dir = new File(DirectoryPreferences.getApplicationDirectory(),
                    path.substring(9));
            } else {
                dir = new File(path);
            }

            result.put(qualifier, dir);
        }

        // register the cms data directory as the "default" persistence
        // location.
        result.put(null, new File(dataDir, "cms"));

        return result;
    }

    private String maybeFollowDataDirLinkFile() {
        String linkFileName = System.getProperty(DATA_DIR_LINK_FILE_SETTING);
        if (!StringUtils.hasValue(linkFileName))
            return null;

        String location = readDataDirLinkFile(linkFileName);
        if (TeamServerSelector.isUrlFormat(location))
            return location;

        if (location.startsWith("~")) {
            location = System.getProperty("user.home") + location.substring(1);
            File dataDirFile = new File(location);
            if (!dataDirFile.isDirectory())
                dataDirFile.mkdirs();
        }

        try {
            File f = new File(location);
            if (f.isDirectory()) {
                return location;
            } else {
                throw new IOException("Directory '" + location
                        + "' specified by link file '" + linkFileName
                        + "' does not exist");
            }
        } catch (Exception e) {
            displayStartupIOError("Errors.Read_File_Error.Data_Directory",
                location, e);
        }
        System.exit(1);
        // the following line is not reached, but must be present to keep
        // the compiler happy:
        return null;
    }

    private String readDataDirLinkFile(String linkFileName) {
        try {
            File f = new File(linkFileName);
            BufferedReader in = new BufferedReader(new FileReader(f));
            String line = null;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#"))
                    break;
            }
            in.close();
            if (line != null)
                return line;
            else
                throw new IOException("No data directory info found in file '"
                        + linkFileName + "' - aborting");
        } catch (Exception e) {
            System.err.println("Unexpected error reading dataDirLinkFile '"
                    + linkFileName + "' - aborting");
            e.printStackTrace();
            displayStartupIOError("Errors.Read_File_Error.Data_Dir_Link_File",
                linkFileName, e);
        }
        System.exit(1);
        // the following line is not reached, but must be present to keep
        // the compiler happy:
        return null;
    }

    private void configureWorkingDirectory(String location) {
        workingDirectory = WorkingDirectoryFactory.getInstance().get(
            location, WorkingDirectoryFactory.PURPOSE_DASHBOARD);
        if (JnlpRelauncher.maybeRelaunch(workingDirectory))
            System.exit(0);

        String locationDescr = workingDirectory.getDescription();

        try {
            lockMessageHandler = new LockMsgHandler();
            workingDirectory.acquireProcessLock(ACTIVATE_MESSAGE,
                lockMessageHandler);
        } catch (SentLockMessageException e) {
            System.exit(0);
        } catch (LockFailureException e) {
            displaySharingError(locationDescr);
            System.exit(1);
        }

        try {
            workingDirectory.prepare();
        } catch (HttpException.Unauthorized e) {
            displayStartupPermissionError("Unauthorized");
            System.exit(1);
        } catch (HttpException.Forbidden e) {
            displayStartupPermissionError("Forbidden");
            System.exit(1);
        } catch (IOException e) {
            String resKey;
            if (workingDirectory instanceof BridgedWorkingDirectory) {
                resKey = "Errors.Read_File_Error.Data_Server";
            } else {
                resKey = "Errors.Read_File_Error.Data_Directory";
            }
            displayStartupIOError(resKey, locationDescr, e);
            System.exit(1);
        }

        // Check to see if a legacy shortcut is being used to launch a dataset
        // that is now hosted on a team server.  If so, possibly configure the
        // team server URL as the "default team server."
        TeamServerSelector.maybeSetDefaultTeamServerUrl(workingDirectory);

        File cwd = workingDirectory.getDirectory();
        System.setProperty("user.dir", cwd.getAbsolutePath());
    }

    private class LockMsgHandler implements LockMessageHandler {
        public String handleMessage(LockMessage e) {
            String msg = e.getMessage();
            if (ACTIVATE_MESSAGE.equals(msg)) {
                DashController.raiseWindow();
                return "OK";
            }
            else if (LockMessage.LOCK_LOST_MESSAGE.equals(msg)) {
                logger.severe("The write lock was lost on the "
                        + workingDirectory);
                showLostLockMessage("Ongoing_Advice");
                return "OK";
            }
            else {
                throw new IllegalArgumentException("Unrecognized message");
            }
        }
    }
    private static final String ACTIVATE_MESSAGE = "activate";


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

    private void initializeOsHelper() {
        try {
            osHelper = new FallbackObjectFactory<Initializable>(Initializable.class)
                    .add(MAC_OS_X_HELPER_CLASS)
                    .get();
            osHelper.initialize(this);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    private static final String MAC_OS_X_HELPER_CLASS =
        "net.sourceforge.processdash.ui.macosx.DashboardMacOSXHelper";

    private void maybeRecordRecentDataset() {
        // if this dataset was opened via the quick launcher, don't record it.
        String quickLaunchMode = System
                .getProperty(QuickLauncher.QUICK_LAUNCH_MODE_PROP);
        if (quickLaunchMode != null)
            return;

        // check to see if this dataset appears to be a third-party launch
        // of a personal dataset from the PDES.  If so, don't record it.
        String windowTitle = getTitle();
        if (windowTitle != null
                && windowTitle.startsWith("Process Dashboard <"))
            return;

        // retrieve the location of the working directory
        String location = workingDirectory.getDescription();

        try {
            boolean isTeam = Settings.isTeamMode();
            datasetRegistration = RecentDatasets.register(location, isTeam);
            datasetRegistrationElapsedTimestamp = System.currentTimeMillis();
        } catch (Throwable t) {}
    }
    private Preferences datasetRegistration;
    private long datasetRegistrationElapsedTimestamp;

    private void maybeUpdateRecentDataset() {
        if (datasetRegistration != null) {
            long now = System.currentTimeMillis();
            long elapsed = now - datasetRegistrationElapsedTimestamp;
            int elapsedMinutes = (int) (elapsed / DateUtils.MINUTES);
            RecentDatasets.elapsed(datasetRegistration, elapsedMinutes);

            datasetRegistrationElapsedTimestamp += elapsedMinutes
                    * DateUtils.MINUTES;
        }
    }

    private int hierChangeCount = 0;
    private void registerHierarchyDataElement() {
        ListData propItem = new ListData();
        propItem.add(props);
        propItem.add(String.valueOf(hierChangeCount++));
        data.putValue(DashHierarchy.DATA_REPOSITORY_NAME, propItem);
    }

    private void configureTeamOrPersonalDatasetMode() {
        String mode = Settings.getVal(Settings.DATASET_MODE);
        if (mode == null) {
            mode = inferDatasetMode();
            InternalSettings.set(Settings.DATASET_MODE, mode);

            if (Settings.isHybridMode()) {
                // If we configure hybrid mode (an unusual case), record this
                // as a "manual" setting. This will make it easy for users to
                // edit the value under "C > Tools > Preferences > Advanced."
                String manualItems = Settings.getVal("manuallyEntered");
                if (StringUtils.hasValue(manualItems))
                    manualItems = manualItems + ";" + Settings.DATASET_MODE;
                else
                    manualItems = Settings.DATASET_MODE;
                InternalSettings.set("manuallyEntered", manualItems);
            }
        }
    }

    private String inferDatasetMode() {
        // Look through the hierarchy for existing team/personal projects
        boolean containsTeamProject = false;
        boolean containsPersonalProject = false;

        for (Iterator i = props.values().iterator(); i.hasNext();) {
            Prop p = (Prop) i.next();
            String templateId = (p == null ? null : p.getID());
            if (templateId == null)
                ;

            else if (templateId.endsWith("/TeamRoot")
                    || templateId.endsWith("/MasterRoot"))
                containsTeamProject = true;

            else if (templateId.endsWith("/IndivRoot")
                    || templateId.endsWith("/Indiv2Root")
                    || templateId.startsWith("PSP"))
                containsPersonalProject = true;
        }

        // if we found team/personal projects, infer the mode from their type
        if (containsTeamProject && containsPersonalProject)
            return Settings.DATASET_MODE_HYBRID;
        else if (containsTeamProject)
            return Settings.DATASET_MODE_TEAM;
        else if (containsPersonalProject)
            return Settings.DATASET_MODE_PERSONAL;

        // no team projects were found.  Make our next best guess based on
        // whether a non-default HTTP port has been configured.
        int httpPort = Settings.getInt(HTTP_PORT_SETTING, DEFAULT_WEB_PORT);
        if (httpPort == DEFAULT_WEB_PORT)
            return Settings.DATASET_MODE_PERSONAL;
        else
            return Settings.DATASET_MODE_TEAM;
    }

    /**
     * Create the database plugin and possibly start it.
     * 
     * @return true if the plugin was started.
     */
    private boolean createDatabasePlugin() {
        try {
            // create the database plugin object
            List extensions = ExtensionManager.getExecutableExtensions(
                DatabasePlugin.EXTENSION_POINT_ID, this);
            if (extensions == null || extensions.isEmpty())
                return false;

            // there should always only be one instance of this extension
            // point.  If multiple are present, go with the first one
            DatabasePlugin plugin = (DatabasePlugin) extensions.get(0);
            this.databasePlugin = plugin;

            // register the plugin as an element in the data repository
            ListData dbItem = new ListData();
            dbItem.add(databasePlugin);
            data.putValue(DatabasePlugin.DATA_REPOSITORY_NAME, dbItem);
            data.pinElement(DatabasePlugin.DATA_REPOSITORY_NAME);

            // check a user setting to see whether the database plugin
            // should be started. If the user doesn't have a value for the
            // setting, only start the plugin for team dashboard datasets.
            if (Settings.getBool("tpidw.enabled", Settings.isTeamMode())) {
                plugin.initialize();
                data.addGlobalDefineDeclarations("#define DATABASE_PLUGIN t");
                return true;
            }

        } catch (Exception e) {
            // problem starting database plugin
            logger.log(Level.SEVERE, "Unable to start the database plugin", e);
        }
        return false;
    }

    private void configureStandardIconSize() {
        double height = configure_button.getPreferredSize().getHeight();
        JButton dummy = new JButton();
        if (!MacGUIUtils.isMacOSX())
            dummy.setMargin(new Insets(0,0,0,0));
        Insets buttonInsets = dummy.getBorder().getBorderInsets(dummy);
        height = height - buttonInsets.top - buttonInsets.bottom;
        DashboardIconFactory.setStandardIconSize((int) height);
    }

    private void buildPersonalUI(ProfTimer pt) {
        getContentPane().setLayout(new GridBagLayout());

        addToMainWindow(configure_button, 0, 2, 2);
        if (Settings.isReadWrite())
            addToMainWindow(pause_button, 0);

        PercentSpentIndicator pct_spent_indicator = new PercentSpentIndicator(
                this, getTimeLoggingModel());
        addToMainWindow(pct_spent_indicator, 0);
        pt.click("Created percent spent indicator");

        DefectButton defect_button = new DefectButton(this);
        if (Settings.isReadWrite())
            addToMainWindow(defect_button, 0);
        pt.click("Created defect button");

        ScriptButton script_button = new ScriptButton(this);
        addToMainWindow(script_button, 0);
        pt.click("Created script button");

        hierarchy_menubar = new JMenuBar();
        addToMainWindow(hierarchy_menubar, 1.0);

        TaskCommenterButton taskCommenterButton = new TaskCommenterButton(this,
                activeTaskModel);
        addToMainWindow(taskCommenterButton, 0);
        pt.click("Created task commenter button");

        DependencyIndicator dependencyIndicator = new DependencyIndicator(this,
                activeTaskModel);
        addToMainWindow(dependencyIndicator, 0, 0, 0);
        pt.click("Created dependency indicator");

        CompletionButton completion_button = new CompletionButton(this,
                activeTaskModel);
        addToMainWindow(completion_button, 0, 0, 1);
        new TaskTimeLoggingErrorWatcher(this, activeTaskModel,
                timeLog.getTimeLoggingModel(), data);
        pt.click("Created completion button");

        if (Settings.isReadWrite()) {
            AddTaskButton addTaskButton = new AddTaskButton(this);
            addToMainWindow(addTaskButton, 0, 0,
                Settings.getInt("mainWindow.paddingRight", 5));
        }

        taskNav = new TaskNavigationSelector
                (this, hierarchy_menubar, activeTaskModel);
        completion_button.setNavSelector(taskNav);
        pt.click("Created task navigation selector");

        dependencyIndicator.update();
        pt.click("Updated dependency indicator");
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

    private void buildTeamUI(ProfTimer pt) {
        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());

        pane.add(configure_button, BorderLayout.NORTH);

        TeamProjectBrowser teamProjectBrowser = new TeamProjectBrowser(this);
        pane.add(teamProjectBrowser, BorderLayout.CENTER);
        pt.click("Created team project browser");
    }

    private void setupWindowTitle(String titleFromCommandLine) {
        // if a title was given on the command line, register it as the default
        // (overriding the locale-specific default value).
        if (StringUtils.hasValue(titleFromCommandLine))
            InternalSettings.setDefaultValue(WINDOW_TITLE_SETTING,
                titleFromCommandLine);

        // now consult the user preferences and set the window title
        setWindowTitleFromPreferences();

        // register a listener to update the title when the user
        // preference changes
        InternalSettings.addPropertyChangeListener(WINDOW_TITLE_SETTING,
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    setWindowTitleFromPreferences();
                }});
    }

    private void setWindowTitleFromPreferences() {
        // check for the "forced title" override setting.  (This is used in
        // very uncommon situations, like for taking screen shots to put in
        // user documentation.)
        String title = Settings.getVal("window.forcedTitle");

        // if no forced title is set, read the regular user setting
        if (!StringUtils.hasValue(title)) {
            title = Settings.getVal(WINDOW_TITLE_SETTING);

            // possibly apply the historical or read-only modifier
            if (TeamServerSelector.isHistoricalModeEnabled())
                title += " @ " + TeamServerSelector.getHistoricalDateStr();
            else if (Settings.isReadOnly())
                title = resources.format("ReadOnly.Title_FMT", title);
        }

        // set the title of the window
        setTitle(title);
    }

    private void displayFirstTimeUserHelp() {
        new DashboardWelcomePane(this);
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

        if (charsetName == null || "auto".equals(charsetName)) {
            charsetName = "UTF-8";
        }

        return charsetName;
    }

    private void configureSystemPropertiesFromSettings() {
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

        // potentially reset locale-specific defaults based on user settings
        String altTimeZone = Settings.getVal("timezone");
        if (StringUtils.hasValue(altTimeZone)) {
            try {
                TimeZone.setDefault(TimeZone.getTimeZone(altTimeZone));
            } catch (Exception e) {
                logger.warning("Could not apply user-selected timezone '"
                        + altTimeZone + "'");
            }
        }
        // special handling for language support
        String altLanguage = Settings.getVal("language");
        String langLevel = Settings.getVal("userPref.i18n.masterLevel", "high");
        if ("disabled".equalsIgnoreCase(langLevel)) {
            altLanguage = "en";
        } else if ("low".equalsIgnoreCase(langLevel)) {
            Locale rootLocale = resetResourceBundleCache();
            if (rootLocale != null) {
                Resources.setTargetLocale(rootLocale);
                JComponent.setDefaultLocale(rootLocale);
            }
        } else if ("medium".equalsIgnoreCase(langLevel)) {
            System.setProperty(Settings.SYS_PROP_PREFIX
                    + "i18n.translationMode", "off");
        }
        if (StringUtils.hasValue(altLanguage)) {
            try {
                String origLanguage = Locale.getDefault().getLanguage();
                Locale newLocale = new Locale(altLanguage);
                System.setProperty("user.language", altLanguage);
                System.setProperty("user.origLanguage", origLanguage);
                Locale.setDefault(newLocale);
                Resources.setTargetLocale(newLocale);
                JComponent.setDefaultLocale(newLocale);
                resetResourceBundleCache();
            } catch (Exception e) {
                logger.warning("Could not apply user-selected language '"
                        + altLanguage + "'");
            }
        }
        String defaultTeamServer = Settings.getVal("teamServer.defaultURL");
        if (StringUtils.hasValue(defaultTeamServer))
            System.setProperty(TeamServerSelector.DEFAULT_TEAM_SERVER_PROPERTY,
                defaultTeamServer);
    }
    private Locale resetResourceBundleCache() {
        return new FallbackObjectFactory<LocaleResetSupport>(
                LocaleResetSupport.class).add("LocaleResetSupportJava6Impl") //
                .get().clearResourceBundleCache();
    }

    private void maybeSetupHistoricalMode() {
        if (TeamServerSelector.isHistoricalModeEnabled()) {
            // if we are viewing historical PDES data, turn on the
            // read only flag and register the effective date.
            InternalSettings.setReadOnly(true);
            InternalSettings.set(EVCalculator.FIXED_EFFECTIVE_DATE_SETTING, //
                System.getProperty(TeamServerSelector.DATA_EFFECTIVE_DATE_PROPERTY));
        }
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
            String resKey = setting.endsWith("Personal")
                    ? "ReadOnly.Recommended.Personal_Message"
                    : "ReadOnly.Recommended.Message";
            Object[] message = new Object[] {
                    res.getString(resKey).split("\n"),
                    readOnlyOption, readWriteOption };
            JOptionPane.showMessageDialog(hideSS(), message, title,
                    JOptionPane.QUESTION_MESSAGE);
            if (readOnlyOption.isSelected())
                InternalSettings.setReadOnly(true);
        } else if (setting.equalsIgnoreCase("follow")) {
            InternalSettings.setReadOnlyFollowMode();
        }
    }

    private void tryToLockDataForWriting() {
        String lockOwnerName = getOwnerName();
        String otherUser = null;
        try {
            workingDirectory.acquireWriteLock(lockMessageHandler,
                lockOwnerName);
            return;
        } catch (ReadOnlyLockFailureException ro) {
            showFilesAreReadOnlyMessage(workingDirectory.getDescription(), ro);
            return;
        } catch (CannotCreateLockException e) {
            showCannotCreateLockMessage(workingDirectory.getDescription(), e);
            return;
        } catch (OfflineLockLostException e) {
            showLostOfflineLockMessage(workingDirectory.getDescription(), e);
            return;
        } catch (AlreadyLockedException e) {
            otherUser = e.getExtraInfo();
        } catch (LockFailureException e) {
            showCannotCreateLockMessage(workingDirectory.getDescription(), e);
            return;
        }

        ResourceBundle r = ResourceBundle
                .getBundle("Templates.resources.ProcessDashboard");

        if (!StringUtils.hasValue(otherUser))
            otherUser = r.getString("Errors.Concurrent_Use_Someone_Else");
        String title = r.getString("Errors.Concurrent_Use_Title");
        String message = MessageFormat.format(r
                .getString("Errors.Concurrent_Use_Message2_FMT"), otherUser);

        if (JOptionPane.showConfirmDialog(hideSS(), message.split("\n"), title,
            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            InternalSettings.setReadOnly(true);
        } else {
            System.exit(0);
        }
    }

    /**
     * Get the name of the person running this dashboard.  This is <b>not</b>
     * necessarily the same as the owner of the metrics data!  For example,
     * a team leader might be opening an individual's data, or anyone might
     * be opening a shared team dashboard instance.
     */
    private String getOwnerName() {
        // check the preferences node for this class first.
        Preferences prefs = Preferences
                .userNodeForPackage(ProcessDashboard.class);
        String result = prefs.get("ownerName", null);
        if (result != null)
            return result;

        // If our preferences node didn't contain the data, try looking in
        // the preferences node for the WBS editor.  Many people will already
        // have a value stored there, so we will be able to avoid pestering
        // them for their name again.
        Preferences teamPrefs = Preferences.userRoot().node("/teamdash/wbs");
        result = teamPrefs.get("ownerName", null);
        if (result != null) {
            prefs.put("ownerName", result);
            return result;
        }

        // We really don't want to ask people for their name before they open
        // their personal dashboard instance - that would strike users as odd.
        // So if we get this far, we'll only prompt them for their name if
        // they are opening a dashboard instance served by a remote server.
        if (workingDirectory instanceof BridgedWorkingDirectory) {
            ResourceBundle res = ResourceBundle
                    .getBundle("Templates.resources.ProcessDashboard");
            String title = res.getString("Enter_Name_Dialog.Title");
            String message = res.getString("Enter_Name_Dialog.Prompt");
            result = JOptionPane.showInputDialog(hideSS(), message, title,
                JOptionPane.PLAIN_MESSAGE);
            if (result != null) {
                prefs.put("ownerName", result);
                return result;
            }
        }

        // if we make it all the way here, just look up the user's login
        // username and go with that.
        return System.getProperty("user.name");
    }

    private void showFilesAreReadOnlyMessage(String location,
            ReadOnlyLockFailureException ro) {
        // the user does not have write access to all of the files in
        // the data directory.  They either need to open the dashboard
        // in read only mode, or exit.
        showMustOpenReadOnlyMessage("ReadOnly.File_Permissions", location,
            ro.getFilePath());
    }
    private void showCannotCreateLockMessage(String location, Exception e) {
        // the dashboard was unable to acquire a file lock on the specified
        // directory. The user either needs to open the dashboard in read
        // only mode, or exit.
        showMustOpenReadOnlyMessage("ReadOnly.Cannot_Lock", location, e);
    }
    private void showLostOfflineLockMessage(String location,
            OfflineLockLostException e) {
        // the user locked their dataset for offline use, but someone broke
        // the lock while the dashboard was not running. The user either needs
        // to open the dashboard in read only mode, or exit.
        showMustOpenReadOnlyMessage("ReadOnly.Lost_Offline_Lock",
            e.getSyncTimestamp(), null);
    }

    private void showMustOpenReadOnlyMessage(String resKey, Object messageArg,
            final Object detailsMessage) {
        ResourceBundle res = ResourceBundle
                .getBundle("Templates.resources.ProcessDashboard");
        final String title = res.getString(resKey + ".Title");
        String[] text = MessageFormat.format(
                res.getString(resKey + ".Message_FMT"),
                new Object[] { messageArg }).split("\n");

        ActionListener linkListener = null;
        if (detailsMessage != null) {
            linkListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ExceptionDialog.show(null, title, detailsMessage);
                }};
        }

        Object[] message = new Object[text.length];
        for (int i = 0; i < message.length; i++) {
            if (text[i].contains("<a") == false)
                message[i] = text[i];
            else if (linkListener != null)
                message[i] = new JLinkLabel(text[i], linkListener);
            else {
                String s = text[i];
                s = StringUtils.findAndReplace(s, "<a>", "");
                s = StringUtils.findAndReplace(s, "</a>", "");
                message[i] = s;
            }
        }

        int userResponse = JOptionPane.showConfirmDialog(hideSS(), message,
                title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.ERROR_MESSAGE);
        if (userResponse == JOptionPane.OK_OPTION) {
            InternalSettings.setReadOnly(true);
        } else {
            System.exit(1);
        }
    }

    private void displaySharingError(String location) {
        if (resources == null)
            resources = Resources.getDashBundle("ProcessDashboard");

        String title = resources.getString("Errors.Data_Sharing_Violation_Title");
        String[] message = resources.formatStrings(
            "Errors.Data_Sharing_Violation_Message_FMT", location);
        JOptionPane.showMessageDialog(hideSS(), message, title,
            JOptionPane.ERROR_MESSAGE);
    }


    private void displayStartupPermissionError(String resourceKey) {
        Resources res = Resources.getDashBundle("Authentication.Errors");
        String title = res.getString(resourceKey + ".Title");
        Object message = res.getStrings(resourceKey + ".Message");
        JOptionPane.showMessageDialog(hideSS(), message, title,
            JOptionPane.ERROR_MESSAGE);
    }


    private void displayStartupIOError(String resourceKey, String filename,
            Throwable t) {
        if (resources == null)
            resources = Resources.getDashBundle("ProcessDashboard");

        if (!TeamServerSelector.isUrlFormat(filename)) {
            try {
                File f = new File(filename);
                filename = f.getAbsolutePath();
                filename = f.getCanonicalPath();
            } catch (Exception e) {}
        }

        ExceptionDialog.showWithSubdialog(hideSS(), //
            resources.getString("Errors.Read_File_Error.Title"), //
            resources.formatStrings("Errors.Read_File_Error.Message_FMT",
                resources.getString(resourceKey), filename), " ", //
            "<a>" + resources.getString("More_Information") + "</a>", //
            t);
    }

    private static final String BULLET = "\u2022 ";

    private static void displayStartupUnexpectedError(Throwable t) {
        ResourceBundle res = ResourceBundle
                .getBundle("Templates.resources.ProcessDashboard");
        String title = res.getString("Errors.Unexpected_Error_Title");
        String[] message = res.getString("Errors.Unexpected_Error_Message")
                .split("\n");
        String traceHeader = res.getString("Errors.Unexpected_Error_Trace_Prompt");

        ExceptionDialog.show(null, title, message, " ", traceHeader, t);
    }


    public List<String> getBrokenDataPaths() {
        return brokenData.getMissingDataPrefixes();
    }

    public void openDatafile (String prefix, String dataFile) {
        try {
            data.openDatafile (prefix, property_directory + dataFile);
        } catch (FileNotFoundException fnfe) {
            brokenData.logMissingDataFileError(prefix, fnfe.getMessage());
        } catch (Exception exc) {
            logErr("when opening datafile, '" + dataFile + "' for path '"
                    + prefix + "', caught exception:", exc);
        }
    }

    private void registerEnvironmentalData() {
        try {
            Map env = new HashMap();

            String os = InternalSettings.getOSPrefix();
            env.put("OS.TYPE", new ImmutableStringData(os, false, true));
            env.put("OS.IS_" + os.toUpperCase(), ImmutableDoubleData.TRUE);
            if (!os.equals("windows"))
                env.put("OS.IS_UNIX", ImmutableDoubleData.TRUE);

            data.mountPhantomData("//Env", env);
        } catch (InvalidDatafileFormat e) {
            logger.log(Level.WARNING, "Unexpected error", e);
        }
    }

    static String getBackupQualifier(DataContext data) {
        String result = getOwnerName(data);
        if (!StringUtils.hasValue(result))
            result = Settings.getVal(WINDOW_TITLE_SETTING);
        return result;
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

    public JMenuBar getConfigurationMenus() {
        return configure_button;
    }

    public Action getChangeTaskAction() {
        return taskNav == null ? null : taskNav.getChangeTaskAction();
    }

    public void showAboutDialog() {
        configure_button.startAboutDialog();
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

        Thread shutdownThread = new Thread("Application Shutdown Thread") {
            public void run() {
                synchronized (exitProgramSyncToken) {
                    exitProgramImpl();
                }
            }
        };
        shutdownThread.start();
    }
    private Object exitProgramSyncToken = new Object();

    private void exitProgramImpl() {
        PleaseWaitDialog dialog = new PleaseWaitDialog(this,
            resources.getString("Shutdown.Title"), "", 0);

        String backupQualifier = getBackupQualifier(data);
        try {
            if (quit(dialog) == false) {
                dialog.dispose();
                return;
            }
        } catch (Throwable t) {
            // if the shutdown sequence encounters an uncaught exception,
            // display an error message, but still exit.
            logErr("When shutting down, encountered the exception:", t);
        }

        try {
            if (osHelper != null) osHelper.dispose();
            SystemTrayManagement.getIcon().dispose();
            UserNotificationManager.getInstance().maybeHideNotifications();

            logger.fine("Backing up data directory");
            dialog.setMessage(resources.getString("Shutdown.Saving_Backup"));
            fileBackupManager.maybeRun(FileBackupManager.SHUTDOWN,
                  backupQualifier);
            logger.fine("Shutdown complete");

        } finally {
            System.exit(0);
        }
    }

    boolean quit(PleaseWaitDialog dialog) {
        if (dialog != null)
            dialog.setMessage(resources.getString("Shutdown.Background_Tasks"));
        BackgroundTaskManager.getInstance().suspend(30 * DateUtils.MINUTES,
              30 * DateUtils.SECONDS);

        if (dialog != null)
            dialog.setMessage(resources.getString("Shutdown.Saving_Data"));
        List unsavedData = saveAllData();
        if (unsavedData.isEmpty() == false
                && warnUserAboutUnsavedData(unsavedData) == false) {
            return false;
        }

        fireApplicationEvent(ApplicationEventListener.APP_EVENT_SHUTTING_DOWN);
        if (unsavedData.isEmpty())
            logger.info("Successfully saved all data.");
        else
            logger.severe("Unable to save all data; unsavedData = " + unsavedData);

        // If we reach this point, all data has been successfully saved, and
        // we know for *certain* that we will be shutting down.  We disable
        // further modifications to settings to prevent something in the
        // shutdown sequence from spuriously altering them as a byproduct of
        // dismantling objects/data structures.
        InternalSettings.setDisableChanges(true);

        if (!Settings.getBool(DISABLE_AUTO_EXPORT_SETTING, false)) {
            logger.fine("Performing auto exports");
            if (dialog != null)
                dialog.setMessage(resources.getString("Shutdown.Exporting_Data"));
            ExportManager.getInstance().exportAll(null, this);
        }
        if (webServer != null) {
            logger.fine("Shutting down web server");
            webServer.quit();
            webServer = null;
        }
        if (data != null) {
            logger.fine("Finalizing data repository");
            data.shutDown();
            data = null;
        }
        ImportedTimeLogManager.getInstance().dispose();

        // The final shutdown sequence above (including, for example, the "export
        // all" operation) might have made minor/trivial changes to a few data
        // files.  Perform one last flush (on a best-effort basis) to persist
        // those changes.  If the flush does not succeed, we can ignore it because
        // the trivial changes can be lost without significant repercussions.
        flushWorkingData();

        logger.fine("Removing concurrency lock");
        workingDirectory.releaseLocks();

        return true;
    }

    private boolean warnUserAboutUnsavedData(List unsavedData) {
        if (unsavedData.isEmpty())
            return true;

        if (unsavedData.contains(FLUSH_FAILED_NO_LOCK)) {
            InternalSettings.setReadOnly(true);
            showLostLockMessage("Shutdown_Warning");
            return true;
        }

        String[] message;
        if (unsavedData.contains(FLUSH_FAILED_TRANSIENT_PROBLEM)) {
            message = resources.getStrings("Errors.Save_Error.Remote_Message");

        } else {
            StringBuffer dataItems = new StringBuffer();
            for (Iterator i = unsavedData.iterator(); i.hasNext();) {
                String dataDescr = (String) i.next();
                dataItems.append(BULLET).append(dataDescr).append("\n");
            }
            message = resources.formatStrings(
                "Errors.Save_Error.Message_FMT", dataItems.toString());
        }

        String title = resources.getString("Errors.Save_Error.Title");
        int userChoice = JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.YES_NO_OPTION);
        return userChoice == JOptionPane.YES_OPTION;
    }

    private void showLostLockMessage(String resKey) {
        String title = resources.getString("Errors.Lost_Lock.Title");
        Object[] message = {
                resources.getStrings("Errors.Lost_Lock.Opening_Message"),
                " ",
                resources.getStrings("Errors.Lost_Lock." + resKey)
        };
        JOptionPane.showMessageDialog(this, message, title,
            JOptionPane.ERROR_MESSAGE);
    }

    public List saveAllData() {
        maybeUpdateRecentDataset();

        List unsavedData = new LinkedList();
        if (Settings.isReadOnly())
            return unsavedData;

        // First, ask all GUIs to save their dirty data
        fireApplicationEvent(ApplicationEventListener.APP_EVENT_SAVE_ALL_DATA);

        // Next, save and close the Hierarchy Editor if it is open/dirty
        if (configure_button != null)
            configure_button.saveAndCloseHierarchyEditor();

        // save the size of the team dashboard window
        maybeSaveWindowSize();

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

        if (saveUserGroupData() == false)
            recordUnsavedItem(unsavedData, "Group_Data");

        if (savePermissionsData() == false)
            recordUnsavedItem(unsavedData, "Permissions_Data");

        String flushResult = flushWorkingData();
        if (flushResult != null)
            unsavedData.add(flushResult);

        return unsavedData;
    }

    boolean saveMetricsData() {
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
        if (InternalSettings.isDirty())
            InternalSettings.saveSettings();
        return InternalSettings.isDirty() == false;
    }

    public boolean saveUserGroupData() {
        return UserGroupManager.getInstance().saveAll();
    }

    public boolean savePermissionsData() {
        return PermissionsManager.getInstance().saveAll();
    }

    public static final String FLUSH_SUCCESSFUL = null;
    public static final String FLUSH_FAILED_NO_LOCK = "flushNoLock";
    public static final String FLUSH_FAILED_TRANSIENT_PROBLEM = "flushTransient";

    public String flushWorkingData() {
        if (Settings.isReadOnly())
            return FLUSH_SUCCESSFUL;

        try {
            if (workingDirectory.flushData())
                return FLUSH_SUCCESSFUL;
            else
                return FLUSH_FAILED_TRANSIENT_PROBLEM;
        } catch (LockUncertainException lue) {
            return FLUSH_FAILED_TRANSIENT_PROBLEM;
        } catch (LockFailureException e) {
            return FLUSH_FAILED_NO_LOCK;
        } catch (IOException e) {
            return FLUSH_FAILED_TRANSIENT_PROBLEM;
        }
    }

    public WorkingDirectory getWorkingDirectory() {
        new DashboardPermission("getWorkingDirectory").checkPermission();
        return workingDirectory;
    }


    public static String getVersionNumber() { return versionNumber; }

    static DashboardSplashScreen ss = null;

    public static void dropSplashScreen() {
        if (ss != null) ss.okayToDispose();
        ss = null;
    }

    private static Component hideSS() {
        // on many flavors of Unix (reported on Solaris and RedHat Linux),
        // JOptionPane dialog boxes inexplicably appear behind the splash
        // screen.  After testing, it appears that calling toBack() on the
        // splash screen will resolve the problem.  However, this must not
        // be called for Windows or Mac, because on those platforms toBack()
        // will truly bury the splash screen behind all other open windows
        // for all other applications.
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("windows") && !osName.contains("mac os"))
            if (ss != null)
                ss.toBack();
        return ss;
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

    private void initializeWindowSize() {
        if (Settings.isPersonalMode()) {
            this.pack();

        } else {
            int width, height;
            try {
                String setting = Settings.getVal(DIMENSION_SETTING);
                String[] parts = setting.split(",");
                width = Integer.parseInt(parts[0]);
                height = Integer.parseInt(parts[1]);
            } catch (Exception e) {
                width = 550;
                height = 300;
            }
            this.setSize(width, height);
            this.validate();
        }
    }

    private void maybeSaveWindowSize() {
        if (!Settings.isPersonalMode()) {
            String setting = getWidth() + "," + getHeight();
            InternalSettings.set(DIMENSION_SETTING, setting);
        } else if (taskNav != null) {
            taskNav.storePrefs();
        }
    }

    private static final String DIMENSION_SETTING = "mainWindow.dimensions";

    public void windowSizeRequirementsChanged() {
        if (Settings.isPersonalMode() && this.isVisible()) {
            // the task selector prefers to maintain its current width
            hierarchy_menubar.setPreferredSize(null);
            Dimension d = hierarchy_menubar.getPreferredSize();
            d.width = hierarchy_menubar.getWidth();
            hierarchy_menubar.setPreferredSize(d);

            this.pack();
        }
    }

    public static void main(final String args[]) {
        try {
            AccessController.doPrivileged(new PrivilegedAction() {
                  public Object run() {
                      mainImpl(args);
                      return null;
                  }
            });
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Unexpected exception during startup", t);
            displayStartupUnexpectedError(t);
            System.exit(1);
        }
    }
    private static void mainImpl(String[] args) {
        DashboardSecurity.setupSecurityManager();

        LargeFontsHelper.maybeInitialize();

        ss = new DashboardSplashScreen();
        ss.displayFor(3000);      // show for at least 3 seconds.

        if (Boolean.getBoolean("readOnly"))
            InternalSettings.setReadOnly(true);

        String title = null;
        String location = null;
        for (int i = 0;  i < args.length;  i++) {
            if ("readOnly".equalsIgnoreCase(args[i]))
                InternalSettings.setReadOnly(true);
            else if ("readOnlyFollow".equalsIgnoreCase(args[i]))
                InternalSettings.setReadOnlyFollowMode();
            else if (args[i].startsWith(LOCATION_ARG_PREFIX))
                location = args[i].substring(LOCATION_ARG_PREFIX.length());
            else
                title = args[i];
        }

        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        HttpAuthenticator.maybeInitialize(title);
        MacGUIUtils.tweakLookAndFeel();

        ProcessDashboard dash = new ProcessDashboard(location, title);
        HttpAuthenticator.setParentComponent(dash);

        DashboardIconFactory.setWindowIcon(dash);
        dash.setVisible(true);
        dash.initializeWindowSize();

        dropSplashScreen();
        dash.maybeNotifyOpened();
        dash.aum.maybePerformCheck(dash);
        DataImporter.refreshCachedFiles();
    }
    public DashHierarchy getHierarchy() { return props; }
    public DataRepository getData() { return data; }
    public ObjectCache getCache() { return objectCache; }
    public WebServer getWebServer() { return webServer; }
    public DatabasePlugin getDatabasePlugin() { return databasePlugin; }

}
