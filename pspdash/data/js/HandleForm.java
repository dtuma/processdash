// Process Dashboard - Data Automation Tool for high-maturity processes
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package pspdash.data.js;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import pspdash.Settings;
import pspdash.data.SimpleData;
import pspdash.data.DataRepository;
import pspdash.TinyCGIBase;
import pspdash.StringUtils;


public class HandleForm extends TinyCGIBase {

    static int REFRESH_DELAY =
        1000 * Settings.getInt("dataApplet.refreshInterval", 15);

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

    protected void writeContents() throws IOException {
        String action = getParameter("action");
        if ("register".equals(action))
            handleRegistration();
        else if ("edit".equals(action))
            handleEdit();
        else if ("listen".equals(action))
            handleListen();
    }
    protected void handleRegistration() throws IOException {
        log.entering("HandleForm", "handleRegistration");
        log.finer("query=" + env.get("QUERY_STRING"));

        DataRepository data = getDataRepository();
        String prefix = getDataPrefix();
        boolean unlocked = getUnlocked();
        if (!requiredTagOK(data, prefix)) {
            data = null;
            prefix = "No such project OR project/process mismatch";
        }
        FormDataSession session = new FormDataSession(data, prefix, unlocked);

        registerFields(session);

        writeHTMLHeader();
        writeSessionCommand(session);
        writePrintCommands(session, 0, true);
        writeHTMLFooter();
    }

    private void registerFields(FormDataSession session) {
        int i = 0;
        while (true) {
            String name = getParameter("f"+i);
            if (name == null || name.length() == 0) break;
            String type = getFieldType(name.substring(0,1));
            name = name.substring(1);
            session.registerField(Integer.toString(i), name, type);
            i++;
        }
    }

    private String getFieldType(String typeFlag) {
        if ("c".equals(typeFlag)) return "checkbox";
        if ("s".equals(typeFlag)) return "select-one";
        if ("t".equals(typeFlag)) return "text";
        return "text";
    }

    private void writeHTMLHeader() {
        out.write("<html><body>");
    }

    private void writeHTMLFooter() {
        out.write("</body></html>");
    }


    private void writeSessionCommand(FormDataSession session) {
        out.write("<script>parent.setSessionID(");
        out.write(session.getSessionID());
        out.write(")</script>");
    }


    protected String getDataPrefix() {
        String uri = getParameter("uri");
        int pos = uri.indexOf("//");
        String prefix = "";
        if (pos != -1)
            prefix = uri.substring(0, pos);
        prefix = URLDecoder.decode(prefix);
        return prefix;
    }

    private boolean getUnlocked() {
        String uri = getParameter("uri");
        int queryPos = uri.indexOf('?');
        if (queryPos == -1) return false;
        return uri.indexOf("unlock", queryPos) != -1;
    }

    protected boolean requiredTagOK(DataRepository data, String prefix) {
        String requiredTag = getParameter("requiredTag");
        if (requiredTag == null ||
            requiredTag.length() == 0 ||
            requiredTag.equals("null"))
            return true;

        String dataName = DataRepository.createDataName(prefix, requiredTag);
        SimpleData t = data.getSimpleValue(dataName);
        return (t != null && t.test());
    }

    private void handleEdit() {
        String sessionID = getParameter("s");
        FormDataSession session = FormDataSession.getSession(sessionID);
        int coupon = Integer.parseInt(getParameter("c"));

        String fieldID = getParameter("f");
        String fieldValue = getParameter("v");
        if (session != null)
            session.notifyListener(fieldID, fieldValue);
        writePrintCommands(session, coupon, true);
    }

    private void handleListen() {
        String sessionID = getParameter("s");
        FormDataSession session = FormDataSession.getSession(sessionID);
        int coupon = Integer.parseInt(getParameter("c"));
        writePrintCommands(session, coupon, false);
    }

    private void writeAckCommand() {
        String msgID = getParameter("msgid");
        out.write("<script>parent.ackMessage("+msgID+")</script>");
    }

    private void writeDoneCommand(int suggestedDelay) {
        out.write("<script>parent.messageDone("+suggestedDelay+")</script>");
    }

    private void writePrintCommands(FormDataSession session, int coupon,
                                    boolean waitForCmd) {
        writeAckCommand();

        if (session == null) {
            writeReregisterCommand();
            writeDoneCommand(100);
            return;
        }

        int delay = REFRESH_DELAY;
        FormDataEvent e;
        while (true) {
            e = session.getNextEvent(coupon, waitForCmd || REFRESH_DELAY == 0);
            if (e == null) {
                log.finer("No events found.");
                if (REFRESH_DELAY == 0)
                    writeNoOp();
                else
                    break;
            } else {
                writePrintCommand(e);
                coupon = e.getCoupon();
            }
        }

        writeDoneCommand(delay);
        out.flush();
    }

    private void writeReregisterCommand() {
        out.write("<script>parent.registerData();</script>");
        out.flush();
    }

    private void writeNoOp() {
        out.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        out.flush();
    }

    private void writePrintCommand(FormDataEvent e) {
        log.entering("HandleForm", "writePrintCommand", e);

        out.write("<script>parent.paintField(");
        out.write(e.getId());
        out.write(",\"");
        out.write(escapeString(e.getValue()));
        out.write("\",");
        out.write(Boolean.toString(e.isReadOnly()));
        out.write(",");
        out.write(Integer.toString(e.getCoupon()));
        out.write(")</script>");
        out.flush();
    }

    private String escapeString(String s) {
        s = StringUtils.findAndReplace(s, "\\", "\\\\");
        s = StringUtils.findAndReplace(s, "\"", "\\\"");
        s = StringUtils.findAndReplace(s, "\'", "\\\'");
        s = StringUtils.findAndReplace(s, "\t", "\\t");
        s = StringUtils.findAndReplace(s, "\r", "\\r");
        s = StringUtils.findAndReplace(s, "\n", "\\n");
        return s;
    }

    private static Logger log = Logger.getLogger(HandleForm.class.getName());
}
