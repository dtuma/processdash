// Copyright (C) 2003-2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.i18n;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.processdash.templates.TemplateLoader;



/** A special classloader for use with java.util.ResourceBundle.
 * This loads ".properties" files from the TemplateLoader search
 * path, allowing dashboard add-on files to contribute localization
 * information.  In addition, if this classloader finds more than
 * one matching ".properties" file in the TemplateLoader search
 * path, it will merge their contents.
 */
public class MergingTemplateClassLoader extends SafeTemplateClassLoader {

    private Map cache = Collections.synchronizedMap(new HashMap());
    private boolean reverseOrder;
    private File tempDir;

    public MergingTemplateClassLoader() {
        try {
            // we're going to merge properties files by appending them.
            // keys will commonly appear twice as a result.  So we perform
            // a test to see how the current JVM's implementation of
            // java.util.Properties handles such a case.
            String s = "a=1\na=2\n";
            byte[] data = s.getBytes("ISO-8859-1");
            Properties p = new Properties();
            p.load(new ByteArrayInputStream(data));
            // if java.lang.Properties favors the last definition in the
            // stream for a given key, then we need to load files in
            // reverse TemplateLoader search order.
            reverseOrder = ("2".equals(p.get("a")));

            // Now, we need to find the system default temp directory.
            // One way to accomplish this is to create a temporary file
            // and find out what directory it is in.
            File tempFile = File.createTempFile("res", ".tmp");
            tempDir = tempFile.getParentFile();
            tempFile.delete();
        } catch (IOException ioe) {
            // can't happen?
            ioe.printStackTrace();
        }
    }


    protected URL findResourceImpl(String mappedName) {
        try {
            if (cache.containsKey(mappedName))
                return (URL) cache.get(mappedName);
            else {
                URL result = lookupTemplateResource(mappedName);
                cache.put(mappedName, result);
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private URL lookupTemplateResource(String mappedName) {
        // find all the URLs available to the template loader that match
        // the given name
        URL[] result = TemplateLoader.resolveURLs(mappedName);
        if (result.length == 0)
            // no matches found?  return null.
            return null;

        else if (result.length == 1)
            // exactly one match?  return it.
            return result[0];

        else try {
            // more than one match?  concatenate them.
            return concatenateResources(mappedName, result);
        } catch (IOException ioe) {
            // if the concatenation process fails, revert back to the
            // simple behavior (return the item with highest priority).
            return result[0];
        }
    }


    private URL concatenateResources(String mappedName, URL[] itemsToMerge)
        throws IOException
    {
        // Create a temporary file to hold the merged resources.  Although
        // this is a fairly simple-minded strategy, it is dramatically
        // simpler than the alternative (creating and registering our own
        // URLStreamHandler).  This solution is good enough for our purposes
        // because only a tiny fraction of dashboard users will need to use
        // the resource-merging functionality - namely, people who are
        // actively using the Localization Tool.
        //
        // Rather than using File.createTempFile(), we build the filename
        // manually.  This strategy ensures that, if the file is not deleted
        // on exit (due to a dashboard crash), the file will be replaced the
        // next time the dashboard runs. (In contrast, createTempFile() would
        // continue creating additional files to avoid overwriting files
        // that were already present.

        String name = "temp-" + mappedName.replace('/', ',');
        File f = new File(tempDir, name);
        f.deleteOnExit();

        // copy all the resources found into the temporary file.
        FileOutputStream out = new FileOutputStream(f);
        if (reverseOrder) {
            for (int i = itemsToMerge.length;   i-- > 0;  )
                copyData(itemsToMerge[i], out);
        } else {
            for (int i = 0;   i < itemsToMerge.length;   i++)
                copyData(itemsToMerge[i], out);
        }
        out.close();

        // return a URL to the temporary file.
        return f.toURL();
    }


    private void copyData(URL url, FileOutputStream out) throws IOException {
        InputStream in = url.openConnection().getInputStream();
        byte[] buf = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buf)) != -1)
            out.write(buf, 0, bytesRead);
        out.write('\n');
        in.close();
    }
}
