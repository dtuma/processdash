// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.hist;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import teamdash.wbs.WBSNode;

public class ProjectChangedNode extends ProjectChange {

    private WBSNode parent;

    private Map<WBSNode, Object> children;

    protected ProjectChangedNode(WBSNode parent, WBSNode child,
            Object changeType, String author, Date timestamp) {
        super(author, timestamp);
        this.parent = parent;
        this.children = new HashMap();
        addChild(child, changeType);
    }

    public WBSNode getParent() {
        return parent;
    }

    public void setParent(WBSNode parent) {
        this.parent = parent;
    }

    public void addChild(WBSNode child, Object changeType) {
        children.put(child, changeType);
    }

    public Map<WBSNode, Object> getChildren() {
        return children;
    }

    @Override
    public String getDescription() {
        // FIXME
        return parent.getName() + " -> " + children;
    }

    public static class Moved {

        private WBSNode oldParent;

        public Moved(WBSNode oldParent) {
            this.oldParent = oldParent;
        }

        public WBSNode getOldParent() {
            return oldParent;
        }

        @Override
        public String toString() {
            return "Moved from " + oldParent;
        }
    }

    public static class Renamed {

        private String oldName;

        public Renamed(String oldName) {
            this.oldName = oldName;
        }

        public String getOldName() {
            return oldName;
        }

        @Override
        public String toString() {
            return "Renamed from " + oldName;
        }

    }

}
