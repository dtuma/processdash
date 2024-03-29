// Copyright (C) 2007-2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev;

import java.util.List;
import java.util.Set;

public interface TaskLabeler {

    public interface Listener {

        public void taskLabelsChanged();

    }

    public void recalculate();

    public List<String> getLabelsForTask(EVTask t);

    public Set<String> getHiddenLabels();

    public int compare(String labelA, String labelB);

    public void dispose();


    String LABELS_DATA_NAME = "Task_Labels";

    String LABEL_ORDER_DATA_NAME = "Task_Label_Sort_Order";

    String LABEL_PREFIX = "label:";

    String NO_LABEL = LABEL_PREFIX + "none";

    String LABEL_DATA_PREFIX = "label_data:";

    String LABEL_HIDDEN_MARKER = LABEL_DATA_PREFIX + "hidden";

}
