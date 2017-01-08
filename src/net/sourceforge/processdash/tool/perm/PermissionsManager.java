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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.templates.ExtensionManager;

public class PermissionsManager {

    public static final DashboardPermission PERMISSION = new DashboardPermission(
            "permissionsManager.config");

    private static final Logger logger = Logger
            .getLogger(PermissionsManager.class.getName());

    private static final PermissionsManager INSTANCE = new PermissionsManager();

    public static PermissionsManager getInstance() {
        return INSTANCE;
    }

    private Map<String, PermissionSpec> specs;

    private PermissionsManager() {}

    public void init() {
        PERMISSION.checkPermission();

        this.specs = Collections.unmodifiableMap(loadPermissionSpecs());
    }

    private Map loadPermissionSpecs() {
        Map result = new HashMap();
        for (Element xml : ExtensionManager
                .getXmlConfigurationElements("permission")) {
            try {
                PermissionSpec oneSpec = new PermissionSpec(xml);

                if (result.containsKey(oneSpec.getId()))
                    logger.severe("Conflicting definitions were provided "
                            + "for the permission with id '" + oneSpec.getId()
                            + "'; discarding the definition in "
                            + ExtensionManager
                                    .getDebugDescriptionOfSource(xml));
                else
                    result.put(oneSpec.getId(), oneSpec);

            } catch (Exception e) {
                logger.warning("Could not create permission with id '"
                        + xml.getAttribute("id") + "' in "
                        + ExtensionManager.getDebugDescriptionOfSource(xml)
                        + ": " + e.getMessage());
            }
        }
        return result;
    }

    public List<PermissionSpec> getChildSpecsFor(PermissionSpec parent) {
        List<PermissionSpec> result = new ArrayList<PermissionSpec>();
        for (PermissionSpec oneSpec : specs.values()) {
            boolean isChild;
            if (parent == null)
                isChild = oneSpec.getImpliedBy().isEmpty();
            else
                isChild = oneSpec.getImpliedBy().contains(parent.getId())
                        || parent.getImplies().contains(oneSpec.getId());
            if (isChild)
                result.add(oneSpec);
        }
        Collections.sort(result);
        return result;
    }

}
