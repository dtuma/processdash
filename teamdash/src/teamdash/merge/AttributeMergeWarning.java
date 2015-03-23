// Copyright (C) 2012 Tuma Solutions, LLC
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

package teamdash.merge;


public class AttributeMergeWarning<ID> extends MergeWarning<ID> {

    private String attributeName;

    private Object baseValue;

    private Object mainValue;

    private Object incomingValue;

    public AttributeMergeWarning(Severity severity, String key, ID nodeID,
            String attributeName, Object baseValue, Object mainValue,
            Object incomingValue) {
        super(severity, key, nodeID, nodeID);
        this.attributeName = attributeName;
        this.baseValue = baseValue;
        this.mainValue = mainValue;
        this.incomingValue = incomingValue;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public Object getBaseValue() {
        return baseValue;
    }

    public Object getMainValue() {
        return mainValue;
    }

    public Object getIncomingValue() {
        return incomingValue;
    }

    @Override
    public boolean matches(String s) {
        return attributeName.equals(s) || super.matches(s);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj))
            return false;
        AttributeMergeWarning that = (AttributeMergeWarning) obj;
        return this.attributeName.equals(that.attributeName);
    }

    @Override
    public int hashCode() {
        return (super.hashCode() << 3) ^ attributeName.hashCode();
    }

}
