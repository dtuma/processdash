// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash;


/**
 * <P>  The <B>ProfTimer</B> class is used for crude profiling.
 * <P>
 */
public class ProfTimer {

    String name;
    long lastTime;
    boolean print = true;

    private void init(String name, boolean print) {
        int idnum = (int) (Math.random() * 10000.0);
        this.name = name + "(" + idnum + "): ";
        lastTime = System.currentTimeMillis();
        this.print = print;
        if (print)
            System.out.println(this.name + "starting.");
    }
    public ProfTimer(String name) {
        init(name, true);
    }
    public ProfTimer(String name, boolean print) {
        init(name, print);
    }

    public void click( String msg )
    {
        long currTime = System.currentTimeMillis();
        long diff = currTime - lastTime;
        if (print)
            System.out.println(name + msg + ", took " + diff + "ms.");
        lastTime = currTime;
    }
}
