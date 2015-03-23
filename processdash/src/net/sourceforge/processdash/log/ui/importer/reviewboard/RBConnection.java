// Copyright (C) 2013 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui.importer.reviewboard;

import java.net.MalformedURLException;

import org.w3c.dom.Element;

import net.sourceforge.processdash.ui.lib.binding.AbstractBoundConnection;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.ui.lib.binding.DynamicAttributeValue;
import net.sourceforge.processdash.ui.lib.binding.ErrorData;
import net.sourceforge.processdash.ui.lib.binding.ErrorDataValueException;

public class RBConnection extends AbstractBoundConnection<RBRestClient> {

    public static final String DEFAULT_ID = "reviewBoard";


    protected DynamicAttributeValue baseUrl;

    protected DynamicAttributeValue username;

    protected DynamicAttributeValue password;


    public RBConnection(BoundMap map, Element xml) {
        this(map, xml, DEFAULT_ID);
    }


    public RBConnection(BoundMap map, Element xml, String defaultId) {
        super(map, xml, defaultId);

        this.baseUrl = getDynamicValue(xml, "url", NO_URL);
        this.username = getDynamicValue(xml, "username", NO_USERNAME);
        this.password = getDynamicValue(xml, "password", NO_PASSWORD);
    }

    @Override
    protected void disposeConnectionImpl(RBRestClient connection) {}

    @Override
    protected RBRestClient openConnectionImpl() throws ErrorDataValueException {
        try {
            // create a client for this URL
            String baseUrl = this.baseUrl.getValue();
            RBRestClient result = new RBRestClient(baseUrl);

            // retrieve user credentials
            String username = getUsername(this.username.getValue());
            String password = getPassword(this.password.getValue());

            // authenticate
            if (result.authenticate(username, password) == false)
                throw getBadCredentialsException(username, password);

            return result;

        } catch (MalformedURLException mue) {
            throw new ErrorDataValueException(INVALID_URL, ErrorData.SEVERE);

        } catch (ErrorDataValueException edve) {
            throw edve;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
