// Copyright (C) 2010-2015 Tuma Solutions, LLC
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

package teamdash.team;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Element;

import net.sourceforge.processdash.util.XMLUtils;


public class TeamMemberClipData implements Serializable {

    private static final String DOC_TAG = "ClipboardTeamMemberData";

    public transient List<TeamMember> teamMembers;

    public String xmlData;

    public TeamMemberClipData(List<TeamMember> teamMembers) {
        this.teamMembers = teamMembers;
        this.xmlData = getXML(teamMembers);
    }

    private String getXML(List<TeamMember> teamMembers) {
        try {
            StringWriter out = new StringWriter();
            out.write("<" + DOC_TAG + ">\n");
            for (TeamMember t : teamMembers) {
                t.getAsXML(out, true, null);
            }
            out.write("</" + DOC_TAG + ">\n");
            return out.toString();
        } catch (IOException e) {
            // can't happen
            return null;
        }
    }

    public List<TeamMember> getTeamMembers(Date zeroDay) {
        if (teamMembers != null)
            // this indicates that we haven't been serialized and deserialized.
            // just return the original list of nodes we are holding.  (This
            // behavior is important for the "move" logic.)
            return teamMembers;

        try {
            // reconstruct our node list from the XML data.
            Element e = XMLUtils.parse(xmlData).getDocumentElement();
            if (!DOC_TAG.equals(e.getTagName()))
                return null;

            List<TeamMember> result = new ArrayList();
            for (Element child : XMLUtils.getChildElements(e)) {
                if (TeamMember.TAG_NAME.equals(child.getTagName()))
                    result.add(new TeamMember(child, zeroDay, null));
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

}
