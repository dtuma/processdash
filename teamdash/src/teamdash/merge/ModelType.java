// Copyright (C) 2012-2015 Tuma Solutions, LLC
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

package teamdash.merge;

import teamdash.team.TeamMember;
import teamdash.wbs.ProxyWBSModel;
import teamdash.wbs.TeamProject;
import teamdash.wbs.WBSNode;

public enum ModelType {

    Wbs {
        public Object getAssociatedModel(TeamProject teamProject) {
            return teamProject.getWBS();
        }
    },

    Workflows {
        public Object getAssociatedModel(TeamProject teamProject) {
            return teamProject.getWorkflows();
        }
    },

    Proxies {
        public Object getAssociatedModel(TeamProject teamProject) {
            return teamProject.getProxies();
        }

        @Override
        public String getNodeName(Object node) {
            return ProxyWBSModel.getProxyItemName((WBSNode) node);
        }
    },

    Milestones {
        public Object getAssociatedModel(TeamProject teamProject) {
            return teamProject.getMilestones();
        }
    },

    Columns {
        public Object getAssociatedModel(TeamProject teamProject) {
            return teamProject.getColumns();
        }

        @Override
        public String getNodeName(Object node) {
            return node.toString();
        }
    },

    TeamList {
        public Object getAssociatedModel(TeamProject teamProject) {
            return teamProject.getTeamMemberList();
        }

        @Override
        public String getNodeName(Object node) {
            return ((TeamMember) node).getName();
        }
    };

    public abstract Object getAssociatedModel(TeamProject teamProject);

    public String getNodeName(Object node) {
        return ((WBSNode) node).getName();
    }

}