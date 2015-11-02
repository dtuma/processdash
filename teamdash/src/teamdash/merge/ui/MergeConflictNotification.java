// Copyright (C) 2012-2015 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.merge.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.merge.AttributeMergeWarning;
import teamdash.merge.MergeWarning;
import teamdash.merge.ModelType;
import teamdash.team.TeamMember;
import teamdash.wbs.WBSNode;

public class MergeConflictNotification {

    private ModelType model;

    private MergeWarning mergeWarning;

    private Map<String, Object> attributes;

    private String messageKey;

    private List<String> argAttrNames;

    private LinkedHashMap<String, MergeConflictHandler> userOptions;



    public MergeConflictNotification(ModelType model,
            MergeWarning mergeWarning) {
        this.model = model;
        this.mergeWarning = mergeWarning;

        this.messageKey = model.name() + "." + mergeWarning.getKey();

        this.argAttrNames = new ArrayList<String>();
        this.attributes = new HashMap<String, Object>();
        this.userOptions = new LinkedHashMap<String, MergeConflictHandler>();

        putAttribute(MODEL_TYPE, MergeConflictDialog.resources.getString(
            model.name() + ".Model_Name"));

        this.argAttrNames.add(MAIN_NODE);
        this.argAttrNames.add(INCOMING_NODE);
        if (mergeWarning instanceof AttributeMergeWarning) {
            AttributeMergeWarning amw = (AttributeMergeWarning) mergeWarning;
            putAttribute(ATTR_NAME, amw.getAttributeName());
            putValueAttributes(amw.getBaseValue(), amw.getMainValue(),
                amw.getIncomingValue());
        }
    }

    public ModelType getModelType() {
        return model;
    }

    public MergeWarning getMergeWarning() {
        return mergeWarning;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public void setMessageArgNames(String... argAttrNames) {
        this.argAttrNames.clear();
        this.argAttrNames.addAll(Arrays.asList(argAttrNames));
    }

    public void putAttribute(String attrName, Object attrValue) {
        putAttribute(attrName, attrValue, true);
    }

    public void putAttribute(String attrName, Object attrValue,
            boolean addToArgs) {
        attributes.put(attrName, attrValue);
        if (addToArgs && !argAttrNames.contains(attrName))
            argAttrNames.add(attrName);
    }

    public void putAttributes(Object... attributes) {
        for (int i = 0; i < attributes.length; i += 2) {
            String attrName = (String) attributes[i];
            Object attrValue = attributes[i + 1];
            putAttribute(attrName, attrValue);
        }
    }

    public void putNodeAttributes(Object main, Object incoming) {
        putAttribute(MAIN_NODE, main);
        putAttribute(INCOMING_NODE, incoming);
    }

    public void putValueAttributes(Object base, Object main, Object incoming) {
        putAttribute(BASE_VAL, base);
        putAttribute(MAIN_VAL, main);
        putAttribute(INCOMING_VAL, incoming);
    }

    public <T> T getAttribute(String attrName) {
        return (T) attributes.get(attrName);
    }

    public void addUserOption(String optionKey, MergeConflictHandler handler) {
        userOptions.put(optionKey, handler);
    }

    public LinkedHashMap<String, MergeConflictHandler> getUserOptions() {
        return userOptions;
    }

    public String getAsHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append(formatDescription());
        html.append("<ul>");
        for (String userOption : getUserOptions().keySet())
            html.append("<li>").append(formatUserOption(userOption))
                    .append("</li>");
        html.append("</ul><hr></body></html>");
        return html.toString();
    }

    public static boolean definesDescription(String messageKey) {
        return definesResKey(messageKey + ".Message_HTML_FMT");
    }

    public String formatDescription() {
        return formatImpl(messageKey + ".Message_HTML_FMT");
    }

    public boolean definesMessageForUserOption(String key) {
        return definesResKey(messageKey + "." + key + "_HTML_FMT");
    }

    public String formatUserOption(String key) {
        String resKey = messageKey + "." + key + "_HTML_FMT";
        String result;
        try {
            result = formatImpl(resKey);
        } catch (MissingResourceException mre) {
            result = formatImpl(key + "_HTML_FMT");
        }

        // decorate hyperlinks with the user option key
        String hyperlink = "<a href='http://Opt/" + key + "'>";
        result = StringUtils.findAndReplace(result, "<a>", hyperlink);

        return result;
    }

    private static boolean definesResKey(String resKey) {
        try {
            MergeConflictDialog.resources.getString(resKey);
            return true;
        } catch (MissingResourceException mre) {
            return false;
        }
    }

    private String formatImpl(String resKey) {
        return MergeConflictDialog.resources.format(resKey, getMessageArgs());
    }

    private Object[] getMessageArgs() {
        Object[] result = new Object[argAttrNames.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = getMessageArg(argAttrNames.get(i));
        }
        return result;
    }

    private Object getMessageArg(String attrName) {
        Object value = getAttribute(attrName);
        if (value == null || value instanceof Date || value instanceof Number) {
            return value;
        } else if (value instanceof WBSNode) {
            WBSNode wbsNode = (WBSNode) value;
            int wbsNodeId = wbsNode.getTreeNodeID();
            return getNodeHyperlink(model.getNodeName(wbsNode), wbsNodeId);
        } else if (value instanceof TeamMember) {
            TeamMember teamMember = (TeamMember) value;
            return getNodeHyperlink(teamMember.getName(), teamMember.getId());
        } else {
            String text = value.toString();
            if (text == null)
                return null;
            else if (text.startsWith("<html>") && text.endsWith("</html>"))
                return text.substring(6, text.length()- 7);
            else
                return HTMLUtils.escapeEntities(text);
        }
    }

    private String getNodeHyperlink(String name, int id) {
        String href = "http://" + model.name() + "/" + id;
        String htmlName = HTMLUtils.escapeEntities(name);
        return "<a href='" + href + "'>" + htmlName + "</a>";
    }



    public static final String MODEL_TYPE = "modelType";

    public static final String BASE_NODE = "baseNode";

    public static final String MAIN_NODE = "mainNode";

    public static final String INCOMING_NODE = "incomingNode";

    public static final String ATTR_NAME = "attributeName";

    public static final String BASE_VAL = "baseVal";

    public static final String MAIN_VAL = "mainVal";

    public static final String INCOMING_VAL = "incomingVal";

    public static final String DISMISS = "Dismiss";

    public static final String ACCEPT = "Accept";

    public static final String OVERRIDE = "Override";

}
