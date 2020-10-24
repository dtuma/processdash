// Copyright (C) 2020 Tuma Solutions, LLC
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

import java.awt.Image;
import java.awt.Window;
import java.lang.reflect.Method;
import java.util.List;

import com.apple.eawt.Application;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

public class WindowIconImageSetterMacOSX implements WindowIconImageSetter {

    private Image icon;

    public WindowIconImageSetterMacOSX() throws Exception {
        // only run this class on Mac OS X
        if (!MacGUIUtils.isMacOSX())
            throw new RuntimeException(
                    "This class implements logic specific to Mac OS X");
    }

    @Override
    public void init(List<? extends Image> icons) {
        this.icon = DashboardIconFactory.selectImageClosestToSize(icons, 256,
            false);
    }

    @Override
    public void setWindowIconImage(Window w) {
        if (w instanceof ProcessDashboard && icon != null) {
            // by default, use the Taskbar class (introduced in Java 9).
            // Use reflection so this will compile under Java 8
            try {
                Class taskbarClass = Class.forName("java.awt.Taskbar");
                Method getMethod = taskbarClass.getMethod("getTaskbar");
                Object taskbar = getMethod.invoke(null);
                Method setIconMethod = taskbarClass.getMethod("setIconImage",
                    Image.class);
                setIconMethod.invoke(taskbar, icon);
                return;
            } catch (Throwable t) {
            }

            // on earlier versions of Java, try the Apple EAWT class
            try {
                Application.getApplication().setDockIconImage(icon);
            } catch (Throwable t) {
            }
        }
    }

}
