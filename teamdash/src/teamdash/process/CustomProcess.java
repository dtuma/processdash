
package teamdash.process;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import teamdash.XMLUtils;

public class CustomProcess {

    public static final String PHASE_ITEM = "phase";
    public static final String LONG_NAME = "longName";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String READ_ONLY = "readOnly";
    public static final String SIZE_METRIC = "sizeMetric";



    public class Item {

        private String itemName;
        private Map attrs;

        public Item(String itemName) {
            this.itemName = itemName;
            this.attrs = new TreeMap();
        }

        public Item(Element xml) {
            this.itemName = xml.getTagName();
            this.attrs = new TreeMap();

            NamedNodeMap xmlAttrs = xml.getAttributes();
            for (int i=0; i < xmlAttrs.getLength();   i++) {
                Node n = xmlAttrs.item(i);
                attrs.put(n.getNodeName(), n.getNodeValue());
            }
        }

        public Map getAttributes() {
            return attrs;
        }

        public String getAttr(String name) {
            return (String) attrs.get(name);
        }

        public void putAttr(String name, String value) {
            attrs.put(name, value);
        }

        protected void writeXMLAttrs(Writer out) throws IOException {
            for (Iterator iter = attrs.entrySet().iterator(); iter.hasNext();) {
                Map.Entry e = (Map.Entry) iter.next();
                out.write((String) e.getKey());
                out.write("='");
                out.write(XMLUtils.escapeAttribute((String) e.getValue()));
                out.write("' ");
            }
        }

        protected void writeXMLSettings(Writer out) throws IOException {
            out.write("    <");
            out.write(itemName);
            out.write(" ");
            writeXMLAttrs(out);
            out.write("/>\n");
        }
    }


    protected String processName, processVersion, generatorScript;
    Map itemLists = new TreeMap();
    boolean isDirty = false;
    boolean structureChanged = false;

    /** Create a default custom process. */
    public CustomProcess() {
        List phaseList = getItemList(PHASE_ITEM);
        for (int i = 0;   i < defaultPhases.length;   i++) {
            Item phase = new Item(PHASE_ITEM);
            phase.putAttr(LONG_NAME, defaultPhases[i][0]);
            phase.putAttr(NAME, defaultPhases[i][1]);
            phase.putAttr(TYPE, defaultPhases[i][2]);
            if (defaultPhases[i][3] != null)
                phase.putAttr(SIZE_METRIC, defaultPhases[i][3]);
            if (defaultPhases[i][4] != null)
                phase.putAttr(READ_ONLY, "true");

            phaseList.add(phase);
        }

        processName = processVersion = "";
        generatorScript = "/team/lib/script-v1.xml";
    }

    /** Load a custom process from an xml settings file */
    public CustomProcess(Document xmlSettings) throws IOException {
        this(xmlSettings.getDocumentElement());
    }
    /** Load a custom process from an xml settings file */
    public CustomProcess(Element root) throws IOException {
        processName = root.getAttribute(NAME);
        processVersion = root.getAttribute("version");
        generatorScript = root.getAttribute("generator");
        if (!XMLUtils.hasValue(processName) ||
            !XMLUtils.hasValue(generatorScript)) throw new IOException();

        NodeList elements = root.getChildNodes();
        for (int i=0;   i < elements.getLength();   i++) {
            if (elements.item(i) instanceof Element) {
                Element e = (Element) elements.item(i);
                List l = getItemList(e.getTagName());
                l.add(new Item(e));
            }
        }
    }

    private static final String DLD_LINES = "DLD Lines";
    private static final String RO = "t"; // uncomment to make PSP phases read-only
    // private static String RO = null;   // uncomment to make PSP phases editable
    private static String[][] defaultPhases = {
     // { "Add your own phases",    "Short Name",    "develop", null,    null },
        { "Planning",               "Planning",      "PLAN",    null,      RO },
        { "Detailed Design",        "Design",        "DLD",     DLD_LINES, RO },
        { "Detailed Design Review", "Design Review", "DLDR",    DLD_LINES, RO },
        { "Code",                   "Code",          "CODE",    null,      RO },
        { "Code Review",            "Code Review",   "CR",      null,      RO },
        { "Compile",                "Compile",       "COMP",    null,      RO },
        { "Unit Test",              "Test",          "UT",      null,      RO },
        { "Postmortem",             "Postmortem",    "PM",      null,      RO },
    };


    // Utility methods

    public Collection getErrors() {
        Set errors = new HashSet();

        if (processName == null || processName.length() == 0)
            errors.add("You must specify a name for the process.");

        Set s = new HashSet();
        Item phase;
        List phaseList = getItemList(PHASE_ITEM);
        Iterator i = phaseList.iterator();
        while (i.hasNext()) {
            phase = (Item) i.next();

            String longName = phase.getAttr(LONG_NAME);
            if (longName == null || longName.length() == 0)
                errors.add("Every phase must have a descriptive name.");
            else if (s.contains(longName))
                errors.add("There is more than one phase named \"" +
                           longName + "\". Phase names must be unique.");

            String name = phase.getAttr(NAME);
            if (name == null || name.length() == 0)
                errors.add("Every phase must have a short name.");
            else if (s.contains(name))
                errors.add("There is more than one phase named \"" +
                           name + "\". Phase names must be unique.");

            s.add(longName);
            s.add(name);
        }

        return errors;
    }

    // Mutator methods

    public String getName() { return processName; }
    public void setName(String name) { processName = makeSafe(name); }
    public String getVersion() { return processVersion; }
    public void setVersion(String version) {
        version = makeSafe(version);
        if (isLegalVersionNum(version))
            processVersion = version;
    }
    public List getItemList(String type) {
        List result = (List) itemLists.get(type);
        if (result == null) {
            result = new ArrayList();
            itemLists.put(type, result);
        }
        return result;
    }
    public Set getItemTypes() {
        return itemLists.keySet();
    }

    public String getGeneratorScript() { return generatorScript; }

    // Input/output functionality

    String getProcessID() {
        StringBuffer result = new StringBuffer();
        result.append(makeUltraSafe(processName));
        if (processVersion.length() > 0)
            result.append("-v").append(makeUltraSafe(processVersion));
        return result.toString();
    }

    public String getJarName() {
        return getProcessID() + ".zip";
    }

    public void writeXMLSettings(Writer out) throws IOException {
        out.write("<?xml version='1.0' encoding='UTF-8'?>\n");
        out.write("<custom-process name='");
        out.write(XMLUtils.escapeAttribute(processName));
        out.write("' version='");
        out.write(XMLUtils.escapeAttribute(processVersion));
        out.write("' generator='");
        out.write(XMLUtils.escapeAttribute(generatorScript));
        out.write("'>\n");
        for (Iterator iter = itemLists.values().iterator(); iter.hasNext();) {
            List items = (List) iter.next();
            Iterator i = items.iterator();
            while (i.hasNext())
                ((Item) i.next()).writeXMLSettings(out);
        }
        out.write("</custom-process>\n");
    }



    public static String makeSafe(String s) {
        if (s == null) return "";
        s = s.trim();
        // perform a round-trip through the default platform encoding.
        s = new String(s.getBytes());
        return s;
    }

    public static boolean isLegalVersionNum(String s) {
        for (int i = s.length();   i-- > 0; )
            if (-1 == "0123456789.,".indexOf(s.charAt(i)))
                return false;
        return true;
    }

    public static String makeUltraSafe(String s) {
        StringBuffer result = new StringBuffer(makeSafe(s));
        char c;
        for (int i = result.length();   i-- > 0; ) {
            c = result.charAt(i);
            if (c == '.')
                result.setCharAt(i, ',');
            else if (-1 == ULTRA_SAFE_CHARS.indexOf(c))
                result.setCharAt(i, '_');
        }
        return result.toString();
    }
    private static final String ULTRA_SAFE_CHARS =
        "abcdefghijklmnopqrstuvwxyz" +
        "0123456789" + ",_" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String bouncyCapsToUnderlines(String text) {
        if (text == null || text.length() == 0)
            return "";

        StringBuffer result = new StringBuffer();
        result.append(Character.toUpperCase(text.charAt(0)));
        boolean lastLetterWasLowerCase = false;
        for (int i=1;  i < text.length();   i++) {
            if (Character.isUpperCase(text.charAt(i))) {
                if (lastLetterWasLowerCase || nextLetterIsLowerCase(text, i))
                    result.append('_');
                lastLetterWasLowerCase = false;
            } else {
                lastLetterWasLowerCase = true;
            }
            result.append(text.charAt(i));
        }
        return result.toString();
    }

    private static boolean nextLetterIsLowerCase(String text, int i) {
        return i+1 < text.length()
        && Character.isLowerCase(text.charAt(i+1));
    }
}
