// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.snippet;

import net.sourceforge.processdash.net.http.HTMLPreprocessor;

public interface SnippetEnvironment {

    /** Key that maps to the text persisted by a former snippet instance. */
    public String PERSISTED_TEXT = "cmsSnippetPersistedText";

    /** Key that maps to the id of the snippet that created persisted text */
    public String SNIPPET_ID = "cmsSnippetID";

    /** Key that maps to the version the snippet that created persisted text */
    public String SNIPPET_VERSION = "cmsSnippetVersion";

    /** Key that maps to the resource bundle named by the snippet declaration */
    public String RESOURCES = HTMLPreprocessor.RESOURCES_PARAM;

}
