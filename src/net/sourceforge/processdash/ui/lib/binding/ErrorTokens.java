// Copyright (C) 2007-2021 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib.binding;

public interface ErrorTokens {

    int MISSING_DATA_SEVERITY = ErrorData.INFORMATION;

    String NO_URL = "URL_Missing";

    String INVALID_URL = "URL_Invalid";

    String NO_USERNAME = "Username_Missing";

    String NO_PASSWORD = "Password_Missing";

    String BAD_USERNAME_PASS = "Invalid_Username_Password";

    String NO_NETWORK = "No_Network";

    String CANNOT_CONNECT = "Cannot_Connect";

    String DATA_MISSING = "Data_Missing";

    String LOADING = "Loading";

    String NO_CONNECTION = "No_Connection";

    String SQL_ERROR = "Sql_Error";

    String REST_ERROR = "Rest_Error";

    String XMLRPC_ERROR = "XmlRpc_Error";

    String NO_VALUES_FOUND = "No_Values_Found";

}
