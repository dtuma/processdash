// Copyright (C) 2006-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cms;

import java.util.List;

import net.sourceforge.processdash.util.StringUtils;

public class PageSectionHelper {

    private String sectionInstanceID;

    private int status;

    public static final int STATUS_BEFORE = 0;

    public static final int STATUS_START = 1;

    public static final int STATUS_DURING = 2;

    public static final int STATUS_END = 3;

    public static final int STATUS_AFTER = 4;

    public static final String SECTION_HEADING_SNIP_ID = "pdash.sectionHeading";

    public PageSectionHelper(String sectionInstanceID) {
        if (StringUtils.hasValue(sectionInstanceID))
            this.sectionInstanceID = sectionInstanceID;
        else
            this.sectionInstanceID = null;
        reset();
    }

    public String getSectionInstanceID() {
        return sectionInstanceID;
    }

    public void reset() {
        this.status = STATUS_BEFORE;
    }

    public boolean test(SnippetInstanceTO snip) {
        if (status == STATUS_AFTER)
            return false;

        getStatus(snip);

        return (status == STATUS_DURING);
    }

    public int getStatus(SnippetInstanceTO snip) {
        boolean isSectionHeading = (snip == null ? true
                : isSectionHeading(snip));
        if (isSectionHeading) {
            if (status == STATUS_BEFORE && instanceIdMatches(snip)) {
                status = STATUS_DURING;
                return STATUS_START;
            } else if (status == STATUS_DURING) {
                status = STATUS_AFTER;
                return STATUS_END;
            }
        }
        return status;
    }

    /** Find any nonempty sections on the page which contain only invalid
     * snippets (snippets which can not be run for any reason), and hide
     * the headings for those sections.
     * 
     * Hiding is performed by setting the generated content to null and the
     * status to {@link SnippetInvoker#STATUS_HIDDEN}.
     * 
     * @param page
     */
    public static void hideInvalidSections(PageContentTO page) {
        List snippets = page.getSnippets();
        int sectionStatus = EMPTY_SECTION;
        for (int i = snippets.size();  i-- > 0; ) {
            SnippetInstanceTO snip = (SnippetInstanceTO) snippets.get(i);
            if (snip.getPageRegion() != PageContentTO.REGION_CONTENT)
                continue;

            if (isSectionHeading(snip)) {
                if (sectionStatus > SnippetInvoker.STATUS_OK) {
                    snip.setStatus(SnippetInvoker.STATUS_HIDDEN);
                    snip.setGeneratedContent(null);
                }
                sectionStatus = EMPTY_SECTION;
            } else {
                int oneStatus = snip.getStatus();
                if (oneStatus == SnippetInvoker.NO_PERMISSION
                        && AbstractPageAssembler.hasEditPermission())
                    sectionStatus = SnippetInvoker.STATUS_OK;
                else if (sectionStatus == EMPTY_SECTION)
                    sectionStatus = oneStatus;
                else if (oneStatus <= SnippetInvoker.STATUS_OK)
                    sectionStatus = SnippetInvoker.STATUS_OK;
            }
        }

    }
    private static int EMPTY_SECTION = -100;

    public static boolean isSectionHeading(SnippetInstanceTO snip) {
        return SECTION_HEADING_SNIP_ID.equals(snip.getSnippetID());
    }

    public static boolean isDefaultSectionHeading(SnippetInstanceTO snip) {
        if (!isSectionHeading(snip))
            return false;
        String params = snip.getPersistedText();
        return (params != null
                && params.indexOf("\"DefaultSectionHeading\"") != -1);
    }

    private boolean instanceIdMatches(SnippetInstanceTO snip) {
        return snip != null
                && (sectionInstanceID == null || sectionInstanceID.equals(snip
                        .getInstanceID()));
    }

}
