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

import pspdash.data.DataRepository;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.*;
import java.util.*;
import java.net.JarURLConnection;
import java.util.jar.JarInputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

public class TemplateLoader {

    private static final String TEMPLATE_SUFFIX = ".template";
    private static final String DATAFILE_SUFFIX = ".globaldata";
    private static final String TEMPLATE_DIR = "Templates/";

    public static PSPProperties loadTemplates(DataRepository data,
                                              AutoUpdateManager aum) {
        PSPProperties templates = new PSPProperties(null);

        URL[] roots = getTemplateURLs();

        String templateDirURL;
        for (int i=0;  i < roots.length;  i++) {
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

                if (filename.endsWith(TEMPLATE_SUFFIX)) {
                    debug("loading template: " + filename);
                    templates.load(jarFile, false);
                    foundTemplates = true;
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
            if (filename.endsWith(TEMPLATE_SUFFIX)) {
                try {
                    debug("loading template: " + f);
                    templates.load(new FileInputStream(f));
                    foundTemplates = true;
                } catch (IOException ioe) {
                    debug("unable to load process template: " + f);
                }
            } else if (filename.endsWith(DATAFILE_SUFFIX)) {
                try {
                    debug("loading data: " + f);
                    data.addGlobalDefinitions(new FileInputStream(f), true);
                } catch (Exception e) {
                    System.out.println
                        ("unable to load global process data from " + f +
                         ": " + e);
                }
            }
        }
        return foundTemplates;
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

        addTemplateURLs(Settings.getDir("templates.directory", false), result);

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
        String name;
        for (int i=0;  i < dirContents.length;  i++) try {
            name = dirContents[i].toURL().toString();
            if (name.toLowerCase().endsWith(".jar") &&
                ! name.toLowerCase().endsWith(JARFILE_NAME))
                v.add(new URL("jar:" + name + "!/" + TEMPLATE_DIR));
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
}
