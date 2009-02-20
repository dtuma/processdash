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

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.w3c.dom.Element;

import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.ui.lib.binding.BoundXmlRpcConnection;
import net.sourceforge.processdash.util.MD5;
import net.sourceforge.processdash.util.StringUtils;

public class CCWebService extends BoundXmlRpcConnection {

    public static final String CC_DEFAULT_ID = "codeCollaborator";

    public CCWebService(BoundMap map, Element xml) {
        super(map, xml, CC_DEFAULT_ID);
    }

    @Override
    protected boolean validateCredentials(XmlRpcClient client, String username,
            String password) throws XmlRpcException {
        if (!StringUtils.hasValue(username) || !StringUtils.hasValue(password))
            return false;

        String passwordHash;
        try {
            passwordHash = (String) CCQuerySupport.lookupSingleValue(client,
                CCQuerySupport.USER_CLASS, "password", "login", username);
        } catch (CCQuerySupport.SingleValueNotFoundException svnfe) {
            // unrecognized username
            return false;
        }

        MD5 md5 = new MD5();
        md5.Update(password);
        String localPasswordHash = md5.asHex();
        return localPasswordHash.equalsIgnoreCase(passwordHash);
    }

}
