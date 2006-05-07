// Copyright (C) 2003-2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.data.repository;

import java.util.Vector;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.MalformedValueException;
import net.sourceforge.processdash.data.NumberData;


class PercentageFunction extends DoubleData implements DataListener {

    public static final String PERCENTAGE_FLAG = "/%/";

    String myName, numeratorName, denominatorName;
    double numeratorValue, denominatorValue;
    DataRepository data;


    public PercentageFunction(String name, String params, String saveAs,
                              DataRepository r, String prefix)
        throws MalformedValueException
    {
        this(name, r);
    }

    public PercentageFunction(String name, DataRepository r)
        throws MalformedValueException
    {
        super(Double.NaN, false);
        //System.err.println("Autocreating " + name);
        data = r;

        try {
            parseName(name);
        } catch (MalformedValueException mve) {
            throw mve;
        } catch (Exception e) {
            throw new MalformedValueException(e.toString());
        }

        numeratorValue = denominatorValue = Double.NaN;
        data.addActiveDataListener(numeratorName, this, name);
        data.addActiveDataListener(denominatorName, this, name);

        myName = name;
    }

    private void parseName(String name) throws MalformedValueException {
        int flagPos = name.indexOf(PERCENTAGE_FLAG);
        if (flagPos <= 0)
            throw new MalformedValueException
                ("PercentageFunction: '" + name + "' does not contain '" +
                 PERCENTAGE_FLAG + "'.");

        String dataName = name.substring(flagPos + PERCENTAGE_FLAG.length());
        String childPrefix = name.substring(0, flagPos);
        numeratorName = DataRepository.createDataName(childPrefix, dataName);

        int slashPos = childPrefix.lastIndexOf('/');
        if (slashPos == -1)
            throw new MalformedValueException
                ("PercentageFunction: '" + name + "' does not have a parent.");

        String parentPrefix = name.substring(0, slashPos);
        denominatorName = DataRepository.createDataName(parentPrefix,dataName);
    }


    public void dataValuesChanged(Vector v) {
        if (v == null || v.size() == 0) return;
        for (int i = 0;  i < v.size();  i++)
            dataValueChanged((DataEvent) v.elementAt(i));
    }

    public void dataValueChanged(DataEvent e) {
        if (e.getName().equals(numeratorName))
            numeratorValue = getDoubleValue(e);
        else if (e.getName().equals(denominatorName))
            denominatorValue = getDoubleValue(e);
        else
            return;

        if (Double.isNaN(numeratorValue) || Double.isNaN(denominatorValue))
            value = Double.NaN;
        else if (Double.isInfinite(numeratorValue) ||
                 Double.isInfinite(denominatorValue) ||
                 denominatorValue == 0)
            value = Double.POSITIVE_INFINITY;
        else
            value = numeratorValue / denominatorValue;

        if (myName != null)
            data.valueRecalculated(myName, this);
    }

    private double getDoubleValue(DataEvent e) {
        if (e.getValue() instanceof NumberData)
            return ((NumberData) e.getValue()).getDouble();
        else
            return Double.NaN;
    }

    public String saveString() { return ""; }

    public void dispose() {
        String oldNumName = numeratorName;
        String oldDenName = denominatorName;

        myName = numeratorName = denominatorName = null;
        if (data != null) {
            data.removeDataListener(oldNumName, this);
            data.removeDataListener(oldDenName, this);
            data.removeActiveDataListener(this);
            data = null;
        }
    }

    static boolean isPercentageDataName(String name) {
        return (name != null && name.indexOf(PERCENTAGE_FLAG) > 0);
    }
}
