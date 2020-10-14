// Copyright (C) 2020 Tuma Solutions, LLC
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

package teamdash.wbs;

import teamdash.merge.ModelType;

public class SizeMetricsMerger
        extends AbstractWBSModelMerger<SizeMetricsWBSModel> {

    public SizeMetricsMerger(TeamProject base, TeamProject main,
            TeamProject incoming) {
        this(getModel(base), getModel(main), getModel(incoming));
    }

    private static SizeMetricsWBSModel getModel(TeamProject project) {
        return project.getSizeMetrics();
    }

    public SizeMetricsMerger(SizeMetricsWBSModel base, SizeMetricsWBSModel main,
            SizeMetricsWBSModel incoming) {
        super(base, main, incoming);

        // register handlers for attributes as needed.
        ignoreAttributeConflicts(NODE_NAME,
            "^" + SizeMetricsWBSModel.HIST_ID_ATTR_PREFIX);
    }

    @Override
    protected SizeMetricsWBSModel createWbsModel() {
        return new SizeMetricsWBSModel();
    }

    @Override
    protected ModelType getModelType() {
        return ModelType.SizeMetrics;
    }

}
