// Copyright (C) 2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.http;

import java.net.URL;

import org.eclipse.jetty.http.PathMap;

import net.sourceforge.processdash.templates.TemplateLoader;


class WebAppContextLegacy extends WebAppContextDashboard {

    /**
     * Create a web app context that is capable of serving files out of a legacy
     * Process Dashboard add-on. In a legacy add-on, the class files are stored
     * at the top level, and web content is in a "Templates/" subdirectory)
     * 
     * @param legacyTemplateUrl
     *            the URL to the "Templates/" subdirectory
     */
    WebAppContextLegacy(URL legacyTemplateUrl) {
        String urlStr = legacyTemplateUrl.toString();

        // lookup the template classloader for this add-on
        if (urlStr.startsWith("jar:"))
            setClassLoader(TemplateLoader
                    .getTemplateClassLoader(legacyTemplateUrl));

        // initialize the web application context
        init(urlStr, true, true);

        // dashboard add-ons are mounted at the root context
        setContextPath("/");

        // do not explode the contents of a dashboard add-on
        setExtractWAR(false);
    }

    @Override
    protected void startContext() throws Exception {
        try {
            PathMap.setPathSpecSeparators("!");
            super.startContext();
        } finally {
            PathMap.setPathSpecSeparators(":,");
        }
    }

}
