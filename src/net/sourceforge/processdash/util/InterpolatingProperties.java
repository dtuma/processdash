// Copyright (C) 2017 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;

import java.util.Properties;

import net.sourceforge.processdash.util.StringMapper;
import net.sourceforge.processdash.util.StringUtils;

/**
 * An extension of Properties which automatically expands <tt>${variable}</tt>
 * references within property values.
 * 
 * @since 2.4.2
 */
public class InterpolatingProperties extends Properties {

    private StringMapper raw, interpolating;

    public InterpolatingProperties() {
        this(null);
    }

    public InterpolatingProperties(Properties defaults) {
        super(defaults);

        // create a string mapper that looks up raw (uninterpolated) values
        this.raw = new StringMapper() {
            public String getString(String str) {
                return InterpolatingProperties.super.getProperty(str);
            }
        };

        // create a string mapper that interpolates values while retrieving them
        this.interpolating = new StringMapper() {
            public String getString(String str) {
                return getProperty(str);
            }
        };
    }

    @Override
    public String getProperty(String key) {
        String rawResult = super.getProperty(key);
        return StringUtils.interpolate(raw, rawResult);
    }

    public String getProperty(String key, StringMapper escape) {
        String rawResult = super.getProperty(key);
        StringMapper map = StringUtils.concat(escape, interpolating);
        return StringUtils.interpolate(map, rawResult);
    }

}
