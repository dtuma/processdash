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

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;


public class SafeTemplateClassLoader extends ClassLoader {

    protected Class findClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }
    protected URL findResource(String name) {
        final String name1 = name.replace('$', '.');

        try {
            return (URL) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    return findResourceImpl(name1);
                }});
        } catch (PrivilegedActionException e) {
            if (e.getException() instanceof RuntimeException)
                throw (RuntimeException) e.getException();
            else
                throw new RuntimeException(e);
        }
    }

    protected URL findResourceImpl(String mappedName) {
        if (!mappedName.startsWith("/"))
            mappedName = "/" + mappedName;
        mappedName = "Templates" + mappedName;
        return Resources.class.getClassLoader().getResource(mappedName);
    }

}
