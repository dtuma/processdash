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

import pspdash.data.DataRepository;
import pspdash.data.compiler.Compiler;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.io.*;
import java.util.*;
import java.net.JarURLConnection;
import java.util.jar.JarInputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class TemplateLoader {

    private static final String TEMPLATE_SUFFIX = ".template";
    private static final String XML_TEMPLATE_SUFFIX = "-template.xml";
    private static final String DATAFILE_SUFFIX = ".globaldata";
    private static final String FILTER_SUFFIX = "filter.class";
    private static final String TEMPLATE_DIR = "Templates/";

    private static long templateTimestamp = 0;

    public static PSPProperties loadTemplates(DataRepository data,
                                              AutoUpdateManager aum) {
        PSPProperties templates = new PSPProperties(null);

        URL[] roots = getTemplateURLs();

        String templateDirURL;
        for (int i=roots.length;   i-- > 0;  ) {
            templateDirURL = roots[i].toString();

            if (templateDirURL.startsWith("file:/")) {
                /* If the /Templates directory exists as a local file
                 * somewhere, search through that directory for process
                 * templates.
                 */

                // strip "file:" from the beginning of the url.
                String dirname = templateDirURL.substring(5);
                searchDirForTemplates(templates, dirname, data);

            } else {
                /* If the /Templates directory found is in a jar somewhere,
                 * search through the jar for process templates.
                 */

                // Strip "jar:" from the beginning and the "!/Templates/"
                // from the end of the URL.
                String jarFileURL = templateDirURL.substring
                    (4, templateDirURL.indexOf('!'));
                searchJarForTemplates(templates, jarFileURL, data, aum);
            }
        }

        createProcessRoot(templates);

        return templates;
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
    protected static void createProcessRoot(PSPProperties templates) {
        Enumeration nodes = templates.keys();
        Vector processes = new Vector();
        PropertyKey key;
        while (nodes.hasMoreElements()) {
            key = (PropertyKey) nodes.nextElement();
            if (key.getParent().equals(PropertyKey.ROOT))
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

    protected static boolean searchJarForTemplates(PSPProperties templates,
                                                   String jarURL,
                                                   DataRepository data,
                                                   AutoUpdateManager aum) {
        boolean foundTemplates = false;
        try {
            debug("searching for templates in " + jarURL);

            JarInputStream jarFile =
                new JarInputStream((new URL(jarURL)).openStream());
            aum.addPackage(jarURL, jarFile.getManifest());

            ZipEntry file;
            String filename;
            while ((file = jarFile.getNextEntry()) != null) {
                filename = file.getName().toLowerCase();
                if (!filename.startsWith(TEMPLATE_DIR.toLowerCase()) ||
                    filename.lastIndexOf('/') != 9)
                    continue;

                if (filename.endsWith(XML_TEMPLATE_SUFFIX)) {
                    debug("loading template: " + filename);
                    loadXMLProcessTemplate(templates, data, jarFile, false);
                    foundTemplates = true;
                } else if (filename.endsWith(TEMPLATE_SUFFIX)) {
                    debug("loading template: " + filename);
                    loadProcessTemplate(templates, jarFile, false);
                    foundTemplates = true;
                } else if (filename.endsWith(FILTER_SUFFIX)) {
                    debug("loading language filter: " + filename);
                    loadLanguageFilter(file.getName());
                } else if (filename.endsWith(DATAFILE_SUFFIX)) {
                    try {
                        debug("loading data: " + filename);
                        data.addGlobalDefinitions(jarFile, false);
                    } catch (Exception e) {
                        System.out.println
                            ("unable to load global process data from " +
                             file.getName() + " in " + jarURL + ": " + e);
                    }
                }
            }

        } catch (IOException ioe) {
            System.out.println("error looking for templates in " + jarURL);
            ioe.printStackTrace(System.out);
        }
        return foundTemplates;
    }

    protected static boolean searchDirForTemplates(PSPProperties templates,
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
                        (templates, data, new FileInputStream(f), true);
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
            } else if (filename.endsWith(FILTER_SUFFIX)) {
                debug("loading language filter: " + filename);
                loadLanguageFilter(f.getName());
            } else if (filename.endsWith(DATAFILE_SUFFIX)) {
                try {
                    debug("loading data: " + f);
                    data.addGlobalDefinitions(new FileInputStream(f), true);
                    processTimestamp(f);
                } catch (Exception e) {
                    System.out.println
                        ("unable to load global process data from " + f +
                         ": " + e);
                }
            }
        }
        return foundTemplates;
    }

    private static List languageFilters = new ArrayList();
    public static List getLanguageFilters() { return languageFilters; }
    private static void loadLanguageFilter(String filename) {
        int pos = filename.lastIndexOf('/');
        if (pos != -1) filename = filename.substring(pos + 1);
        if (!languageFilters.contains(filename))
            languageFilters.add(0, filename);
    }


    private static void loadProcessTemplate(PSPProperties templates,
                                            InputStream in, boolean close)
        throws IOException
    {
        PSPProperties template = new PSPProperties(null);
        template.load(in, close);
        createScriptMaps(template);

        templates.putAll(template);
    }

    private static void loadXMLProcessTemplate(PSPProperties templates,
                                               DataRepository data,
                                               InputStream in, boolean close)
        throws IOException
    {
        Element root = null;
        try {
            if (!close)
                in = new ByteArrayInputStream
                    (TinyWebServer.slurpContents(in, false));

            // this closes the file without our permission.
            Document doc = XMLUtils.parse(in);
            root = doc.getDocumentElement();
        } catch (SAXException se) {
            // FIXME: print an error message
            return;
        }

        AutoData.registerTemplates(root, data);

        createScriptMaps(root);

        try {
            PSPProperties template = new PSPProperties(null);
            template.loadXMLTemplate(root);
            template.premove(PropertyKey.ROOT);
            createScriptMaps(template);
            templates.putAll(template);
        } catch (SAXException se) {
            // Can this happen?
        }
    }

    protected static void debug(String msg) {
        // System.out.println("TemplateLoader: " + msg);
    }

    /** Returns a list of URLs to templates, in logical search order.
     *
     * The URL search order is:<OL>
     * <LI>Look first in the directory specified by the user setting
     *     "templates.directory" (should end in "Templates").
     * <LI>Next, look in any JAR files contained in that directory.
     * <LI>Next, look in any JAR files contained in the parent of that
     *     directory.
     * <LI>If there is a Templates directory alongside the pspdash.jar file,
     *     look there, first for the file, then for JARs containing the file.
     * <LI>If there are any JAR files next to the pspdash.jar file, look in
     *     them.
     * <LI>Finally, look in the classpath (which includes the pspdash.jar
     *     file).</OL>
     */
    public static URL[] getTemplateURLs() {
        if (template_url_list != null) return template_url_list;

        Vector result = new Vector();

        String userSetting = Settings.getFile("templates.directory");
        if (userSetting != null && userSetting.length() != 0) {
            StringTokenizer tok = new StringTokenizer(userSetting, ";");
            while (tok.hasMoreTokens())
                addTemplateURLs(tok.nextToken(), result);
        }

        addTemplateURLs(getBaseDir(), result);

        try {
            Enumeration e = TemplateLoader.class.getClassLoader()
                .getResources(TEMPLATE_DIR);
            while (e.hasMoreElements())
                result.addElement(e.nextElement());
        } catch (IOException ioe) {
            System.err.println("An exception was encountered when searching " +
                               "for process templates in the classpath:\n\t" +
                               ioe);
        }

        int i = result.size();
        URL[] roots = new URL[i];
        debug("The template roots (in reverse order) are:");
        while (i-- > 0) {
            roots[i] = (URL) result.elementAt(i);
            debug(roots[i].toString());
        }

        template_url_list = roots;
        return roots;
    }
    private static URL[] template_url_list = null;
    private static final String JARFILE_NAME = "pspdash.jar";
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
            parentDir = templateDir.getParentFile(); // may return null

        } else {
            parentDir = new File(directory);
            if (!parentDir.isDirectory()) return;
            templateDir = new File(parentDir, TEMPLATE_DIRNAME);
            if (!templateDir.isDirectory()) templateDir = null;
        }

        if (templateDir != null) try {
            v.add(templateDir.toURL());
        } catch (MalformedURLException mue) {}

        scanDirForJarFiles(templateDir, v);
        scanDirForJarFiles(parentDir, v);
    }
    private static void scanDirForJarFiles(File dir, Vector v) {
        if (dir == null) return;

        File[] dirContents = dir.listFiles();
        String name, lname;
        for (int i=0;  i < dirContents.length;  i++) try {
            name = dirContents[i].toURL().toString();
            lname = name.toLowerCase();
            if ((lname.endsWith(".jar") || lname.endsWith(".zip"))) {
                processTimestamp(dirContents[i]);
                if (! lname.endsWith(JARFILE_NAME))
                    v.add(new URL("jar:" + name + "!/" + TEMPLATE_DIR));
            }
        } catch (MalformedURLException mue) {}
    }
    /** Figure out what directory contains the pspdash.jar file. */
    private static String getBaseDir() {
        URL u =
            TemplateLoader.class.getResource("/pspdash/TemplateLoader.class");
        if (u == null) return null;

        String myURL = u.toString();
        // we are expecting a URL like (Windows)
        // jar:file:/D:/path/to/pspdash.jar!/pspdash/TemplateLoader.class
        // or (Unix)
        // jar:file:/usr/path/to/pspdash.jar!/pspdash/TemplateLoader.class
        if (myURL.startsWith("jar:file:")) try {
            String jarFileName = myURL.substring(9,myURL.indexOf("!/pspdash"));
            File jarFile = new File(jarFileName);
            if (jarFile.exists())
                return jarFile.getParent();
            else
                return null;
        } catch (IndexOutOfBoundsException ioobe) {}
        return null;
    }

    public static URL resolveURL(String url) {
        URLConnection result = resolveURLConnection(url);
        return (result == null ? null : result.getURL());
    }

    public static URLConnection resolveURLConnection(String url) {
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

    public static void processTimestamp(long time) {
        if (time > templateTimestamp)
            templateTimestamp = time;
    }
    public static void processTimestamp(File f) {
        //System.out.println("timestamp for file " + f.getPath() + " is " +
        //                   f.lastModified());
        processTimestamp(f.lastModified());
    }
    public static long getTemplateTimestamp() { return templateTimestamp; }

    private static void createScriptMaps(PSPProperties templates) {
        Enumeration nodes = templates.keys();
        PropertyKey key;
        while (nodes.hasMoreElements()) {
            key = (PropertyKey) nodes.nextElement();
            if (key.getParent().equals(PropertyKey.ROOT))
                addScriptMaps(templates, key, null, null);
        }
    }
    private static void addScriptMaps(PSPProperties templates,
                                      PropertyKey key, String ID,
                                      Vector v) {
        Prop val = (Prop) templates.get(key);
        if (val == null) return;
        if (val.hasValue(val.getID())) {
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
        if (v == null || !Prop.hasValue(scriptFile))
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
        for (int i = templates.getLength();  i-- > 0; ) try {
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
        String htmlID, htmlName, htmlHref;

        for (int i=0;  i<htmlPages.getLength();  i++) try {
            htmlPage = (Element) htmlPages.item(i);
            htmlHref = htmlPage.getAttribute(HTML_HREF_ATTR);
            if (!hasValue(htmlHref)) {
                // Print out an error message?
                continue;
            }

            if (hasValue(htmlID = htmlPage.getAttribute(ID_ATTR)))
                result.put(htmlID, htmlHref);
            if (hasValue(htmlName = htmlPage.getAttribute(HTML_NAME_ATTR)))
                ScriptNameResolver.precacheName(htmlHref, htmlName);
            maybeAddScriptID(v, htmlHref);
        } catch (ClassCastException cce) {}

        return result;
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
            for (int i=children.getLength();  i-- > 0; ) try {
                resolveScriptIDs((Element) children.item(i), idMap);
            } catch (ClassCastException cce) {}
        }
    }

    private static boolean hasValue(String v) {
        return (v != null && v.length() > 0);
    }



    private static final String HTML_NODE_NAME = PSPProperties.HTML_NODE_NAME;
    private static final String TEMPLATE_NODE_NAME =
        PSPProperties.TEMPLATE_NODE_NAME;

    private static final String HTML_ID_ATTR   = "htmlID";
    private static final String HTML_NAME_ATTR = "title";
    private static final String HTML_HREF_ATTR = PSPProperties.HTML_HREF_ATTR;
    private static final String ID_ATTR   = PSPProperties.ID_ATTR;
    static final String NAME_ATTR = PSPProperties.NAME_ATTR;


    private static Hashtable scriptMaps = new Hashtable();

    public static Vector getScriptIDs(String templateID, String path) {
        Vector scriptMap = (Vector) scriptMaps.get(templateID);
        if (scriptMap == null) return null;

        Vector result = new Vector();
        for (int i = 0;   i < scriptMap.size();   i++)
            result.addElement(new ScriptID((ScriptID) scriptMap.elementAt(i),
                                           path));
        return result;
    }
}
