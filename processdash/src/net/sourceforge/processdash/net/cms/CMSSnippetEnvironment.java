// Copyright (C) 2006-2008 Tuma Solutions, LLC
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

import net.sourceforge.processdash.ui.snippet.SnippetEnvironment;

/** Constants for values placed in the environment when invoking a snippet.
 */
public interface CMSSnippetEnvironment extends SnippetEnvironment {

    /** Key that maps to the title of the current page, as designated by the
     * user */
    public static final String CMS_PAGE_TITLE = "cmsPageTitle";

    /** Key that maps to a localized version of the current prefix.
     * 
     * Example: if the user is visiting the page /To+Date/PSP/All//cms/foo/bar,
     * and their language is English, this would map to the string
     * "All PSP Data To Date".
     */
    public static final String CMS_LOCALIZED_PREFIX = "cmsLocalizedPrefix";

    /** Key that maps to the filename of the cms page the user is viewing or
     * editing.
     * 
     * Example: if the user is visiting the page /To+Date/PSP/All//cms/foo/bar,
     * this would map to the string "/foo/bar".
     */
    public static final String CMS_PAGE_FILENAME = "cmsPageFilename";

    /** Key that maps to a URI that can be used to regenerate the current
     * frame.
     * 
     * If the user is not viewing the current page in frames, this will contain
     * the same value as {@link FULL_PAGE_URI}.
     */
    public String CURRENT_FRAME_URI = "cmsSnippetCurrentFrameUri";

    /** Key that maps to a URI that can be used to regenerate the entire page */
    public String FULL_PAGE_URI = "cmsFullPageUri";

    /** Key that maps to the value which should be used as a frame target when
     * replacing the entire page */
    public String FULL_PAGE_TARGET = "cmsFullPageTarget";

}
