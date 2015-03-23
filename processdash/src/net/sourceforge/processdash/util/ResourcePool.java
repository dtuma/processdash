// Copyright (C) 2001-2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.util.Stack;

public abstract class ResourcePool {

    String name;
    Stack availableResources, busyResources;

    public ResourcePool(String name) {
        this.name = name;
        availableResources = new Stack();
        busyResources      = new Stack();
    }

    protected abstract Object createNewResource();

    public synchronized Object get() {
        Object result = null;
        if (availableResources.empty()) {
            result = createNewResource();
            //int count = busyResources.size() + 1;
            //System.err.println(name + " pool contains " + count + " items.");
        } else
            result = availableResources.pop();
        if (result != null) busyResources.push(result);
        return result;
    }

    public synchronized void release(Object resource) {
        if (resource != null)
            if (busyResources.remove(resource))
                availableResources.push(resource);
    }
}
