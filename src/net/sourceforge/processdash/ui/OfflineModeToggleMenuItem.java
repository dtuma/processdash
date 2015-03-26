// Copyright (C) 2012 Tuma Solutions, LLC
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

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;

import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.bridge.OfflineLockStatus;
import net.sourceforge.processdash.tool.bridge.OfflineLockStatusListener;
import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.ui.lib.ExceptionDialog;
import net.sourceforge.processdash.util.lock.LockUncertainException;

public class OfflineModeToggleMenuItem extends JCheckBoxMenuItem implements
        ActionListener, OfflineLockStatusListener {

    BridgedWorkingDirectory workingDirectory;

    private static Resources resources = ConfigureButton.resources;

    public OfflineModeToggleMenuItem(WorkingDirectory workingDir) {
        super(resources.getString("Work_Offline.Title"));
        setToolTipText(resources.getString("Work_Offline.Tooltip"));

        if (workingDir instanceof BridgedWorkingDirectory
                && Settings.isReadWrite()) {
            workingDirectory = (BridgedWorkingDirectory) workingDir;
            workingDirectory.addOfflineLockStatusListener(this);
            setOfflineLockStatus(workingDirectory.getOfflineLockStatus());

            addActionListener(this);

        } else {
            setOfflineLockStatus(OfflineLockStatus.Unsupported);
        }
    }

    public void setOfflineLockStatus(OfflineLockStatus status) {
        boolean shouldShow = (status != OfflineLockStatus.NotLocked
                && status != OfflineLockStatus.Unsupported
                && Settings.isReadWrite());
        setVisible(shouldShow);
        setEnabled(shouldShow);

        boolean isOffline = (status == OfflineLockStatus.Enabled);
        setSelected(isOffline);
    }

    public void actionPerformed(ActionEvent e) {
        if (workingDirectory == null)
            return;

        boolean enableOffline = isSelected();
        if (enableOffline) {
            // When enabling offline mode, possibly show a confirmation
            // message.  If the user changes their mind, abort.
            boolean userConfirmed = showConfirmationMessage();
            if (userConfirmed == false) {
                setSelected(false);
                return;
            }
        }

        try {
            workingDirectory.setOfflineLockEnabled(enableOffline);
            showSuccessMessage(enableOffline);
        } catch (LockUncertainException lue) {
            showServerConnectivityError(enableOffline);
        } catch (Exception ex) {
            showUnexpectedError(enableOffline, ex);
        }

        setOfflineLockStatus(workingDirectory.getOfflineLockStatus());
    }

    private boolean showConfirmationMessage() {
        if (PREFS.getBoolean(SKIP_CONFIMATION, false))
            return true;

        String title = resources.getString("Work_Offline.Title");
        JCheckBox skipNextTime = new JCheckBox(resources
                .getString("Work_Offline.Confirmation.Disable"));
        skipNextTime.setFont(skipNextTime.getFont().deriveFont(
            skipNextTime.getFont().getSize() * 0.8f));
        skipNextTime.setMargin(new Insets(10, 20, 10, 40));
        Object[] message = new Object[] {
                resources.getStrings("Work_Offline.Confirmation.Message"),
                skipNextTime
        };
        int userChoice = JOptionPane.showConfirmDialog(null, message, title,
            JOptionPane.OK_CANCEL_OPTION);
        if (skipNextTime.isSelected())
            PREFS.putBoolean(SKIP_CONFIMATION, true);
        return (userChoice == JOptionPane.OK_OPTION);
    }

    private void showSuccessMessage(boolean enableOffline) {
        String resKey = enableOffline ? "Offline" : "Online";
        String title = resources.getString("Work_Offline.Success." + resKey
                + "_Title");
        String[] message = resources.getStrings("Work_Offline.Success."
                + resKey + "_Message");
        JOptionPane.showMessageDialog(null, message, title,
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showServerConnectivityError(boolean enableOffline) {
        String resKey = enableOffline ? "Offline" : "Online";
        String title = resources.getString("Work_Offline.Error." + resKey
                + "_Title");
        Object[] message = new Object[] {
                resources.getStrings("Work_Offline.Error.Connectivity_Message"),
                " ",
                resources.getString("Work_Offline.Error." + resKey + "_Footer")
        };
        JOptionPane.showMessageDialog(null, message, title,
            JOptionPane.ERROR_MESSAGE);
    }

    private void showUnexpectedError(boolean enableOffline, Exception ex) {
        String resKey = enableOffline ? "Offline" : "Online";
        String title = resources.getString("Work_Offline.Error." + resKey
                + "_Title");
        ExceptionDialog.show(null, title, resources
                .getStrings("Work_Offline.Error.General_Message"), ex);
    }

    private static final Preferences PREFS = Preferences
            .userNodeForPackage(OfflineModeToggleMenuItem.class);
    private static final String SKIP_CONFIMATION = "workOffline.skipConfirm";

}
