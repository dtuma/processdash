// Copyright (C) 2017 Tuma Solutions, LLC
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.util.StringUtils;


/**
 * Finds template resources that appear in the packaged WBSEditor.jar file
 */
public class WBSRuntimeTemplateClassLoader
        extends AbstractMergingTemplateClassLoader {

    private String[] userTranslations;

    public WBSRuntimeTemplateClassLoader() {
        userTranslations = findUserTranslations();
    }

    private String[] findUserTranslations() {
        // find the directory where user translations are stored.
        File appDir = DirectoryPreferences.getApplicationDirectory();
        File appTemplateDir = new File(appDir, "Templates");
        if (!appTemplateDir.isDirectory())
            return null;

        // find files containing user translations, and build a list of URLs
        List<String> translationJars = new ArrayList<String>();
        for (File file : appTemplateDir.listFiles()) {
            if (file.getName().startsWith("pspdash_")) {
                try {
                    String url = "jar:" + file.toURI().toURL() + "!/";
                    translationJars.add(url);
                } catch (MalformedURLException e) {
                }
            }
        }

        // return the results we found
        return (translationJars.isEmpty() ? null
                : translationJars.toArray(new String[translationJars.size()]));
    }

    @Override
    protected URL[] lookupUrlsForResource(String resourceName) {
        String templateName = mapToTemplates(resourceName);

        // look for the resource within the WBSEditor.jar file. Files copied
        // from the dashboard are in a "resources/dash" subdirectory.
        String localName = templateName;
        if (!localName.startsWith("Templates/resources/WBSEditor"))
            localName = StringUtils.findAndReplace(localName, "/resources/",
                "/resources/dash/");
        URL localResult = WBSRuntimeTemplateClassLoader.class.getClassLoader()
                .getResource(localName);

        // find any user translations that may be in use. If none, return
        // the result that we found locally in this JAR
        List<URL> result = getUserResources(templateName);
        if (result.isEmpty())
            return new URL[] { localResult };

        // add the local result to the user translations, and return
        if (localResult != null)
            result.add(localResult);
        return result.toArray(new URL[result.size()]);
    }

    private List<URL> getUserResources(String templateName) {
        if (userTranslations == null)
            return Collections.EMPTY_LIST;

        // Look for the named resource in each of the user translation JARs
        List<URL> result = new ArrayList<URL>(userTranslations.length + 1);
        for (String oneUserJar : userTranslations) {
            try {
                URL u = new URL(oneUserJar + templateName);
                u.openStream().close();
                result.add(u);
            } catch (Exception e) {
            }
        }
        return result;
    }

}
