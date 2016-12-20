// Copyright (C) 2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.group;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.NullSafeObjectUtils;

public class UserGroup implements UserFilter, Comparable<UserGroup> {

    static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.Groups");

    public static final String EVERYONE_ID = "0";

    /**
     * At times, there is a need to capture the concept of "everyone." This
     * object serves that purpose. Its meaning is symbolic, because its set of
     * members will be empty; but its name will be a language-appropriate
     * version of "Everyone," and its ID will be {@link #EVERYONE_ID}.
     */
    public static final UserGroup EVERYONE = new UserGroup(
            resources.getString("Everyone"), EVERYONE_ID, false,
            Collections.EMPTY_SET);



    private String displayName;

    String id;

    private boolean custom;

    private Set<UserGroupMember> members;

    public UserGroup(String displayName, String id, boolean custom,
            Set<UserGroupMember> members) {
        this.displayName = displayName;
        this.id = id;
        this.custom = custom;
        this.members = Collections.unmodifiableSet( //
                new HashSet<UserGroupMember>(members));
    }

    public UserGroup(Element xml, boolean custom) {
        // read group attributes from the XML tag
        this.displayName = xml.getAttribute(NAME_ATTR);
        this.id = xml.getAttribute(ID_ATTR);
        this.custom = custom;
        this.members = new HashSet<UserGroupMember>();

        // add group members from our XML children
        NodeList nl = xml.getElementsByTagName(UserGroupMember.PERSON_TAG);
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            UserGroupMember m = new UserGroupMember(e);
            this.members.add(m);
        }
        this.members = Collections.unmodifiableSet(this.members);
    }

    public void getAsXml(XmlSerializer xml) throws IOException {
        // start an XML tag and write attributes describing this group
        xml.startTag(null, GROUP_TAG);
        xml.attribute(null, NAME_ATTR, displayName);
        xml.attribute(null, ID_ATTR, id);

        // write child XML tags for each group member. sort the members to
        // ensure that the file contents will be written deterministically
        List<UserGroupMember> list = new ArrayList<UserGroupMember>(members);
        Collections.sort(list);
        for (UserGroupMember m : list)
            m.getAsXml(xml);

        // end the XML tag
        xml.endTag(null, GROUP_TAG);
    }


    public String getDisplayName() {
        return displayName;
    }

    public String getId() {
        return id;
    }

    public boolean isCustom() {
        return custom;
    }

    public Set<UserGroupMember> getMembers() {
        return members;
    }

    @Override
    public Set<String> getDatasetIDs() {
        if (EVERYONE_ID.equals(id))
            return null;

        Set<String> result = new HashSet<String>(members.size());
        for (UserGroupMember m : members)
            result.add(m.getDatasetID());
        return result;
    }


    @Override
    public int compareTo(UserGroup that) {
        if (this.custom != that.custom)
            return this.custom ? 1 : -1;
        else
            return this.displayName.compareToIgnoreCase(that.displayName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof UserGroup) {
            UserGroup that = (UserGroup) obj;
            return NullSafeObjectUtils.EQ(this.id, that.id)
                    && this.displayName.equals(that.displayName)
                    && (this.custom == that.custom)
                    && this.members.equals(that.members);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return id == null ? super.hashCode() : id.hashCode();
    }

    @Override
    public String toString() {
        if (isCustom())
            return resources.getString("Custom_Prefix") + " "
                    + getDisplayName();
        else
            return getDisplayName();
    }

    public static boolean isEveryone(UserFilter f) {
        return (f != null && EVERYONE_ID.equals(f.getId()));
    }


    static final String GROUP_TAG = "group";

    private static final String NAME_ATTR = "name";

    private static final String ID_ATTR = "id";

}
