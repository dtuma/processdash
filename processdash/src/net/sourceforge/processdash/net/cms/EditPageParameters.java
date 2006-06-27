// Copyright (C) 2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cms;

/** Interface holding String constants used in the generation of editing pages
 * 
 */
public interface EditPageParameters {

    /** Name of form field holding the title of the page */
    String PAGE_TITLE = "pageTitle";

    /** Name of form field indicating the presence of a snippet instance, and
     * holding its namespace */
    String SNIPPET_INSTANCE = "snippetInstance";

    /** Prefix for form field name indicating the id of a snippet instance.
     * (The namespace should be appended.) */
    String SNIPPET_ID_ = "snippetId";

    /** Prefix for form field name indicating the version of a snippet instance.
     * (The namespace should be appended.) */
    String SNIPPET_VERSION_ = "snippetVersion";

    /** Prefix for form field name holding the verbatim text that was persisted
     * by a past snippet instance. (The namespace should be appended.) */
    String SNIPPET_VERBATIM_TEXT_ = "snippetVerbatimText";

    /** Prefix for form field name flagging that a snippet instance has been
     * discarded by the user. (The namespace should be appended.) */
    String SNIPPET_DISCARDED_ = "snippetDiscarded";

}
