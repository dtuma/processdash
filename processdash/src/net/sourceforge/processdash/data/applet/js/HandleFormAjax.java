// Copyright (C) 2011-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.applet.js;

import java.io.IOException;

import net.sourceforge.processdash.data.applet.js.FormDataSession.EventWaitDelay;

public class HandleFormAjax extends HandleFormAbstract {

    public HandleFormAjax() {
        this.charset = "UTF-8";
    }

    protected void doGet() {
        writeCSRFFence();
    }

    protected void doPost() throws IOException {
        parseFormData();
        super.doPost();
    }

    /** Write the CGI header. */
    protected void writeHeader() {
        out.print("Content-type: text/plain; charset=UTF-8\r\n");
        out.print("Expires: 0\r\n\r\n");
        out.flush();
    }

    @Override
    protected void handleRegistrationImpl(final FormDataSession session)
            throws IOException {
        writeCSRFFence();
        writeAckCommand();
        writeSessionCommand(session);
        writeDoneCommand(10);

        new Thread() {
            public void run() {
                registerFields(session);
            }
        }.start();
    }

    @Override
    protected void afterHandleEdit(FormDataSession session) {
        writeCSRFFence();
        writeAckCommand();
        if (session == null)
            writeReregisterCommand();
        writeDoneCommand(10);
    }

    @Override
    protected void handleListenImpl(FormDataSession session, int coupon) {
        writeCSRFFence();
        // if the session was unrecognized, ask the client to reregister.
        if (session == null) {
            writeReregisterCommand();
            return;
        } else {
            out.write("assertSessionID(");
            out.write(session.getSessionID());
            out.write(");\n");
        }

        // if print commands were waiting in the queue, send them immediately.
        if (writePendingPrintCommands(session, coupon))
            return;

        // if no commands were waiting in the queue, wait for one to arrive.
        EventWaitDelay delay = parameters.containsKey("poll")
                ? EventWaitDelay.AUTO : EventWaitDelay.LONG;
        FormDataEvent e = session.getNextEvent(coupon, delay);
        if (e == null) {
            // if no print command arrived, print a no-op.
            writeNoOp();
        } else {
            // if a print command arrived, send it to the client. Then pause
            // for a fraction of a second for other calculations to arrive,
            // and then send everything that is present in the queue.
            writePrintCommand(e);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {
            }
            writePendingPrintCommands(session, e.coupon);
        }
    }

    /**
     * If any print commands are waiting in the queue, send them to the client
     * and return true. If no commands are present in the queue, return false
     * immediately without waiting.
     */
    private boolean writePendingPrintCommands(FormDataSession session,
            int coupon) {
        boolean foundData = false;

        FormDataEvent e;
        do {
            e = session.getNextEvent(coupon, EventWaitDelay.NONE);
            if (e != null) {
                foundData = true;
                writePrintCommand(e);
                coupon = e.getCoupon();
            }
        } while (e != null);

        return foundData;
    }


    protected void writeCSRFFence() {
        // write a poison pill to help protect against cross-site request
        // forgery attacks
        out.write("alert('XSS!'); while(1); // CUTCUTCUT\n");
    }

}
