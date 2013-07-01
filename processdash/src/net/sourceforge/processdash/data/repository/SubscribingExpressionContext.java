// Copyright (C) 2006-2013 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.repository;

import java.util.Iterator;
import java.util.Set;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.util.LightweightSet;

/** This context looks up data on behalf of a DataListener.  Along the way,
 * it registers that DataListener for all items that were looked up.
 */
public class SubscribingExpressionContext implements ExpressionContext {

    public static final String LISTENERVAR_NAME = ".";

    private DataRepository data;

    private String prefix;

    private String listenerName;

    private DataListener listener;

    private Set currentSubscriptions;

    private Set namesSeen;

    public SubscribingExpressionContext(DataRepository data, String prefix,
            DataListener listener, String listenerName, Set currentSubscriptions) {
        this.data = data;
        this.prefix = prefix;
        this.listener = listener;
        this.listenerName = listenerName;
        this.currentSubscriptions = currentSubscriptions;
        this.namesSeen = new LightweightSet();
    }

    public SimpleData get(String dataName) {
        if (PREFIXVAR_NAME.equals(dataName))
            return StringData.create(prefix);
        else if (LISTENERVAR_NAME.equals(dataName))
            return StringData.create(listenerName);

        dataName = resolveName(dataName);

        if (!currentSubscriptions.contains(dataName)) {
            String nameListenedTo = data.addActiveDataListener(dataName,
                    listener, listenerName, false);
            currentSubscriptions.add(nameListenedTo);
            dataName = nameListenedTo;
        }

        namesSeen.add(dataName);
        return data.getSimpleValue(dataName);
    }

    public String resolveName(String dataName) {
        return DataRepository.createDataName(prefix, dataName);
    }

    public void removeOldSubscriptions() {
        synchronized (currentSubscriptions) {
            for (Iterator i = currentSubscriptions.iterator(); i.hasNext();) {
                String dataName = (String) i.next();
                if (!namesSeen.contains(dataName)) {
                    data.removeDataListener(dataName, listener);
                    i.remove();
                }
            }
        }
    }

}
