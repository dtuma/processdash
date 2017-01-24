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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.util.XMLUtils;

public class PermissionSpec implements Comparable<PermissionSpec> {

    private String id;

    private Resources resources;

    private String displayName;

    private Class permissionClass;

    private PermissionEditor editor;

    private Map<String, String> defaultParams;

    private Set<String> impliedBy;

    private Set<String> implies;

    private int ordinal;

    private static final Resources UNRECOGNIZED_PERM_RESOURCES = Resources
            .getDashBundle("Permissions.Unrecognized_Permission");


    PermissionSpec(Element xml) throws Exception {
        loadID(xml);
        loadResources(xml);
        loadPermissionsClass(xml);
        loadEditor(xml);
        loadDefaultParams(xml);
        loadRelations(xml);
    }

    PermissionSpec(String id) {
        this.id = id;
        this.resources = UNRECOGNIZED_PERM_RESOURCES;
        this.displayName = resources.format("Description_FMT", id);
        this.permissionClass = UnrecognizedPermission.class;
        this.defaultParams = Collections.EMPTY_MAP;
        this.impliedBy = this.implies = Collections.EMPTY_SET;
        this.ordinal = -1;
    }

    private void loadID(Element xml) {
        // read the ID of this permission
        this.id = xml.getAttribute("id");
        if (!XMLUtils.hasValue(this.id))
            throw error("id attribute missing");
    }

    private void loadResources(Element xml) {
        // get the resource bundle we will use to produce user-readable text
        Set<String> resourceTags = getTagValues(xml, "resources");
        if (resourceTags.isEmpty())
            this.resources = UNRECOGNIZED_PERM_RESOURCES;
        else if (resourceTags.size() > 1)
            throw error("more than 1 <resources> tag was provided");
        else {
            String resBundleKey = resourceTags.iterator().next();
            try {
                this.resources = Resources.getDashBundle(resBundleKey);
            } catch (Exception e) {
                throw error("could not find resource bundle " + resBundleKey);
            }
        }

        // look up the display name of this permission
        try {
            this.displayName = xml.getAttribute("name");
            if (XMLUtils.hasValue(displayName))
                ;
            else if (resourceTags.isEmpty())
                this.displayName = id;
            else
                this.displayName = resources.getString("Display_Name");

        } catch (Exception e) {
            throw error("could not find Display_Name in resource bundle");
        }
    }

    private void loadPermissionsClass(Element xml) throws Exception {
        if (xml.hasAttribute("class")) {
            try {
                Permission p = (Permission) ExtensionManager
                        .getExecutableExtension(xml, "class", null);
                this.permissionClass = p.getClass();
            } catch (ClassCastException cce) {
                throw error("class attribute does not name a Permission class");
            } catch (Exception e) {
                throw error("can't instantiate " + xml.getAttribute("class"));
            }

        } else {
            this.permissionClass = Permission.class;
        }
    }

    private void loadEditor(Element xml) throws Exception {
        if (xml.hasAttribute("editor")) {
            try {
                this.editor = (PermissionEditor) ExtensionManager
                        .getExecutableExtension(xml, "editor", null);
            } catch (ClassCastException cce) {
                throw error(
                    "editor attribute does not name a PermissionEditor class");
            } catch (Exception e) {
                throw error("can't instantiate " + xml.getAttribute("editor"));
            }
        } else {
            this.editor = null;
        }
    }

    private void loadDefaultParams(Element xml) {
        // look for "param" tags in this xml element.
        NodeList nl = xml.getElementsByTagName("param");
        if (nl.getLength() == 0) {
            this.defaultParams = Collections.EMPTY_MAP;

        } else {
            Map<String, String> defaultParams = new HashMap<String, String>();
            for (int i = 0; i < nl.getLength(); i++) {
                Element p = (Element) nl.item(i);
                defaultParams.put(p.getAttribute("name"),
                    p.getAttribute("defaultValue"));
            }
            this.defaultParams = Collections.unmodifiableMap(defaultParams);
        }
    }

    private void loadRelations(Element xml) {
        // get a list of other permissions that we are implied by
        Set<String> impliedBy = getTagValues(xml, "parent");
        impliedBy.addAll(getTagValues(xml, "impliedBy"));
        this.impliedBy = Collections.unmodifiableSet(impliedBy);

        // get a list of other permissions that this permission implies
        this.implies = Collections
                .unmodifiableSet(getTagValues(xml, "implies"));

        // get the ordinal for sorting relative to other permissions
        this.ordinal = XMLUtils.getXMLInt(xml, "ordinal");
        if (this.ordinal == -1)
            this.ordinal = 999999;
    }

    private Set<String> getTagValues(Element xml, String tagName) {
        Set<String> result = new HashSet<String>();
        NodeList nl = xml.getElementsByTagName(tagName);
        for (int i = 0; i < nl.getLength(); i++)
            result.add(XMLUtils.getTextContents((Element) nl.item(i)));
        result.remove(null);
        return result;
    }

    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException(msg);
    }

    public String getId() {
        return id;
    }

    public Resources getResources() {
        return resources;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Class getPermissionClass() {
        return permissionClass;
    }

    public PermissionEditor getEditor() {
        return editor;
    }

    public Map<String, String> getDefaultParams() {
        return defaultParams;
    }

    public Set<String> getImpliedBy() {
        return impliedBy;
    }

    public Set<String> getImplies() {
        return implies;
    }

    public Permission createPermission(boolean inactive,
            Map<String, String> params) {
        try {
            Permission result = (Permission) getPermissionClass().newInstance();
            result.init(this, inactive, params);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public Permission createChildPermission(Permission parent) {
        try {
            Permission result = (Permission) getPermissionClass().newInstance();
            Map<String, String> params = result.getChildParams(parent);
            result.init(this, parent.isInactive(), params);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public int compareTo(PermissionSpec that) {
        int result = this.ordinal - that.ordinal;
        if (result == 0)
            result = this.id.compareTo(that.id);
        return result;
    }

}
