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
import java.net.URL;
import java.io.*;
import java.util.*;
import java.net.JarURLConnection;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

public class TemplateLoader {

    private static final String TEMPLATE_SUFFIX = ".template";
    private static final String TEMPLATE_DIR = "Templates/";

    public static PSPProperties loadTemplates(DataRepository data) {
        PSPProperties templates = new PSPProperties(null);

        String template_directory =
            Settings.getDir("templates.directory", true);

        if (template_directory != null) {
            /* If the user has specified a templates directory, search
             * through it for process templates.
             */
            if (searchDirForTemplates(templates, template_directory))
                data.addDatafileSearchDir(template_directory);

        } else {
            // search for process template directories in the classpath.
            Enumeration templateDirs;
            try {
                templateDirs = TemplateLoader.class.getClassLoader()
                    .getResources(TEMPLATE_DIR);
            } catch (IOException ioe) {
                debug("error when searching for Templates directories in " +
                      "the classpath: " + ioe);
                return templates;
            }

            String templateDirURL;
            while (templateDirs.hasMoreElements()) {
                templateDirURL = templateDirs.nextElement().toString();

                if (templateDirURL.startsWith("file:/")) {
                    /* If the /Templates directory exists as a local file
                     * somewhere, search through that directory for process
                     * templates.
                     */

                    // strip "file:/" from the beginning of the url.
                    String dirname = templateDirURL.substring(6);
                    if (searchDirForTemplates(templates, dirname))
                        data.addDatafileSearchDir(dirname);

                } else {
                    /* If the /Templates directory found is in a jar somewhere,
                     * search through the jar for process templates.
                     */

                    // Strip "jar:" from the beginning and the "!/Templates/"
                    // from the end of the URL.
                    String jarFileURL = templateDirURL.substring
                        (4, templateDirURL.indexOf('!'));
                    searchJarForTemplates(templates, jarFileURL);
                }
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
                                                   String jarURL) {
        boolean foundTemplates = false;
        try {
            // debug("searching for templates in " + jarURL);
            ZipInputStream jarFile =
                new ZipInputStream((new URL(jarURL)).openStream());

            ZipEntry file;
            String filename;
            while ((file = jarFile.getNextEntry()) != null) {
                filename = file.getName().toLowerCase();
                if (filename.startsWith(TEMPLATE_DIR.toLowerCase()) &&
                    filename.endsWith(TEMPLATE_SUFFIX) &&
                    filename.lastIndexOf('/') == 9) {
                    // debug("loading template: " + filename);
                    templates.load(jarFile, false);
                    foundTemplates = true;
                }
            }

        } catch (IOException ioe) {
            debug("error looking for templates in " + jarURL);
            ioe.printStackTrace(System.out);
        }
        return foundTemplates;
    }

    protected static boolean searchDirForTemplates(PSPProperties templates,
                                                   String directoryName) {
        // debug("searching for templates in " + directoryName);
        File[] process_templates = new File(directoryName).listFiles();
        if (process_templates == null) return false;

        int i = process_templates.length;
        boolean foundTemplates = false;
        File f;
        while (i-- > 0) {
            f = process_templates[i];
            if (f.getName().toLowerCase().endsWith(TEMPLATE_SUFFIX)) try {
                // debug("loading template: " + f);
                templates.load(new FileInputStream(f));
                foundTemplates = true;
            } catch (IOException ioe) {
                debug("unable to load process template: " + f);
            }
        }
        return foundTemplates;
    }

    protected static void debug(String msg) {
        System.out.println("TemplateLoader: " + msg);
    }
}
