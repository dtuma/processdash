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

package net.sourceforge.processdash.data;



class FrozenUtil {

    public String currentSaveString = null;
    private String formerSaveString = null;
    public boolean formerEditable = true;

    private static final char BEGIN     = '#';
    private static final char MID       = '\u0001';
    private static final String DEFAULT = "DEFAULT";

    public FrozenUtil() {}

    public FrozenUtil(String s) throws MalformedValueException {
        if (s == null || s.length() == 0 || s.charAt(0) != BEGIN)
            throw new MalformedValueException();

        int pos = s.indexOf(MID);
        if (pos == -1) throw new MalformedValueException();

        currentSaveString = s.substring(1, pos);

        if (s.charAt(++pos) == '=') {
            formerEditable = false;
            pos++;
        } else
            formerEditable = true;

        formerSaveString = s.substring(pos);
    }

    public String buildSaveString() {
        StringBuffer result = new StringBuffer();
        result.append(BEGIN).append(currentSaveString).append(MID);
        if (!formerEditable)
            result.append("=");
        result.append(formerSaveString);

        return result.toString();
    }

    public void setFormer(String former, boolean isDefaultVal) {
        if (isDefaultVal)
            formerSaveString = DEFAULT;
        else
            formerSaveString = former;
    }

    public String getFormer() {
        if (formerSaveString.equals(DEFAULT))
            return null;
        else
            return formerSaveString;
    }

}
