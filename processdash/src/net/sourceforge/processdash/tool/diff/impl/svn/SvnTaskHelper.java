// Copyright (C) 2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.impl.svn;

import java.io.IOException;

public class SvnTaskHelper {

    private SvnTask task;

    private IOException ioe;

    private volatile boolean complete;

    public SvnTaskHelper(SvnTask task) {
        this.task = task;
        this.ioe = null;
        this.complete = false;
    }

    public synchronized void waitTillReady() throws IOException {
        InterruptedException interruptedException = null;
        if (!complete) {
            try {
                wait();
            } catch (InterruptedException e) {
                interruptedException = null;
            }
        }
        if (ioe != null)
            throw ioe;
        else if (interruptedException != null)

            throw newIOException(interruptedException);
    }

    public void execute(SvnExecutor svn) {
        IOException resultEx = null;
        try {
            task.execute(svn);
        } catch (IOException ioe) {
            resultEx = ioe;
        } catch (Exception ex) {
            resultEx = newIOException(ex);
        }
        synchronized (this) {
            this.ioe = resultEx;
            this.complete = true;
            notifyAll();
        }
    }

    private static IOException newIOException(Throwable t) {
        IOException ioe = new IOException();
        ioe.initCause(t);
        return ioe;
    }

}
