// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
// E-Mail POC:  processdash-devel@lists.sourceforge.net


import pspdash.*;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;

public class setPath extends TinyCGIBase implements Runnable {

    /** Write the CGI header. */
    protected void writeHeader() {
        out.print("Expires: 0\r\n");
        super.writeHeader();
    }



    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        DashController.checkIP(env.get("REMOTE_ADDR"));
        try {
            javax.swing.SwingUtilities.invokeAndWait(this);
        } catch (InterruptedException ie) {
        } catch (InvocationTargetException ite) {
            if (ite.getTargetException() instanceof IOException)
                throw (IOException) ite.getTargetException();
        }
        DashController.printNullDocument(out);
    }

    public void run() {
        boolean startTiming = (parameters.get("start") != null);
        String phase = (String) parameters.get("phase");
        if (DashController.setPath(getPrefix()) == false)
            startTiming = false;
        else if (phase != null) {
            if (DashController.setPhase(phase) == false)
                startTiming = false;
        }
        if (startTiming)
            DashController.startTiming();
    }

}
