// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.i18n;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class UpdateIzPackResourceFile extends Task {

    private static final String LINE_SEP = System.getProperty("line.separator");
    private static final String BUNDLE_NAME = "Installer";
    private static final String OPENING_COMMENT =
        "<!-- Process Dashboard strings -->";
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
        if (!langpack.isFile() || !langpack.canRead())
            throw new BuildException("cannot read file '" + langpack + "'");
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
        long result = -1;
        File[] bundles = resourcesDir.listFiles();
        for (int i = 0; i < bundles.length; i++) {
            File file = bundles[i];
            if (file.getName().startsWith(BUNDLE_NAME))
                result = Math.max(result, file.lastModified());
        }
        return result;
    }

    private ResourceBundle readResourceBundle() throws Exception {
        URL[] classpath = new URL[] { resourcesDir.toURL() };
        ClassLoader cl = new URLClassLoader(classpath);

        ResourceBundle result = ResourceBundle.getBundle(BUNDLE_NAME,
                new Locale(javaLang), cl);
        return result;
    }

    private void updateLangpack(ResourceBundle bundle) throws Exception {
        log("Updating langpack '" + langpack + "'");
        ByteArrayOutputStream newContents = new ByteArrayOutputStream();
        InputStream in =
            new BufferedInputStream(new FileInputStream(langpack));

        byte[] oneLineBytes = readLine(in);
        newContents.write(oneLineBytes);
        String charset = getCharset(oneLineBytes);

        while ((oneLineBytes = readLine(in)) != null) {
            if (needAName(oneLineBytes))
                break;
            else
                newContents.write(oneLineBytes);
        }
        in.close();

        Writer out = new OutputStreamWriter(newContents, charset);
        out.write("    ");
        out.write(OPENING_COMMENT);
        out.write("\r\n\r\n");

        TreeSet bundleKeys = new TreeSet(Collections.list(bundle.getKeys()));
        for (Iterator i = bundleKeys.iterator(); i.hasNext();) {
            String key = (String) i.next();
            String value = bundle.getString(key);
            out.write("    <str id=\"");
            out.write(key);
            out.write("\">");
            out.write(escape(value));
            out.write("</str>\r\n");
        }

        out.write("\r\n");
        out.write(CLOSING_TAG);
        out.write("\r\n");
        out.close();

        FileOutputStream fout = new FileOutputStream(langpack);
        fout.write(newContents.toByteArray());
        fout.close();
    }

    private byte[] readLine(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            out.write(c);
            if (c == '\n')
                break;
        }

        if (out.size() == 0)
            return null;
        else
            return out.toByteArray();
    }

    private String getCharset(byte[] prologLineBytes) throws IOException {
        String prolog = new String(prologLineBytes, "ISO-8859-1");
        Pattern p = Pattern.compile("encoding\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher m = p.matcher(prolog);
        if (m.find())
            return m.group(1);
        else
            return "UTF-8";
    }

    private boolean needAName(byte[] lineBytes) throws IOException {
        String line = new String(lineBytes, "ISO-8859-1");
        return (line.indexOf(OPENING_COMMENT) != -1 ||
                 line.indexOf(CLOSING_TAG) != -1);
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
