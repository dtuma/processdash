
package teamdash.process;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import teamdash.XMLUtils;

public class CustomProcess extends AbstractTableModel {

    public static String[] PHASE_TYPES = {
        "MGMT", "STRAT", "PLAN", "REQ", "STP", "REQINSP", "HLD", "ITP",
        "HLDRINSP", "DLD", "DLDR", "TD", "DLDINSP", "CODE", "CR", "COMP",
        "CODEINSP", "UT", "PM", "IT", "ST", "DOC", "AT", "PL" };


    public class CustomPhase {
        public String longName;
        public String name;
        public String type;
        public boolean readOnly;
        public boolean inserted = false;
        public CustomPhase(String longName, String name,
                           String type, boolean readOnly) {
            this.longName = longName;
            this.name = name;
            this.type = type;
            this.readOnly = readOnly;
        }
        CustomPhase(String[] args) {
            this(args[0], args[1], args[2], args[3] != null);
        }
        public CustomPhase() {
            this("Enter Phase Name", "Short Name", "MGMT", false);
            inserted = true;
        }
        public CustomPhase(Element xml) throws IOException {
            longName = xml.getAttribute("longName");
            name = xml.getAttribute("name");
            type = xml.getAttribute("type");
            readOnly = "true".equals(xml.getAttribute("readOnly"));
            if (!XMLUtils.hasValue(longName) ||
                !XMLUtils.hasValue(name) ||
                !XMLUtils.hasValue(type))
                throw new IOException("Invalid CustomPhase settings");
        }
        public void writeXMLSettings(Writer out) throws IOException {
            out.write("    <phase longName='");
            out.write(XMLUtils.escapeAttribute(longName));
            out.write("' name='");
            out.write(XMLUtils.escapeAttribute(name));
            out.write("' type='");
            out.write(XMLUtils.escapeAttribute(type));
            out.write("' ");
            if (readOnly)
                out.write("readOnly='true' ");
            out.write("/>\n");
        }
    }


    protected String processName, processVersion, generatorScript;
    ArrayList phaseList = new ArrayList();
    boolean isDirty = false;
    boolean structureChanged = false;

    /** Create a default custom process. */
    public CustomProcess() {
        for (int i = 0;   i < defaultPhases.length;   i++)
            phaseList.add(new CustomPhase(defaultPhases[i]));
        processName = processVersion = "";
        generatorScript = "/team/lib/script-v1.xml";
    }

    /** Load a custom process from an xml settings file */
    public CustomProcess(Document xmlSettings) throws IOException {
        this(xmlSettings.getDocumentElement());
    }
    /** Load a custom process from an xml settings file */
    public CustomProcess(Element root) throws IOException {
        processName = root.getAttribute("name");
        processVersion = root.getAttribute("version");
        generatorScript = root.getAttribute("generator");
        if (!XMLUtils.hasValue(processName) ||
            !XMLUtils.hasValue(generatorScript)) throw new IOException();

        NodeList phases = root.getElementsByTagName("phase");
        for (int i=0;   i < phases.getLength();   i++)
            phaseList.add(new CustomPhase((Element) phases.item(i)));
    }

//  private static String RO = "t"; // uncomment to make PSP phases read-only
    private static String RO = null; // uncomment to make PSP phases editable
    public static String[][] defaultPhases = {
        { "Management and Miscellaneous", "Misc",          "MGMT",     null },
        { "Launch and Strategy",          "Strategy",      "STRAT",    null },
        { "Planning",                     "Planning",      "PLAN",     RO   },
        { "Requirements",                 "Reqts",         "REQ",      null },
        { "System Test Plan",             "Sys Test Plan", "STP",      null },
        { "Requirements Inspection",      "Reqts Inspect", "REQINSP",  null },
        { "High-Level Design",            "HLD",           "HLD",      RO   },
        { "Integration Test Plan",        "Int Test Plan", "ITP",      null },
        { "HLD Review",                   "HLD Review",    "HLDRINSP", RO   },
        { "HLD Inspection",               "HLD Inspect",   "HLDRINSP", null },
        { "Detailed Design",              "Design",        "DLD",      RO   },
        { "Detailed Design Review",       "Design Review", "DLDR",     RO   },
        { "Test Development",             "Test Devel",    "TD",       null },
        { "Detailed Design Inspection",   "Design Inspect", "DLDINSP", null },
        { "Code",                         "Code",          "CODE",     RO   },
        { "Code Review",                  "Code Review",   "CR",       RO   },
        { "Compile",                      "Compile",       "COMP",     RO   },
        { "Code Inspection",              "Code Inspect",  "CODEINSP", null },
        { "Unit Test",                    "Test",          "UT",       RO   },
        { "Build and Integration Test",   "Int Test",      "IT",       null },
        { "System Test",                  "Sys Test",      "ST",       null },
        { "Documentation",                "Documentation", "DOC",      null },
        { "Acceptance Test",              "Accept Test",   "AT",       null },
        { "Postmortem",                   "Postmortem",    "PM",       RO   },
        { "Product Life",                 "Product Life",  "PL",       null }
    };

    // Utility methods

    public Collection getErrors() {
        Set errors = new HashSet();

        if (processName == null || processName.length() == 0)
            errors.add("You must specify a name for the process.");

        Set s = new HashSet();
        CustomPhase phase;
        Iterator i = phaseList.iterator();
        while (i.hasNext()) {
            phase = (CustomPhase) i.next();

            if (phase.longName == null || phase.longName.length() == 0)
                errors.add("Every phase must have a descriptive name.");
            else if (s.contains(phase.longName))
                errors.add("There is more than one phase named \"" +
                           phase.longName + "\". Phase names must be unique.");

            if (phase.name == null || phase.name.length() == 0)
                errors.add("Every phase must have a short name.");
            else if (s.contains(phase.name))
                errors.add("There is more than one phase named \"" +
                           phase.name + "\". Phase names must be unique.");

            s.add(phase.longName);
            s.add(phase.name);
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
    public Iterator getPhaseIterator() { return phaseList.iterator(); }
    public String getGeneratorScript() { return generatorScript; }

    public void insertPhase(int pos) {
        CustomPhase newPhase = new CustomPhase();
        newPhase.inserted = true;
        phaseList.add(pos, newPhase);
        isDirty = true;
        fireTableRowsInserted(pos, pos);
    }

    public void deletePhase(int pos) {
        if (pos >= 0 && pos < phaseList.size()) {
            CustomPhase deletedPhase = get(pos);
            phaseList.remove(pos);
            isDirty = true;
            if (!deletedPhase.inserted)
                structureChanged = true;
            fireTableRowsDeleted(pos, pos);
        }
    }

    public void movePhaseUp(int pos) {
        if (pos > 0 && pos < phaseList.size()) {
            Object temp = phaseList.get(pos);
            phaseList.set(pos, phaseList.get(pos-1));
            phaseList.set(pos-1, temp);
            isDirty = true;
            fireTableRowsUpdated(pos-1, pos);
        }
    }
    public CustomPhase get(int pos) {
        return (CustomPhase) phaseList.get(pos);
    }

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
        Iterator i = phaseList.iterator();
        while (i.hasNext())
            ((CustomPhase) i.next()).writeXMLSettings(out);
        out.write("</custom-process>\n");
    }

    // TableModel interface methods

    private static final String[] columnNames = {
        "Descriptive Name", "Short Name", "Type" };

    public int getRowCount() { return phaseList.size(); }
    public int getColumnCount() { return columnNames.length; }
    public Class getColumnClass(int columnIndex) { return String.class; }
    public String getColumnName(int col) { return columnNames[col]; }
    public Object getValueAt(int row, int column) {
        CustomPhase phase = get(row);
        switch (column) {
        case 0: return phase.longName;
        case 1: return phase.name;
        case 2: default: return phase.type;
        }
    }
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return ! get(rowIndex).readOnly;
    }
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        CustomPhase phase = get(rowIndex);
        if (phase.readOnly) return;

        String value = makeSafe(String.valueOf(aValue));
        if (value.indexOf('/') != -1) return;

        switch (columnIndex) {
        case 0: phase.longName = value; break;
        case 1: phase.name = value;     structureChanged = true; break;
        case 2: phase.type = value;     break;
        }
        isDirty = true;
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


}
