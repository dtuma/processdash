// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib.binding;

import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class DynamicAttributeValue {

    private BoundMap map;
    private String explicitValue;
    private String propName;
    private String errorToken;

    public DynamicAttributeValue(BoundMap map, Element xml,
            Object owner, String ownerMethod, String ownerId,
            String attrName, String errorToken) {
        this.map = map;

        if (xml.hasAttribute(attrName))
            this.explicitValue = xml.getAttribute(attrName);

        else {
            this.propName = xml.getAttribute(attrName + "Id");
            if (!StringUtils.hasValue(this.propName))
                this.propName = ownerId + "." + attrName;
            map.addPropertyChangeListener(this.propName, owner, ownerMethod);
        }

        this.errorToken = errorToken;
    }

    public DynamicAttributeValue(String explicitValue) {
        if (explicitValue == null)
            throw new NullPointerException("explicitValue cannot be null");

        this.explicitValue = explicitValue;
    }

    public String getValue() throws ErrorDataValueException {
        if (explicitValue != null)
            return explicitValue;

        Object propVal = map.get(propName);
        if (propVal instanceof String) {
            String result = (String) propVal;
            if (StringUtils.hasValue(result))
                // the map contained a valid property with a
                // non-empty value. return it.
                return result;
        }

        if (errorToken == null)
            // no data was found, but for this attribute, that isn't
            // an error. Just return null.
            return null;

        // no data was found for this attribute.
        ErrorData errorData = map.getErrorDataForAttr(propName);
        if (errorData != null)
            // if the attribute itself had associated error data,
            // propagate it along.
            throw new ErrorDataValueException(errorData);
        else
            // otherwise, use our default error token.
            throw new ErrorDataValueException(errorToken,
                ErrorTokens.MISSING_DATA_SEVERITY);
    }

}
