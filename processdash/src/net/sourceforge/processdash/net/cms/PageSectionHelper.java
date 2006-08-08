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

    public static boolean isSectionHeading(SnippetInstanceTO snip) {
        return SECTION_HEADING_SNIP_ID.equals(snip.getSnippetID());
    }

    private boolean instanceIdMatches(SnippetInstanceTO snip) {
        return snip != null
                && (sectionInstanceID == null || sectionInstanceID.equals(snip
                        .getInstanceID()));
    }

}
