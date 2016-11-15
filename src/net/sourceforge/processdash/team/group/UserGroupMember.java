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
import java.util.Collections;
import java.util.Set;

import org.w3c.dom.Element;
import org.xmlpull.v1.XmlSerializer;

public class UserGroupMember implements UserFilter, Comparable<UserGroupMember> {

    private String displayName;

    private String datasetID;

    public UserGroupMember(String displayName, String datasetID) {
        this.displayName = displayName;
        this.datasetID = datasetID;
    }

    public UserGroupMember(Element xml) {
        this.displayName = xml.getAttribute(NAME_ATTR);
        this.datasetID = xml.getAttribute(DATASET_ID_ATTR);
    }

    public void getAsXml(XmlSerializer xml) throws IOException {
        xml.startTag(null, PERSON_TAG);
        xml.attribute(null, NAME_ATTR, displayName);
        xml.attribute(null, DATASET_ID_ATTR, datasetID);
        xml.endTag(null, PERSON_TAG);
    }


    public String getDisplayName() {
        return displayName;
    }

    public String getDatasetID() {
        return datasetID;
    }

    @Override
    public Set<String> getDatasetIDs() {
        return Collections.singleton(datasetID);
    }


    @Override
    public int compareTo(UserGroupMember that) {
        return this.displayName.compareToIgnoreCase(that.displayName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof UserGroupMember) {
            UserGroupMember that = (UserGroupMember) obj;
            return this.datasetID.equals(that.datasetID);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return datasetID.hashCode();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }


    static final String PERSON_TAG = "person";

    private static final String NAME_ATTR = "name";

    private static final String DATASET_ID_ATTR = "datasetID";

}
