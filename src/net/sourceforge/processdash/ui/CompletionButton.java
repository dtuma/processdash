// Copyright (C) 2000-2014 Tuma Solutions, LLC
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.JCheckBox;

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

public class CompletionButton extends JCheckBox implements ActionListener,
        PropertyChangeListener, DataListener {

    ProcessDashboard parent = null;
    ActiveTaskModel activeTaskModel;
    TaskNavigationSelector navSelector;
    String dataName = null;
    Resources resources;

    public CompletionButton(ProcessDashboard dash, ActiveTaskModel taskModel) {
        super();
        PCSH.enableHelp(this, "CompletionButton");
        parent = dash;
        activeTaskModel = taskModel;
        activeTaskModel.addPropertyChangeListener(this);

        resources = Resources.getDashBundle("ProcessDashboard");
        setMargin(new Insets(0, 2, 0, 2));
        addActionListener(this);
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
            setToolTipText(resources.getString("Completion_Button_Tooltip"));
        } else if (d instanceof DateData) {
            setEnabled(true);
            setSelected(true);
            setToolTipText(((DateData) d).formatDate());
        } else {
            setSelected(false);
            setEnabled(false);
        }
        if (Settings.isReadOnly())
            setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
        parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if (isSelected()) {
            parent.getData().userPutValue(dataName, new DateData());
            if (selectNextTask() == false) {
                parent.pauseTimer(); // stop the timer if it is running.
                update();
            }
        } else {
            parent.getData().userPutValue(dataName, null);
            setToolTipText(resources.getString("Completion_Button_Tooltip"));
        }
        parent.setCursor(null);
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
