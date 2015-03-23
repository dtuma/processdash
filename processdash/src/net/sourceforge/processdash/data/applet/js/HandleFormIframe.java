// Copyright (C) 2003-2011 Tuma Solutions, LLC
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import net.sourceforge.processdash.data.applet.js.FormDataSession.EventWaitDelay;
import net.sourceforge.processdash.net.http.TinyCGIStreaming;


public class HandleFormIframe extends HandleFormAbstract implements TinyCGIStreaming {


    static Hashtable BATCH_PARAMETERS = new Hashtable();
    static final String BATCH_ID = "&batchID=";
    static final String BATCH_DONE_ID = "&batchDoneID=";

    public void service(InputStream in, OutputStream out, Map env)
            throws IOException
    {
        String query = (String) env.get("QUERY_STRING");
        if (query.indexOf(BATCH_ID) != -1)
            handleQueryBatchInstruction(out, env, query);

        else {
            if (query.indexOf(BATCH_DONE_ID) != -1)
                prependQueryBatch(env, query);
            super.service(in, out, env);
        }
    }

    private void handleQueryBatchInstruction(OutputStream out, Map env,
                                             String query) throws IOException {
        int pos = query.indexOf(BATCH_ID);
        String queryPart = query.substring(0, pos);
        String batchID = extractParamVal(query, BATCH_ID);
        String messageID = extractParamVal(query, "&msgid=");

        synchronized (BATCH_PARAMETERS) {
            String existingParam = (String) BATCH_PARAMETERS.get(batchID);
            String newParam;
            if (existingParam == null)
                newParam = queryPart;
            else {
                if (queryPart.startsWith("action=")) {
                    pos = queryPart.indexOf('&');
                    queryPart = queryPart.substring(pos+1);
                }
                newParam = existingParam + queryPart;
            }
            BATCH_PARAMETERS.put(batchID, newParam);
        }

        this.out = new PrintWriter(new OutputStreamWriter(out, charset));
        parameters.clear();
        parameters.put("msgid", messageID);

        writeHeader();
        writeHTMLHeader();
        writeAckCommand();
        writeDoneCommand(10);
        writeHTMLFooter();
        this.out.flush();
    }

    private void prependQueryBatch(Map env, String query) {
        String batchID = extractParamVal(query, BATCH_DONE_ID);
        String batchedQuery = (String) BATCH_PARAMETERS.remove(batchID);
        if (batchedQuery == null) {
            // shouldn't happen!!
            return;
        } else {
            if (query.startsWith("action=")) {
                int pos = query.indexOf('&');
                query = query.substring(pos+1);
            }

            query = batchedQuery + query;
            env.put("QUERY_STRING", query);
        }
    }

    private String extractParamVal(String query, String param) {
        int beg = query.lastIndexOf(param);
        if (beg == -1) return null;

        beg += param.length();
        int end = query.indexOf('&', beg);
        if (end == -1)
            return query.substring(beg);
        else
            return query.substring(beg, end);
    }


    /** Write the CGI header. */
    protected void writeHeader() {
        out.print("Expires: 0\r\n");
        super.writeHeader();
    }

    @Override
    protected void handleRegistrationImpl(FormDataSession session)
            throws IOException {
        registerFields(session);

        writeHTMLHeader();
        writeSessionCommand(session);
        writePrintCommands(session, 0, true);
        writeHTMLFooter();
    }

    @Override
    protected void afterHandleEdit(FormDataSession session) {
        int coupon = Integer.parseInt(getParameter("c"));

        writeHTMLHeader();
        writePrintCommands(session, coupon, true);
        writeHTMLFooter();
    }

    @Override
    protected void handleListenImpl(FormDataSession session, int coupon) {
        writeHTMLHeader();
        writePrintCommands(session, coupon, false);
        writeHTMLFooter();
    }

    private void writeHTMLHeader() {
        out.write("<html><body>");
    }

    private void writeHTMLFooter() {
        out.write("</body></html>");
    }

    @Override
    protected void writeScriptStart() {
        out.write("<script>parent.");
    }

    @Override
    protected void writeScriptEnd() {
        out.write("</script>");
    }

    private void writePrintCommands(FormDataSession session, int coupon,
                                    boolean waitForCmd) {
        writeAckCommand();

        if (session == null) {
            writeReregisterCommand();
            writeDoneCommand(100);
            return;
        }

        EventWaitDelay delay = waitForCmd ? EventWaitDelay.SHORT
                : EventWaitDelay.AUTO;
        while (true) {
            FormDataEvent e = session.getNextEvent(coupon, delay);
            if (e == null) {
                log.finer("No events found.");
                break;
            } else {
                writePrintCommand(e);
                coupon = e.getCoupon();
            }
        }

        writeDoneCommand(FormDataSession.REFRESH_DELAY);
        out.flush();
    }

    private static Logger log = Logger.getLogger(HandleFormIframe.class.getName());
}
