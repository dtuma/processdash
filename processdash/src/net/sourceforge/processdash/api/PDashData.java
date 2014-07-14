// Copyright (C) 2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.api;

import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * An object providing access to precalculated, derived process metrics.
 * <p>
 * The Process Dashboard contains an embedded calculation engine that computes
 * derived metrics (such as time in phase, yield, defect density, etc). This
 * object provides access to the data values produced by that computation
 * engine.
 * <p>
 * This object can be retrieved from a {@link PDashContext}, and will return
 * data for the project named by that context. If the context does not name a
 * real project in the current dashboard, this object is unlikely to return any
 * meaningful values.
 */
public interface PDashData extends Map<String, Object> {

    /**
     * Retrieve the value of a data element.
     * <p>
     * This method (inherited from {@link Map}) is provided for convenience from
     * JSP EL expressions. It will retrieve data elements and return them as
     * Double, String, Date, or Boolean values. This allows the construction of
     * succinct EL expressions such as:
     * 
     * <pre>
     * pdash.data['Estimated Time'] / pdash.data['Time']
     * </pre>
     * 
     * When used from Java classes (such as servlets or beans), the other
     * methods in this class may be more convenient, as they provide type-safe
     * ways to access each kind of data value.
     * 
     * @param dataName
     *            the name of a data element
     * @return the value of the named data element, or null if the element does
     *         not exist.
     * 
     */
    Object get(Object dataName);

    /**
     * Retrieve a data value as a number.
     * 
     * @param dataName
     *            the name of a data element
     * @return the numeric value of the named data element. If no data element
     *         with this value is present, returns null. If the named data
     *         element is not a number, returns {@link Double#NaN}.
     */
    Double getNumber(String dataName);

    /**
     * Retrieve a data value preformatted as text.
     * 
     * @param dataName
     *            the name of a data element
     * @return the textual value of the named data element. If no data element
     *         with this value is present, returns null. Numbers, dates, and
     *         lists will be formatted as text.
     */
    String getString(String dataName);

    /**
     * Retrieve a data value as a Date.
     * 
     * @param dataName
     *            the name of a data element
     * @return the Date value of the named data element. If no data element with
     *         this value is present, or if the given element is not a date,
     *         returns null.
     */
    Date getDate(String dataName);

    /**
     * Retrieve a data value as a List.
     * 
     * @param dataName
     *            the name of a data element
     * @return the value of the named data element, cast to a List. If no data
     *         element with this value is present, or if the given data element
     *         is neither a List nor a String, returns null.
     */
    List getList(String dataName);

    /**
     * Retrieve a data value and test it for "trueness."
     * <p>
     * The following data values are considered to be "true:"
     * <ul>
     * <li>Numbers which are not zero, NaN, or Infinity</li>
     * <li>Strings with length greater than zero</li>
     * <li>Non-null dates</li>
     * <li>Non-empty lists</li>
     * <li>"Tag" markers</li>
     * </ul>
     * 
     * @param dataName
     *            the name of a data element
     * @return true if the named data element exists, and has a "true" value
     */
    boolean getTest(String dataName);

    /**
     * Return a data context which is a child of the current context.
     * <p>
     * The resulting "subcontext" can be used to retrieve data values from this
     * context whose names begin with the specified <tt>childName</tt> and a
     * slash. For example, the following expressions are equivalent:
     * 
     * <pre>        data.get(&quot;Code/Time&quot;) == data.getChild(&quot;Code&quot;).get(&quot;Time&quot;)
     * </pre>
     * 
     * @param childName
     *            the name of the child context
     * @return a {@link PDashData} object which will look up data elements
     *         relative to a subcontext of this object.
     */
    PDashData getChild(String childName);

}
