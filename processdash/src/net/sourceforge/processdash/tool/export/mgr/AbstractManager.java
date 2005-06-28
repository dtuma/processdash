// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.tool.export.mgr;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.StringTokenizer;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.util.XMLUtils;


public abstract class AbstractManager {

    protected DataRepository data;
    protected List instructions;

    AbstractManager(DataRepository data) {
        this.data = data;
        this.instructions = new ArrayList();
    }

    protected abstract String getTextSettingName();
    protected abstract String getXmlSettingName();
    protected abstract void parseXmlInstruction(Element element);
    protected abstract void parseTextInstruction(String left, String right);

    protected void initialize() {
        boolean foundTextSetting = false;

        String userSetting = Settings.getVal(getTextSettingName());
        if (userSetting != null && userSetting.length() != 0) try {
            parseTextSetting(userSetting);
            foundTextSetting = true;
        } catch (Exception e) {
            System.err.println
                ("Couldn't understand " + getTextSettingName() + " value: '" +
                 userSetting + "'");
        }

        userSetting = Settings.getVal(getXmlSettingName());
        if (userSetting != null && userSetting.length() != 0) try {
            parseXmlSetting(userSetting);
        } catch (Exception e) {
            System.err.println
                ("Couldn't understand " + getXmlSettingName() + " value: '" +
                 userSetting + "'");
            e.printStackTrace();
        }

        if (foundTextSetting) {
            saveSetting();
            InternalSettings.set(getTextSettingName(), null);
        }
    }

    private void parseXmlSetting(String userSetting) throws Exception {
        Element doc = XMLUtils.parse(userSetting).getDocumentElement();
        NodeList instrElems = doc.getChildNodes();
        int len = instrElems.getLength();
        for (int i = 0;   i < len;   i++) {
            Node n = instrElems.item(i);
            if (n instanceof Element)
                parseXmlInstruction((Element) n);
        }
    }

    protected void parseTextSetting(String userSetting) {
        StringTokenizer tok = new StringTokenizer(userSetting, "|;");
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            int separatorPos = token.indexOf("=>");
            if (separatorPos == -1) continue;

            String left = token.substring(0, separatorPos);
            String right = token.substring(separatorPos+2);
            parseTextInstruction(left, right);
        }
    }

    protected void saveSetting() {
        String value = null;
        if (!instructions.isEmpty()) {
            StringBuffer result = new StringBuffer();
            result.append("<list>");
            for (Iterator i = instructions.iterator(); i.hasNext();) {
                AbstractInstruction instr = (AbstractInstruction) i.next();
                instr.getAsXML(result);
            }
            result.append("</list>");
            value = result.toString();
        }
        System.out.println("saving setting: " + value);
        InternalSettings.set(getXmlSettingName(), value);
    }

}
