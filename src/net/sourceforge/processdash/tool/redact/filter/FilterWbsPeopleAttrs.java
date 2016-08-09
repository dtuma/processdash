// Copyright (C) 2012-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact.filter;

import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.PersonMapper;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;

@EnabledFor(RedactFilterIDs.PEOPLE)
public class FilterWbsPeopleAttrs extends AbstractWbsAttrFilter {

    @Override
    public void filter(DataFileEntry e) {
        if (maybeRenameInitialsAttr(e) == false)
            super.filter(e);
    }

    /**
     * Rename attributes that have the initials of people in their name.
     */
    private boolean maybeRenameInitialsAttr(DataFileEntry e) {
        String attrName = e.getKey();
        String attrSuffix = getMatchingInitialsAttr(attrName);
        if (attrSuffix == null)
            return false;

        String initials = attrName.substring(0,
            attrName.length() - attrSuffix.length());
        String newAttr = PersonMapper.hashInitials(initials) + attrSuffix;
        e.setKey(newAttr);
        return true;
    }

    private String getMatchingInitialsAttr(String attrName) {
        for (String suffix : INITIALS_ATTRS)
            if (attrName.endsWith(suffix))
                return suffix;

        if (attrName.endsWith(" Max Client ID")) {
            int dashPos = attrName.indexOf('-');
            if (dashPos != -1)
                return attrName.substring(dashPos);
        }

        return null;
    }

    private String[] INITIALS_ATTRS = {
        "-Time (Top Down)", "-Time (Synced)", " (Assigned With Zero)"
    };

    @EnabledFor(" Author$")
    public String scrambleNoteAuthors(String author) {
        return PersonMapper.hashPersonName(author);
    }

    @EnabledFor("^Client Unique ID$")
    public String scrambleClientIDs(String clientID) {
        return PersonMapper.hashClientNodeID(clientID);
    }

}
