// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.hist.ui;

import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Date;

import javax.swing.JDialog;

import com.toedter.calendar.JDateChooser;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.NullSafeObjectUtils;

import teamdash.hist.BlameData;
import teamdash.hist.BlameDataFactory;
import teamdash.hist.ProjectHistory;
import teamdash.hist.ProjectHistoryException;
import teamdash.hist.ProjectHistoryFactory;
import teamdash.wbs.WBSEditor;

public class BlameHistoryDialog extends JDialog implements
        PropertyChangeListener {

    private WBSEditor wbsEditor;

    private String dataLocation;

    private JDateChooser dateChooser;

    private ProjectHistory projectHistory;

    private Date historyDate;

    protected static final Resources resources = Resources
            .getDashBundle("WBSEditor.Blame");


    BlameHistoryDialog(WBSEditor wbsEditor, String dataLocation) {
        super((Frame) null, resources.getString("Title"), false);
        this.wbsEditor = wbsEditor;
        this.dataLocation = dataLocation;

        dateChooser = new JDateChooser();
        dateChooser.getDateEditor().addPropertyChangeListener("date", this);

        getContentPane().add(dateChooser);
        pack();
        setAlwaysOnTop(true);
        setVisible(true);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        setHistoryDate(dateChooser.getDate());
    }

    private void setHistoryDate(Date historyDate) {
        if (NullSafeObjectUtils.EQ(historyDate, this.historyDate))
            return;

        try {
            if (projectHistory == null)
                projectHistory = ProjectHistoryFactory
                        .getProjectHistory(dataLocation);

            BlameData blameData = BlameDataFactory.getBlameData(projectHistory,
                historyDate);
            wbsEditor.setBlameData(blameData);

            this.historyDate = historyDate;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ProjectHistoryException e) {
            e.printStackTrace();
        }
    }

}
