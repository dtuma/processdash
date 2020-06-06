// Copyright (C) 2000-2020 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui;

import java.awt.Cursor;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.RemoteException;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.JDateTimeChooser;
import net.sourceforge.processdash.util.HTMLUtils;

public class CompletionButton extends JCheckBox implements ActionListener,
        PropertyChangeListener, DataListener {

    ProcessDashboard parent = null;
    ActiveTaskModel activeTaskModel;
    TaskNavigationSelector navSelector;
    String dataName = null;
    Resources resources;
    Date dateToEdit;

    public CompletionButton(ProcessDashboard dash, ActiveTaskModel taskModel) {
        super();
        PCSH.enableHelp(this, "CompletionButton");
        parent = dash;
        activeTaskModel = taskModel;
        activeTaskModel.addPropertyChangeListener(this);

        resources = Resources.getDashBundle("ProcessDashboard");
        setMargin(new Insets(0, 2, 0, 2));
        addActionListener(this);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                checkForCtrlClick(e);
            }});
        updateAll();
    }

    public void setNavSelector(TaskNavigationSelector navSelector) {
        this.navSelector = navSelector;
    }

    public void updateAll() {
        if (dataName != null)
            parent.getData().removeDataListener(dataName, this);

        String path = activeTaskModel.getPath();
        dataName = DataRepository.createDataName(path, "Completed");
        parent.getData().addDataListener(dataName, this, false);

        update();
    }

    public void update() {
        Object d = parent.getData().getValue(dataName);
        if (d == null) {
            setEnabled(true);
            setSelected(false);
            setToolTip(resources.getString("Completion_Button_Tooltip"));
        } else if (d instanceof DateData) {
            setEnabled(true);
            setSelected(true);
            setToolTip(((DateData) d).formatDate());
        } else {
            setSelected(false);
            setEnabled(false);
            setToolTipText(null);
        }
        if (Settings.isReadOnly())
            setEnabled(false);
    }

    private void setToolTip(String tip) {
        String htmlTip = "<html>" + HTMLUtils.escapeEntities(tip) + "<br/>"
            + resources.getHTML("Completion_Button_Edit_Tooltip") + "</html>";
        setToolTipText(htmlTip);
    }

    private void checkForCtrlClick(MouseEvent e) {
        if (e.isControlDown()) {
            Object d = parent.getData().getSimpleValue(dataName);
            if (d instanceof DateData)
                dateToEdit = ((DateData) d).getValue();
            else
                dateToEdit = new Date();
        } else {
            dateToEdit = null;
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (dateToEdit == null) {
            setCompletionDate(isSelected() ? new DateData() : null);

        } else {
            // The click on the checkbox will have caused its selection status
            // to toggle. Reset its appearance to match underlying data while
            // the user edits the date.
            update();

            // display a dialog to edit the completion date/time
            JDateTimeChooser dtc = new JDateTimeChooser(dateToEdit);
            dtc.getJCalendar().setMaxSelectableDate(new Date());
            int userChoice = JOptionPane.showConfirmDialog(parent, dtc,
                resources.getString("Completion_Button_Edit_Title"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            // if the user clicked OK, save the selected completion date/time
            if (userChoice == JOptionPane.OK_OPTION)
                setCompletionDate(new DateData(dtc.getDateTime(), true));
        }
    }

    private void setCompletionDate(DateData completionDate) {
        try {
            parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            if (completionDate != null) {
                boolean wasPreviouslyComplete = parent.getData()
                        .getSimpleValue(dataName) != null;
                parent.getData().userPutValue(dataName, completionDate);
                if (wasPreviouslyComplete || selectNextTask() == false) {
                    parent.pauseTimer(); // stop the timer if it is running.
                }
            } else {
                parent.getData().userPutValue(dataName, null);
            }
        } finally {
            parent.setCursor(null);
        }
    }

    private boolean selectNextTask() {
        return Settings.getBool("completionCheckbox.autoSelectNext", true)
                && Settings.getBool("userPref.completionCheckbox.autoSelectNext", true)
                && navSelector != null
                && navSelector.selectNext();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        updateAll();
    }

    public void dataValueChanged(DataEvent e) throws RemoteException {
        update();
    }

    public void dataValuesChanged(Vector v) throws RemoteException {
        update();
    }

}
