//Process Dashboard - Data Automation Tool for high-maturity processes
//Copyright (C) 2003 Software Process Dashboard Initiative
//
//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 2
//of the License, or (at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
//The author(s) may be contacted at:
//Process Dashboard Group
//c/o Ken Raisor
//6137 Wardleigh Road
//Hill AFB, UT 84056-5843
//
//E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.ev.ci;

import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;

import pspdash.EVTask;


public class EVCostConfidenceInterval extends LinearRatioConfidenceInterval {


    public EVCostConfidenceInterval(List tasks) {
        if (tasks == null || tasks.size() == 0) {
            viability = CANNOT_CALCULATE;
        }

        Iterator i = tasks.iterator();
        while (i.hasNext()) {
            EVTask task = (EVTask) i.next();
            if (task.getDateCompleted() != null)
                addDataPoint(task.getPlanValue(), task.getActualDirectTime());
        }
        dataPointsComplete();
    }


    public EVCostConfidenceInterval(Element xml) {
        super(xml);
    }

}
