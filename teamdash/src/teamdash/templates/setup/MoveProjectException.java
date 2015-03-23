// Copyright (C) 2002-2010 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.templates.setup;

import net.sourceforge.processdash.util.HTMLUtils;

public class MoveProjectException extends Exception {

    String page;

    String query;

    MoveProjectException(String page, String query) {
        this.page = page;
        this.query = query;
    }

    MoveProjectException(String query) {
        this(null, query);
    }

    MoveProjectException(Throwable t) {
        this("generalError");
        initCause(t);
    }

    MoveProjectException append(String name, String value) {
        if (value != null)
            query = query + "&" + name + "=" + HTMLUtils.urlEncode(value);
        return this;
    }

}
