// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.hist;

import net.sourceforge.processdash.util.HTMLUtils;

public class ProjectHistoryException extends Exception {

    private String errorKey;

    private String[] arguments;

    public ProjectHistoryException(String errorKey, String... arguments) {
        this.errorKey = "Errors." + errorKey;
        for (int i = arguments.length; i-- > 0;)
            arguments[i] = HTMLUtils.escapeEntities(arguments[i]);
        this.arguments = arguments;
    }

    public ProjectHistoryException(Throwable t, String errorKey,
            String... arguments) {
        this(errorKey, arguments);
        initCause(t);
    }

    public String getHtml() {
        return ProjectDiff.resources.format(errorKey, arguments);
    }

}