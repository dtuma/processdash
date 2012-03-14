// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.snippet;

import java.awt.Component;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.SAXException;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.binding.BoundForm;
import net.sourceforge.processdash.util.XMLUtils;

public class WidgetConfigurationHelper extends BoundForm {

    private String[] paramNames;

    /**
     * Create a WidgetConfigurationHelper
     * 
     * @param specUrl a URL that the XML spec can be read from
     * @param resources the resources that should be used to build the form
     * @param parameters the map of parameters that were passed to the chart
     * @param paramNames the name of parameters that represent
     *                   user-configurable settings
     */
    public WidgetConfigurationHelper(URL specUrl, Resources resources,
            Map parameters, String... paramNames) {
        this.paramNames = paramNames;
        setResources(resources);
        setConfigurationParameters(parameters);

        try {
            addFormElements(XMLUtils.parse(specUrl.openStream())
                    .getDocumentElement());
        } catch (SAXException e) {
            throw new IllegalArgumentException("Bad XML spec", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to open XML spec", e);
        }
    }

    public Component getWidgetConfigurationPane() {
        return getContainer();
    }

    public Map getConfigurationParameters() {
        return copyParameters(this, new HashMap());
    }

    public void setConfigurationParameters(Map parameters) {
        copyParameters(parameters, this);
    }

    protected Map copyParameters(Map from, Map to) {
        for (int i = 0; i < paramNames.length; i++) {
            String key = paramNames[i];
            Object value = from.get(key);
            if (value != null)
                value = value.toString();
            to.put(key, value);
        }
        return to;
    }

}
