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

package net.sourceforge.processdash.tool.bridge.bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.util.ComputerName;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.StringUtils;


public class DeviceID {

    private static final int LENGTH = 25;

    static final String REGEX = "[A-Za-z0-9_]{10,}";

    private static final String ENCODING = "ASCII";


    private static String DEVICE_ID;

    public static String get() throws IOException {
        if (DEVICE_ID == null)
            DEVICE_ID = readOrCreate();

        return DEVICE_ID;
    }

    private static synchronized String readOrCreate() throws IOException {
        File storage = getStorageFile();
        String result = readFromStorage(storage);
        if (result == null)
            result = createAndStore(storage);
        return result;
    }

    private static final File getStorageFile() {
        File appDir = DirectoryPreferences.getApplicationDirectory(true);
        File metaDir = new File(appDir, "metadata");
        metaDir.mkdirs();
        return new File(metaDir, "deviceID.txt");
    }

    private static String readFromStorage(File storage) {
        try {
            if (!storage.isFile())
                return null;
            return new String(FileUtils.slurpContents( //
                new FileInputStream(storage), true), ENCODING).trim();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
    }

    private static String createAndStore(File storage) throws IOException {
        // create a new device ID
        String result = create();

        // write the device ID to storage
        RobustFileWriter out = new RobustFileWriter(storage, ENCODING);
        out.write(result);
        out.close();

        // return the new value
        return result;
    }

    private static String create() {
        // create a device ID with the current username and computer name
        StringBuilder token = new StringBuilder();
        append(token, getUsername(), 10);
        append(token, getComputerName(), 20);

        // pad with random digits to produce a fixed length token
        Random rand = new Random();
        while (token.length() < LENGTH)
            token.append((char) ('0' + rand.nextInt(10)));

        return token.toString();
    }

    private static void append(StringBuilder token, String value, int maxLen) {
        // if we have no real value to append, abort
        if (!StringUtils.hasValue(value))
            return;

        // look for letters and numbers in the value and append to our token
        for (int i = 0; i < value.length() && token.length() < maxLen; i++) {
            char c = value.charAt(i);
            if ((c >= '0' && c <= '9') || c == '_' //
                    || (c >= 'A' && c <= 'Z') //
                    || (c >= 'a' && c <= 'z')) {
                token.append(c);
            }
        }

        // make the token end with an underscore if it doesn't already.
        if (token.length() > 0 && token.charAt(token.length() - 1) != '_')
            token.append('_');
    }

    private static String getUsername() {
        return System.getProperty("user.name");
    }

    private static String getComputerName() {
        // get the name of the local computer, abort if it couldn't be found
        String name = ComputerName.getName();
        if (!StringUtils.hasValue(name))
            return null;

        // if we received "localhost" or a numeric IP address, reject it
        if (name.equals("localhost") || Character.isDigit(name.charAt(0)))
            return null;

        // return the portion of the name that precedes the first "."
        int pos = name.indexOf('.');
        return (pos == -1 ? name : name.substring(0, pos));
    }

    /**
     * Use the given tokens to create a string that would be acceptable as a
     * device ID
     */
    public static String createPseudoID(String... tokens) {
        // append the given tokens to the string, scrubbing chars as we go
        StringBuilder result = new StringBuilder();
        for (String token : tokens)
            append(result, token, LENGTH);

        // pad with deterministic digits to reach the desired length
        while (result.length() < LENGTH)
            result.append(Math.abs(result.toString().hashCode()));

        // return a string with the target length
        return result.substring(0, LENGTH);
    }

}
