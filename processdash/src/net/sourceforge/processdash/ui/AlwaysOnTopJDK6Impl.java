// Copyright (C) 2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui;

import java.awt.Toolkit;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;

public class AlwaysOnTopJDK6Impl implements AlwaysOnTopHandler,
        PropertyChangeListener {

    private static final String SETTING_NAME = "window.alwaysOnTop";


    Window alwaysOnTopWindow;

    public AlwaysOnTopJDK6Impl() {
        if (!Toolkit.getDefaultToolkit().isAlwaysOnTopSupported())
            throw new UnsupportedOperationException(
                    "Always on top is not supported");
    }

    public void initialize(Window window) {
        alwaysOnTopWindow = window;
        InternalSettings.addPropertyChangeListener(SETTING_NAME, this);
        propertyChange(null);
    }

    public void dispose() {
        InternalSettings.removePropertyChangeListener(SETTING_NAME, this);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        boolean alwaysOnTop = Settings.getBool(SETTING_NAME, false);
        alwaysOnTopWindow.setAlwaysOnTop(alwaysOnTop);
    }

}
