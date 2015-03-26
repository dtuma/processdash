// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib.chart;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

public class DiscItemRecord {

    private Comparable key;

    private double x;

    private double y;

    private double r;

    private Ellipse2D.Double location;

    public DiscItemRecord(Comparable key, double r) {
        this.key = key;
        this.r = r;
    }

    public Comparable getKey() {
        return key;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
        this.location = null;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
        this.location = null;
    }

    public double getR() {
        return r;
    }

    public void setTranslation(double dx, double dy, double scale) {
        if (r > 0) {
            double rr = r * scale;
            location = new Ellipse2D.Double(x * scale + dx - rr,
                    y * scale + dy - rr, rr * 2, rr * 2);
        } else {
            location = null;
        }
    }

    public Ellipse2D.Double getLocation() {
        return location;
    }

    public Rectangle2D.Double getBounds() {
        return new Rectangle2D.Double(x - r, y - r, r * 2, r * 2);
    }

}
