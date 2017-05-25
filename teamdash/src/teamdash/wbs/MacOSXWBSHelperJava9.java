// Copyright (C) 2017 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs;

import java.awt.Desktop;
import java.lang.reflect.Method;

import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

/**
 * This object registers the WBSEditor object to correctly handle the
 * operating-system-initiated shutdown event on Mac OS X, when running under
 * Java 9.
 *
 * Java 9 no longer includes the com.apple.eawt classes, instead providing
 * support through the java.awt.Desktop class. This object uses the latter
 * support; but makes all calls reflectively so this class can be compiled with
 * a Java 8 JDK.
 */
public class MacOSXWBSHelperJava9 {

    public MacOSXWBSHelperJava9() throws Exception {
        if (!MacGUIUtils.isMacOSX())
            throw new IllegalArgumentException("Not Mac OS X");

        Class quitStrategyClz;
        try {
            quitStrategyClz = Class.forName("java.awt.desktop.QuitStrategy");
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalArgumentException("Not Java 9");
        }

        // retrieve the enum constant for QuitStrategy.CLOSE_ALL_WINDOWS
        Method m = quitStrategyClz.getMethod("valueOf", String.class);
        Object closeAllWindows = m.invoke(null, "CLOSE_ALL_WINDOWS");

        // invoke Desktop.getDesktop().setQuitStrategy(CLOSE_ALL_WINDOWS)
        m = Desktop.class.getMethod("setQuitStrategy", quitStrategyClz);
        m.invoke(Desktop.getDesktop(), closeAllWindows);
    }

}
