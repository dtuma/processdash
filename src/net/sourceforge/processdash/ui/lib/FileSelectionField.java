// Copyright (C) 2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;

import net.sourceforge.processdash.util.StringUtils;

public class FileSelectionField extends JPanel {

    private Preferences prefs;

    private String prefsKey;

    private int fileSelectionMode;

    private List<String> prefDirectories;

    private JTextField textField;

    private DropDownButton browseButton;

    private ActionHandler handler;

    private JFileChooser fileChooser;

    public FileSelectionField(Preferences prefs, String prefsKey,
            int fileSelectionMode, String buttonText) {
        this.prefs = prefs;
        this.prefsKey = prefsKey;
        this.fileSelectionMode = fileSelectionMode;
        this.prefDirectories = loadRecentPaths();

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        textField = new JTextField();
        if (!prefDirectories.isEmpty())
            textField.setText(prefDirectories.get(0));
        add(textField);
        add(Box.createHorizontalStrut(5));

        browseButton = new DropDownButton(buttonText);
        browseButton.setMainButtonBehavior(DropDownButton.NO_ACTION);
        handler = new ActionHandler();
        browseButton.getButton().addActionListener(handler);
        rebuildMenus();
        add(browseButton);

        Dimension d = getPreferredSize();
        d.width = 400;
        setSize(d);
        setPreferredSize(d);
    }

    public JTextField getTextField() {
        return textField;
    }

    public String getSelectedPath() {
        String path = textField.getText().trim();
        return (path.length() == 0 ? null : path);
    }

    public File getSelectedFile() {
        String path = getSelectedPath();
        return (path == null ? null : new File(path));
    }

    public void savePreferences() {
        savePreferences(true);
    }

    private void savePreferences(boolean updateFromSelection) {
        if (updateFromSelection) {
            String path = getSelectedPath();
            if (path != null) {
                prefDirectories.remove(path);
                prefDirectories.add(0, path);
                rebuildMenus();
            }
        }
        prefs.put(prefsKey, StringUtils.join(prefDirectories,
            File.pathSeparator));
    }


    private List<String> loadRecentPaths() {
        List<String> result = new ArrayList<String>();
        String pref = prefs.get(prefsKey, null);
        if (pref != null) {
            String[] dirs = pref.split(File.pathSeparator);
            result.addAll(Arrays.asList(dirs));
        }
        return result;
    }

    private void rebuildMenus() {
        JMenu browseMenu = browseButton.getMenu();
        browseMenu.removeAll();
        for (String dir : prefDirectories) {
            JMenuItem dirItem = new DeletableMenuItem(dir, browseMenu);
            dirItem.addActionListener(handler);
            browseMenu.add(dirItem);
        }
    }

    private void browse() {
        if (fileChooser == null)
            fileChooser = createFileChooser();

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fileChooser.getSelectedFile();
            if (f != null) {
                String path = f.getAbsolutePath();
                textField.setText(path);
            }
        }
    }

    protected JFileChooser createFileChooser() {
        JFileChooser result = new JFileChooser();
        result.setFileSelectionMode(fileSelectionMode);
        return result;
    }


    private class ActionHandler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) e.getSource();
                textField.setText(item.getText());
            } else if (e.getSource() == browseButton.getButton()) {
                browse();
            }
        }

    }


    private class DeletableMenuItem extends JMenuItem implements MenuKeyListener {

        private JMenu menu;

        public DeletableMenuItem(String dirPath, JMenu menu) {
            super(dirPath);
            this.menu = menu;
        }

        @Override
        public void addNotify() {
            super.addNotify();
            addMenuKeyListener(this);
        }

        @Override
        public void removeNotify() {
            removeMenuKeyListener(this);
            super.removeNotify();
        }

        public void menuKeyPressed(MenuKeyEvent e) {}

        public void menuKeyReleased(MenuKeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_DELETE && isArmed()) {
                prefDirectories.remove(getText());
                savePreferences(false);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setVisible(false);
                        menu.remove(DeletableMenuItem.this);
                    }
                });
            }
        }

        public void menuKeyTyped(MenuKeyEvent e) {}
    }

}
