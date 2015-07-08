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

import java.util.List;

import teamdash.merge.ModelType;

public class BlameCaretPos {

    private ModelType modelType;

    private List<Integer> nodes;

    private List<String> columns;

    public BlameCaretPos(ModelType modelType, List<Integer> nodes,
            List<String> columns) {
        this.modelType = modelType;
        this.nodes = nodes;
        this.columns = columns;
    }

    public ModelType getModelType() {
        return modelType;
    }

    public boolean isSingleCell() {
        return nodes.size() == 1 && columns.size() == 1;
    }

    public List<Integer> getNodes() {
        return nodes;
    }

    public Integer getSingleNode() {
        return nodes.get(0);
    }

    public List<String> getColumns() {
        return columns;
    }

    public String getSingleColumn() {
        return columns.get(0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof BlameCaretPos) {
            BlameCaretPos that = (BlameCaretPos) obj;
            return this.modelType.equals(that.modelType)
                    && this.nodes.equals(that.nodes)
                    && this.columns.equals(that.columns);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + columns.hashCode();
        result = prime * result + modelType.hashCode();
        result = prime * result + nodes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Blame Caret: " + modelType + " #" + nodes + ", $" + columns;
    }

}
