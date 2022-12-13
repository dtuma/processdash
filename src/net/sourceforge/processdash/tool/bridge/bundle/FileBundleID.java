// Copyright (C) 2021-2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileBundleID {

    private String token;

    private String timestamp;

    private String deviceID;

    private String bundleName;


    public String getToken() {
        return token;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public String getBundleName() {
        return bundleName;
    }


    public FileBundleID(String token) {
        // if we were given a filename, discard the suffix
        int dotPos = token.indexOf('.');
        if (dotPos != -1)
            token = token.substring(0, dotPos);

        // use the regular expression pattern to break the token into parts
        Matcher m = validateAndParseToken(token);
        this.token = token;
        this.timestamp = m.group(1);
        this.deviceID = m.group(2);
        this.bundleName = m.group(3);
    }


    public FileBundleID(long timestamp, FileBundleTimeFormat timeFormat,
            String deviceID, String bundleName) {
        this(new Date(timestamp), timeFormat, deviceID, bundleName);
    }


    public FileBundleID(Date timestamp, FileBundleTimeFormat timeFormat,
            String deviceID, String bundleName) {
        // reject null values
        if (timestamp == null || deviceID == null || bundleName == null)
            throw new NullPointerException();

        // store values into fields
        this.timestamp = timeFormat.format(timestamp);
        this.deviceID = deviceID;
        this.bundleName = filenameToBundleName(bundleName);
        this.token = this.timestamp + "-" + deviceID + "-" + this.bundleName;

        // if the resulting token doesn't match expectations, reject it
        validateAndParseToken(this.token);
    }


    private Matcher validateAndParseToken(String token) {
        Matcher m = TOKEN_PAT.matcher(token);
        if (m.matches())
            return m;
        else
            throw new IllegalArgumentException(
                    "Bad file bundle ID '" + token + "'");
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FileBundleID) {
            FileBundleID that = (FileBundleID) obj;
            return this.token.equals(that.token);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }

    @Override
    public String toString() {
        return getToken();
    }


    public static String filenameToBundleName(String filename) {
        return filename.replace('.', ',').replace('/', '$').replace('\\', '$');
    }

    public static String bundleNameToFilename(String bundleName) {
        return bundleName.replace(',', '.').replace('$', '/');
    }


    public static List<FileBundleID> list(Collection<String> tokens) {
        List<FileBundleID> result = new ArrayList<FileBundleID>();
        if (tokens != null) {
            for (String oneToken : tokens) {
                try {
                    result.add(new FileBundleID(oneToken));
                } catch (IllegalArgumentException iae) {
                }
            }
        }
        return result;
    }


    private static final Pattern TOKEN_PAT = Pattern.compile("" //
            + "^(\\d{8}-\\d{6})-"         // group 1: timestamp
            + "(" + DeviceID.REGEX + ")-" // group 2: device ID
            + "([^. ]+)");                // group 3: bundle name

    public static final int TIMESTAMP_LEN = 15;


    /** Sorts bundle IDs in chronological order from oldest to newest */
    public static final Comparator<FileBundleID> CHRONOLOGICAL_ORDER = new Comparator<FileBundleID>() {
        public int compare(FileBundleID a, FileBundleID b) {
            return a.getToken().compareTo(b.getToken());
        }
    };

    /** Sorts bundle IDs in reverse chronological order from newest to oldest */
    public static final Comparator<FileBundleID> REVERSE_ORDER = new Comparator<FileBundleID>() {
        public int compare(FileBundleID a, FileBundleID b) {
            return b.getToken().compareTo(a.getToken());
        }
    };

}
