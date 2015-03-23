// Copyright (C) 2005-2013 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.mgr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class AbstractManager {

    private static final String SETTING_XML_HEADER =
        "<?xml version='1.1' standalone='yes' ?>";

    protected static final Resources resource = Resources
            .getDashBundle("ImportExport");

    protected DataRepository data;

    protected List instructions;

    private ManagerTableModel tableModel;

    private List instructionAdditionAuditLog;

    private List<ActionListener> listeners;

    AbstractManager() {
        this.instructions = new ArrayList();
        this.listeners = new ArrayList();
    }

    protected abstract String getTextSettingName();

    protected abstract String getXmlSettingName();

    protected abstract void parseXmlInstruction(Element element);

    protected abstract void parseTextInstruction(String left, String right);

    /** Read the settings for this manager, and possibly normalize them.
     */
    protected void initialize() {
        boolean foundTextSetting = false;

        String userSetting = Settings.getVal(getTextSettingName());
        if (userSetting != null && userSetting.length() != 0)
            try {
                parseTextSetting(userSetting);
                foundTextSetting = true;
            } catch (Exception e) {
                System.err.println("Couldn't understand "
                        + getTextSettingName() + " value: '" + userSetting
                        + "'");
            }

        parseXmlSetting();

        if (foundTextSetting) {
            saveSetting();
            InternalSettings.set(getTextSettingName(), null);
        }

        if (Settings.isFollowMode())
            InternalSettings.addPropertyChangeListener(getXmlSettingName(),
                new SettingChangeWatcher());
    }

    /** Save a reference to the data repository.
     * 
     * @param data the data repository.
     * @param handleExisting if true, handleAddedInstruction will be run
     *     on all instructions currently in the list.
     */
    protected void setData(DataRepository data, boolean handleExisting) {
        if (this.data != null)
            throw new IllegalStateException();

        this.data = data;

        if (handleExisting) {
            for (Iterator i = instructions.iterator(); i.hasNext();) {
                AbstractInstruction instr = (AbstractInstruction) i.next();
                handleAddedInstruction(instr);
            }
        }
    }

    private void parseXmlSetting() {
        String userSetting = Settings.getVal(getXmlSettingName());
        if (userSetting != null && userSetting.length() != 0) {
            try {
                parseXmlSetting(userSetting);
            } catch (Exception e) {
                System.err.println("Couldn't understand " + getXmlSettingName()
                        + " value: '" + userSetting + "'");
                e.printStackTrace();
            }
        }
    }

    private void parseXmlSetting(String userSetting) throws Exception {
        if (!userSetting.startsWith("<?xml"))
            userSetting = SETTING_XML_HEADER + userSetting;

        Element doc = XMLUtils.parse(userSetting).getDocumentElement();
        NodeList instrElems = doc.getChildNodes();
        int len = instrElems.getLength();
        for (int i = 0; i < len; i++) {
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
            if (separatorPos == -1)
                continue;

            String left = token.substring(0, separatorPos);
            String right = token.substring(separatorPos + 2);
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

    public void addListener(ActionListener l) {
        listeners.add(l);
    }

    public void removeListener(ActionListener l) {
        listeners.remove(l);
    }

    void fireEvent(int id, String actionCommand) {
        if (listeners.isEmpty())
            return;

        ActionEvent evt = new ActionEvent(this, id, actionCommand);
        for (ActionListener l : new ArrayList<ActionListener>(listeners)) {
            try {
                l.actionPerformed(evt);
            } catch (Exception e) {}
        }
    }

    public int getInstructionCount() {
        return instructions.size();
    }

    public AbstractInstruction getInstruction(int pos) {
        if (pos >= 0 && pos < instructions.size())
            return (AbstractInstruction) instructions.get(pos);
        else
            return null;
    }

    // support for adding instructions

    public void addInstruction(AbstractInstruction instr) {
        doAddInstruction(instr);
        saveSetting();
    }

    protected void doAddInstruction(AbstractInstruction instr) {
        if (instructionAdditionAuditLog != null)
            instructionAdditionAuditLog.add(instr);

        if (!instructions.contains(instr)) {
            instructions.add(instr);
            int pos = instructions.indexOf(instr);
            fireInstructionAdded(pos);
            if (data != null)
                handleAddedInstruction(instr);
        }
    }

    protected abstract void handleAddedInstruction(AbstractInstruction instr);

    // support for changing instructions

    public void changeInstruction(AbstractInstruction oldInstr,
            AbstractInstruction newInstr) {
        doChangeInstruction(oldInstr, newInstr);
        saveSetting();
    }

    private void doChangeInstruction(AbstractInstruction oldInstr,
            AbstractInstruction newInstr) {
        int pos = instructions.indexOf(oldInstr);
        if (pos == -1) {
            doAddInstruction(newInstr);
        } else {
            instructions.set(pos, newInstr);
            fireInstructionChanged(pos);
            if (data != null) {
                handleRemovedInstruction(oldInstr);
                handleAddedInstruction(newInstr);
            }
        }
    }

    // support for deleting instructions

    public void deleteInstruction(AbstractInstruction instr) {
        doDeleteInstruction(instr);
        saveSetting();
    }

    protected void doDeleteInstruction(AbstractInstruction instr) {
        int pos = instructions.indexOf(instr);
        instructions.remove(instr);
        fireInstructionRemoved(pos);
        if (data != null)
            handleRemovedInstruction(instr);
    }

    protected abstract void handleRemovedInstruction(AbstractInstruction instr);

    public TableModel getTableModel() {
        if (tableModel == null)
            tableModel = new ManagerTableModel();
        return tableModel;
    }

    protected void fireInstructionAdded(int pos) {
        if (tableModel != null) {
            tableModel.fireTableRowsInserted(pos, pos);
        }
    }

    protected void fireInstructionRemoved(int pos) {
        if (tableModel != null) {
            tableModel.fireTableRowsDeleted(pos, pos);
        }
    }

    protected void fireInstructionChanged(int pos) {
        if (tableModel != null) {
            tableModel.fireTableRowsUpdated(pos, pos);
        }
    }

    public static final String[] COLUMN_KEYS = new String[] { "Enabled",
            "Description" };

    public static final String[] COLUMN_NAMES = resource.getStrings(
            "Wizard.Columns.", COLUMN_KEYS, ".Name");

    public static final int[] COLUMN_WIDTHS = resource.getInts(
            "Wizard.Columns.", COLUMN_KEYS, ".Width_");

    private class ManagerTableModel extends AbstractTableModel {

        private static final int ENABLED_COL = 0;

        private static final int DESCR_COL = 1;

        public int getColumnCount() {
            return 2;
        }

        public Class getColumnClass(int columnIndex) {
            switch (columnIndex) {
            case ENABLED_COL:
                return Boolean.class;
            default:
                return String.class;
            }
        }

        public String getColumnName(int column) {
            if (column >= 0 && column < COLUMN_NAMES.length)
                return COLUMN_NAMES[column];
            else
                return super.getColumnName(column);
        }

        public int getRowCount() {
            return instructions.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= instructions.size())
                return null;

            AbstractInstruction instr = (AbstractInstruction) instructions
                    .get(rowIndex);

            switch (columnIndex) {
            case ENABLED_COL:
                return Boolean.valueOf(instr.isEnabled());
            case DESCR_COL:
                return instr.getDescription();
            }

            return null;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // claim that all cells are editable, so a UI can receive editing
            // gestures
            return true;
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            // do nothing - this should be handled a different way.
        }

        public void removeTableModelListener(TableModelListener l) {
            super.removeTableModelListener(l);

            // if no one is listening to us anymore, clear the reference
            // to this object so it can be garbage collected.
            Object[] listeners = getTableModelListeners();
            if (listeners == null || listeners.length == 0)
                tableModel = null;
        }
    }

    protected String getDebugContents() {
        if (instructions.isEmpty())
            return "-EMPTY-";

        StringBuffer msg = new StringBuffer();
        for (Iterator i = instructions.iterator(); i.hasNext();) {
            AbstractInstruction instr = (AbstractInstruction) i.next();
            msg.append("\n").append(instr);
        }
        return msg.substring(1);
    }

    private class SettingChangeWatcher implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            List oldInstructions = new ArrayList(instructions);
            List<AbstractInstruction> newInstructions = new ArrayList();

            try {
                instructionAdditionAuditLog = newInstructions;
                parseXmlSetting();
            } catch (Exception e) {
                return;
            } finally {
                instructionAdditionAuditLog = null;
            }

            for (Object instr : oldInstructions) {
                int instrPos = newInstructions.indexOf(instr);
                if (instrPos == -1) {
                    // the existing instruction is no longer in the list of
                    // new instructions.  Delete it.
                    doDeleteInstruction((AbstractInstruction) instr);
                } else {
                    // the existing instruction is still in the list of new
                    // instructions.  Check to see if the enabled flag has
                    // been modified;  if so, process the change.
                    AbstractInstruction oldInstr = (AbstractInstruction) instr;
                    AbstractInstruction newInstr = newInstructions.get(instrPos);
                    if (oldInstr.isEnabled() != newInstr.isEnabled())
                        doChangeInstruction(oldInstr, newInstr);
                }
            }
        }
    }

}
