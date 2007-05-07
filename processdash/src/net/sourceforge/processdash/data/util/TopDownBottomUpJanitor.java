// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.data.util;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.PropertyKeyHierarchy;

public class TopDownBottomUpJanitor {

    private String dataName;

    private String tagName;

    private double tolerance = 0.9;

    public TopDownBottomUpJanitor(String dataName) {
        this(dataName, "Auto_Delete_Top_Down/" + dataName);
    }

    public TopDownBottomUpJanitor(String dataName, String tagName) {
        this.dataName = dataName;
        this.tagName = tagName;
    }

    public void cleanup(DashboardContext ctx) {
        cleanup(ctx.getData(), ctx.getHierarchy());
    }

    public void cleanup(DataContext data, PropertyKeyHierarchy hier) {
        cleanup(data, hier, PropertyKey.ROOT);
    }

    public void cleanup(DataContext data, PropertyKeyHierarchy hier,
            PropertyKey start) {
        doCleanup(data, hier, start);
    }

    protected double doCleanup(DataContext data, PropertyKeyHierarchy hier,
            PropertyKey node) {
        double topDownValue = getValueAt(data, node);
        int childCount = hier.getNumChildren(node);
        if (childCount == 0)
            return topDownValue;

        double bottomUpValue = 0;
        for (int i = 0; i < childCount; i++)
            bottomUpValue += doCleanup(data, hier, hier.getChildKey(node, i));
        if (bottomUpValue == 0)
            return topDownValue;
        else if (topDownValue == 0)
            return bottomUpValue;

        double delta = Math.abs(topDownValue - bottomUpValue);
        if (delta < tolerance && testTagAt(data, node))
            clearValueAt(data, node);

        return bottomUpValue;
    }

    protected double getValueAt(DataContext data, PropertyKey node) {
        String fullDataName = getDataName(node);
        SimpleData sd = data.getSimpleValue(fullDataName);
        if (sd instanceof DoubleData) {
            DoubleData dd = (DoubleData) sd;
            return dd.getDouble();
        }
        return 0;
    }

    protected boolean testTagAt(DataContext data, PropertyKey node) {
        String fullTagName = DataRepository
                .createDataName(node.path(), tagName);
        SimpleData sd = data.getSimpleValue(fullTagName);
        return sd != null && sd.test();
    }

    protected void clearValueAt(DataContext data, PropertyKey node) {
        String fullDataName = getDataName(node);
        data.putValue(fullDataName, null);
    }

    protected String getDataName(PropertyKey node) {
        return DataRepository.createDataName(node.path(), dataName);
    }
}
