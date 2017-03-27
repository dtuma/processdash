// Copyright (C) 2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ui.icons;

import java.net.URL;

import javax.swing.Icon;

import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.lib.ScalableImageIcon;

public class TaskScheduleIcons {

    private static Icon create(int height, String... names) {
        return new ScalableImageIcon(height, TaskScheduleIcons.class, names);
    }

    private static URL url(String name) {
        return TaskScheduleIcons.class.getResource(name);
    }

    public static int getDependencyIconSize() {
        return (int) (DashboardIconFactory.getStandardIconSize() * 11 / 17f);
    }

    public static Icon checkIcon() {
        return create(getDependencyIconSize(), "check.png");
    }

    public static URL checkUrl() {
        return url("check.png");
    }

    public static Icon stopIcon() {
        return new StopIcon(getDependencyIconSize());
    }

    public static URL stopUrl() {
        return url("stop.png");
    }

    public static Icon warningIcon() {
        return create(getDependencyIconSize(), "warning.png");
    }

    public static URL warningUrl() {
        return url("warning.png");
    }

    public static Icon warningRedIcon() {
        return create(getDependencyIconSize(), "warningRed.png");
    }

    public static URL warningRedUrl() {
        return url("warningRed.png");
    }

    public static Icon groupIcon() {
        return create(getDependencyIconSize(), "group-32.png", "group-22.png",
            "group-16.png", "group-11.png");
    }

    public static URL groupUrl() {
        return url("group-32.png");
    }

    public static Icon chartOptionsIcon() {
        return create(32, "chart-options.png");
    }

    public static Icon chartOptionsGlowIcon() {
        return create(32, "chart-options-glow.png");
    }

}
