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

package net.sourceforge.processdash.log.ui.importer.codecollab;

import net.sourceforge.processdash.ui.lib.binding.AbstractBoundQuery;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.ui.lib.binding.ErrorData;
import net.sourceforge.processdash.ui.lib.binding.ErrorValue;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.w3c.dom.Element;

public abstract class CCAbstractQuery extends AbstractBoundQuery<XmlRpcClient> {

    public CCAbstractQuery(BoundMap map, Element xml) {
        super(map, xml, CCWebService.CC_DEFAULT_ID);
    }

    protected void addParameter(Element xml, String attrName, String defaultName) {
        String parameterName = getXmlAttr(xml, attrName, defaultName);
        addParameter(parameterName);
    }

    protected static final ErrorValue XMLRPC_ERROR_VALUE = new ErrorValue(
            XMLRPC_ERROR, ErrorData.SEVERE);

}
