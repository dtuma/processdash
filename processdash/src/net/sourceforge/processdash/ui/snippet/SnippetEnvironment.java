// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

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
