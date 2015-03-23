// Copyright (C) 2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.dash;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class DocumentOpenerImpl implements DocumentOpener {

    private List<DocumentOpener> delegates;

    DocumentOpenerImpl() {
        String packageName = DocumentOpenerImpl.class.getPackage().getName();

        delegates = new ArrayList<DocumentOpener>();
        for (String className : CLASSES) {
            try {
                String classFullName = packageName + "." + className;
                Object oneDelegate = Class.forName(classFullName).newInstance();
                delegates.add((DocumentOpener) oneDelegate);
            } catch (Throwable t) {
            }
        }
    }

    public boolean isSupported() {
        return !delegates.isEmpty();
    }

    public boolean openDocument(File doc) {
        for (DocumentOpener opener : delegates)
            if (opener.openDocument(doc))
                return true;

        return false;
    }

    private static final String[] CLASSES = { "DocumentOpenerJava16",
            "DocumentOpenerWindows" };

}
