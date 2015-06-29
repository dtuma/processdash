// Copyright (C) 1998-2015 Tuma Solutions, LLC
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


package net.sourceforge.processdash.templates;

import static org.eclipse.jetty.util.StringUtil.startsWithIgnoreCase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarInputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.MCFURLConnection;
import net.sourceforge.processdash.process.AutoData;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.process.ScriptNameResolver;
import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.team.mcf.MCFManager;
import net.sourceforge.processdash.templates.DashPackage.InvalidDashPackage;
import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.tool.export.impl.ExternalResourceManifestXMLv1.MCFEntry;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.tool.quicklauncher.CompressedInstanceLauncher;
import net.sourceforge.processdash.ui.lib.ErrorReporter;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.NonclosingInputStream;
import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.ProfTimer;
import net.sourceforge.processdash.util.XMLUtils;



public class TemplateLoader {

    private static final String TEMPLATE_SUFFIX = ".template";
    private static final String XML_TEMPLATE_SUFFIX = "-template.xml";
    private static final String XML_TEMPLATE_FILE = "processdash.xml";
    private static final String DATAFILE_SUFFIX = ".globaldata";
    public static final String MCF_PROCESS_XML = "settings.xml";
    private static final String TEMPLATE_DIR = "Templates/";
    private static final String WEB_INF_DIR = "WEB-INF/";
    private static final String WEB_INF_XML_FILE = WEB_INF_DIR + XML_TEMPLATE_FILE;

    public static final DashboardPermission LOAD_TEMPLATES_PERMISSION =
        new DashboardPermission("templateLoader.loadTemplates");
    public static final DashboardPermission RESET_TEMPLATES_PERMISSION =
        new DashboardPermission("templateLoader.resetTemplates");
    public static final DashboardPermission ADD_TEMPLATE_JAR_PERMISSION =
        new DashboardPermission("templateLoader.addTemplateJar");
    private static final DashboardPermission GET_TEMPLATE_URLS_PERMISSION =
        new DashboardPermission("templateLoader.getTemplateURLs");
    private static final DashboardPermission GET_CLASSLOADER_PERMISSION =
        new DashboardPermission("templateLoader.getTemplateClassloader");
    private static final DashboardPermission CLEAR_CLASSLOADER_PERMISSION =
        new DashboardPermission("templateLoader.clearTemplateClassloader");

    private static final Logger logger = Logger.getLogger(TemplateLoader.class
            .getName());


    private static long templateTimestamp = 0;

    public static DashHierarchy loadTemplates(DataRepository data) {
        LOAD_TEMPLATES_PERMISSION.checkPermission();

        DashHierarchy templates = new DashHierarchy(null);
        ProfTimer pt = new ProfTimer(TemplateLoader.class,
            "TemplateLoader.loadTemplates");

        List<URL> roots = new ArrayList<URL>(Arrays.asList(getTemplateURLs()));
        pt.click("Got template roots");

        for (int i = roots.size(); i-- > 0;) {
            String templateDirURL = roots.get(i).toString();

            if (templateDirURL.startsWith("file:/")) {
                /* If the /Templates directory exists as a local file
                 * somewhere, search through that directory for process
                 * templates.
                 */

                // strip "file:" from the beginning of the url.
                String dirname = templateDirURL.substring(5);
                dirname = HTMLUtils.urlDecode(dirname);
                searchDirForTemplates(templates, dirname, data);
                pt.click("searched dir '" + dirname + "' for templates");

            } else if (templateDirURL.startsWith("jar:")) {
                /* If the /Templates directory found is in a jar somewhere,
                 * search through the jar for process templates.
                 */

                // Strip "jar:" from the beginning and the "!/Templates/"
                // from the end of the URL.
                String jarFileURL = templateDirURL.substring
                    (4, templateDirURL.indexOf('!'));
                JarSearchResult searchResult = searchJarForTemplates(templates,
                    jarFileURL, data);

                // if this was an MCF JAR, remove its URL from the search path
                if (searchResult == JarSearchResult.Mcf) {
                    logger.fine("Processed MCF URL " + templateDirURL);
                    mcf_url_list.add(roots.remove(i));
                }
                pt.click("searched jar '" + jarFileURL + "' for templates");
            }
        }

        // find any missing MCFs that came bundled within a data backup file
        searchExternalResourcesForMissingMcfs(templates, data);

        // store the new list of template URLs, stripped of MCF JAR files
        template_url_list = roots.toArray(new URL[roots.size()]);

        generateRollupTemplates(templates, data);

        createProcessRoot(templates);

        pt.click("done loading templates");
        return templates;
    }

    /** Add a specific template to the search list.
     *
     * Note: this bypasses the package consistency checking that is
     * normally performed.  The package will be added even if it is
     * incompatible with the current version of the dashboard.  In
     * addition, if it makes another dashboard package obsolete, that
     * package will not be removed.  The package named will be added
     * to the beginning of the search list.
     */
    public static boolean addTemplateJar(DataRepository data,
                                  DashHierarchy templates,
                                  String jarfileName) {
        ADD_TEMPLATE_JAR_PERMISSION.checkPermission();

        try {
            // compute the "template url" of the jarfile.
            File jarfile = new File(jarfileName);
            String jarURL = jarfile.toURI().toURL().toString();
            URL jarfileTemplateURL = jarfileTemplateURL(jarURL);

            // if the jar url is already in the lists of template/MCF urls,
            // then nothing needs to be done.
            if (mcf_url_list.contains(jarfileTemplateURL))
                return true;
            for (int i = template_url_list.length;   i-- > 0; )
                if (jarfileTemplateURL.equals(template_url_list[i]))
                    return true;

            // find and process templates in the jarfile.
            JarSearchResult searchResult = searchJarForTemplates(templates,
                jarURL, data);
            if (searchResult != JarSearchResult.None) {

                // add applicable rollup templates. (This will regenerate
                // other rollup templates, but that shouldn't hurt anything.)
                generateRollupTemplates(templates, data);

                // recreate the process root.
                createProcessRoot(templates);
            }

            // insert the new url at the beginning of the template list.
            if (searchResult != JarSearchResult.Mcf) {
                URL[] new_list = new URL[template_url_list.length + 1];
                System.arraycopy(template_url_list, 0,      // src
                                 new_list, 1,               // dest
                                 template_url_list.length);
                new_list[0] = jarfileTemplateURL;
                template_url_list = new_list;
            }

            // create a dash package and add it to our list
            try {
                dashPackages.add(0, new DashPackage(jarfileTemplateURL));
            } catch (InvalidDashPackage idp) {}

            return true;

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return false;
    }


    /** Dynamically create templates for any orphaned data rollups.
     */
    private static void generateRollupTemplates(DashHierarchy templates,
                                                  DataRepository data) {
        String rollupXML = AutoData.generateRollupTemplateXML();
        if (rollupXML == null) return;

        try {
            ByteArrayInputStream in =
                new ByteArrayInputStream(rollupXML.getBytes("UTF-8"));
            loadXMLProcessTemplate(templates, data, null, null, in, true);
        } catch (IOException ioe) {}
    }


    /**
     * The templates hierarchy needs a root.  However, that root cannot
     * be read from any file because its children are determined dynamically
     * by searching classpaths and template directories.  Once the children
     * are loaded from their various locations, this routine dynamically
     * builds their parent.
     *
     * NOTE: currently, the children are placed in alphabetical order.
     */
    private static void createProcessRoot(DashHierarchy templates) {
        Enumeration nodes = templates.keys();
        Vector processes = new Vector();
        PropertyKey key, parent;
        while (nodes.hasMoreElements()) {
            key = (PropertyKey) nodes.nextElement();
            parent = key.getParent();
            if (parent != null && parent.equals(PropertyKey.ROOT))
                processes.add(key);
        }

        if (processes.size() > 0) {
            Collections.sort(processes);
            Prop p = new Prop();
            while (processes.size() > 0)
                p.addChild((PropertyKey)processes.remove(0), -1);
            templates.put(PropertyKey.ROOT, p);
        }
    }

    private static void searchExternalResourcesForMissingMcfs(
            DashHierarchy templates, DataRepository data) {
        for (MCFEntry mcf : ExternalResourceManager.getInstance().getMcfs()) {
            try {
                loadMissingMcfFromExternalResource(templates, data, mcf);
            } catch (IOException ioe) {
                logger.severe("error looking for MCF data in "
                        + mcf.getBaseDirectory());
                ioe.printStackTrace(System.out);
            }
        }
    }

    private static void loadMissingMcfFromExternalResource(
            DashHierarchy templates, DataRepository data, MCFEntry mcf)
            throws IOException {

        // See if we already have an installed package for this MCF. If so,
        // prefer it over the one loaded from external resources.
        String mcfID = mcf.getFrameworkID();
        if (getPackage(mcfID) != null)
            return;

        // Skip external MCFs if we cannot find the process XML file
        File mcfDir = mcf.getBaseDirectory();
        if (mcfDir == null)
            return;
        File mcfXmlFile = new File(mcfDir, MCF_PROCESS_XML);
        if (!mcfXmlFile.isFile())
            return;

        // Open the MCF and register it with the MCF manager
        String baseUrl = mcfDir.toURI().toURL().toString();
        FileInputStream processXml = new FileInputStream(mcfXmlFile);
        String mcfVersion = mcf.getFrameworkVersion();

        InputStream mcfXmlData = MCFManager.getInstance().registerMcf(baseUrl,
            processXml, mcfVersion, false);
        processXml.close();

        // load MCF process templates if the above steps were successful
        if (mcfXmlData != null) {
            loadXMLProcessTemplate(templates, data, mcfXmlFile.getPath(), null,
                mcfXmlData, true);
            dashPackages.add(new DashPackage(mcfID, mcfVersion));
        }
    }

    private enum JarSearchResult { None, Template, Mcf }

    private static JarSearchResult searchJarForTemplates(
            DashHierarchy templates, String jarURL, DataRepository data) {

        boolean foundTemplates = false;
        try {
            debug("searching for templates in " + jarURL);

            URL jarFileUrl = new URL(jarURL);
            JarInputStream jarFile =
                new JarInputStream((jarFileUrl).openStream());

            ZipEntry file;
            String filename;
            while ((file = jarFile.getNextEntry()) != null) {
                filename = file.getName().toLowerCase();
                if (filename.equals(MCF_PROCESS_XML)) {
                    String baseURL = "jar:" + jarURL + "!/";
                    InputStream mcfXmlData = MCFManager.getInstance()
                            .registerMcf(baseURL, jarFile, null, true);
                    if (mcfXmlData != null) {
                        String n = MCF_PROCESS_XML + " (in " + jarURL + ")";
                        loadXMLProcessTemplate(templates, data, n, null,
                            mcfXmlData, true);
                        jarFile.close();
                        return JarSearchResult.Mcf;
                    }
                }

                if (startsWithIgnoreCase(filename, TEMPLATE_DIR)) {
                    if (filename.lastIndexOf('/') != 9) continue;
                } else if (startsWithIgnoreCase(filename, WEB_INF_DIR)) {
                    if (!filename.equalsIgnoreCase(WEB_INF_XML_FILE)) continue;
                } else {
                    continue;
                }

                if (filename.endsWith(XML_TEMPLATE_SUFFIX)
                        || filename.endsWith(XML_TEMPLATE_FILE)) {
                    debug("loading template: " + filename);
                    String n = file.getName() + " (in " + jarURL + ")";
                    loadXMLProcessTemplate(templates, data, n, jarFileUrl,
                            jarFile, false);
                    foundTemplates = true;
                } else if (filename.endsWith(TEMPLATE_SUFFIX)) {
                    debug("loading template: " + filename);
                    loadProcessTemplate(templates, jarFile, false);
                    foundTemplates = true;
                } else if (filename.endsWith(DATAFILE_SUFFIX)) {
                    try {
                        debug("loading data: " + filename);
                        data.addGlobalDefinitions(jarFile, false);
                    } catch (Exception e) {
                        logger.severe
                            ("unable to load global process data from " +
                             file.getName() + " in " + jarURL + ": " + e);
                    }
                }
            }
            jarFile.close();

        } catch (IOException ioe) {
            logger.severe("error looking for templates in " + jarURL);
            ioe.printStackTrace(System.out);
        }
        return foundTemplates ? JarSearchResult.Template : JarSearchResult.None;
    }

    private static boolean searchDirForTemplates(DashHierarchy templates,
                                                 String directoryName,
                                                 DataRepository data) {
        debug("searching for templates in " + directoryName);
        File[] process_templates = new File(directoryName).listFiles();
        if (process_templates == null) return false;

        int i = process_templates.length;
        boolean foundTemplates = false;
        File f;
        String filename;
        while (i-- > 0) {
            f = process_templates[i];
            filename = f.getName().toLowerCase();
            if (filename.endsWith(XML_TEMPLATE_SUFFIX)) {
                try {
                    debug("loading template: " + f);
                    loadXMLProcessTemplate
                        (templates, data, f.getPath(), null,
                         new FileInputStream(f), true);
                    processTimestamp(f);
                    foundTemplates = true;
                } catch (IOException ioe) {
                    debug("unable to load process template: " + f);
                }
            } else if (filename.endsWith(TEMPLATE_SUFFIX)) {
                try {
                    debug("loading template: " + f);
                    loadProcessTemplate
                        (templates, new FileInputStream(f), true);
                    processTimestamp(f);
                    foundTemplates = true;
                } catch (IOException ioe) {
                    debug("unable to load process template: " + f);
                }
            } else if (filename.endsWith(DATAFILE_SUFFIX)) {
                try {
                    debug("loading data: " + f);
                    data.addGlobalDefinitions(new FileInputStream(f), true);
                    processTimestamp(f);
                } catch (Exception e) {
                    logger.severe
                        ("unable to load global process data from " + f +
                         ": " + e);
                }
            }
        }
        File webInfXml = new File(directoryName, WEB_INF_XML_FILE);
        if (webInfXml.isFile()) {
            try {
                debug("loading template: " + webInfXml);
                loadXMLProcessTemplate
                    (templates, data, webInfXml.getPath(), null,
                     new FileInputStream(webInfXml), true);
                processTimestamp(webInfXml);
                foundTemplates = true;
            } catch (IOException ioe) {
                debug("unable to load process template: " + webInfXml);
            }
        }
        return foundTemplates;
    }

    private static void loadProcessTemplate(DashHierarchy templates,
                                            InputStream in, boolean close)
        throws IOException
    {
        DashHierarchy template = new DashHierarchy(null);
        template.load(in, close);
        createScriptMaps(template);

        templates.putAll(template);
    }

    private static void loadXMLProcessTemplate(DashHierarchy templates,
                                               DataRepository data,
                                               String filename,
                                               URL baseUrl,
                                               InputStream in, boolean close)
        throws IOException
    {
        Element root = null;
        try {
            if (!close)
                in = new NonclosingInputStream(in);

            // this closes the file without our permission.
            Document doc = XMLUtils.parse(in);
            ExtensionManager.addXmlDoc(doc, filename, baseUrl);
            root = doc.getDocumentElement();
        } catch (SAXException se) {
            String message = XMLUtils.exceptionMessage(se);
            Resources r = Resources.getDashBundle("Templates");
            if (message == null)
                message = r.format("Error_FMT", filename);
            else
                message = r.format("Error_Message_FMT", filename, message);
            logTemplateError(message);
            return;
        }

        AutoData.registerTemplates(root, data);

        createScriptMaps(root);

        try {
            DashHierarchy template = new DashHierarchy(null);
            template.loadXMLTemplate(root);
            template.premove(PropertyKey.ROOT);
            createScriptMaps(template);
            templates.putAll(template);
        } catch (SAXException se) {
            // Can this happen?
        }

        generateDefaultScriptMaps(root);
    }

    protected static void debug(String msg) {
        logger.finest(msg);
    }


    /** Clear any previously computed list of template URLs, so the next
     * call to {@link #getTemplateURLs()} will recalculate the list anew.
     */
    public static void resetTemplateURLs() {
        RESET_TEMPLATES_PERMISSION.checkPermission();
        template_url_list = null;
        mcf_url_list = null;
    }

    /** Returns a list of URLs to templates, in logical search order.
     *
     * The URL search order is:<OL>
     * <LI>Look first in the directory specified by the user setting
     *     "templates.directory" (should end in "Templates").
     * <LI>Next, look in any JAR files contained in that directory.
     * <LI>Next, look in any JAR files contained in the parent of that
     *     directory.
     * <LI>If there is a Templates directory underneath the master application
     *     directory, look there for the file, or for JARs containing the file.
     * <LI>If there is a Templates directory alongside the pspdash.jar file,
     *     look there, first for the file, then for JARs containing the file.
     * <LI>If there are any JAR files next to the pspdash.jar file, look in
     *     them.
     * <LI>Finally, look in the classpath (which includes the pspdash.jar
     *     file).</OL>
     */
    public static URL[] getTemplateURLs() {
        GET_TEMPLATE_URLS_PERMISSION.checkPermission();

        if (template_url_list != null) return template_url_list;

        Vector result = new Vector();
        ProfTimer pt = new ProfTimer(TemplateLoader.class,
                "TemplateLoader.getTemplateURLs");

        String userSetting = getSearchPath();
        if (userSetting != null && userSetting.length() != 0) {
            StringTokenizer tok = new StringTokenizer(userSetting, ";");
            while (tok.hasMoreTokens())
                addTemplateURLs(tok.nextToken(), result);
        }

        File appTemplateDir = getApplicationTemplateDir();
        if (appTemplateDir.isDirectory()) {
            try {
                result.add(appTemplateDir.toURI().toURL());
                scanDirForJarFiles(appTemplateDir, result);
            } catch (MalformedURLException e) {}
        }

        String baseDir = getBaseDir();
        if (unpackagedBinaryBaseDir != null)
            addTemplateURLs(unpackagedBinaryBaseDir + Settings.sep + "dist",
                    result);
        addTemplateURLs(baseDir, result);

        try {
            Enumeration e = TemplateLoader.class.getClassLoader()
                .getResources(TEMPLATE_DIR);
            while (e.hasMoreElements())
                result.addElement(e.nextElement());
        } catch (IOException ioe) {
            logger.severe("An exception was encountered when searching "
                    + "for process templates in the classpath:\n\t" + ioe);
        }

        filterURLList(result);

        try {
            URL mcfUrl = new URL(MCFURLConnection.PROTOCOL + ":/");
            result.add(result.size() - 1, mcfUrl);
        } catch (MalformedURLException mue) {
            // this will occur if the URL stream handler factory hasn't been
            // installed yet. This should generally only happen as the Quick
            // Launcher opens, and the problem will be resolved during the
            // Process Dashboard startup sequence.
        }

        template_url_list = urlListToArray(result);
        mcf_url_list = new ArrayList<URL>();
        pt.click("Calculated template URL list");
        return template_url_list;
    }
    private static String getSearchPath() {
        if (Settings.getBool("templates.disableSearchPath", false)) {
            logger.config("Template search path is disabled");
            return null;
        }

        if (Settings.getBool("slowNetwork", false)
                && !Settings.getBool("slowNetwork.searchForTemplates", false)) {
            logger.config("Disabling template search path for slow network");
            return null;
        }

        String result = Settings.getFile("templates.directory");
        logger.config("Template search path is " + result);
        return result;
    }
    private static URL[] urlListToArray(List list) {
        int i = list.size();
        URL[] result = new URL[i];
        debug("The template result (in reverse order) are:");
        while (i-- > 0) {
            result[i] = (URL) list.get(i);
            debug(result[i].toString());
        }
        return result;
    }
    private static URL[] template_url_list = null;
    private static List<URL> mcf_url_list = null;
    //private static final String JARFILE_NAME = "pspdash.jar";
    private static final String TEMPLATE_DIRNAME = "Templates";
    private static final String SEP_TEMPL_DIR =
        Settings.sep + TEMPLATE_DIRNAME.toLowerCase();

    private static void addTemplateURLs(String directory, Vector v) {
        // abort if directory is null or zero length.
        if (directory == null || directory.length() == 0) return;

        // If directory name ends with a path separator, remove it.
        if (directory.endsWith(Settings.sep))
            directory = directory.substring(0, directory.length() -
                                            Settings.sep.length());

        File parentDir, templateDir;

        // Check to see if we were given the "Templates" directory, or its
        // parent.
        if (directory.toLowerCase().endsWith(SEP_TEMPL_DIR)) {
            templateDir = new File(directory);
            if (!templateDir.isDirectory()) return;
            try {
                templateDir = templateDir.getCanonicalFile();
            } catch (IOException ioe) {}
            parentDir = templateDir.getParentFile(); // may return null

        } else {
            parentDir = new File(directory);
            if (!parentDir.isDirectory()) return;
            try {
                parentDir = parentDir.getCanonicalFile();
            } catch (IOException ioe) {}
            templateDir = new File(parentDir, TEMPLATE_DIRNAME);
            if (!templateDir.isDirectory()) templateDir = null;
        }

        if (templateDir != null) try {
            v.add(templateDir.toURI().toURL());
        } catch (MalformedURLException mue) {}

        scanDirForJarFiles(templateDir, v);
        scanDirForJarFiles(parentDir, v);
    }
    private static void scanDirForJarFiles(File dir, Vector v) {
        if (dir == null) return;

        File[] dirContents = dir.listFiles();
        Arrays.sort(dirContents);
        String name, lname;
        for (int i=0;  i < dirContents.length;  i++) try {
            name = dirContents[i].toURI().toURL().toString();
            lname = name.toLowerCase();
            if ((lname.endsWith(".jar") || lname.endsWith(".zip"))
                    && !NONTEMPLATE_FILENAMES.matches(lname)) {
                processTimestamp(dirContents[i]);
                if (!isDashboardJarfile(dirContents[i]))
                    v.add(jarfileTemplateURL(name));
            } else if (lname.endsWith(".war")) {
                processTimestamp(dirContents[i]);
                v.add(warfileWebInfURL(name));
            }
        } catch (MalformedURLException mue) {}
    }
    private static final PatternList NONTEMPLATE_FILENAMES = new PatternList(
            new String[] { "/pdash-install-", "-launch-profile-",
                    "/pdash-.*-cd", "/pdash-src-" });
    private static URL jarfileTemplateURL(String jarfileURL)
        throws MalformedURLException
    {
        return new URL("jar:" + jarfileURL + "!/" + TEMPLATE_DIR);
    }
    private static URL warfileWebInfURL(String warfileURL)
            throws MalformedURLException {
        return new URL("jar:" + warfileURL + "!/" + WEB_INF_DIR);
    }

    /** Figure out what directory contains the pspdash.jar file. */
    private static String getBaseDir() {
//        String className = TemplateLoader.class.getName();
//        String path = "/" + className.replace('.', '/') + ".class";
        URL u = TemplateLoader.class.getResource("TemplateLoader.class");
        if (u == null) return null;

        String myURL = u.toString();
        // we are expecting a URL like (Windows)
        // jar:file:/D:/path/to/pspdash.jar!/net/.../TemplateLoader.class
        // or (Unix)
        // jar:file:/usr/path/to/pspdash.jar!/net/.../TemplateLoader.class
        if (myURL.startsWith("jar:file:")) try {
            String jarFileName = myURL.substring(9,myURL.indexOf("!/net/"));
            jarFileName = HTMLUtils.urlDecode(jarFileName);
            File jarFile = new File(jarFileName);
            if (jarFile.exists())
                return jarFile.getParent();
            else
                return null;
        } catch (IndexOutOfBoundsException ioobe) {}
        // if the URL doesn't start with 'jar:file:' (as it would when the
        // code is packaged in a jar file), perhaps the class files are
        // unpackaged and located in a directory (a common scenario for
        // running within an IDE).
        else if (myURL.startsWith("file:")) try {
            String classDirName = myURL.substring(5,myURL.lastIndexOf("/net/"));
            classDirName = HTMLUtils.urlDecode(classDirName);
            File classDir = new File(classDirName);
            if (classDir.isDirectory()) {
                unpackagedBinaryBaseDir = classDir.getParent();
                return unpackagedBinaryBaseDir;
            }
        } catch (Exception e) {}
        return null;
    }
    private static String unpackagedBinaryBaseDir = null;


    public static File getDefaultTemplatesDir() {
        String baseDir = getBaseDir();
        if (baseDir == null)
            return null;
        else
            return new File(baseDir, "Templates");
    }

    private static boolean isDashboardJarfile(File f) {
        Object entry1 = null, entry2 = null;
        try {
            ZipFile zipFile = new ZipFile(f);
            entry1 = zipFile.getEntry("pspdash/PSPDashboard.class"); // legacy
            entry2 = zipFile.getEntry("net/sourceforge/processdash/ProcessDashboard.class");
            zipFile.close();
        } catch (Throwable t) { }
        return (entry1 != null || entry2 != null);
    }

    private static void filterURLList(Vector urls) {
        deleteDuplicates(urls);
        Map packages = makePackages(urls);
        removeIncompatiblePackages(packages, urls);
        removeDuplicatePackages(packages, urls);
        dashPackages = new ArrayList(packages.keySet());
    }

    private static void deleteDuplicates(Vector list) {
        Set itemsSeen = new HashSet();
        Iterator i = list.iterator();
        while (i.hasNext()) {
            Object item = i.next();
            if (itemsSeen.contains(item))
                i.remove();
            else
                itemsSeen.add(item);
        }
    }

    private static Map makePackages(Vector urls) {
        HashMap result = new HashMap();
        Iterator i = urls.iterator();
        while (i.hasNext()) try {
            URL url = (URL) i.next();
            result.put(new DashPackage(url), url);
        } catch (DashPackage.InvalidDashPackage idp) {}

        if (unpackagedBinaryBaseDir != null) {
            try {
                Properties p = new Properties();
                File libDir = new File(unpackagedBinaryBaseDir, "lib");
                File versionFile = new File(libDir, "version.properties");
                FileInputStream in = new FileInputStream(versionFile);
                p.load(in);
                in.close();
                String version = p.getProperty("dashboard.version");
                result.put(new DashPackage(version), null);
            } catch (Exception e) {}
        }

        return result;
    }

    /** Remove add-ons that are incompatible with this version of the
     * dashboard. */
    private static void removeIncompatiblePackages(Map packages, Vector urls) {
        String dashVersion = getDashboardVersion(packages);
        Iterator i = packages.keySet().iterator();
        while (i.hasNext()) {
            DashPackage pkg = (DashPackage) i.next();
            if (pkg.isIncompatible(dashVersion)
                    || isDeprecatedLegacyPackage(pkg)) {
                Object url = packages.get(pkg);
                urls.remove(url);
                i.remove();
            }
        }
    }

    private static String getDashboardVersion(Map packages) {
        // look through the packages for one with the id "pspdash".
        Iterator i = packages.keySet().iterator();
        while (i.hasNext()) {
            DashPackage pkg = (DashPackage) i.next();
            if ("pspdash".equals(pkg.id)) // legacy
                return pkg.version;
        }

        // look through the packages for a locale-specific "pspdash"
        // distribution
        i = packages.keySet().iterator();
        while (i.hasNext()) {
            DashPackage pkg = (DashPackage) i.next();
            if (pkg.id != null && pkg.id.startsWith("pspdash_")) // legacy
                return pkg.version;
        }

        return null;            // shouldn't happen...
    }

    private static boolean isDeprecatedLegacyPackage(DashPackage pkg) {
        if ("pspForEng".equals(pkg.id)) {
            return DashPackage.compareVersions(pkg.version, "3.0") < 0;
        }
        return false;
    }

    private static void removeDuplicatePackages(Map packages, Vector urls) {
        List packagesToDelete = new ArrayList();
        Map packagesToKeep = new HashMap();

        for (Iterator i = packages.keySet().iterator(); i.hasNext();) {
            DashPackage pkg = (DashPackage) i.next();
            String id = pkg.id;
            DashPackage match = (DashPackage) packagesToKeep.get(id);
            if (match == null)
                packagesToKeep.put(id, pkg);
            else if (selectBetterPackage(pkg, match) == match)
                packagesToDelete.add(pkg);
            else {
                packagesToKeep.put(id, pkg);
                packagesToDelete.add(match);
            }
        }

        for (Iterator i = packagesToDelete.iterator(); i.hasNext();) {
            DashPackage pkg = (DashPackage) i.next();
            Object url = packages.remove(pkg);
            urls.remove(url);
        }
    }

    private static DashPackage selectBetterPackage(DashPackage a, DashPackage b) {
        if (a == null) return b;
        if (b == null) return a;

        // obviously, we want to prefer packages with higher version numbers.
        int versionCompare = DashPackage.compareVersions(a.version, b.version);
        if (versionCompare > 0) return a;
        if (versionCompare < 0) return b;

        // if we have two identical packages (same id, same version), prefer
        // the one that is installed locally.  This offers better performance
        // and offers certain advantages regarding locking of network files.
        String baseDir = getBaseDir();
        if (baseDir != null)
            try {
                if (a.filename != null && a.filename.indexOf(baseDir) != -1)
                    return a;
                if (b.filename != null && b.filename.indexOf(baseDir) != -1)
                    return b;
            } catch (Exception e) {}

        // we can't see much difference between the two files.  Just pick one.
        return a;
    }

    /**
     * Return the master application template directory.
     */
    public static File getApplicationTemplateDir() {
        File appDir = DirectoryPreferences.getApplicationDirectory();
        File appTemplateDir = new File(appDir, TEMPLATE_DIRNAME);
        return appTemplateDir;
    }

    /**
     * Return true if the given directory is present in the user-defined
     * template search path.
     * 
     * Note this does not test whether the directory might be the baseDir where
     * the Process Dashboard was installed - it only tests to see if a directory
     * is in the user-configured list of additional search directories.
     */
    public static boolean templateSearchPathContainsDir(File dir) {
        if (dir == null)
            return false;

        String userSetting = Settings.getFile("templates.directory");
        if (userSetting == null || userSetting.length() == 0)
            return false;

        String[] searchPath = userSetting.split(";");
        for (int i = 0; i < searchPath.length; i++) {
            if (searchPath[i].trim().length() > 0) {
                File onePath = new File(searchPath[i].trim());
                if (onePath.equals(dir))
                    return true;
            }
        }

        return false;
    }



    private static ArrayList dashPackages = new ArrayList();

    /** Returns a list of all the packages currently installed.
     */
    public static List getPackages() {
        return Collections.unmodifiableList(dashPackages);
    }


    /** Return the version number of an installed package, or null if
     *  the package is not installed. */
    public static String getPackageVersion(String packageID) {
        if (packageID == null) return null;
        if ("java".equalsIgnoreCase(packageID))
            return getJREVersion();

        DashPackage pkg = getPackage(packageID);
        return (pkg != null ? pkg.version : null);
    }

    private static String getJREVersion() {
        String result = AccessController.doPrivileged(
            new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty("java.version");
                }});
        return result;
    }

    /** Return the DashPackage object for an installed package, or null if
     *  the package is not installed. */
    public static DashPackage getPackage(String packageID) {
        // look through the packages for an exact match.
        Iterator i = dashPackages.iterator();
        DashPackage pkg;
        while (i.hasNext()) {
            pkg = (DashPackage) i.next();
            if (packageID.equals(pkg.id))
                return pkg;
        }

        return null;
    }


    /** Returns a classloader capable of loading classes from the given
     * template URL.
     * 
     * @param baseUrl the URL of a JAR file in the template search path.
     * @return a classloader for loading classes from that JAR.  Repeated
     *    calls to this method for the same JAR will return an identical
     *    classloader object.
     */
    public static ClassLoader getTemplateClassLoader(URL baseUrl) {
        GET_CLASSLOADER_PERMISSION.checkPermission();

        if (baseUrl == null)
            return TemplateLoader.class.getClassLoader();

        // if we've been passed a url inside a template JAR file, normalize
        // it to the URL of the JAR itself.
        String urlStr = baseUrl.toString();
        if (urlStr.startsWith("jar:")) {
            int exclPos = urlStr.indexOf("!/");
            if (exclPos == -1) return null;
            urlStr = urlStr.substring(4, exclPos);
            try {
                baseUrl = new URL(urlStr);
            } catch (Exception e) { return null; }
        }

        ClassLoader result;
        synchronized (TEMPLATE_CLASSLOADERS) {
            result = (ClassLoader) TEMPLATE_CLASSLOADERS.get(baseUrl);
            if (result == null) {
                result = new URLClassLoader(new URL[] { baseUrl },
                    TemplateLoader.class.getClassLoader());
                TEMPLATE_CLASSLOADERS.put(baseUrl, result);
            }
        }

        return result;
    }

    public static void clearTemplateClassLoaderCache() {
        CLEAR_CLASSLOADER_PERMISSION.checkPermission();
        TEMPLATE_CLASSLOADERS.clear();
    }

    private static Map TEMPLATE_CLASSLOADERS = new Hashtable();



    /**
     * Return the servlet context path that should be used for an add-on whose
     * file and/or contents are specified by the given URL.
     * 
     * @param url
     *            the URL of an add-on file, or of a resource within an add-on
     * @return the context path that should be used for serving resources
     *         packaged within the given add-on.
     */
    public static String getAddOnContextPath(String url) {
        Matcher m = WAR_FILE_PAT.matcher(url);
        if (m.find()) {
            String result = m.group(1);
            if (!"/ROOT".equalsIgnoreCase(result))
                return result;
        }

        return "";
    }

    private static final Pattern WAR_FILE_PAT = Pattern.compile(
        "(/[^/]+).war(!/.*)?$", Pattern.CASE_INSENSITIVE);



    /** Looks through the various loaded templates, and determines which
     * absolute URL the given String maps to.  If the given URL does not
     * map to any real resource, returns null.
     *
     * Note: since this locates a resource which is known to exist, it must
     * make a connection to that URL.  However, the named resource is not
     * downloaded, and is not interpreted.  In particular, if the resulting URL
     * names a CGI script, that script will not be executed;  the URL
     * connection made is only looking at the file, not loading or running the
     * class named within.
     */
    public static URL resolveURL(String url) {
        URLConnection result = resolveURLConnection(url);
        return (result == null ? null : result.getURL());
    }

    /** Looks through the various loaded templates, determines which
     * absolute URL the given String maps to, and returns a connection to that
     * URL.  If the given URL does not map to any real resource, returns null.
     *
     * Note: although this opens a connection to the named URL, it does not
     * interpret the results.  In particular, if the resulting URL names a CGI
     * script, that script will not be executed; the URL connection
     * returned will serve the raw binary bytes that make up the script, not
     * the bytes returned by executing the script.  To execute the script, use
     * the {@link TinyWebServer} instead.
     */
    public static URLConnection resolveURLConnection(String url) {
        if (!isValidTemplateURL(url))
            return null;

        URL [] roots = getTemplateURLs();
        if (url.startsWith("/")) url = url.substring(1);
        URL u;
        URLConnection result;
        for (int i = 0;  i < roots.length;  i++) try {
            u = new URL(roots[i], url);
            result = u.openConnection();
            result.connect();
            return result;
        } catch (IOException ioe) { }

        return null;
    }


    /** Looks through the various loaded templates, and determines which
     * absolute URLs the given String maps to.  The URLs are returned in
     * search order: the first item in the array corresponds to the URL
     * which would be returned by a call to {@link #resolveURL(String)}.
     * If the given URL does not map to any real resource, returns a
     * zero-length array.
     *
     * Note: since this locates resources which are known to exist, it must
     * make a connection to that URL.  However, the named resources are not
     * downloaded, and are not interpreted.  In particular, if a resulting URL
     * names a CGI script, that script will not be executed;  the URL
     * connection made is only looking at the file, not loading or running the
     * class named within.
     */
    public static URL[] resolveURLs(String url) {
        if (!isValidTemplateURL(url))
            return new URL[0];

        Vector result = new Vector();

        URL [] roots = getTemplateURLs();
        if (url.startsWith("/")) url = url.substring(1);
        URL u;
        URLConnection conn;
        for (int i = 0;  i < roots.length;  i++) try {
            u = new URL(roots[i], url);
            conn = u.openConnection();
            conn.connect();
            result.add(u);
        } catch (IOException ioe) { }

        return (URL[]) result.toArray(new URL[0]);
    }

    /** Returns true if the given string is a valid URI that can be resolved
     * relative to the template search path.
     */
    public static boolean isValidTemplateURL(String uri) {
        if (uri == null || uri.length() == 0)
            return false;
        if (uri.endsWith("/")) return false;
        if (uri.indexOf("..") != -1) return false;
        if (uri.indexOf("//") != -1) return false;

        for (int i = uri.length();  i-- > 0; ) {
            char c = uri.charAt(i);
            if (c >= VALID_TEMPLATE_CHARS.length)
                return false;
            if (VALID_TEMPLATE_CHARS[c] == false)
                return false;
        }

        return true;
    }
    private static final boolean VALID_TEMPLATE_CHARS[] = new boolean[128];
    static {
        Arrays.fill(VALID_TEMPLATE_CHARS, false);
        Arrays.fill(VALID_TEMPLATE_CHARS, '0', '9'+1, true);
        Arrays.fill(VALID_TEMPLATE_CHARS, 'A', 'Z'+1, true);
        Arrays.fill(VALID_TEMPLATE_CHARS, 'a', 'z'+1, true);
        String validChars = Settings.getVal("templates.safePunctuation",
                "-.,_/()");
        for (int i = validChars.length();  i-- > 0; )
            VALID_TEMPLATE_CHARS[validChars.charAt(i)] = true;
    }

    private static void processTimestamp(long time) {
        if (time > templateTimestamp)
            templateTimestamp = time;
    }
    private static void processTimestamp(File f) {
        long modTime = f.lastModified();
        debug("timestamp for file " + f.getPath() + " is " + modTime);
        processTimestamp(modTime);
    }
    public static long getTemplateTimestamp() { return templateTimestamp; }

    private static void createScriptMaps(DashHierarchy templates) {
        Enumeration nodes = templates.keys();
        PropertyKey key;
        while (nodes.hasMoreElements()) {
            key = (PropertyKey) nodes.nextElement();
            if (key.getParent().equals(PropertyKey.ROOT))
                addScriptMaps(templates, key, null, null);
        }
    }
    private static void addScriptMaps(DashHierarchy templates,
                                      PropertyKey key, String ID,
                                      Vector v) {
        Prop val = (Prop) templates.get(key);
        if (val == null) return;
        if (Prop.hasValue(val.getID())) {
            ID = val.getID();
            v = (Vector) scriptMaps.get(ID);
            if (v == null)
                scriptMaps.put(ID, (v = new Vector()));
        }

        val.setScriptFile(processScriptFlag(v, val.getScriptFile()));

        for (int i = 0;   i < val.getNumChildren();   i++)
            addScriptMaps(templates, val.getChild(i), ID, v);
    }
    private static String processScriptFlag(Vector v, String scriptFlag) {
        if (v == null || !Prop.hasValue(scriptFlag))
            return scriptFlag;
        StringTokenizer tok = new StringTokenizer(scriptFlag, ";");
        String scriptFile = "";
        while (tok.hasMoreTokens()) {
            scriptFile = tok.nextToken();
            maybeAddScriptID(v, scriptFile);
        }
        return scriptFile;
    }
    private static void maybeAddScriptID(Vector v, String scriptFile) {
        if (v == null || !Prop.hasValue(scriptFile) ||
            "none".equals(scriptFile))
            return;
        int hashPos = scriptFile.indexOf('#');
        if (hashPos != -1) scriptFile = scriptFile.substring(0, hashPos);
        for (int i = v.size();  i-- > 0;  )
            if (scriptFile.equals(((ScriptID) v.elementAt(i)).getScript()))
                return;
        v.addElement(new ScriptID(scriptFile, null, null));
    }

    private static void createScriptMaps(Element e) {
        NodeList templates = e.getElementsByTagName(TEMPLATE_NODE_NAME);
        int len = templates.getLength();
        for (int i = 0;  i < len;   i++) try {
            Element template = (Element) templates.item(i);
            Map idMap = addScriptMaps
                (template.getAttribute(ID_ATTR),
                 template.getElementsByTagName(HTML_NODE_NAME));
            resolveScriptIDs(template, idMap);
        } catch (ClassCastException cce) {}
    }
    private static Map addScriptMaps(String id, NodeList htmlPages) {
        if (id == null || id.length() == 0 ||
            htmlPages == null || htmlPages.getLength() == 0)
            return null;

        Map result = new HashMap();
        Vector v = (Vector) scriptMaps.get(id);
        if (v == null)
            scriptMaps.put(id, (v = new Vector()));

        Element htmlPage;
        String htmlID, htmlName, htmlHref, htmlPackage;
        int htmlPagesLen = htmlPages.getLength();

        for (int i=0;  i<htmlPagesLen;  i++) try {
            htmlPage = (Element) htmlPages.item(i);
            htmlHref = htmlPage.getAttribute(HTML_HREF_ATTR);
            if (!hasValue(htmlHref)) {
                // Print out an error message?
                continue;
            }
            htmlPackage = htmlPage.getAttribute(HTML_PACKAGE_ATTR);
            if (meetsPackageRequirement(htmlPackage) == false)
                continue;

            if (hasValue(htmlID = htmlPage.getAttribute(ID_ATTR)))
                result.put(htmlID, htmlHref);
            if (hasValue(htmlName = htmlPage.getAttribute(HTML_NAME_ATTR)))
                ScriptNameResolver.precacheName(htmlHref, htmlName);
            maybeAddScriptID(v, htmlHref);
        } catch (ClassCastException cce) {}

        return result;
    }

    /** Check to see if the currently installed packages satisfy a particular
     * set of requirements.
     * 
     * The input value can be one of the following:
     * <ul>
     * <li>An unadorned string, denoting the ID of a package that must be
     *     installed</li>
     * <li>A string of the form
     *     <pre>{packageID} version {packageVersion}</pre>
     *     where <code>{packageID}</code> is the ID of a package that must be
     *     present, and <code>{packageVersion}</code> is the minimum required
     *     version of that package.</li>
     * <li>A semicolon-separated list of individual package requirements (each
     *     of which is a string meeting either of the forms above)</li>
     * </ul>
     * 
     * @param packageList a package requirement description
     * @return true if the installed packages meet the given requirement.
     */
    public static boolean meetsPackageRequirement(String packageList) {
        if (!hasValue(packageList))
            return true;

        String[] requirements = packageList.split(";");
        for (int i = 0; i < requirements.length; i++) {
            String packageID = requirements[i].trim();
            if (!hasValue(packageID))
                continue;
            String requiredVersion = null;
            Matcher m = HTML_PACKAGE_VERSION_PATTERN.matcher(packageID);
            if (m.matches()) {
                packageID = m.group(1);
                requiredVersion = m.group(2);
            }

            // if we don't meet this package requirement, then the overall
            // requirement is not met.
            if (!meetsPackageRequirement(packageID, requiredVersion))
                return false;
        }

        // All requirements were met.
        return true;
    }

    public static boolean meetsPackageRequirement(String packageID,
            String requiredVersion) {

        // check pseudo-packages relating to the dataset mode. (since 1.15.8)
        if ("teamMode".equals(packageID))
            return Settings.isTeamMode();
        else if ("personalMode".equals(packageID))
            return Settings.isPersonalMode();
        else if ("hybridMode".equals(packageID))
            return Settings.isHybridMode();
        else if ("liveMode".equals(packageID))
            return !CompressedInstanceLauncher.isRunningFromCompressedData();

        String installedVersion = getPackageVersion(packageID);
        if (installedVersion == null)
            // this package is not installed, so the requirement isn't met
            return false;

        else if (requiredVersion == null)
            // any version of this package will do.
            return true;

        else
            // check to see if the version meets the requirement.
            return (DashPackage.compareVersions(installedVersion,
                requiredVersion) >= 0);
    }

    private static void resolveScriptIDs(Element node, Map idMap) {
        if (idMap == null) return;
        String htmlID = node.getAttribute(HTML_ID_ATTR), htmlHref;
        if (!hasValue(htmlID))
            htmlID = node.getAttribute(NAME_ATTR);
        String anchor = "";
        int hashPos = htmlID.indexOf('#');
        if (hashPos != -1) {
            anchor = htmlID.substring(hashPos);
            htmlID = htmlID.substring(0, hashPos);
        }
        htmlHref = (String) idMap.get(htmlID);
        if (hasValue(htmlHref))
            node.setAttribute(HTML_HREF_ATTR, htmlHref + anchor);

        if (node.hasChildNodes()) {
            NodeList children = node.getChildNodes();
            int numChildren = children.getLength();
            for (int i=0;   i < numChildren;   i++) try {
                resolveScriptIDs((Element) children.item(i), idMap);
            } catch (ClassCastException cce) {}
        }
    }

    private static void generateDefaultScriptMaps(Element e) {
        NodeList templates = e.getElementsByTagName(TEMPLATE_NODE_NAME);
        int len = templates.getLength();
        for (int i = 0;   i < len;   i++) try {
            Element template = (Element) templates.item(i);
            String ID = template.getAttribute(ID_ATTR);
            if (!hasValue(ID)) continue;
            if (hasValue(template.getAttribute(HTML_HREF_ATTR))) continue;

            // if there is no script ID for this element, and it
            // hasn't specifically requested otherwise, give it a
            // default href.
            Vector v = (Vector) scriptMaps.get(ID);
            if (v == null)
                scriptMaps.put(ID, (v = new Vector()));

            if (v.size() == 0) {
                String planSummaryName = Resources.getDashBundle
                    ("Templates").format("Plan_Summary_Name_FMT", ID);
                v.addElement(new ScriptID("dash/summary.shtm", null,
                                          planSummaryName));
                debug("adding default HTML form for "+ID);
            }

        } catch (ClassCastException cce) {}
    }

    private static boolean hasValue(String v) {
        return (v != null && v.length() > 0);
    }



    private static final String HTML_NODE_NAME = DashHierarchy.HTML_NODE_NAME;
    private static final String TEMPLATE_NODE_NAME =
        DashHierarchy.TEMPLATE_NODE_NAME;

    private static final String HTML_ID_ATTR   = "htmlID";
    private static final String HTML_NAME_ATTR = "title";
    private static final String HTML_HREF_ATTR = DashHierarchy.HTML_HREF_ATTR;
    private static final String HTML_PACKAGE_ATTR = "inPackage";
    private static final Pattern HTML_PACKAGE_VERSION_PATTERN = Pattern.compile(
            "(.*)[\\p{Punct}\\p{Space}]+version[\\p{Punct}\\p{Space}]+(.*)",
            Pattern.CASE_INSENSITIVE);
    private static final String ID_ATTR   = DashHierarchy.ID_ATTR;
    static final String NAME_ATTR = DashHierarchy.NAME_ATTR;


    private static Hashtable scriptMaps = new Hashtable();


    public static Vector<ScriptID> getScriptIDs(String templateID, String path) {
        Vector scriptMap = (Vector) scriptMaps.get(templateID);
        if (scriptMap == null) return null;

        Vector<ScriptID> result = new Vector<ScriptID>();
        for (int i = 0;   i < scriptMap.size();   i++)
            result.addElement(new ScriptID((ScriptID) scriptMap.elementAt(i),
                                           path));
        return result;
    }


    /** Singleton object for reporting errors in template definition
     *  files.
     */
    private static ErrorReporter errorReporter = null;
    public synchronized static void logTemplateError(String error) {
        if (errorReporter == null) {
            Resources r = Resources.getDashBundle("Templates");
            errorReporter = new ErrorReporter
                (r.getString("Error_Title"),
                 r.getStrings("Error_Header"),
                 r.getStrings("Error_Footer"));
        }
        errorReporter.logError(error);
    }
    public synchronized static void showTemplateErrors() {
        if (errorReporter != null)
            errorReporter.done();
    }
}
