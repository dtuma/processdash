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


public class TreeNodeChange<ID, Content> {

    public enum Type {
        /** A new node was added to a tree */
        Add,

        /** The content of an existing node was changed */
        Edit,

        /** An existing node still has the same parent, but it has changed to a
         *  different location among its siblings */
        Reorder,

        /** An existing node has been moved underneath a different parent */
        Move,

        /** An existing node was deleted from the tree */
        Delete
    };

    private Type type;

    private TreeNode<ID, Content> node;

    public TreeNodeChange(Type type, TreeNode<ID, Content> node) {
        this.type = type;
        this.node = node;
    }

    public Type getType() {
        return type;
    }

    void setType(Type type) {
        this.type = type;
    }

    public TreeNode<ID, Content> getNode() {
        return node;
    }

    public ID getNodeID() {
        return node.getID();
    }

    public TreeNode<ID, Content> getParent() {
        return node.getParent();
    }

    public ID getParentID() {
        TreeNode<ID, Content> parent = getParent();
        return (parent == null ? null : parent.getID());
    }

}
