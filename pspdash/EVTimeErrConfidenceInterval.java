// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package pspdash;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;


public class EVTimeErrConfidenceInterval extends LognormalConfidenceInterval {


    public EVTimeErrConfidenceInterval(EVSchedule sched) {
        Date effDate = sched.getEffectiveDate();

        double plan, act, autoPlan = 0;
        for (int i = 0;   i < sched.getRowCount();   i++) {
            EVSchedule.Period p = sched.get(i);

            // only use completed periods.  If the period in question
            // ends after the effective date, we're done.
            if (p.endDate.compareTo(effDate) > 0)
                break;

            if (p.planDirectTime > 0 || p.actualDirectTime > 0) {
                plan = p.planDirectTime;
                act = p.actualDirectTime;
                if (p.automatic)
                    plan = autoPlan;
                else
                    autoPlan = plan;

                addDataPoint(plan, act);
            }
        }
        dataPointsComplete();
    }


    public EVTimeErrConfidenceInterval(Element xml) {
        super(xml);
    }


}
