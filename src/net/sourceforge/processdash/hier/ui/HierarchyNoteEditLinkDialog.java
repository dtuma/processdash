// Copyright (C) 2020 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.JHintTextField;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;

public class HierarchyNoteEditLinkDialog {

    public static final int SAVE = 0, DELETE = 1, CANCEL = 2;

    private String fullPath, linkPath, url, linkText;

    private boolean add;

    private JPanel editPanel;

    private JComboBox pathSelector;

    private JTextField linkTextField, urlField;

    private Resources resources = Resources
            .getDashBundle("ProcessDashboard.ScriptEditor");


    public HierarchyNoteEditLinkDialog(String fullPath, String linkPath,
            String url, String linkText) {
        this.fullPath = fullPath;
        this.linkPath = linkPath;
        this.url = url;
        this.linkText = linkText;
        this.add = (url == null);
    }


    public int display(Component parentComponent) {
        if (editPanel == null)
            editPanel = buildUI();

        String title = resources.getString(add ? "Add.Title" : "Edit.Title");
        String[] options = add ? getOptions("Save", "Cancel")
                : getOptions("Save", "Delete", "Cancel");

        while (true) {
            // display a dialog to the user with our UI and options
            int userChoice = JOptionPane.showOptionDialog(parentComponent,
                editPanel, title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

            // check for "delete" and "cancel" scenarios and return accordingly
            if (add == false && userChoice == 1)
                return DELETE;
            else if (userChoice != 0 || isModified() == false)
                return CANCEL;

            // validate input and return "save" if the values are OK
            if (getNewURL().length() == 0)
                showError(parentComponent, "URL.Missing");
            else if (!getNewURL().startsWith("http"))
                showError(parentComponent, "URL.Not_HTTP");
            else
                return SAVE;
        }
    }

    private String[] getOptions(String... keys) {
        String[] result = new String[keys.length];
        for (int i = keys.length; i-- > 0;)
            result[i] = resources.getString(keys[i]);
        return result;
    }

    private void showError(Component parentComponent, String resKey) {
        JOptionPane.showMessageDialog(parentComponent,
            resources.getString(resKey), resources.getString("Error_Title"),
            JOptionPane.ERROR_MESSAGE);
    }


    public boolean isModified() {
        return !(getNewPath().equals(linkPath) //
                && getNewLinkText().equals(linkText) //
                && getNewURL().equals(url));
    }

    public boolean isLinkPathChanged() {
        return !getNewPath().equals(linkPath);
    }

    public String getNewPath() {
        PathComponent newPath = (PathComponent) pathSelector.getSelectedItem();
        return newPath == null ? linkPath : newPath.fullPath;
    }

    public String getNewLinkText() {
        return linkTextField.getText().trim();
    }

    public String getNewURL() {
        return urlField.getText().trim();
    }



    private JPanel buildUI() {
        pathSelector = makePathSelector();
        pathSelector.setToolTipText(resources.getString("Path.Tooltip"));
        linkTextField = new JTextField(linkText);
        linkTextField.setToolTipText(resources.getString("Text.Tooltip"));
        urlField = new JHintTextField(resources.getString("URL.Hint"));
        urlField.setToolTipText(resources.getString("URL.Tooltip"));
        urlField.setText(url);

        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints g = new GridBagConstraints();
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.setLayout(layout);

        g.gridx = g.gridy = 0;
        g.insets = new Insets(1, 1, 1, 5);
        g.anchor = GridBagConstraints.EAST;
        add(panel, layout, g, new JLabel(resources
                .getString(add ? "Path.Add_Label" : "Path.Edit_Label")));
        g.gridy++;
        add(panel, layout, g, new JLabel(resources.getString("Text.Label")));
        g.gridy++;
        add(panel, layout, g, new JLabel(resources.getString("URL.Label")));

        g.gridx = 1; g.gridy = 0;
        g.insets = new Insets(1, 1, 1, 1);
        g.fill = GridBagConstraints.BOTH;
        add(panel, layout, g, pathSelector);
        g.gridy++;
        add(panel, layout, g, linkTextField);
        g.gridy++;
        add(panel, layout, g, urlField);
        g.gridy++;
        add(panel, layout, g, new JOptionPaneTweaker.GrabFocus(linkTextField));

        return panel;
    }

    private void add(JPanel panel, GridBagLayout layout, GridBagConstraints g,
            Component c) {
        layout.setConstraints(c, g);
        panel.add(c);
    }

    private JComboBox makePathSelector() {
        JComboBox result = new JComboBox();
        String path = fullPath;
        while (path.length() > 1) {
            result.insertItemAt(new PathComponent(path), 0);
            if (path.equals(linkPath))
                result.setSelectedIndex(0);
            path = DataRepository.chopPath(path);
        }
        Dimension d = result.getPreferredSize();
        d.width = Math.max(Math.min(d.width, 500), 250);
        result.setPreferredSize(d);
        return result;
    }

    private static class PathComponent {

        String fullPath, name;

        public PathComponent(String fullPath) {
            this.fullPath = fullPath;
            this.name = fullPath.substring(fullPath.lastIndexOf('/') + 1);
        }

        public String toString() {
            return name;
        }
    }

}
