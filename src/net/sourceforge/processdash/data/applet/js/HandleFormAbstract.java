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
import java.util.logging.Logger;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


abstract class HandleFormAbstract extends TinyCGIBase {

    protected void writeContents() throws IOException {
        String action = getParameter("action");
        if ("register".equals(action))
            handleRegistration();
        else if ("edit".equals(action))
            handleEdit();
        else if ("listen".equals(action))
            handleListen();
    }

    private void handleRegistration() throws IOException {
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

        handleRegistrationImpl(session);
    }

    protected abstract void handleRegistrationImpl(FormDataSession session)
            throws IOException;

    protected void registerFields(FormDataSession session) {
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

    protected String getDataPrefix() {
        String uri = getParameter("uri");
        int pos = uri.indexOf("//");
        if (pos == -1) pos = uri.indexOf("/+/");
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

    private boolean requiredTagOK(DataRepository data, String prefix) {
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

        String fieldID = getParameter("f");
        String fieldValue = getParameter("v");
        if (session != null)
            session.notifyListener(fieldID, fieldValue);

        afterHandleEdit(session);
    }

    protected abstract void afterHandleEdit(FormDataSession session);

    private void handleListen() {
        String sessionID = getParameter("s");
        FormDataSession session = FormDataSession.getSession(sessionID);
        int coupon = Integer.parseInt(getParameter("c"));
        handleListenImpl(session, coupon);
    }

    protected abstract void handleListenImpl(FormDataSession session, int coupon);

    /*
     * Reusable routines to print javascript fragments
     */

    protected void writeScriptStart() {}

    protected void writeScriptEnd() {}

    protected void writeSessionCommand(FormDataSession session) {
        writeScriptStart();
        out.write("setSessionID(");
        out.write(session.getSessionID());
        out.write(");\n");
        writeScriptEnd();
    }

    protected void writeAckCommand() {
        String msgID = getParameter("msgid");
        if (msgID != null) {
            writeScriptStart();
            out.write("ackMessage("+msgID+");\n");
            writeScriptEnd();
        }
    }

    protected void writeDoneCommand(int suggestedDelay) {
        writeScriptStart();
        out.write("messageDone("+suggestedDelay+");");
        writeScriptEnd();
    }

    protected void writeReregisterCommand() {
        writeScriptStart();
        out.write("registerData();");
        writeScriptEnd();
        out.flush();
    }

    protected void writeNoOp() {
        out.write(" \n");
        out.flush();
    }

    protected void writePrintCommand(FormDataEvent e) {
        log.entering("HandleForm", "writePrintCommand", e);

        writeScriptStart();
        out.write("paintField(");
        out.write(e.getId());
        out.write(",\"");
        out.write(escapeString(e.getValue()));
        out.write("\",");
        out.write(Boolean.toString(e.isReadOnly()));
        out.write(",");
        out.write(Integer.toString(e.getCoupon()));
        out.write(");\n");
        writeScriptEnd();
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

    private static Logger log = Logger.getLogger(HandleFormAbstract.class.getName());
}
