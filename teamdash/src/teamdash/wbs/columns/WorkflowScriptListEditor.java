// Copyright (C) 2020-2023 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.JHintTextField;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.PaintUtils;
import net.sourceforge.processdash.ui.lib.WrappingText;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.IconFactory;

public class WorkflowScriptListEditor {

    private DefaultListModel model;

    private ScriptLinkJList list;

    private MouseClickHandler clickHandler;

    private WrappingText header;

    private JPanel dialogContents;

    private LinkEditForm linkEditForm;

    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.Columns.Workflow.Script_URLs");


    public WorkflowScriptListEditor() {
        this.model = new DefaultListModel();
        this.list = new ScriptLinkJList(model);
        this.clickHandler = new MouseClickHandler(list);
        JScrollPane scrollPane = new JScrollPane(list);

        this.header = new WrappingText("");

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setMargin(new Insets(0, 0, 0, 0));
        toolbar.add(toolbarButton(new AddLinkAction()));
        toolbar.add(toolbarButton(new EditLinkAction()));
        toolbar.add(toolbarButton(new DeleteLinkAction()));
        toolbar.addSeparator();
        toolbar.add(toolbarButton(new MoveLinkUpAction()));
        toolbar.add(toolbarButton(new MoveLinkDownAction()));
        toolbar.addSeparator();
        toolbar.add(toolbarButton(new TestLinkAction()));
        toolbar.add(Box.createHorizontalGlue());
        int prefHeight = toolbar.getPreferredSize().height;
        toolbar.setPreferredSize(new Dimension(300, prefHeight));
        toolbar.setMaximumSize(new Dimension(4000, prefHeight));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(header);
        panel.add(Box.createVerticalStrut(10));
        panel.add(toolbar);
        panel.add(scrollPane);
        panel.add(new JOptionPaneTweaker.MakeResizable());
        this.dialogContents = panel;
    }

    private JButton toolbarButton(Action a) {
        JButton button = new JButton(a);
        button.setFocusPainted(false);
        button.putClientProperty("hideActionText", Boolean.TRUE);
        button.setToolTipText((String) a.getValue(Action.NAME));
        button.setText(null);
        IconFactory.setDisabledIcon(button);
        return button;
    }


    public void setValue(String value) {
        model.clear();
        while (StringUtils.hasValue(value)) {
            int pos = value.lastIndexOf("http");
            if (pos == -1)
                break;

            String oneSpec = value.substring(pos).trim();
            model.add(0, new ScriptLink(oneSpec));
            value = value.substring(0, pos);
        }
    }


    public String getValue() {
        if (model.isEmpty())
            return null;

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < model.size(); i++)
            result.append(" ").append(((ScriptLink) model.get(i)).saveStr());
        return result.substring(1);
    }


    public boolean showDialog(Component parent, String workflowName,
            String taskName) {
        // calculate the title and header for the dialog
        String title = resources.format("Edit_Title_FMT", workflowName);
        String headerKey = (taskName == null ? "Workflow" : "Task");
        String headerText = resources.format(headerKey + "_Header_FMT",
            workflowName, taskName);
        header.setText(headerText);

        // display the dialog to the user, allow editing, wait for OK/Cancel
        int userChoice = JOptionPane.showConfirmDialog(parent, dialogContents,
            title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        // if the user resized the window, save the preferred size
        dialogContents.setPreferredSize(dialogContents.getSize());

        // return true if the user pressed OK
        return userChoice == JOptionPane.OK_OPTION;
    }



    private class ScriptLink {

        private String url, display;

        ScriptLink(String s) {
            Matcher m = WHITESPACE_PAT.matcher(s);
            if (m.find()) {
                this.url = s.substring(0, m.start());
                this.display = s.substring(m.end());
            } else {
                this.url = s;
                this.display = null;
            }
        }

        String saveStr() {
            if (StringUtils.hasValue(display))
                return url + " " + display;
            else
                return url;
        }

        public String toString() {
            if (StringUtils.hasValue(display))
                return display + "  \u2192   " + url;
            else
                return url;
        }

    }

    private static final Pattern WHITESPACE_PAT = Pattern.compile("\\s+");


    private class ScriptLinkJList extends JList {

        private String hint;

        private Color hintColor;

        private Font italic;

        public ScriptLinkJList(ListModel dataModel) {
            super(dataModel);
            this.hint = resources.getString("Empty_Hint");
            this.hintColor = PaintUtils.mixColors(getForeground(),
                getBackground(), 0.5);
            this.italic = getFont().deriveFont(Font.ITALIC);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            if (getModel().getSize() == 0) {
                ((Graphics2D) g).setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                g.setColor(hintColor);
                g.setFont(italic);

                Insets ins = getInsets();
                FontMetrics fm = g.getFontMetrics();
                g.drawString(hint, ins.left + 2, ins.top + fm.getAscent() + 2);
            }
        }

    }


    private class MouseClickHandler extends MouseAdapter {

        private Action addAction, editAction;

        MouseClickHandler(JList list) {
            list.addMouseListener(this);
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int row = getClickedIndex(e.getPoint());
                if (row == -1) {
                    addAction.actionPerformed(null);
                } else {
                    list.setSelectedIndex(row);
                    editAction.actionPerformed(null);
                }
            }
        }

        private int getClickedIndex(Point p) {
            int row = list.locationToIndex(p);
            if (row < 0 || row >= model.size())
                return -1;

            Rectangle b = list.getCellBounds(row, row);
            return (b != null && b.contains(p) ? row : -1);
        }

    }


    private abstract class AbstractLinkAction extends AbstractAction
            implements ListSelectionListener {

        AbstractLinkAction() {
            putValue(SMALL_ICON, getIcon());
            putValue(NAME, resources.getString(getTooltipKey()));
            setEnabled(false);
            list.addListSelectionListener(this);
        }

        abstract Icon getIcon();

        abstract String getTooltipKey();

        int getTargetLinkPos() {
            return list.getSelectedIndex();
        }

        boolean isEnabledForPos(int pos) {
            return (pos >= 0 && pos < model.size());
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(isEnabledForPos(getTargetLinkPos()));
        }

    }


    private class AddLinkAction extends AbstractAction {

        AddLinkAction() {
            putValue(SMALL_ICON, IconFactory.getAddTabIcon());
            putValue(NAME, resources.getString("Add"));
            clickHandler.addAction = this;
        }

        public void actionPerformed(ActionEvent e) {
            if (linkEditForm == null)
                linkEditForm = new LinkEditForm();

            ScriptLink newLink = new ScriptLink("");
            if (linkEditForm.showForm(newLink)) {
                model.addElement(newLink);
                list.setSelectedIndex(model.size() - 1);
            }
        }

    }


    private class EditLinkAction extends AbstractLinkAction {

        EditLinkAction() {
            clickHandler.editAction = this;
        }

        Icon getIcon() {
            return IconFactory.getRenameIcon();
        }

        String getTooltipKey() {
            return "Edit";
        }

        public void actionPerformed(ActionEvent e) {
            if (linkEditForm == null)
                linkEditForm = new LinkEditForm();

            int pos = getTargetLinkPos();
            ScriptLink link = (ScriptLink) model.elementAt(pos);
            if (linkEditForm.showForm(link)) {
                model.setElementAt(link, pos);
                list.setSelectedIndex(pos);
            }
        }

    }


    private class DeleteLinkAction extends AbstractLinkAction {

        Icon getIcon() {
            return IconFactory.getDeleteIcon();
        }

        String getTooltipKey() {
            return "Delete";
        }

        public void actionPerformed(ActionEvent e) {
            model.remove(getTargetLinkPos());
        }

    }


    private class MoveLinkUpAction extends AbstractLinkAction {

        Icon getIcon() {
            return IconFactory.getMoveUpIcon();
        }

        String getTooltipKey() {
            return "Move_Up";
        }

        int getOffset() {
            return 0;
        }

        int getTargetLinkPos() {
            return super.getTargetLinkPos() + getOffset();
        }

        boolean isEnabledForPos(int pos) {
            return pos > 0 && super.isEnabledForPos(pos);
        }

        public void actionPerformed(ActionEvent e) {
            int pos = getTargetLinkPos();
            if (isEnabledForPos(pos)) {
                ScriptLink link = (ScriptLink) model.remove(pos);
                model.add(pos - 1, link);
                list.setSelectedIndex(pos - 1 + getOffset());
            }
        }

    }


    private class MoveLinkDownAction extends MoveLinkUpAction {

        Icon getIcon() {
            return IconFactory.getMoveDownIcon();
        }

        String getTooltipKey() {
            return "Move_Down";
        }

        int getOffset() {
            return 1;
        }

    }


    private class TestLinkAction extends AbstractLinkAction {

        Icon getIcon() {
            return IconFactory.getExternalLinkIcon();
        }

        String getTooltipKey() {
            return "Test";
        }

        public void actionPerformed(ActionEvent e) {
            try {
                // open the URL in a web browser
                ScriptLink link = (ScriptLink) list.getSelectedValue();
                URI uri = new URI(link.url);
                Desktop.getDesktop().browse(uri);

            } catch (Exception ex) {
                ex.printStackTrace();
                Toolkit.getDefaultToolkit().beep();
            }
        }

    }


    private class LinkEditForm {

        private JTextField displayField, urlField;

        private JPanel editPanel;

        LinkEditForm() {
            displayField = new JTextField();
            displayField.setToolTipText(resources.getString("Text.Tooltip"));
            urlField = new JHintTextField(resources.getString("URL.Hint"));
            urlField.setToolTipText(resources.getString("URL.Tooltip"));

            editPanel = new JPanel();
            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints g = new GridBagConstraints();
            editPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            editPanel.setLayout(layout);

            g.gridx = g.gridy = 0;
            g.insets = new Insets(1, 1, 1, 5);
            g.anchor = GridBagConstraints.EAST;
            add(layout, g, new JLabel(resources.getString("Text.Label")));
            g.gridy++;
            add(layout, g, new JLabel(resources.getString("URL.Label")));

            g.gridx = 1;
            g.gridy = 0;
            g.insets = new Insets(1, 1, 1, 1);
            g.fill = GridBagConstraints.BOTH;
            g.weightx = 1;
            add(layout, g, displayField);
            g.gridy++;
            add(layout, g, urlField);
            g.gridy++;
            add(layout, g, new JOptionPaneTweaker.GrabFocus(displayField));
            g.gridy++;
            add(layout, g, new JOptionPaneTweaker.MakeResizable());
        }

        private void add(GridBagLayout layout, GridBagConstraints g,
                Component c) {
            layout.setConstraints(c, g);
            editPanel.add(c);
        }

        boolean showForm(ScriptLink link) {
            displayField.setText(link.display);
            urlField.setText(link.url);

            boolean add = !StringUtils.hasValue(link.url);
            String title = resources.getString(add ? "Add" : "Edit");

            while (true) {
                // display a dialog to the user with our UI and options
                int userChoice = JOptionPane.showConfirmDialog(list, editPanel,
                    title, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
                editPanel.setPreferredSize(editPanel.getSize());

                // check for "cancel" and return accordingly
                if (userChoice != JOptionPane.OK_OPTION)
                    return false;

                // validate input and return "save" if the values are OK
                String newURL = urlField.getText().trim();
                String newDisplay = displayField.getText().trim();
                if (newURL.length() == 0)
                    showError(list, "URL.Missing");
                else if (!newURL.startsWith("http"))
                    showError(list, "URL.Not_HTTP");
                else {
                    link.url = newURL;
                    link.display = newDisplay;
                    return true;
                }
            }
        }

        private void showError(Component parentComponent, String resKey) {
            JOptionPane.showMessageDialog(parentComponent,
                resources.getString(resKey), resources.getString("Error_Title"),
                JOptionPane.ERROR_MESSAGE);
        }

    }

}
