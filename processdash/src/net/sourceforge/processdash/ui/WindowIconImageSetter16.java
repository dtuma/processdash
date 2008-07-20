// Copyright (C) 2007 Tuma Solutions, LLC
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
import java.util.List;

import net.sourceforge.processdash.util.RuntimeUtils;

public class WindowIconImageSetter16 implements WindowIconImageSetter {

    private List<? extends Image> icons;

    public WindowIconImageSetter16() {
        RuntimeUtils.assertMethod(Window.class, "setIconImages");
    }

    public void init(List<? extends Image> icons) {
        this.icons = icons;
    }

    public void setWindowIconImage(Window w) {
        w.setIconImages(icons);
    }

}
