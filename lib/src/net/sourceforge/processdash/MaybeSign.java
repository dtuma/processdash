// Copyright (C) 2007-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash;

import java.lang.reflect.Method;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.SignJar;

/**
 * This class is designed to <b>optionally</b> sign a JAR file.
 * 
 * This task takes a prefix as an argument. It prepends that prefix to words
 * like "alias", "storepass", etc, and checks for project properties with the
 * associated names. If those properties are empty, no signing is performed. If
 * values can be read from those properties, they will be used as input to the
 * signing process.
 * 
 * @author Tuma
 * 
 */
public class MaybeSign extends SignJar {

    String prefix;

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }


    private static final String[] PROPAGATED_PROPERTIES = { "alias",
            "storepass", "keystore", "storetype", "keypass", "tsaurl",
            "tsacert", "sigfile", "strict", "executable", "sigAlg", "digestAlg",
            "TSADigestAlg" };

    @Override
    public void execute() throws BuildException {
        if (!hasValue(prefix))
            return;

        for (int i = 0; i < PROPAGATED_PROPERTIES.length; i++)
            propagateProperty(PROPAGATED_PROPERTIES[i]);

        if (!hasValue(this.alias) || !hasValue(this.storepass))
            return;

        super.execute();
    }

    protected void propagateProperty(String propName) {
        String fullPropName = prefix + "." + propName;
        String propValue = getProject().getProperty(fullPropName);
        if (propValue == null) {
            fullPropName = "signDefault." + propName;
            propValue = getProject().getProperty(fullPropName);
        }
        if (!hasValue(propValue))
            return;

        try {
            String setterMethodName = "set"
                    + propName.substring(0, 1).toUpperCase()
                    + propName.substring(1);
            Method m = getClass().getMethod(setterMethodName, String.class);
            m.invoke(this, propValue);
        } catch (NoSuchMethodException nsme) {
            getProject().log("Property '" + propName + "' not supported by " //
                    + "this version of ant; ignoring", null, Project.MSG_WARN);
        } catch (Exception e) {
            throw new BuildException("Unexpected internal error", e);
        }
    }

    private static boolean hasValue(String s) {
        return (s != null)                  // the value is non-null,
                && (s.trim().length() > 0)  // is not all whitespace, and
                && (s.indexOf("${") == -1); // has no unresolved ant variables
    }

}
