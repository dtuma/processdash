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

package net.sourceforge.processdash.data.applet.js;


import java.io.IOException;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.net.http.TinyCGIStreaming;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class HandleForm extends TinyCGIBase implements TinyCGIStreaming {

    private static int REFRESH_DELAY =
        1000 * Settings.getInt("dataApplet.refreshInterval", 15);

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
        System.out.println("query="+env.get("QUERY_STRING"));

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
        prefix = HTMLUtils.urlDecode(prefix);
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
                System.out.println("\t\tNo events found.");
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
        System.out.println("\t\tpaintField("+e.getId()+","+e.getValue()+","+
                           e.isReadOnly()+")");
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

}
