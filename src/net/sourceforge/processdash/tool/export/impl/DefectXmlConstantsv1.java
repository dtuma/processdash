// Copyright (C) 2004-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;


public interface DefectXmlConstantsv1 extends XmlConstants {

    public static final String DEFECT_START_TOKEN = "<!-- start defects -->";

    public static final String DEFECT_TAG = "defect";

    public static final String DESCRIPTION_ATTR = "desc";
    public static final String FIX_DEFECT_ATTR = "fd";
    public static final String FIX_TIME_ATTR = "ft";
    public static final String FIX_COUNT_ATTR = "count";
    public static final String FIX_PENDING_ATTR = "pending";
    public static final String REMOVED_ATTR = "rem";
    public static final String INJECTED_ATTR = "inj";
    public static final String ID_ATTR_SUFFIX = "ID";
    public static final String NAME_ATTR_SUFFIX = "Name";
    public static final String DEFECT_TYPE_ATTR = "type";
    public static final String NUM_ATTR = "num";
    public static final String DATE_ATTR = "date";
    public static final String PATH_ATTR = "path";

}
