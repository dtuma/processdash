// Copyright (C) 2005-2011 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.i18n;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class UpdateIzPackResourceFile extends Task {

    private static final String XML_PROLOG = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>";
    private static final String LINE_SEP = System.getProperty("line.separator");
    private static final String BUNDLE_NAME = "Installer";
    private static final String CLOSING_TAG = "</langpack>";

    private File langpack;

    private File resourcesDir;

    private String javaLang;

    public void setLangpack(File langpack) {
        this.langpack = langpack;
    }

    public void setResourcesDir(File resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    public void setJavaLang(String javaLang) {
        this.javaLang = javaLang;
    }

    public void execute() {
        validate();

        if (noUpdatesNeeded())
            return;

        ResourceBundle bundle;
        try {
            bundle = readResourceBundle();
        } catch (Exception e) {
            throw new BuildException("Can't load resource bundle.", e);
        }

        try {
            updateLangpack(bundle);
        } catch (Exception e) {
            throw new BuildException("Couldn't update langpack.", e);
        }
    }

    private void validate() {
        if (javaLang == null)
            throw new BuildException("must specify javaLang attribute.");
        if (langpack == null)
            throw new BuildException("must specify langpack attribute.");
        if (resourcesDir == null)
            throw new BuildException("must specify resourcesDir attribute.");
        if (!resourcesDir.isDirectory())
            throw new BuildException("cannot find directory '" + resourcesDir
                    + "'");
    }

    private boolean noUpdatesNeeded() {
        long resourceBundleDate = getResourceBundleDate();
        long langpackDate = langpack.lastModified();
        return (langpackDate > resourceBundleDate);
    }

    private long getResourceBundleDate() {
        String engBundleName = BUNDLE_NAME + ".properties";
        String locBundlePrefix = BUNDLE_NAME + "_" + javaLang;

        long result = -1;
        File[] bundles = resourcesDir.listFiles();
        for (int i = 0; i < bundles.length; i++) {
            File file = bundles[i];
            if (file.getName().equals(engBundleName) ||
                file.getName().startsWith(locBundlePrefix))
                result = Math.max(result, file.lastModified());
        }
        return result;
    }

    private ResourceBundle readResourceBundle() throws Exception {
        URL[] classpath = new URL[] { resourcesDir.toURI().toURL() };
        ClassLoader cl = new URLClassLoader(classpath);

        ResourceBundle result = ResourceBundle.getBundle(BUNDLE_NAME,
                new Locale(javaLang), cl);
        return result;
    }

    private void updateLangpack(ResourceBundle bundle) throws Exception {
        log("Updating langpack '" + langpack + "'");
        FileOutputStream fos = new FileOutputStream(langpack);
        BufferedWriter out =
            new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));

        out.write(XML_PROLOG + LINE_SEP + LINE_SEP);
        out.write("<langpack>" + LINE_SEP);
        writeTranslations(bundle, out);
        out.write(LINE_SEP + CLOSING_TAG + LINE_SEP);
        out.close();
    }

    private void writeTranslations(ResourceBundle bundle, Writer out) throws IOException {
        TreeSet bundleKeys = new TreeSet(Collections.list(bundle.getKeys()));
        for (Iterator i = bundleKeys.iterator(); i.hasNext();) {
            String resKey = (String) i.next();
            String xmlKey = resKey;
            if (xmlKey.startsWith("text.")) {
                // discard the "text." prefix
                xmlKey = xmlKey.substring(5);
                if (xmlKey.startsWith("pack.")) {
                    // discard the "pack." prefix
                    xmlKey = xmlKey.substring(5);
                    if (xmlKey.endsWith(".name"))
                        // discard the ".name" suffix
                        xmlKey = xmlKey.substring(0, xmlKey.length()-5);
                }
            }
            String value = bundle.getString(resKey);
            out.write("    <str id=\"");
            out.write(xmlKey);
            out.write("\">");
            out.write(escape(value));
            out.write("</str>" + LINE_SEP);
        }
    }

    public static String escape(String value) {
        StringTokenizer tok = new StringTokenizer(value, "<>&\"'\r\n", true);
        StringBuffer result = new StringBuffer();
        String token;
        while (tok.hasMoreTokens()) {
            token = tok.nextToken();
            if      ("<".equals(token))  result.append("&lt;");
            else if (">".equals(token))  result.append("&gt;");
            else if ("&".equals(token))  result.append("&amp;");
            else if ("'".equals(token)) result.append("&apos;");
            else if ("\"".equals(token)) result.append("&quot;");
            else if ("\r".equals(token)) ;
            else if ("\n".equals(token)) result.append(LINE_SEP);
            else                         result.append(token);
        }
        return result.toString();
    }

}
