// Copyright (C) 2001-2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.dash;


import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;




/** CGI script for generating rollup selection lists.
 *
 * This script should be #included within a server-parsed HTML form
 * like the project plan summary form; it will generate an appropriate
 * HTML fragment containing &lt;select&gt; elements which will allow
 * the user to choose the rollup set they would like to use for
 * generating "To Date" data.
 *
 * Note that if only one such rollup set exists, the &lt;select&gt;
 * element would only contain one choice, so it would be pointless to
 * display it.  Therefore, in this instance, no &lt;select&gt; element
 * will be generated.  In practical terms, this means that this script
 * will generate <b>absolutely nothing</b> for most users!  This
 * script will generate nothing until a user creates their first
 * rollup set - then drop-down boxes will magically appear on all
 * their Plan Summary Forms, giving them a choice for their To Date
 * data.
 */
public class RollupDatasetSelectElem extends TinyCGIBase {

    private static Resources resources = Resources.getDashBundle("Templates");

    protected void writeContents() throws IOException {
        DataRepository data = getDataRepository();   if (data == null) return;
        init(data);

        // get the [Use_Rollup] data element for the current
        // project. If it is null, return immediately.
        String prefix = getPrefix();   if (prefix == null) return;
        String useRollupName = DataRepository.createDataName(prefix, "Use_Rollup");
        ListData rollupIDs = getList(data, useRollupName);
        if (rollupIDs == null) return;

        String tableStart = TABLE_START, tableEnd = "", tableRow;
        for (int i = 0;   i < rollupIDs.size();   i++) {
            tableRow = getFragment(data, rollupIDs.get(i).toString());
            if (tableRow != null && tableRow.length() > 0) {
                out.print(tableStart);
                out.print(tableRow);
                tableStart = ""; tableEnd = TABLE_END;
            }
        }
        out.print(tableEnd);
    }

    private static final String TABLE_START = "<table>\n";
    private static final String TABLE_END   = "</table>\n";

    protected String getFragment(DataRepository data, String rollupID) {
        // check in the cache to see if there is a cached value for
        // this ID.  if so, return it.
        String cachedResult = CACHE.get(rollupID);
        if (cachedResult != null) return cachedResult;

        // lookup the data element [/ROLLUP_ID/Rollup Instance
        // List]. If it is missing or if it contains less than two
        // items, return an empty fragment (and save the empty
        // fragment to the cache).
        String instanceListName = rollupInstanceList(rollupID);
        ListData instanceList = getList(data, instanceListName);
        if (instanceList == null || instanceList.size() < 2)
            return CACHE.put(rollupID, "");

        // Construct an HTML fragment
        String prompt = resources.format("Rollup_Select_Prompt_FMT", rollupID);
        StringBuffer result = new StringBuffer();
        result.append("<tr><td>").append(HTMLUtils.escapeEntities(prompt))
            .append("&nbsp;</td>\n    <td colspan=10><select name='[")
            .append(rollupPrefix(rollupID)).append("]s'>");
        for (int i = 0;   i < instanceList.size();   i++) {
            String opt = HTMLUtils.escapeEntities((String) instanceList.get(i));
            result.append("\n<option value=\"").append(opt).append("\">")
                    .append(opt);
        }
        result.append("\n</select></td></tr>\n");
        return CACHE.put(rollupID, result.toString());
    }

    private ListData getList(DataRepository data, String dataName) {
        SimpleData d = data.getSimpleValue(dataName);
        if (d instanceof ListData)
            return (ListData) d;
        else if (d instanceof StringData)
            return ((StringData) d).asList();
        else
            return null;
    }


    private static Cache CACHE = null;

    protected void init(DataRepository data) {
        if (CACHE == null) CACHE = new Cache(data);
    }

    private class Cache implements DataListener {
        /** Maps rollupID to stored HTML fragments */
        private Hashtable contents = new Hashtable();
        private DataRepository data;

        public Cache(DataRepository data) { this.data = data; }

        /** Return an entry from the cache. */
        public String get(String rollupID) {
            return (String) contents.get(rollupID);
        }

        /** Save the given fragment in the cache. (Also registers as a
         * DataListener on the appropriate rollup instance list, so
         * it can invalidate this cache entry if the instance list changes.)
         */
        public String put(String rollupID, String fragment) {
            if (!contents.containsKey(rollupID))
                data.addDataListener(rollupInstanceList(rollupID),
                                     this, false);

            contents.put(rollupID, fragment);
            return fragment;
        }

        /**
         * Invalidate the cache entry for the rollupID associated with
         * the given rollup instance list data element, whose value
         * just changed.
         */
        public void dataValueChanged(DataEvent e) {
            contents.remove(rollupID(e.getName()));
        }

        /**
         * Procedure called to notify the DataListener that several
         * data values have changed.
         */
        public void dataValuesChanged(Vector v) {
            if (v == null || v.size() == 0) return;
            for (int i = v.size();  i-- > 0; )
                dataValueChanged((DataEvent) v.elementAt(i));
        }
    }

    /** Calculate the name of a rollup instance list, from its rollup
     * ID.  This must agree with the "rollupData.txt" file in the
     * net/sourceforge/processdash/process directory.
     */
    private static String rollupInstanceList(String rollupID) {
        return "/" + rollupID + "/Rollup Instance List";
    }

    /** Calculate a rollup ID from the name of its instance list. */
    private static String rollupID(String instanceList) {
        int pos = instanceList.indexOf("/Rollup Instance List");
        if (pos == -1) return "";
        return instanceList.substring(1, pos);
    }

    /** Calculate the name of the element which holds a rollup prefix.
     */
    private static String rollupPrefix(String rollupID) {
        return rollupID + " To Date Subset Prefix";
    }
}
