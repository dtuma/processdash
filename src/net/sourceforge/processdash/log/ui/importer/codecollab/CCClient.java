// Copyright (C) 2021 Tuma Solutions, LLC
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

import org.apache.xmlrpc.client.XmlRpcClient;

public abstract class CCClient<T> {

    private T client;

    public T getClient() {
        return client;
    }

    public abstract Object executeQuery(CCAbstractQuery query,
            Object[] parameterValues) throws Exception;


    public static class XmlRpc extends CCClient<XmlRpcClient> {

        public XmlRpc(XmlRpcClient client) {
            super.client = client;
        }

        @Override
        public Object executeQuery(CCAbstractQuery query,
                Object[] parameterValues) throws Exception {
            return query.executeQuery((XmlRpcClient) getClient(),
                parameterValues);
        }
    }


    public static class Json extends CCClient<CCJsonClient> {

        public Json(CCJsonClient client) {
            super.client = client;
        }

        @Override
        public Object executeQuery(CCAbstractQuery query,
                Object[] parameterValues) throws Exception {
            return query.executeQuery((CCJsonClient) getClient(),
                parameterValues);
        }
    }

}
