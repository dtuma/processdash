// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.systray;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;

public class UserSettingHandler implements PropertyChangeListener {

    private SystemTrayIcon icon;

    public UserSettingHandler(SystemTrayIcon icon) {
        this.icon = icon;

        InternalSettings.addPropertyChangeListener(
            SystemTrayManagement.DISABLED_SETTING, this);

        propertyChange(null);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        boolean disabled = Settings.getBool(
            SystemTrayManagement.DISABLED_SETTING, false);

        icon.setVisible(disabled == false);
    }

}
