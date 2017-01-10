// Copyright (C) 2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.perm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Permission {

    private PermissionSpec spec;

    private boolean inactive;

    private Map<String, String> params;

    public Permission() {}

    final void init(PermissionSpec spec, boolean inactive,
            Map<String, String> params) {
        if (this.spec != null || spec == null)
            throw new IllegalStateException();
        PermissionsManager.PERMISSION.checkPermission();

        this.spec = spec;
        this.inactive = inactive;

        Map<String, String> effParams = new HashMap();
        if (params != null) {
            effParams.putAll(params);
        } else {
            effParams.putAll(getDefaultParams());
            effParams.putAll(spec.getDefaultParams());
        }
        this.params = Collections.unmodifiableMap(effParams);
    }

    public final PermissionSpec getSpec() {
        return spec;
    }

    public final String getId() {
        return spec.getId();
    }

    public final boolean isInactive() {
        return inactive;
    }

    public final Map<String, String> getParams() {
        return params;
    }

    protected Map<String, String> getDefaultParams() {
        return Collections.EMPTY_MAP;
    }

    protected Map<String, String> getChildParams(Permission parent) {
        return getDefaultParams();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj != null && obj.getClass().equals(this.getClass())) {
            Permission that = (Permission) obj;
            return this.spec.getId().equals(that.spec.getId())
                    && this.inactive == that.inactive
                    && this.params.equals(that.params);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = spec.getId().hashCode();
        result = result * 41 ^ params.hashCode();
        if (inactive)
            result++;
        return result;
    }

    @Override
    public String toString() {
        return spec.getDisplayName();
    }

}
