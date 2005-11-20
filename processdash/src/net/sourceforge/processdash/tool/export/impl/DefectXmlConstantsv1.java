// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2004-2005 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.tool.export.impl;


public interface DefectXmlConstantsv1 extends XmlConstants {

    public static final String DEFECT_START_TOKEN = "<!-- start defects -->";

    public static final String DEFECT_TAG = "defect";

    public static final String DESCRIPTION_ATTR = "desc";
    public static final String FIX_DEFECT_ATTR = "fd";
    public static final String FIX_TIME_ATTR = "ft";
    public static final String REMOVED_ATTR = "rem";
    public static final String INJECTED_ATTR = "inj";
    public static final String DEFECT_TYPE_ATTR = "type";
    public static final String NUM_ATTR = "num";
    public static final String DATE_ATTR = "date";
    public static final String PATH_ATTR = "path";

}
