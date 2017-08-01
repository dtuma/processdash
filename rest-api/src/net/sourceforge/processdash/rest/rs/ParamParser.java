// Copyright (C) 2017 Tuma Solutions, LLC
// REST API Add-on for the Process Dashboard
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

package net.sourceforge.processdash.rest.rs;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.processdash.rest.to.JsonDate;

public class ParamParser<T> {

    public static final Object REQUIRED = null;

    public static final ParamParser<Integer> INTEGER = new ParamParser(
            Integer.class);

    public static final ParamParser<Double> DOUBLE = new ParamParser(
            Double.class);

    public static final ParamParser<JsonDate> DATE = new ParamParser(
            JsonDate.class);


    private Method valueOfMethod;

    public ParamParser(Class<T> clazz) {
        try {
            this.valueOfMethod = clazz.getMethod("valueOf", String.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public T parse(String s) {
        if (s == null || s.equals("null") || s.trim().length() == 0)
            return null;

        try {
            return (T) valueOfMethod.invoke(null, s);
        } catch (Exception e) {
            if (e.getCause() instanceof HttpException) {
                throw (HttpException) e.getCause();
            } else {
                throw HttpException.badRequest().causedBy(e.getCause());
            }
        }
    }

    public T parse(HttpServletRequest req, String paramName) {
        return parse(req.getParameter(paramName));
    }

    public T parse(HttpServletRequest req, String paramName, T defaultValue) {
        return parse(req.getParameter(paramName), defaultValue);
    }

    public T parse(String s, T defaultValue) {
        T result = parse(s);
        if (result != null)
            return result;
        else if (defaultValue != REQUIRED)
            return defaultValue;
        else
            throw HttpException.badRequest();
    }

}
