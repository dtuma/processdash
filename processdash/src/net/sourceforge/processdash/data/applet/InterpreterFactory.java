// Copyright (C) 2000-2006 Tuma Solutions, LLC
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


package net.sourceforge.processdash.data.applet;


import java.util.regex.Pattern;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.*;


public class InterpreterFactory {

    private static boolean useHoursMinutes =
        Settings.getBool("forms.useHoursMinutes", true);

    public static DataInterpreter create(Repository r, String inputName,
                                         String prefix) {

                                  // parse the input name
        InputName n = new InputName(inputName, prefix);


                                    // if the name specified a default value,
                                    // set that default value in the repository.
        if (n.value != null && n.value.length() != 0) try {
            String defaultValue = instantiate(n.name, n.value, prefix);
            if (n.hasFlag('s')) defaultValue = "\"" + defaultValue;
            r.maybeCreateValue(n.name, defaultValue, prefix);
        } catch (RemoteException e) {}


        boolean readOnly = n.hasFlag('r');

        DataInterpreter result = null;

        if (n.hasFlag('s'))
            result = new StringInterpreter(r, n.name, readOnly);
        else if (n.hasFlag('n'))
            result = new DoubleInterpreter(r, n.name, n.digitFlag(), readOnly);
        else if (isPercentInputName(n))
            result = new PercentInterpreter(r, n.name, n.digitFlag(), readOnly);
        else if (n.hasFlag('d'))
            result = new DateInterpreter(r, n.name, readOnly);
        else if (isTimeInputName(n))
            result = new TimeInterpreter(r, n.name, n.digitFlag(), readOnly);
        else
            result = new DoubleInterpreter(r, n.name, n.digitFlag(), readOnly);

        boolean optional = !(result instanceof DoubleInterpreter);
        if (n.hasFlag('o')) optional = true;
        if (n.hasFlag('m')) optional = false;
        result.optional = optional;

        if (n.hasFlag('u')) result.unlock();
        if (n.hasFlag('!')) result.setActive(true);

        return result;
    }

    public static boolean isPercentInputName(InputName n) {
        if (n.hasFlag('%'))
            return true;
        if (n.hasFlag('s') || n.hasFlag('n') || n.hasFlag('d') || n.hasFlag('t'))
            return false;
        return isPercentInputDataName(n.relName);
    }

    public static boolean isPercentInputDataName(String relName) {
        for (int i = 0; i < PERCENT_NAMES.length; i++) {
            if (relName.indexOf(PERCENT_NAMES[i]) != -1)
                return true;
        }
        return false;
    }
    private static final String[] PERCENT_NAMES = {
        "%", "Percent", "Estimating Error", "Yield"
    };

    public static boolean isTimeInputName(InputName n) {
        if (useHoursMinutes == false)
            return false;
        if (n.hasFlag('t'))
            return true;
        return isTimeInputDataName(n.relName);
    }
    public static boolean isTimeInputDataName(String relName) {
        if (relName.indexOf("Time") != -1
                && TIME_PATTERN.matcher(relName).find()
                && !FALSE_TIME_PATTERN.matcher(relName).find())
            return true;
        return false;
    }

    private static Pattern TIME_PATTERN = Pattern.compile("\\bTime\\b");
    private static Pattern FALSE_TIME_PATTERN =
        Pattern.compile("Estimating Error|Time Ratio|/(Beta0|Beta1|R Squared)$");

    private static String instantiate(String name,
                                      String defaultValue,
                                      String prefix) {
        StringBuffer val = new StringBuffer();
        String digits;
        int pos;

        if (name.startsWith(prefix + "/"))
            name = name.substring(prefix.length()+1);

        pos = name.length();
        while ((pos > 0) && (Character.isDigit(name.charAt(pos-1)))) pos--;
        digits = name.substring(pos);

        while ((pos = defaultValue.indexOf('=')) != -1) {
            val.append(defaultValue.substring(0, pos));
            switch (defaultValue.charAt(pos+1)) {
            case '#': val.append(digits); break;
            case 'p': val.append(prefix); break;
            case 'n': val.append(name); break;
            };
            defaultValue = defaultValue.substring(pos+2);
        }

        val.append(defaultValue);

        return val.toString();
    }

}
