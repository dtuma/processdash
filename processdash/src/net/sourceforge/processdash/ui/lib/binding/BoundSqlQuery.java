// Copyright (C) 2007-2009 Tuma Solutions, LLC
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.util.SqlResultData;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class BoundSqlQuery extends AbstractBoundQuery<Connection> {

    protected String query;

    protected static final Logger logger = Logger.getLogger(BoundSqlQuery.class
            .getName());


    public BoundSqlQuery(BoundMap map, Element xml) {
        super(map, xml, BoundSqlConnection.DEFAULT_ID);

        String query = xml.getAttribute("query");
        if (!XMLUtils.hasValue(query))
            query = XMLUtils.getTextContents(xml);

        parseQuery(query);

        recalc();
    }

    protected void parseQuery(String query) {
        Matcher m;
        while ((m = PARAM_PATTERN.matcher(query)).find()) {
            String paramName = m.group(1);
            addParameter(paramName);
            query = query.substring(0, m.start(1)) + query.substring(m.end(1));
        }
        this.query = query;
    }

    protected static final Pattern PARAM_PATTERN = Pattern
            .compile("\\?([a-zA-Z_]+)");


    @Override
    protected Object executeQuery(Connection connection,
            Object[] parameterValues) throws ErrorDataValueException {

        int sqlStep = PREPARING;
        try {
            PreparedStatement statement = connection.prepareStatement(query);

            sqlStep = SETTING_PARAMS;
            statement.clearParameters();
            for (int i = 0; i < parameterValues.length; i++)
                statement.setObject(i + 1, parameterValues[i]);

            sqlStep = EXECUTING;
            ResultSet results = statement.executeQuery();

            sqlStep = READING;
            SqlResultData resultData = new SqlResultData(results);

            return resultData;

        } catch (SQLException sqle) {
            logger.log(Level.SEVERE, LOG_MESSAGES[sqlStep], sqle);
            return SQL_ERROR_VALUE;
        }
    }

    private static final int PREPARING = 0;

    private static final int SETTING_PARAMS = 1;

    private static final int EXECUTING = 2;

    private static final int READING = 3;

    private static final String[] LOG_MESSAGES = {
            "Encountered error when preparing SQL statement",
            "Encountered error when setting parameter values",
            "Encountered error while executing statement",
            "Encountered error while reading result data", };

    private static final ErrorValue SQL_ERROR_VALUE = new ErrorValue(SQL_ERROR,
            ErrorData.SEVERE);

}
