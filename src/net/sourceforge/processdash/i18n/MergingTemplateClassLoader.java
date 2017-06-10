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

import java.net.URL;

import net.sourceforge.processdash.templates.TemplateLoader;

/**
 * A special classloader for use with java.util.ResourceBundle. This loads
 * ".properties" files from the TemplateLoader search path, allowing dashboard
 * add-on files to contribute localization information. In addition, if this
 * classloader finds more than one matching ".properties" file in the
 * TemplateLoader search path, it will merge their contents.
 */
public class MergingTemplateClassLoader
        extends AbstractMergingTemplateClassLoader {

    @Override
    protected URL[] lookupUrlsForResource(String resourceName) {
        // ask the TemplateLoader to find all resources matching the given name
        return TemplateLoader.resolveURLs(resourceName);
    }

}
