// Copyright (C) 2018 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import net.sourceforge.processdash.ui.lib.JOptionPaneClickHandler;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

import teamdash.wbs.WBSJTable.EnablementCalculation;
import teamdash.wbs.columns.LinkSource;

public class WBSOpenLinkAction extends AbstractAction
        implements EnablementCalculation {

    private WBSTabPanel tabPanel;

    private static final String ACTION_NAME = WBSEditor.resources
            .getString("Edit.Open_Link");


    public WBSOpenLinkAction(WBSTabPanel tabPanel) {
        super(ACTION_NAME, IconFactory.getExternalLinkIcon());
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_K, //
            MacGUIUtils.getCtrlModifier()));
        setEnabled(false);

        this.tabPanel = tabPanel;
        tabPanel.wbsTable.addEnablementCalculation(this);
    }


    @Override
    public void recalculateEnablement(int[] selectedRows) {
        int numLinks = getLinks(selectedRows).size();
        setEnabled(numLinks > 0);
        if (numLinks < 2)
            putValue(Action.NAME, ACTION_NAME);
        else
            putValue(Action.NAME, ACTION_NAME + "...");
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        // get the links for the selected node
        int[] selectedRows = tabPanel.wbsTable.getSelectedRows();
        Map<String, URI> links = getLinks(selectedRows);
        if (links.size() == 1) {
            // if the selected node has exactly one link, open it.
            openLink(links.values().iterator().next());

        } else if (links.size() > 1) {
            // if the seleted node has multiple links, ask the user to pick one
            Vector<String> items = new Vector<String>(links.keySet());
            JList list = new JList(items);
            list.setFixedCellHeight(
                new JLabel("X").getPreferredSize().height * 3 / 2);
            list.setVisibleRowCount(Math.min(items.size(), 10));
            list.setSelectedIndex(0);
            new JOptionPaneClickHandler(1).install(list);
            int userChoice = JOptionPane.showConfirmDialog(tabPanel, //
                new Object[] { new JScrollPane(list),
                        new JOptionPaneTweaker.GrabFocus(list) },
                ACTION_NAME, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

            // if the user selected a link, open it in a web browser
            if (userChoice == JOptionPane.OK_OPTION) {
                Object selectedItem = list.getSelectedValue();
                URI selectedLink = links.get(selectedItem);
                openLink(selectedLink);
            }
        }
    }


    private Map<String, URI> getLinks(int[] selectedRows) {
        if (selectedRows == null || selectedRows.length != 1)
            return Collections.EMPTY_MAP;

        int row = selectedRows[0];
        WBSNode node = (WBSNode) tabPanel.wbsTable.getValueAt(row, 0);
        if (node == null)
            return Collections.EMPTY_MAP;

        Map<String, URI> result = new TreeMap<String, URI>();
        DataTableModel dtm = (DataTableModel) tabPanel.dataTable.getModel();
        for (int i = 0; i < dtm.getColumnCount(); i++) {
            DataColumn column = dtm.getColumn(i);
            if (column instanceof LinkSource) {
                for (String link : ((LinkSource) column).getLinks(node)) {
                    try {
                        String text;
                        int pos = link.indexOf(' ');
                        if (pos == -1) {
                            text = link;
                        } else {
                            text = link.substring(pos + 1).trim();
                            link = link.substring(0, pos);
                        }
                        result.put(text, new URI(link));
                    } catch (Exception e) {
                    }
                }
            }
        }
        return result;
    }

    private void openLink(URI link) {
        try {
            // open the URL in a web browser
            if (link != null)
                Desktop.getDesktop().browse(link);
        } catch (Exception ex) {
            ex.printStackTrace();
            Toolkit.getDefaultToolkit().beep();
        }
    }

}
