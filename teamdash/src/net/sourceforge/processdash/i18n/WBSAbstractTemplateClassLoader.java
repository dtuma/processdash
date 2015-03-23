// Copyright (C) 2003-2012 Tuma Solutions, LLC
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
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;


abstract class WBSAbstractTemplateClassLoader extends ClassLoader {

    private static final String KNOWN_RESOURCE = "resources/WBSEditor.properties";

    protected WBSAbstractTemplateClassLoader() {
        // Test to see if a known resource is locatable.  If not, this class
        // is not the right implementation to use for the current mode of
        // operation.
        if (findResource(KNOWN_RESOURCE) == null)
            throw new UnsupportedOperationException();
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }

    protected URL findResource(String name) {
        // The Resources class will load a special "global" bundle as the
        // parent of all other resource bundles. The WBS Editor does not make
        // use of that functionality, but we must return an empty resource to
        // keep the Resources class happy.
        if (name.contains("(Resources)"))
            return WBSAbstractTemplateClassLoader.class
                    .getResource("EmptyFile.txt");

        final String mappedName = mapName(name);

        try {
            return (URL) AccessController
                    .doPrivileged(new PrivilegedExceptionAction() {
                        public Object run() throws Exception {
                            return findResourceImpl(mappedName);
                        }
                    });
        } catch (PrivilegedActionException e) {
            if (e.getException() instanceof RuntimeException)
                throw (RuntimeException) e.getException();
            else
                throw new RuntimeException(e);
        }
    }

    protected String mapName(String name) {
        name = name.replace('$', '.');
        if (!name.startsWith("/"))
            name = "/" + name;
        return "Templates" + name;
    }

    protected abstract URL findResourceImpl(String mappedName);

}
